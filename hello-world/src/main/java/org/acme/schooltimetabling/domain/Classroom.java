package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class Classroom {

    @PlanningId
    private Long id;

    private Timeslot timeslot;
    private int indexInTimeslot;

    @PlanningVariable
    private Cohort cohort;
    @PlanningVariable
    private Tutor tutor;
    @PlanningVariable
    private List<Student> students;

    // No-arg constructor required for Timefold
    public Classroom() {
    }

    public Classroom(long id, Timeslot timeslot, int indexInTimeslot) {
        this.id = id;
        this.timeslot = timeslot;
        this.indexInTimeslot = indexInTimeslot;
    }

    public Classroom(long id, Timeslot timeslot, int indexInTimeslot, Cohort cohort, Tutor tutor,
            List<Student> students) {
        this(id, timeslot, indexInTimeslot);
        this.cohort = cohort;
        this.tutor = tutor;
        this.students = students;
    }

    @Override
    public String toString() {
        return Timeslot + " - " indexInTimeslot + " (" + id + ")";
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public Long getId() {
        return id;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }

    public int getIndexInTimeslot() {
        return indexInTimeslot;
    }

    public Cohort getCohort() {
        return cohort;
    }

    public void setCohort(Cohort cohort) {
        this.cohort = cohort;
    }

    public Tutor getTutor() {
        return tutor;
    }

    public void setTutor(Tutor tutor) {
        this.tutor = tutor;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

}
