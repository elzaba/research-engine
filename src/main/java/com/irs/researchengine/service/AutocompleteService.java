package com.irs.researchengine.service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AutocompleteService {
    @Value("${index.path}")
    private String INDEX_PATH;

    public List<String> autocomplete(String prefix) throws Exception {
        List<String> suggestions = new ArrayList<>();
        
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             DirectoryReader reader = DirectoryReader.open(dir)) {
             
            IndexSearcher searcher = new IndexSearcher(reader);
            
            // Prefix query for efficient matching of N-grams
            Query prefixQuery = new PrefixQuery(new Term("title", prefix.toLowerCase()));

            // Search for the query
            TopDocs results = searcher.search(prefixQuery, 10);  // Limit to 10 suggestions
            
            for (ScoreDoc scoreDoc : results.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String title = doc.get("title");
                if (!suggestions.contains(title)) {  // Avoid duplicate suggestions
                    suggestions.add(title);
                }
            }
        }
        return suggestions;
    }
}
