package org.acme.schooltimetabling.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

@PlanningSolution
public class TimeTable {

    // PROBLEM FACTS
    private List<Cohort> cohortList;

    private List<Student> studentList;

    // PLANNING VARIABLES
    @ProblemFactCollectionProperty
    private List<Timeslot> timeslotList;

    @ProblemFactCollectionProperty
    private List<Tutor> tutorList;

    @PlanningEntityCollectionProperty
    private List<StudentAssignment> studentAssignmentList;

    @PlanningScore
    private HardSoftScore score;

    // No-arg constructor required for Timefold
    public TimeTable() {
    }

    public TimeTable(List<Cohort> cohortList, List<Student> studentList, List<Timeslot> timeslotList,
            List<Tutor> tutorList, List<StudentAssignment> studentAssignmentList) {
        this.cohortList = cohortList;
        this.studentList = studentList;
        this.timeslotList = timeslotList;
        this.tutorList = tutorList;
        this.studentAssignmentList = studentAssignmentList;
    }

    // GETTERS AND SETTERS

    public List<Cohort> getCohortList() {
        return this.cohortList;
    }

    public List<Student> getStudentList() {
        return this.studentList;
    }

    @ValueRangeProvider(id = "timeslotRange")
    public List<Timeslot> getTimeslotList() {
        return this.timeslotList;
    }

    @ValueRangeProvider(id = "tutorRange")
    public List<Tutor> getTutorList() {
        return this.tutorList;
    }

    public List<StudentAssignment> getStudentAssignmentList() {
        return this.studentAssignmentList;
    }

    public HardSoftScore getScore() {
        return this.score;
    }

}
