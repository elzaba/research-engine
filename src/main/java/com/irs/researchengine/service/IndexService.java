package com.irs.researchengine.service;

import com.irs.researchengine.data.Paper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.irs.researchengine.nlp.CustomAnalyzer;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IndexService {

    @Value("${index.path}")
    private String INDEX_PATH;
    
    @Value("${faiss.api.url}")
    private String faissApiUrl;

    public void indexPapers(List<Paper> papers) throws Exception {
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH))) {
            Analyzer analyzer = new CustomAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
            	List<Map<String, String>> faissDocs = new ArrayList<>();
                for (Paper paper : papers) {
                    indexPaper(writer, paper);
                    Map<String, String> faissDoc = new HashMap<>();
                    faissDoc.put("id", paper.getId());
                    faissDoc.put("text", paper.getSummary());  // Use summary or other relevant fields
                    faissDocs.add(faissDoc);
                }
                // Call FAISS API to index embeddings
                sendDocumentsToFaiss(faissDocs);
            }
        }
    }

    private void indexPaper(IndexWriter writer, Paper paper) throws Exception {
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

        writer.addDocument(doc);
    }
    
    private void sendDocumentsToFaiss(List<Map<String, String>> documents) throws Exception {
        // Use RestTemplate to POST documents to the FAISS indexing API
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<List<Map<String, String>>> request = new HttpEntity<>(documents);
        restTemplate.postForEntity(faissApiUrl + "/index_documents/", request, String.class);
    }
}
