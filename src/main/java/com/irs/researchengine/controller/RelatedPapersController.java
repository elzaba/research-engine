package com.irs.researchengine.controller;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.irs.researchengine.data.Paper;
import com.irs.researchengine.service.ClusterService;

@RestController
@RequestMapping("/api")
public class RelatedPapersController {

    @Autowired
    private ClusterService clusterService;
    
    @Value("${index.path}")
    private String INDEX_PATH;
    
    @GetMapping("/related-papers/{docId}")
    public String viewRelatedPapers(@PathVariable String docId,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size,
                                    Model model) {
        Paper mainPaper = getPaperById(docId);
        List<Paper> relatedPapers = clusterService.getRelatedDocs(docId)
                                                  .stream()
                                                  .map(this::getPaperById)
                                                  .filter(Objects::nonNull)
                                                  .collect(Collectors.toList());
        model.addAttribute("mainPaper", mainPaper);
        model.addAttribute("relatedPapers", relatedPapers);
        model.addAttribute("lastQuery", query);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        return "related-papers";
    }

    private Paper getPaperById(String docId) {
        try (Directory dir = FSDirectory.open(Paths.get(INDEX_PATH));
             DirectoryReader reader = DirectoryReader.open(dir)) {
            IndexSearcher luceneSearcher = new IndexSearcher(reader);

            // Create a Lucene TermQuery to search by document ID
            Query query = new TermQuery(new Term("id", docId));
            TopDocs hits = luceneSearcher.search(query, 1);

            // If a document is found, map it to a Paper object
            if (hits.totalHits.value > 0) {
                Document doc = luceneSearcher.doc(hits.scoreDocs[0].doc);
                return mapDocumentToPaper(doc);
            }
        } catch (IOException e) {
            e.printStackTrace();  // Log or handle exceptions as per your application's error handling policy
        }
        return null;
    }

    // Utility method to map a Lucene Document to a Paper object
    private Paper mapDocumentToPaper(Document doc) {
        List<String> authors = doc.get("authors") != null
            ? Arrays.asList(doc.get("authors").split(", "))
            : new ArrayList<>();

        return new Paper(
            doc.get("id"),
            doc.get("title"),
            doc.get("summary"),
            doc.get("pdfLink"),
            doc.get("comment"),
            doc.get("updated"),
            doc.get("published"),
            doc.get("primaryCategory"),
            doc.get("primaryCategoryCode"),
            authors
        );
    }
}


