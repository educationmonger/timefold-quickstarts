package org.acme.schooltimetabling;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
// import java.util.Collections;
import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.stream.Collectors;
import java.util.Map;
import java.util.stream.Collectors;

import org.acme.schooltimetabling.domain.CSVReaderUtility;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Cohort;
import org.acme.schooltimetabling.domain.Tutor;
import org.acme.schooltimetabling.domain.Student;
import org.acme.schooltimetabling.domain.StudentAssignment;
import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.solver.TimeTableConstraintProvider;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeTableApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeTableApp.class);

    public static void main(String[] args) {
        SolverFactory<TimeTable> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(TimeTable.class)
                .withEntityClasses(StudentAssignment.class)
                .withConstraintProviderClass(TimeTableConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofSeconds(30)));

        // Load the problem
        TimeTable problem = getData();

        // Solve the problem
        Solver<TimeTable> solver = solverFactory.buildSolver();
        TimeTable solution = solver.solve(problem);
        HardSoftScore finalScore = solution.getScore();

        // Visualize the solution
        printTimetable(solution);

        // Print the score summary
        printScoreSummary(finalScore);
    }

    public static TimeTable getData() {

        // Cohorts
        List<Cohort> cohortList = null;
        try {
            cohortList = CSVReaderUtility.readCohorts("src/main/resources/cohorts.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Timeslots
        List<Timeslot> timeslotList = new ArrayList<>(12);
        DayOfWeek[] days = { DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY };
        LocalTime[] startTimes = { LocalTime.of(15, 30), LocalTime.of(16, 30), LocalTime.of(17, 30) };

        for (DayOfWeek day : days) {
            for (LocalTime startTime : startTimes) {
                timeslotList.add(new Timeslot(day, startTime));
            }
        }

        // Students
        List<Student> studentList = null;
        try {
            studentList = CSVReaderUtility.readStudents("src/main/resources/students.csv", timeslotList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Tutors
        List<Tutor> tutorList = null;
        try {
            tutorList = CSVReaderUtility.readTutors("src/main/resources/tutors.csv", timeslotList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // StudentAssignments
        List<StudentAssignment> studentAssignmentList = new ArrayList<>();
        long studentId = 0;
        for (Student student : studentList) {
            studentAssignmentList.add(new StudentAssignment(studentId++, student));
        }

        return new TimeTable(cohortList, studentList, timeslotList, tutorList, studentAssignmentList);
    }

    public static void printSolution(TimeTable solution) {
        // Assuming Tutor and Timeslot have proper toString methods.
        // Group StudentAssignments by Cohort, Tutor, and Timeslot
        Map<Cohort, Map<Tutor, Map<Timeslot, List<StudentAssignment>>>> groupedAssignments = solution
                .getStudentAssignmentList().stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getStudent().getCohort(),
                        Collectors.groupingBy(
                                StudentAssignment::getTutor,
                                Collectors.groupingBy(
                                        StudentAssignment::getTimeslot))));

        // Iterate over the groups and print the solution
        for (Map.Entry<Cohort, Map<Tutor, Map<Timeslot, List<StudentAssignment>>>> cohortEntry : groupedAssignments
                .entrySet()) {
            String cohortLabel = cohortEntry.getKey().getLabel();
            System.out.println("-------------- " + cohortLabel + " --------------------");
            for (Map.Entry<Tutor, Map<Timeslot, List<StudentAssignment>>> tutorEntry : cohortEntry.getValue()
                    .entrySet()) {
                String tutorName = tutorEntry.getKey().getName();
                for (Map.Entry<Timeslot, List<StudentAssignment>> timeslotEntry : tutorEntry.getValue().entrySet()) {
                    String timeslotStr = timeslotEntry.getKey().toString();
                    List<StudentAssignment> assignments = timeslotEntry.getValue();
                    // Sort assignments by some property if needed, e.g., student ID
                    assignments.sort(Comparator.comparing(a -> a.getStudent().getId()));

                    System.out.println(tutorName + ", " + timeslotStr);
                    for (StudentAssignment assignment : assignments) {
                        System.out.println(assignment.getStudent().getId());
                    }
                    System.out.println("-----------------------------------------------------------");
                }
            }
        }
    }

    public static void printScoreSummary(HardSoftScore score) {
        // Assuming HardSoftScore is the score type used in your solution
        int hardScore = score.hardScore();
        int softScore = score.softScore();

        // Print the overall score summary
        System.out.println("Final score summary:");
        System.out.println("Overall score: " + score);
        System.out.println("Number of hard constraints broken: " + (hardScore == 0 ? 0 : -hardScore)); // Hard score is
                                                                                                       // negative if
                                                                                                       // constraints
                                                                                                       // are broken
        System.out.println("Number of soft constraints broken: " + (softScore == 0 ? 0 : -softScore)); // Soft score is
                                                                                                       // negative if
                                                                                                       // constraints
                                                                                                       // are broken
    }

    private static void printTimetable(TimeTable timeTable) {
        LOGGER.info("");
        LOGGER.info("Timetable successfully generated.");

        printSolution(timeTable);
    }

}
