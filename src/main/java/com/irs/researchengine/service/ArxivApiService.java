package com.irs.researchengine.service;

import com.irs.researchengine.config.CategoryConfig;
import com.irs.researchengine.data.Paper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class ArxivApiService {
    private static final String ARXIV_API_BASE_URL = "http://export.arxiv.org/api/query?search_query=";
    private static final int MAX_RESULTS_PER_REQUEST = 1000;
    
    @Autowired
    private IndexService indexService; // Inject IndexService
    
    public void fetchAllPapersFromCategories(List<String> categories) throws Exception {
        for (String category : categories) {
        	fetchAllPapers(category);
        }
    }

    public void fetchAllPapers(String category) throws Exception {
        int start = 0;
        boolean moreResults = true;
        int count = 0;

        while (moreResults) {
        	String url = ARXIV_API_BASE_URL + "cat:" + category + "&start=" + start + "&max_results=" + MAX_RESULTS_PER_REQUEST;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");

            try (InputStream inputStream = connection.getInputStream()) {
                StringBuilder response = new StringBuilder();
                try (Scanner scanner = new Scanner(inputStream)) {
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }
                }
                
                List<Paper> batch = parseArxivResponse(response.toString(), category);
                
                // Check if batch is null or empty before indexing
                if (batch == null || batch.isEmpty()) {
                    System.err.println("No papers found for category " + category);
                    moreResults = false;
                    break;
                }
                
                // Immediately index each paper in the batch
                indexService.indexPapers(batch);
                
                count += batch.size();
                
                if (batch.size() < MAX_RESULTS_PER_REQUEST) {
                    moreResults = false; // Stop if the last batch is smaller than the maximum
                    System.out.printf("\n Total number of papers fetched for category %s = %d", category, count);
                } else {
                    start += MAX_RESULTS_PER_REQUEST; // Increment start for the next batch
                }
            } catch (SAXException e) {
                System.err.println("XML Parsing error for category " + category + ": " + e.getMessage());
                moreResults = false;
            } catch (IOException e) {
                System.err.println("Network error for category " + category + ": " + e.getMessage());
                moreResults = false;
            } catch (Exception e) {
                System.err.println("Unexpected error for category " + category + ": " + e.getMessage());
                moreResults = false;
            }
        }
    }

    private List<Paper> parseArxivResponse(String xmlResponse, String category) throws Exception {
        List<Paper> papers = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(xmlResponse.getBytes()));

        NodeList entries = doc.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            
            // Collect authors
            List<String> authors = new ArrayList<>();
            NodeList authorNodes = entry.getElementsByTagName("author");
            for (int j = 0; j < authorNodes.getLength(); j++) {
                String authorName = getTextContent((Element) authorNodes.item(j), "name");
                if (authorName != null) {
                    authors.add(authorName);
                }
            }
            
            String primaryCategoryCode = getAttributeValue(entry, "arxiv:primary_category", "term");
            String primaryCategory = CategoryConfig.getFullCategoryName(primaryCategoryCode);
            Paper paper = new Paper(
                getTextContent(entry, "id"),
                getTextContent(entry, "title"),
                getTextContent(entry, "summary"),
                getLinkHref(entry, "pdf"),
                getTextContent(entry, "arxiv:comment") != null ? getTextContent(entry, "arxiv:comment") : "No comments available",
                getTextContent(entry, "updated"),
                getTextContent(entry, "published"),
                primaryCategory,
                primaryCategoryCode,
                authors
            );
            papers.add(paper);
        }
        return papers;
    }

    private String getTextContent(Element entry, String tagName) {
        NodeList nodes = entry.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return "";
    }
    
    private String getAttributeValue(Element entry, String tagName, String attributeName) {
        NodeList nodes = entry.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0) {
            Element element = (Element) nodes.item(0);
            return element.getAttribute(attributeName);
        }
        return "";
    }

    private String getLinkHref(Element entry, String relType) {
        NodeList links = entry.getElementsByTagName("link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            if (link.getAttribute("title").equals(relType)) {
                return link.getAttribute("href");
            }
        }
        return "";
    }
}

