package com.irs.researchengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.irs.researchengine.service.SemanticIndexService;

@RestController
public class SemanticIndexController {

    @Autowired
    private SemanticIndexService semanticIndexService;
    
    @Value("${dataset.path}")
    private String datasetPath;

    @PostMapping("/api/index-faiss")
    public ResponseEntity<String> indexDocumentsInFaiss() {
        try {
            semanticIndexService.indexFromDataset(datasetPath);
            return ResponseEntity.ok("Semantic indexing completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during semantic indexing: " + e.getMessage());
        }
    }
}
