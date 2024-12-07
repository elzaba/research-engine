package com.irs.researchengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irs.researchengine.data.DuplicateRecord;
import com.irs.researchengine.data.Paper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.irs.researchengine.nlp.CustomAnalyzer;
import com.irs.researchengine.utils.CorpusUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IndexService {

    @Value("${index.path}")
    private String indexPath;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private CorpusUtils corpusUtils;
    
    private static final int SHINGLE_SIZE = 3; // Shingle size for character n-grams
    private static final double SIMILARITY_THRESHOLD = 0.85; // Near-duplicate similarity threshold
    
 // Data structure to track near-duplicates for manual review
    private final List<DuplicateRecord> flaggedDuplicates = new ArrayList<>();
    
    public void indexFromDataset(String datasetPath) throws IOException {
        List<Paper> papers = Arrays.asList(objectMapper.readValue(new File(datasetPath), Paper[].class));
        try {
            indexPapers(papers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void indexPapers(List<Paper> papers) throws Exception {
        try (Directory dir = FSDirectory.open(Paths.get(indexPath))) {
            Analyzer analyzer = new CustomAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (Paper paper : papers) {
                	if (!isNearDuplicate(writer, paper)) {
                        indexPaper(writer, paper);
                    }
                }
            }
        }
    }

    private void indexPaper(IndexWriter writer, Paper paper) throws Exception {
    	// Check if the paper already exists in the index by ID
        if (!documentExists(writer, paper.getId())) {
            // If not, index the paper
        	Document doc = new Document();
        	doc.add(new StringField("id", paper.getId(), Field.Store.YES));
        	doc.add(new TextField("title", paper.getTitle(), Field.Store.YES));
        	doc.add(new TextField("summary", paper.getSummary(), Field.Store.YES));
        	doc.add(new StringField("pdfLink", paper.getPdfLink(), Field.Store.YES));
        	doc.add(new TextField("comment", paper.getComment(), Field.Store.YES));
        	doc.add(new StringField("updated", paper.getUpdated(), Field.Store.YES));
        	doc.add(new StringField("published", paper.getPublished(), Field.Store.YES));
        	doc.add(new StringField("primaryCategory", paper.getPrimaryCategory(), Field.Store.YES));
        	doc.add(new StringField("primaryCategoryCode", paper.getCategoryCode(), Field.Store.YES));
        	if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
        		String allAuthors = String.join(", ", paper.getAuthors());
        		doc.add(new TextField("authors", allAuthors, Field.Store.YES));
        	}
        	
        	// Extract and add n-grams from title
            List<String> nGrams = extractSignificantNGrams(paper.getTitle());
            for (String nGram : nGrams) {
                doc.add(new TextField("domainTerms", nGram, Field.Store.YES));
            }

        	writer.addDocument(doc);
        }
    }
    // Method to check if a document with the given ID exists
    private boolean documentExists(IndexWriter writer, String paperId) throws IOException {
        try (IndexReader reader = DirectoryReader.open(writer)) {
            // Search for the paper by ID field
            Term term = new Term("id", paperId);
            TermQuery query = new TermQuery(term);
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(query, 1);
            return topDocs.totalHits.value > 0;  // If total hits is greater than 0, it exists
        }
    }
    
    private boolean isNearDuplicate(IndexWriter writer, Paper newPaper) throws IOException {
        try (IndexReader reader = DirectoryReader.open(writer)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Query for all existing papers
            MatchAllDocsQuery query = new MatchAllDocsQuery();
            TopDocs topDocs = searcher.search(query, Integer.MAX_VALUE);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document existingDoc = searcher.doc(scoreDoc.doc);
                String existingSummary = existingDoc.get("summary");

                // Compute similarity
                double similarity = computeJaccardSimilarity(newPaper.getSummary(), existingSummary);
                if (similarity >= SIMILARITY_THRESHOLD) {
                    // Flag as a near-duplicate for manual review
                    flaggedDuplicates.add(new DuplicateRecord(newPaper, existingDoc));
                    return true; // Skip indexing this paper
                }
            }
        }
        return false; // No near-duplicates found
    }
    
    private double computeJaccardSimilarity(String text1, String text2) {
        Set<String> shingles1 = generateShingles(text1, SHINGLE_SIZE);
        Set<String> shingles2 = generateShingles(text2, SHINGLE_SIZE);

        Set<String> intersection = new HashSet<>(shingles1);
        intersection.retainAll(shingles2);

        Set<String> union = new HashSet<>(shingles1);
        union.addAll(shingles2);

        return (double) intersection.size() / union.size();
    }
    
    private Set<String> generateShingles(String text, int shingleSize) {
        text = text.replaceAll("\\s+", "").toLowerCase(); // Normalize text
        Set<String> shingles = new HashSet<>();
        for (int i = 0; i <= text.length() - shingleSize; i++) {
            shingles.add(text.substring(i, i + shingleSize));
        }
        return shingles;
    }

    public List<DuplicateRecord> getFlaggedDuplicates() {
        return flaggedDuplicates;
    }
    
    /**
     * Extracts domain-specific significant n-grams.
     */
    private List<String> extractSignificantNGrams(String text) {
    	// Use CustomAnalyzer for preprocessing
        List<String> tokens = extractTokensWithAnalyzer(text);
        List<String> nGrams = generateNGrams(tokens, 2, 3);

        // Filter by MI score and domain relevance
        return nGrams.stream()
                .filter(nGram -> corpusUtils.getJointProbability(nGram) > 0.5)  // High MI
                .filter(nGram -> corpusUtils.getIndependentProbability(nGram) < 0.01)  // Low general corpus freq
                .collect(Collectors.toList());
    }

    private List<String> extractTokensWithAnalyzer(String text) {
        List<String> tokens = new ArrayList<>();
        try (Analyzer analyzer = new CustomAnalyzer()) {
            TokenStream tokenStream = analyzer.tokenStream("title", text);
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                CharTermAttribute attr = tokenStream.getAttribute(CharTermAttribute.class);
                tokens.add(attr.toString());
            }
            tokenStream.end();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tokens;
    }


    /**
     * Generates n-grams from tokens.
     */
    private List<String> generateNGrams(List<String> tokens, int min, int max) {
        List<String> nGrams = new ArrayList<>();
        for (int n = min; n <= max; n++) {
            for (int i = 0; i <= tokens.size() - n; i++) {
                nGrams.add(String.join(" ", tokens.subList(i, i + n)));
            }
        }
        return nGrams;
    }
}