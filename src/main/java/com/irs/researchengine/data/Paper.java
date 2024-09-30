package com.irs.researchengine.data;

public class Paper {
    private String title;
    private String path;
    private String contents;

    public Paper(String title, String path, String contents) {
        this.title = title;
        this.path = path;
        this.contents = contents;
    }

    public String getTitle() { return title; }
    public String getPath() { return path; }
    public String getContents() { return contents; }

    public void setTitle(String title) { this.title = title; }
    public void setPath(String path) { this.path = path; }
    public void setContents(String contents) { this.contents = contents; }
}
