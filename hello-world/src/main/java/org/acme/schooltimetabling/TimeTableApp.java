package org.acme.schooltimetabling;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.acme.schooltimetabling.domain.CSVReaderUtility;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Cohort;
import org.acme.schooltimetabling.domain.Tutor;
import org.acme.schooltimetabling.domain.Student;
import org.acme.schooltimetabling.domain.Classroom;
import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.solver.TimeTableConstraintProvider;
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
                .withEntityClasses(Classroom.class)
                .withConstraintProviderClass(TimeTableConstraintProvider.class)
                // The solver runs only for 5 seconds on this small dataset.
                // It's recommended to run for at least 5 minutes ("5m") otherwise.
                .withTerminationSpentLimit(Duration.ofSeconds(5)));

        // Load the problem
        TimeTable problem = generateDemoData();

        // Solve the problem
        Solver<TimeTable> solver = solverFactory.buildSolver();
        TimeTable solution = solver.solve(problem);

        // Visualize the solution
        printTimetable(solution);
    }

    public static TimeTable generateDemoData() {
        // Timeslots
        List<Timeslot> timeslotList = new ArrayList<>(12);
        timeslotList.add(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(15, 30), LocalTime.of(16, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(16, 30), LocalTime.of(17, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.MONDAY, LocalTime.of(17, 30), LocalTime.of(18, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(15, 30), LocalTime.of(16, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(16, 30), LocalTime.of(17, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.TUESDAY, LocalTime.of(17, 30), LocalTime.of(18, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(15, 30), LocalTime.of(16, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(16, 30), LocalTime.of(17, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.WEDNESDAY, LocalTime.of(17, 30), LocalTime.of(18, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(15, 30), LocalTime.of(16, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(16, 30), LocalTime.of(17, 30)));
        timeslotList.add(new Timeslot(DayOfWeek.THURSDAY, LocalTime.of(17, 30), LocalTime.of(18, 30)));

        // Cohorts
        List<Cohort> cohortList = null;
        try {
            cohortList = CSVReaderUtility.readCohorts("src/main/resources/cohorts.csv");
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

        // Students
        List<Student> studentList = null;
        try {
            studentList = CSVReaderUtility.readStudents("src/main/resources/students.csv", timeslotList);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Classrooms
        List<Classrrom> classroomList = new ArrayList<>();
        long id = 0;

        for (Timeslot timeslot : timeslots) {
            for (int i = 0; i < 17; i++) { // create maximum of 17 simultaneous "classrooms" (TeamViewer connections) in
                                           // a given timeslot
                classroomList.add(new Classroom(id++, timeslot, i));
            }
        }

        return new TimeTable(timeslotList, cohortList, tutorList, studentList, classroomList);
    }

    private static void printTimetable(TimeTable timeTable) {
        LOGGER.info("");
        //     List<Room> roomList = timeTable.getRoomList();
        //     List<Lesson> lessonList = timeTable.getLessonList();
        //     Map<Timeslot, Map<Room, List<Lesson>>> lessonMap = lessonList.stream()
        //             .filter(lesson -> lesson.getTimeslot() != null && lesson.getRoom() != null)
        //             .collect(Collectors.groupingBy(Lesson::getTimeslot,
        //                     Collectors.groupingBy(Lesson::getRoom)));
        //     LOGGER.info("|            | " + roomList.stream()
        //             .map(room -> String.format("%-10s", room.getName())).collect(Collectors.joining(" | "))
        //             + " |");
        //     LOGGER.info("|" + "------------|".repeat(roomList.size() + 1));
        //     for (Timeslot timeslot : timeTable.getTimeslotList()) {
        //         List<List<Lesson>> cellList = roomList.stream()
        //                 .map(room -> {
        //                     Map<Room, List<Lesson>> byRoomMap = lessonMap.get(timeslot);
        //                     if (byRoomMap == null) {
        //                         return Collections.<Lesson>emptyList();
        //                     }
        //                     List<Lesson> cellLessonList = byRoomMap.get(room);
        //                     return Objects.requireNonNullElse(cellLessonList,
        //                             Collections.<Lesson>emptyList());
        //                 }).toList();

        //         LOGGER.info("| " + String.format("%-10s",
        //                 timeslot.getDayOfWeek().toString().substring(0, 3) + " "
        //                         + timeslot.getStartTime())
        //                 + " | "
        //                 + cellList.stream().map(cellLessonList -> String.format("%-10s",
        //                         cellLessonList.stream().map(Lesson::getSubject)
        //                                 .collect(Collectors.joining(", "))))
        //                         .collect(Collectors.joining(" | "))
        //                 + " |");
        //         LOGGER.info("|            | "
        //                 + cellList.stream().map(cellLessonList -> String.format("%-10s",
        //                         cellLessonList.stream().map(Lesson::getTeacher)
        //                                 .collect(Collectors.joining(", "))))
        //                         .collect(Collectors.joining(" | "))
        //                 + " |");
        //         LOGGER.info("|            | "
        //                 + cellList.stream().map(cellLessonList -> String.format("%-10s",
        //                         cellLessonList.stream().map(Lesson::getStudentGroup)
        //                                 .collect(Collectors.joining(", "))))
        //                         .collect(Collectors.joining(" | "))
        //                 + " |");
        //         LOGGER.info("|" + "------------|".repeat(roomList.size() + 1));
        //     }
        //     List<Lesson> unassignedLessons = lessonList.stream()
        //             .filter(lesson -> lesson.getTimeslot() == null || lesson.getRoom() == null)
        //             .toList();
        //     if (!unassignedLessons.isEmpty()) {
        //         LOGGER.info("");
        //         LOGGER.info("Unassigned lessons");
        //         for (Lesson lesson : unassignedLessons) {
        //             LOGGER.info("  " + lesson.getSubject() + " - " + lesson.getTeacher() + " - "
        //                     + lesson.getStudentGroup());
        //         }
        //     }
        // }
    LOGGER.info("Timetable successfully generated.")
    }

}
