package com.irs.researchengine.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.irs.researchengine.service.AutocompleteService;

@Controller
public class AutocompleteController {

    @Autowired
    private AutocompleteService autocompleteService;

    @GetMapping("/autocomplete")
    @ResponseBody
    public List<String> autocomplete(@RequestParam("query") String query) throws Exception {
        return autocompleteService.autocomplete(query);
    }
}
