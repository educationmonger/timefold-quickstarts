package org.acme.schooltimetabling.domain;

import java.util.Map;

public class Student {
    private String id;
    private Cohort cohort; 
    private Map<Timeslot, Integer> availability; 

    // Constructor
    public Student(String id, Cohort cohort, Map<Timeslot, Integer> availability) {
        this.id = id;
        this.cohort = cohort;
        this.availability = availability;
    }

    // Getters
    public String getId() {
        return id;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public Map<Timeslot, Integer> getAvailability() {
        return availability;
    }

    // Method to check if the student can attend a given timeslot
    public boolean canAttend(Timeslot timeslot) {
        Integer score = availability.get(timeslot);
        return score != null && score > 0;
    }
}
