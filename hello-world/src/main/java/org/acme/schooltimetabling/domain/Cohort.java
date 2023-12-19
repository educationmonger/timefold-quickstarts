package org.acme.schooltimetabling.domain;

import java.util.HashMap;
import java.util.Map;

public class Cohort {

    private static final Map<String, Cohort> cohortMap = new HashMap<>();

    private String label;
    private Level level;
    private Platform platform;

    public Cohort(String label, Level level, Platform platform) {
        this.label = label;
        this.level = level;
        this.platform = platform;
        cohortMap.put(label, this);
    }

    @Override
    public String toString() {
        return label;
    }

    public static Cohort getCohort(String label) {
        return cohortMap.get(label);
    }

    public static Map<String, Cohort> getCohortMap() {
        return cohortMap;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public String getLabel() {
        return label;
    }

    public Level getLevel() {
        return level;
    }

    public Platform getPlatform() {
        return platform;
    }

}