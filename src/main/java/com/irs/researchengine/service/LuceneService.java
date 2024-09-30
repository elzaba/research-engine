package com.irs.researchengine.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.irs.researchengine.data.Paper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Service
public class LuceneService {

    @Value("${docs.path}")
    private String DOCS_PATH;
    @Value("${index.path}")
    private String INDEX_PATH;

 // Indexing papers
    public void indexPapers() throws IOException {
        Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(dir, iwc);

        indexDocs(writer, Paths.get(DOCS_PATH));
        writer.close();
    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                indexDoc(writer, file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Indexes a single document */
    static void indexDoc(IndexWriter writer, Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String title = br.readLine(); // Read the first line as title
            StringBuilder content = new StringBuilder();
            String line;

            // Read the rest of the file for content
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }

            Document doc = new Document();
            doc.add(new StringField("path", file.toString(), Field.Store.YES));
            doc.add(new TextField("title", title, Field.Store.YES));
            doc.add(new TextField("contents", content.toString(), Field.Store.YES));

            writer.addDocument(doc);
        }
    }

    // Searching papers
    public List<Paper> searchPapers(String queryStr, int page, int pageSize) throws Exception {
    	if (queryStr == null || queryStr.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or empty");
        }
    	
        Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser("contents", analyzer);

        Query query = parser.parse(queryStr);
        
        // Fetch pageSize documents
        TopDocs results = searcher.search(query, (page + 1) * pageSize);
        
        List<Paper> papers = new ArrayList<>();
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

        reader.close();
        return papers;
    }
}
