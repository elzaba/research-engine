package com.irs.researchengine.service;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

@Service
public class IndexService {

    @Value("${docs.path}")
    private String DOCS_PATH;

    @Value("${index.path}")
    private String INDEX_PATH;

    public void indexPapers() throws IOException {
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH))) {
            Analyzer analyzer = new StandardAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                indexDocs(writer, Paths.get(DOCS_PATH));
            }
        }
    }

    private void indexDocs(final IndexWriter writer, Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                indexDoc(writer, file);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void indexDoc(IndexWriter writer, Path file) throws IOException {
        try (InputStream stream = Files.newInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {

            String title = br.readLine(); // Read the first line as title
            StringBuilder content = new StringBuilder();
            String line;

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
}
