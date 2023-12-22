package org.acme.schooltimetabling.solver;

import java.util.List;
import java.util.Map;
import java.util.EnumSet;
import java.util.HashMap;
// import java.util.Arrays;

// import java.time.DayOfWeek;
// import java.time.LocalTime;

import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Cohort;
import org.acme.schooltimetabling.domain.Level;
import org.acme.schooltimetabling.domain.Platform;
import org.acme.schooltimetabling.domain.Student;
import org.acme.schooltimetabling.domain.StudentAssignment;
import org.acme.schooltimetabling.domain.Tutor;
// import org.acme.schooltimetabling.domain.Student;
import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.TimeTableApp;

import org.junit.jupiter.api.Test;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;

class TimeTableConstraintProviderTest {

    // make testing possible with all the input data
    private static final TimeTable data = TimeTableApp.getData();
    private static final List<Timeslot> timeSlotList = data.getTimeslotList();
    // private static final List<Cohort> cohortList = data.getCohortList();
    // private static final List<Tutor> tutorList = data.getTutorList();
    // private static final List<Student> studentList = data.getStudentList();
    // private static final List<Classroom> classroomList = data.getClassroomList();

    private static final Cohort B1 = new Cohort("B1", Level.BEGINNER, Platform.SCRATCH);
    private static final Cohort I1 = new Cohort("I1", Level.INTERMEDIATE, Platform.SCRATCH);
    private static final EnumSet<Level> ALL_LEVELS = EnumSet.allOf(Level.class);
    private static final EnumSet<Platform> ALL_PLATFORMS = EnumSet.allOf(Platform.class);
    private static Map<Timeslot, Boolean> ALL_TIMESLOTS = new HashMap<>();
    private static Map<Timeslot, Integer> ALL_TIMESLOTS_STUDENT = new HashMap<>();

    static {
        // Assuming timeSlotList is already populated with Timeslot instances
        for (Timeslot timeslot : timeSlotList) {
            ALL_TIMESLOTS.put(timeslot, Boolean.TRUE);
            ALL_TIMESLOTS_STUDENT.put(timeslot, 4);
        }
    }

    // tutors and students who can make all timeslots
    private static final Tutor TUTOR1 = new Tutor("Tutor1", ALL_LEVELS, ALL_PLATFORMS, ALL_TIMESLOTS, 12, 12);
    // private static final Tutor TUTOR2 = new Tutor("Tutor2", ALL_LEVELS,
    // ALL_PLATFORMS, ALL_TIMESLOTS, 12, 12);

    private static final Student STUDENT1 = new Student("Studentl", B1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT2 = new Student("Student2", B1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT3 = new Student("Student3", B1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT4 = new Student("Student4", B1,
            ALL_TIMESLOTS_STUDENT);

    private static final Student STUDENT5 = new Student("Student5", I1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT6 = new Student("Student6", I1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT7 = new Student("Student7", I1,
            ALL_TIMESLOTS_STUDENT);
    private static final Student STUDENT8 = new Student("Student8", I1,
            ALL_TIMESLOTS_STUDENT);

    // private static final List<Student> BEGINNERS1 = Arrays.asList(STUDENT1,
    // STUDENT2, STUDENT3, STUDENT4);
    // private static final List<Student> INTERMEDIATES1 = Arrays.asList(STUDENT5,
    // STUDENT6, STUDENT7, STUDENT8);

    ConstraintVerifier<TimeTableConstraintProvider, TimeTable> constraintVerifier = ConstraintVerifier.build(
            new TimeTableConstraintProvider(), TimeTable.class, StudentAssignment.class);

    @Test
    void sameCohortInClassroom() {
        StudentAssignment assignment1 = new StudentAssignment(1L, STUDENT1, timeSlotList.get(0), TUTOR1);
        StudentAssignment assignment2 = new StudentAssignment(2L, STUDENT5, timeSlotList.get(0), TUTOR1);
        constraintVerifier.verifyThat(TimeTableConstraintProvider::sameCohortInClassroom)
                .given(assignment1, assignment2)
                .penalizesBy(1);
    }
}
