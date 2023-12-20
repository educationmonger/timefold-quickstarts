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

    private List<Timeslot> timeslotList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Cohort> cohortList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Tutor> tutorList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Student> studentList;

    @PlanningEntityCollectionProperty
    private List<Classroom> classroomList;

    @PlanningScore
    private HardSoftScore score;

    // No-arg constructor required for Timefold
    public TimeTable() {
    }

    public TimeTable(List<Timeslot> timeslotList, List<Cohort> cohortList, List<Tutor> tutorList,
            List<Student> studentList, List<Classroom> classroomList) {
        this.timeslotList = timeslotList;
        this.cohortList = cohortList;
        this.tutorList = tutorList;
        this.studentList = studentList;
        this.classroomList = classroomList;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Timeslot> getTimeslotList() {
        return timeslotList;
    }

    public List<Cohort> getCohortList() {
        return cohortList;
    }

    public List<Tutor> getTutorList() {
        return tutorList;
    }

    public List<Student> getStudentList() {
        return studentList;
    }

    public List<Classroom> getClassroomList() {
        return classroomList;
    }

    public HardSoftScore getScore() {
        return score;
    }

}
