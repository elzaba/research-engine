package com.irs.researchengine.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;

@Service
public class ClusterService {

    private final Map<String, Integer> docToCluster = new HashMap<>();
    
    @Value("${cluster.path}")
    private String clusterFilePath;

    @PostConstruct
    public void loadClusters() throws IOException {
        // Load cluster assignments from the file
    	Path clusterFile = Paths.get(clusterFilePath);
        if (Files.exists(clusterFile)) {
            String json = Files.readString(clusterFile);
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, Integer>> typeRef = new TypeReference<>() {};
            docToCluster.putAll(objectMapper.readValue(json, typeRef));
        }
    }

    public List<String> getRelatedDocs(String docId) {
        Integer clusterLabel = docToCluster.get(docId);
        if (clusterLabel == null) {
            throw new IllegalArgumentException("Document ID not found in clusters.");
        }
        // Find all document IDs in the same cluster
        return docToCluster.entrySet().stream()
                .filter(entry -> entry.getValue().equals(clusterLabel))
                .map(Map.Entry::getKey)
                .filter(id -> !id.equals(docId))  // Exclude the original document
                .collect(Collectors.toList());
    }
}

