package com.irs.researchengine.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irs.researchengine.data.Paper;
import com.irs.researchengine.service.SearchService;

@RestController
public class SearchApiController {

    @Autowired
    private SearchService searchService;

    // API search end point returning JSON data for Angular
    @GetMapping("/api/search")
    public ResponseEntity<List<Paper>> searchPapersApi(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int pageSize,
            @RequestParam(value = "proximity", defaultValue = "false") boolean proximitySearch,
            @RequestParam(value = "proximityDistance", defaultValue = "4") int proximityDistance) throws Exception {

        if (query == null || query.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());  // Empty list for bad query
        }

        // Perform search and return JSON response
        List<Paper> results = searchService.searchPapers(query, page, pageSize, proximitySearch, proximityDistance);
        return ResponseEntity.ok(results);  // Return results as JSON
    }
}

