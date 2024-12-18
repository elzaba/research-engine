package com.irs.researchengine.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class CorpusUtils {
	
	@Value("${corpus.file.path}")
    private String corpusFilePath;
	
    private Map<String, Integer> termFrequencies = new HashMap<>();
    private Map<String, Integer> coOccurrences = new HashMap<>();
    private int totalTerms;

    @PostConstruct
    public void loadCorpus() throws IOException {
    	List<String> lines = Files.readAllLines(Paths.get(corpusFilePath));
        for (String line : lines) {
            String[] tokens = line.split("\\s+");
            for (String token : tokens) {
                termFrequencies.put(token, termFrequencies.getOrDefault(token, 0) + 1);
                totalTerms++;
            }
            for (int i = 0; i < tokens.length; i++) {
                for (int j = i + 1; j < tokens.length; j++) {
                    String pair = tokens[i] + " " + tokens[j];
                    coOccurrences.put(pair, coOccurrences.getOrDefault(pair, 0) + 1);
                }
            }
        }
    }

    public double getJointProbability(String term) {
        return (double) coOccurrences.getOrDefault(term, 0) / totalTerms;
    }

    public double getIndependentProbability(String term) {
        String[] words = term.split("\\s+");
        double probability = 1.0;
        for (String word : words) {
            probability *= (double) termFrequencies.getOrDefault(word, 1) / totalTerms;
        }
        return probability;
    }
}


