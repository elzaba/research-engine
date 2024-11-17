package com.irs.researchengine.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.irs.researchengine.config.CategoryConfig;
import com.irs.researchengine.service.ArxivApiService;

@RestController
public class DatasetController {

	@Autowired
    private ArxivApiService arxivApiService;

    @PostMapping("/api/create-dataset")
    public ResponseEntity<String> createDataset() {
        try {
            List<String> categories = new ArrayList<>(CategoryConfig.getCategoryMap().keySet());
            arxivApiService.createDataset(categories);
            return ResponseEntity.ok("Dataset created successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error creating dataset: " + e.getMessage());
        }
    }
}

