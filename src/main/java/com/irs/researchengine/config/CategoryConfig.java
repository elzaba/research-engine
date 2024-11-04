package com.irs.researchengine.config;

import java.util.HashMap;
import java.util.Map;

public class CategoryConfig {
	
	private static final Map<String, String> CATEGORY_MAP = createCategoryMap();

	private static Map<String, String> createCategoryMap() {
	    Map<String, String> map = new HashMap<>();
	    map.put("cs.AI", "Artificial Intelligence");
	    map.put("cs.AR", "Hardware Architecture");
	    map.put("cs.CC", "Computational Complexity");
	    map.put("cs.CE", "Computational Engineering, Finance, and Science");
	    map.put("cs.CG", "Computational Geometry");
	    map.put("cs.CL", "Computation and Language");
	    map.put("cs.CR", "Cryptography and Security");
	    map.put("cs.CV", "Computer Vision and Pattern Recognition");
	    map.put("cs.CY", "Computers and Society");
	    map.put("cs.DB", "Databases");
	    map.put("cs.DC", "Distributed, Parallel, and Cluster Computing");
	    map.put("cs.DL", "Digital Libraries");
	    map.put("cs.DM", "Discrete Mathematics");
	    map.put("cs.DS", "Data Structures and Algorithms");
	    map.put("cs.ET", "Emerging Technologies");
	    map.put("cs.FL", "Formal Languages and Automata Theory");
	    map.put("cs.GL", "General Literature");
	    map.put("cs.GR", "Graphics");
	    map.put("cs.GT", "Computer Science and Game Theory");
	    map.put("cs.HC", "Human-Computer Interaction");
	    map.put("cs.IR", "Information Retrieval");
	    map.put("cs.IT", "Information Theory");
	    map.put("cs.LG", "Machine Learning");
	    map.put("cs.LO", "Logic in Computer Science");
	    map.put("cs.MA", "Multiagent Systems");
	    map.put("cs.MM", "Multimedia");
	    map.put("cs.MS", "Mathematical Software");
	    map.put("cs.NA", "Numerical Analysis");
	    map.put("cs.NE", "Neural and Evolutionary Computing");
	    map.put("cs.NI", "Networking and Internet Architecture");
	    map.put("cs.OH", "Other Computer Science");
	    map.put("cs.OS", "Operating Systems");
	    map.put("cs.PF", "Performance");
	    map.put("cs.PL", "Programming Languages");
	    map.put("cs.RO", "Robotics");
	    map.put("cs.SC", "Symbolic Computation");
	    map.put("cs.SD", "Sound");
	    map.put("cs.SE", "Software Engineering");
	    map.put("cs.SI", "Social and Information Networks");
	    map.put("cs.SY", "Systems and Control");
	    return map;
	}

    public static Map<String, String> getCategoryMap() {
        return CATEGORY_MAP;
    }

    public static String getFullCategoryName(String code) {
        return CATEGORY_MAP.getOrDefault(code, "Unknown Category");
    }

}
