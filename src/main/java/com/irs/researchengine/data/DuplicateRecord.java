package com.irs.researchengine.data;

import org.apache.lucene.document.Document;

public class DuplicateRecord {
    private final Paper newPaper;
    private final Document existingDocument;

    public DuplicateRecord(Paper newPaper, Document existingDocument) {
        this.newPaper = newPaper;
        this.existingDocument = existingDocument;
    }

    public Paper getNewPaper() {
        return newPaper;
    }

    public Document getExistingDocument() {
        return existingDocument;
    }
}
