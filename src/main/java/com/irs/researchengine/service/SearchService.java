package com.irs.researchengine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irs.researchengine.data.CitationInfo;
import com.irs.researchengine.data.Paper;
import com.irs.researchengine.nlp.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Value("${index.path}")
    private String INDEX_PATH;
    
    @Value("${faiss.api.url}")
    private String faissApiUrl;
    
    private static final String SEMANTIC_SCHOLAR_API_URL = "https://api.semanticscholar.org/graph/v1/paper/arXiv:%s?fields=citationCount,citations.url";

    public List<Paper> searchPapers(String queryStr, int page, int pageSize, boolean proximitySearch, int proximityDistance, boolean semanticSearch) throws Exception {
        if (queryStr == null || queryStr.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }       
        List<Paper> papers = semanticSearch ? getSemanticRanking(queryStr, page, pageSize) : searchLucene(queryStr, page, pageSize, proximitySearch, proximityDistance);

        for (Paper paper : papers) {
            CitationInfo citationInfo = fetchCitationInfo(paper.getId());
            paper.setCitationInfo(citationInfo);
        }
        
        // Sort papers based on citation count
        papers.sort((p1, p2) -> Integer.compare(p2.getCitationInfo().getCitationCount(), p1.getCitationInfo().getCitationCount()));

        return papers;
    }

    private List<Paper> searchLucene(String queryStr, int page, int pageSize, boolean proximitySearch, int proximityDistance) throws Exception {
        List<Paper> papers = new ArrayList<>();
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new MultiFieldQueryParser(
                new String[]{"title", "summary", "authors"}, 
                new CustomAnalyzer()
            );
            if (proximitySearch) {
                queryStr = "\"" + queryStr + "\"~" + proximityDistance;
            }
            Query query = parser.parse(queryStr);
            TopDocs results = searcher.search(query, (page + 1) * pageSize);
            int start = Math.min(page * pageSize, results.scoreDocs.length);
            int end = Math.min(start + pageSize, results.scoreDocs.length);
            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(results.scoreDocs[i].doc);
                String authors = doc.get("authors");
                papers.add(new Paper(
                    doc.get("id"),
                    doc.get("title"),
                    doc.get("summary"),
                    doc.get("pdfLink"),
                    doc.get("comment"),
                    doc.get("updated"),
                    doc.get("published"),
                    doc.get("primaryCategory"),
                    doc.get("primaryCategoryCode"),
                    authors != null ? List.of(authors.split(", ")) : new ArrayList<>()
                ));
            }
        }
        return papers;
    }
    
    // Retrieves a paginated list of relevant papers based on the query from the FAISS semantic search service
    private List<Paper> getSemanticRanking(String query, int page, int pageSize) throws Exception {
    	
    	RestTemplate restTemplate = new RestTemplate();
        
        // Calculate the total number of results needed for the requested page
        int topK = (page + 1) * pageSize;
        
        // Build the FAISS search request URL with encoded query and required number of results (top_k)
        String url = faissApiUrl + "/search?query=" + URLEncoder.encode(query, "UTF-8") + "&top_k=" + topK;
        
        // Send a GET request to the FAISS API and receive a list of result maps (each map contains document info)
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );
        
        // Get the list of results returned by FAISS
        List<Map<String, Object>> faissResults = response.getBody();
        
        // Calculate start and end indices for the requested page of results, ensuring we don’t exceed list size
        int start = Math.min(page * pageSize, faissResults.size());
        int end = Math.min(start + pageSize, faissResults.size());
        
        // Extract the subset of results corresponding to the requested page
        List<Map<String, Object>> pageResults = faissResults.subList(start, end);
        
        // Initialize an empty list to store Paper objects for each result
        List<Paper> papers = new ArrayList<>();
        
        // Loop through each result in the current page
        for (Map<String, Object> result : pageResults) {
            
            // Retrieve the document ID from the result map
            String docId = (String) result.get("id");
            
            // Get the Paper object corresponding to this document ID
            Paper paper = getPaperById(docId);
            
            // If a valid Paper is found, add it to the list of results
            if (paper != null) {
                papers.add(paper);
            }
        }
        
        // Return the list of Paper objects for the current page
        return papers;
    }

    private Paper getPaperById(String docId) throws IOException {
    	try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
    	         DirectoryReader reader = DirectoryReader.open(dir)) {
    	        IndexSearcher luceneSearcher = new IndexSearcher(reader);
    	        Query query = new TermQuery(new Term("id", docId));
    	        TopDocs hits = luceneSearcher.search(query, 1);

    	        if (hits.totalHits.value > 0) {
    	            Document doc = luceneSearcher.doc(hits.scoreDocs[0].doc);
    	            return mapDocumentToPaper(doc);
    	        }
    	    }
    	    return null;
    }
    
    private Paper mapDocumentToPaper(Document doc) {
        List<String> authors = doc.get("authors") != null
            ? Arrays.asList(doc.get("authors").split(", "))
            : new ArrayList<>();

        return new Paper(
            doc.get("id"),
            doc.get("title"),
            doc.get("summary"),
            doc.get("pdfLink"),
            doc.get("comment"),
            doc.get("updated"),
            doc.get("published"),
            doc.get("primaryCategory"),
            doc.get("primaryCategoryCode"),
            authors
        );
    }
    
    public CitationInfo fetchCitationInfo(String arxivId) {
        String cleanId = arxivId.contains("/abs/") ? arxivId.split("/abs/")[1].split("v")[0] : arxivId;
        String url = String.format(SEMANTIC_SCHOLAR_API_URL, cleanId);

        HttpURLConnection connection = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // Create and configure the connection
            URL requestUrl = new URL(url);
            connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Check if the response is successful
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStream inputStream = connection.getInputStream()) {
                    // Parse the response as a Map
                    Map<String, Object> response = objectMapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});

                    // Extract data safely
                    int citationCount = response.get("citationCount") != null ? (int) response.get("citationCount") : 0;
                    List<Map<String, String>> citations = objectMapper.convertValue(
                            response.get("citations"),
                            new TypeReference<List<Map<String, String>>>() {}
                    );

                    List<String> citationUrls = citations.stream()
                            .map(citation -> citation.get("url"))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    return new CitationInfo(citationCount, citationUrls);
                }
            } else {
                System.err.println("Failed to fetch data. Response code: " + connection.getResponseCode());
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception or use a logger
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return new CitationInfo(0, new ArrayList<>());
    }

}
