package com.irs.researchengine.data;

import java.util.List;

public class Paper {
	private String id;
    private String title;
    private String summary;
    private String pdfLink;
    private String comment;
    private String updated;
    private String published;
    private String primaryCategory;
    private String categoryCode;
    private List<String> authors;
    private CitationInfo citationInfo;
    
    // Default constructor (required for Jackson)
    public Paper() {
    }
    
    public Paper(String id, String title, String summary, String pdfLink, String comment, String updated,
            String published, String primaryCategory, String categoryCode, List<String> authors) {
    	this.id = id;
    	this.title = title;
    	this.summary = summary;
    	this.pdfLink = pdfLink;
    	this.comment = comment;
    	this.updated = updated;
    	this.published = published;
    	this.primaryCategory = primaryCategory;
    	this.categoryCode = categoryCode;
    	this.authors = authors;
    	this.citationInfo = null;
    }
    
    public Paper(String id, String title, String summary, String pdfLink, String comment, String updated,
            String published, String primaryCategory, String categoryCode, List<String> authors, CitationInfo citationInfo) {
    	this.id = id;
    	this.title = title;
    	this.summary = summary;
    	this.pdfLink = pdfLink;
    	this.comment = comment;
    	this.updated = updated;
    	this.published = published;
    	this.primaryCategory = primaryCategory;
    	this.categoryCode = categoryCode;
    	this.authors = authors;
    	this.citationInfo = citationInfo;
    }

    // Getters and setters for each field
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPdfLink() {
        return pdfLink;
    }

    public void setPdfLink(String pdfLink) {
        this.pdfLink = pdfLink;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public String getPrimaryCategory() {
        return primaryCategory;
    }

    public void setPrimaryCategory(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }
    
    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }
    
    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

	public CitationInfo getCitationInfo() {
		return citationInfo;
	}

	public void setCitationInfo(CitationInfo citationInfo) {
		this.citationInfo = citationInfo;
	}
    
    
}