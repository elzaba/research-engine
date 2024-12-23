package com.irs.researchengine.controller;

import com.irs.researchengine.data.Paper;
import com.irs.researchengine.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    // Home page
    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Search
    @GetMapping("/search")
    public String searchPapers(@RequestParam(name = "query", required = false) String query,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int pageSize,
                               @RequestParam(value = "proximity", defaultValue = "false") boolean proximitySearch,
                               @RequestParam(value = "proximityDistance", defaultValue = "4") int proximityDistance,
                               @RequestParam(value = "semanticSearch", defaultValue = "false") boolean semanticSearch,
                               Model model) throws Exception {

        if (query == null || query.isEmpty()) {
            return "index"; // Redirect to home if query is empty
        }

        // Perform search
        List<Paper> results = searchService.searchPapers(query, page, pageSize, proximitySearch, proximityDistance, semanticSearch);

        model.addAttribute("results", results);
        model.addAttribute("query", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("proximitySearch", proximitySearch);
        model.addAttribute("proximityDistance", proximityDistance);
        model.addAttribute("semanticSearch", semanticSearch);
        return "search";
    }
}