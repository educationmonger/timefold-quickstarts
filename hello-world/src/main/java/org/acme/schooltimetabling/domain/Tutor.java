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

    // getters and setters

    public String getName() {
        return name;
    }

    public EnumSet<Level> getLevelProficiencies() {
        return levelProficiencies;
    }

    public EnumSet<Platform> getPlatformProficiencies() {
        return platformProficiencies;
    }

    public Map<Timeslot, Boolean> getAvailability() {
        return availability;
    }

    public int getIdealLessons() {
        return idealLessons;
    }

    public int getMaxLessons() {
        return maxLessons;
    }

    // methods

    public boolean canTeach(Level level) {
        return levelProficiencies.contains(level);
    }

    public boolean canTeachOnPlatform(Platform platform) {
        return platformProficiencies.contains(platform);
    }

    public boolean isAvailable(Timeslot timeslot) {
        return availability.getOrDefault(timeslot, false);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
