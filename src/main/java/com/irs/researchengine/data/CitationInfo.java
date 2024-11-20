package com.irs.researchengine.data;

import java.util.List;

public class CitationInfo {
    private int citationCount;
    private List<String> citationUrls;

    // Constructors
    public CitationInfo(int citationCount, List<String> citationUrls) {
        this.citationCount = citationCount;
        this.citationUrls = citationUrls;
    }

    // Getters and setters
    public int getCitationCount() {
        return citationCount;
    }
    
    public void setCitationCount(int citationCount) {
        this.citationCount = citationCount;
    }

    public List<String> getCitationUrls() {
        return citationUrls;
    }
    
    public void setCitationUrls(List<String> citationUrls) {
        this.citationUrls = citationUrls;
    }
}


