package com.irs.researchengine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.irs.researchengine.config.CategoryConfig;
import com.irs.researchengine.data.Paper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
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
    
    @Value("${dataset.path}")
    private String datasetPath;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static final Logger logger = LoggerFactory.getLogger(ArxivApiService.class);

    public void createDataset(List<String> categories) throws IOException {
        List<Paper> allPapers = new ArrayList<>();
        for (String category : categories) {
            try {
                logger.info("Fetching papers for category: {}", category);
                List<Paper> categoryPapers = fetchAllPapers(category);
                logger.info("Fetched {} papers for category: {}", categoryPapers.size(), category);
                allPapers.addAll(categoryPapers);
            } catch (Exception e) {
                logger.error("Error fetching papers for category {}: {}", category, e.getMessage());
            }
        }
        savePapersToDataset(allPapers);
        logger.info("Total number of papers in the dataset: {}", allPapers.size());
    }

    private List<Paper> fetchAllPapers(String category) throws Exception {
        List<Paper> papers = new ArrayList<>();
        int start = 0;
        boolean moreResults = true;

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
                logger.info("Parsed {} papers in current batch for category: {}", batch.size(), category);

                papers.addAll(batch);
                moreResults = batch.size() >= MAX_RESULTS_PER_REQUEST;
                start += MAX_RESULTS_PER_REQUEST;
            } catch (Exception e) {
                logger.error("Error during fetch: {}", e.getMessage());
                moreResults = false;
            }
        }
        return papers;
    }

    private List<Paper> parseArxivResponse(String xmlResponse, String category) throws Exception {
        List<Paper> papers = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlResponse.getBytes()));

        NodeList entries = doc.getElementsByTagName("entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            List<String> authors = new ArrayList<>();
            NodeList authorNodes = entry.getElementsByTagName("author");
            for (int j = 0; j < authorNodes.getLength(); j++) {
                String authorName = getTextContent((Element) authorNodes.item(j), "name");
                if (authorName != null) {
                    authors.add(authorName);
                }
            }
            Paper paper = new Paper(
                getTextContent(entry, "id"),
                getTextContent(entry, "title"),
                getTextContent(entry, "summary"),
                getLinkHref(entry, "pdf"),
                getTextContent(entry, "arxiv:comment", "No comments available"),
                getTextContent(entry, "updated"),
                getTextContent(entry, "published"),
                CategoryConfig.getFullCategoryName(category),
                category,
                authors
            );
            papers.add(paper);
        }
        return papers;
    }

    private void savePapersToDataset(List<Paper> papers) throws IOException {
        File datasetFile = new File(datasetPath);
        if (!datasetFile.getParentFile().exists()) {
            datasetFile.getParentFile().mkdirs();
        }
        objectMapper.writeValue(datasetFile, papers);
        logger.info("Dataset created at: {}", datasetPath);
    }

    private String getTextContent(Element element, String tagName, String defaultValue) {
        String content = getTextContent(element, tagName);
        return (content.isEmpty()) ? defaultValue : content;
    }

    private String getTextContent(Element entry, String tagName) {
        NodeList nodes = entry.getElementsByTagName(tagName);
        if (nodes != null && nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
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


