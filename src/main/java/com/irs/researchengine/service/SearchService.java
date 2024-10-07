package com.irs.researchengine.service;

import com.irs.researchengine.data.Paper;
import com.irs.researchengine.nlp.CustomAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    @Value("${index.path}")
    private String INDEX_PATH;

    public List<Paper> searchPapers(String queryStr, int page, int pageSize, boolean proximitySearch, int proximityDistance) throws Exception {
        if (queryStr == null || queryStr.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }

        List<Paper> papers = new ArrayList<>();
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             DirectoryReader reader = DirectoryReader.open(dir)) {

            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new MultiFieldQueryParser(new String[]{"title", "contents"}, new CustomAnalyzer());

            // If proximity search is enabled, modify the query to use the proximity distance
            if (proximitySearch) {
                queryStr = "\"" + queryStr + "\"~" + proximityDistance;
            }

            Query query = parser.parse(queryStr);
            TopDocs results = searcher.search(query, (page + 1) * pageSize);
            int start = Math.min(page * pageSize, results.scoreDocs.length);
            int end = Math.min(start + pageSize, results.scoreDocs.length);

            for (int i = start; i < end; i++) {
                Document doc = searcher.doc(results.scoreDocs[i].doc);
                papers.add(new Paper(
                        doc.get("title"),
                        doc.get("path"),
                        doc.get("contents")
                ));
            }
        }
        return papers;
    }
}

