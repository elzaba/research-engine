package com.irs.researchengine.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irs.researchengine.service.AutocompleteService;

@RestController
public class AutocompleteController {

    @Autowired
    private AutocompleteService autocompleteService;

    @GetMapping("/api/autocomplete")
    public List<String> autocomplete(@RequestParam("query") String query) throws Exception {
        return autocompleteService.autocomplete(query);
    }
}

