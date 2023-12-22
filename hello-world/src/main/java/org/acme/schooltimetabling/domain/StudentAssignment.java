package org.acme.schooltimetabling.domain;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class StudentAssignment {

    @PlanningId
    private Long id;

    private Student student;

    @PlanningVariable(valueRangeProviderRefs = "timeslotRange")
    private Timeslot timeslot;

    @PlanningVariable(valueRangeProviderRefs = "tutorRange")
    private Tutor tutor;

    // CONSTRUCTORS

    public StudentAssignment() {
        // Required no-arg constructor
    }

    public StudentAssignment(Long id, Student student) {
        this.id = id;
        this.student = student;
    }

    public StudentAssignment(Long id, Student student, Timeslot timeslot, Tutor tutor) {
        this(id, student);
        this.timeslot = timeslot;
        this.tutor = tutor;
    }

    // GETTERS AND SETTERS

    public Long getId() {
        return id;
    }

    public Student getStudent() {
        return student;
    }

    public Timeslot getTimeslot() {
        return timeslot;
    }

    public Tutor getTutor() {
        return tutor;
    }

    @Override
    public String toString() {
        return "StudentAssignment{" +
                "id=" + id +
                ", student=" + student.getId() +
                ", @ " + timeslot.toString() +
                ", with " + tutor.getName() +
                '}';
    }
}
