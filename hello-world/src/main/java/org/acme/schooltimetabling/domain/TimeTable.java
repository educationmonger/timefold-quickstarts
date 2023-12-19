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

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Timeslot> timeslotList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Room> roomList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Cohort> cohortList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Student> studentList;

    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<Tutor> tutorList;

    @PlanningEntityCollectionProperty
    private List<Lesson> lessonList;

    @PlanningScore
    private HardSoftScore score;

    // No-arg constructor required for Timefold
    public TimeTable() {
    }

    public TimeTable(List<Timeslot> timeslotList, List<Room> roomList, List<Cohort> cohortList,
            List<Student> studentList, List<Tutor> tutorList, List<Lesson> lessonList) {
        this.timeslotList = timeslotList;
        this.roomList = roomList;
        this.cohortList = cohortList;
        this.studentList = studentList;
        this.tutorList = tutorList;
        this.lessonList = lessonList;
    }

    // ************************************************************************
    // Getters and setters
    // ************************************************************************

    public List<Timeslot> getTimeslotList() {
        return timeslotList;
    }

    public List<Room> getRoomList() {
        return roomList;
    }

    public List<Cohort> getCohortList() {
        return cohortList;
    }

    public List<Student> getStudentList() {
        return studentList;
    }

    public List<Tutor> getTutorList() {
        return tutorList;
    }

    public List<Lesson> getLessonList() {
        return lessonList;
    }

    public HardSoftScore getScore() {
        return score;
    }

}
