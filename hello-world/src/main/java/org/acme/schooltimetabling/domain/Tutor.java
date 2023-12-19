package org.acme.schooltimetabling.domain;

import java.util.EnumSet;
import java.util.Map;

public class Tutor {
    private String name;
    private EnumSet<Level> levelProficiencies;
    private EnumSet<Platform> platformProficiencies;
    private Map<Timeslot, Boolean> availability;
    private int idealLessons;
    private int maxLessons;

    public Tutor(String name, EnumSet<Level> levelProficiencies, EnumSet<Platform> platformProficiencies,
            Map<Timeslot, Boolean> availability, int idealLessons, int maxLessons) {
        this.name = name;
        this.levelProficiencies = levelProficiencies;
        this.platformProficiencies = platformProficiencies;
        this.availability = availability;
        this.idealLessons = idealLessons;
        this.maxLessons = maxLessons;
    }

    // Getters and setters...

    public boolean canTeach(Level level) {
        return levelProficiencies.contains(level);
    }

    public boolean canTeachOnPlatform(Platform platform) {
        return platformProficiencies.contains(platform);
    }

    public boolean isAvailable(Timeslot timeslot) {
        return availability.getOrDefault(timeslot, false);
    }
}
