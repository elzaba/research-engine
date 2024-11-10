package com.irs.researchengine.controller;

import com.irs.researchengine.config.CategoryConfig;
import com.irs.researchengine.service.ArxivApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class IndexController {

    @Autowired
    private ArxivApiService arxivApiService;

    @PostMapping("/api/index")
    public ResponseEntity<String> indexDocuments() {
        try {
        	List<String> categories = new ArrayList<>(CategoryConfig.getCategoryMap().keySet());
            // Fetch papers from the ArXiv API
            arxivApiService.fetchAllPapersFromCategories(categories);

            return ResponseEntity.ok("Indexing completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during indexing: " + e.getMessage());
        }
    }
}


