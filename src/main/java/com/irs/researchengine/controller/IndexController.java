package com.irs.researchengine.controller;

import com.irs.researchengine.service.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController // Use RestController to handle responses directly
public class IndexController {

    @Autowired
    private IndexService indexService;

    @PostMapping("/index")
    public ResponseEntity<String> indexDocuments() {
        try {
            indexService.indexPapers();
            return ResponseEntity.ok("Indexing completed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error during indexing: " + e.getMessage());
        }
    }
}

