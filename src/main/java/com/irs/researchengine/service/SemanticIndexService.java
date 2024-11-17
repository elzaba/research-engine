package com.irs.researchengine.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irs.researchengine.data.Paper;

@Service
public class SemanticIndexService {

    @Value("${faiss.api.url}")
    private String faissApiUrl;

    public void indexFromDataset(String datasetPath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        List<Paper> papers = mapper.readValue(new File(datasetPath), new TypeReference<List<Paper>>() {});
        List<Map<String, String>> faissDocs = new ArrayList<>();

        for (Paper paper : papers) {
            Map<String, String> faissDoc = new HashMap<>();
            faissDoc.put("id", paper.getId());
            faissDoc.put("text", paper.getSummary());  // Use summary or other relevant fields
            faissDocs.add(faissDoc);
        }

        // Send the documents to the FAISS service
        sendDocumentsToFaiss(faissDocs);
    }

    private void sendDocumentsToFaiss(List<Map<String, String>> documents) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<List<Map<String, String>>> request = new HttpEntity<>(documents);
        restTemplate.postForEntity(faissApiUrl + "/index_documents/", request, String.class);
    }
}

