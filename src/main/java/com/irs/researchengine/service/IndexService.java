package com.irs.researchengine.service;

import com.irs.researchengine.data.Paper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.irs.researchengine.nlp.CustomAnalyzer;

import java.nio.file.Paths;
import java.util.List;

@Service
public class IndexService {

    @Value("${index.path}")
    private String INDEX_PATH;

    public void indexPapers(List<Paper> papers) throws Exception {
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH))) {
            Analyzer analyzer = new CustomAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            try (IndexWriter writer = new IndexWriter(dir, iwc)) {
                for (Paper paper : papers) {
                    indexPaper(writer, paper);
                }
            }
        }
    }

    private void indexPaper(IndexWriter writer, Paper paper) throws Exception {
        Document doc = new Document();
        doc.add(new StringField("id", paper.getId(), Field.Store.YES));
        doc.add(new TextField("title", paper.getTitle(), Field.Store.YES));
        doc.add(new TextField("summary", paper.getSummary(), Field.Store.YES));
        doc.add(new StringField("pdfLink", paper.getPdfLink(), Field.Store.YES));
        doc.add(new TextField("comment", paper.getComment(), Field.Store.YES));
        doc.add(new StringField("updated", paper.getUpdated(), Field.Store.YES));
        doc.add(new StringField("published", paper.getPublished(), Field.Store.YES));
        doc.add(new StringField("primaryCategory", paper.getPrimaryCategory(), Field.Store.YES));
        doc.add(new StringField("primaryCategoryCode", paper.getCategoryCode(), Field.Store.YES));
        if (paper.getAuthors() != null && !paper.getAuthors().isEmpty()) {
            String allAuthors = String.join(", ", paper.getAuthors());
            doc.add(new TextField("authors", allAuthors, Field.Store.YES));
        }

        writer.addDocument(doc);
    }
}
