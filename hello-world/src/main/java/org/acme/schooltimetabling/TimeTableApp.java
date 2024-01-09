package org.acme.schooltimetabling;

import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
// import java.util.Collections;
import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.acme.schooltimetabling.domain.CSVReaderUtility;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Cohort;
import org.acme.schooltimetabling.domain.Level;
import org.acme.schooltimetabling.domain.Platform;
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
        // default search (some kind of heuristic)
        SolverFactory<TimeTable> solverFactory = SolverFactory.create(new SolverConfig()
                .withSolutionClass(TimeTable.class)
                .withEntityClasses(StudentAssignment.class)
                .withConstraintProviderClass(TimeTableConstraintProvider.class)
                .withTerminationSpentLimit(Duration.ofMinutes(7)));
        // .withTerminationSpentLimit(Duration.ofSeconds(10)));

        // For an exhaustive search, with the solver built from an XML config file
        // SolverFactory<TimeTable> solverFactory =
        // SolverFactory.createFromXmlResource("solverConfig.xml");

        // Load the problem
        TimeTable problem = getData();

        // Solve the problem
        Solver<TimeTable> solver = solverFactory.buildSolver();
        TimeTable solution = solver.solve(problem);
        HardSoftScore finalScore = solution.getScore();

        // Visualize the solution
        printTimetable(solution);
        printTutorUtilization(solution);
        printTutorUtilisationSummary(solution);
        printTimeslotSummary(solution);
        printEmptyClassesSummary(solution);
        printCohortSummary(solution);
        printStudentsWithImpossibleSlots(solution);
        printImpossibleTimeslotsForTutors(solution);
        printClassroomsOutsideSizeRange(solution);
        printUnqualifiedTutorClassrooms(solution);
        printMixedCohortClassrooms(solution);
        // Print the score summary
        printScoreSummary(finalScore);
        // Write the final timetable to CSV
        writeTimetableToCSV(solution, "timetable.csv");

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
        System.out.println("");
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
            System.out.println("");
            System.out.println("########################### " + cohortLabel + " ###########################");
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
        System.out.println("");
    }

    public static void printScoreSummary(HardSoftScore score) {
        System.out.println("");
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
        System.out.println("");
        System.out.println("TIMETABLE");
        printSolution(timeTable);
    }

    public static void writeTimetableToCSV(TimeTable solution, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("student_id,cohort_label,level,platform,tutor,timeslot\n");

            // Sorting and writing each student assignment
            solution.getStudentAssignmentList().stream()
                    .sorted(Comparator.comparing((StudentAssignment a) -> a.getStudent().getCohort().getLabel())
                            .thenComparing(a -> a.getTutor().getName())
                            .thenComparing(a -> a.getTimeslot().toString()))
                    .forEach(assignment -> {
                        try {
                            Student student = assignment.getStudent();
                            Cohort cohort = student.getCohort();
                            Tutor tutor = assignment.getTutor();
                            Timeslot timeslot = assignment.getTimeslot();

                            String line = String.join(",",
                                    student.getId(),
                                    cohort.getLabel(),
                                    cohort.getLevel().toString(),
                                    cohort.getPlatform().toString(),
                                    tutor.getName(),
                                    timeslot.toString()) + "\n";

                            writer.write(line);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void printTutorUtilization(TimeTable solution) {
        System.out.println("");
        System.out.println("TUTOR UTILISATION SUMMARY");
        // Count the number of unique timeslots for each tutor
        Map<Tutor, Set<Timeslot>> tutorTimeslotMap = new HashMap<>();
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            tutorTimeslotMap.computeIfAbsent(tutor, k -> new HashSet<>()).add(timeslot);
        }

        // List all tutors to track the ones not used
        Set<Tutor> allTutors = new HashSet<>(solution.getTutorList());

        // Calculate the utilization percentage and print
        tutorTimeslotMap.entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(
                        entry2.getValue().size() * 100 / entry2.getKey().getMaxLessons(), // Descending order
                        entry1.getValue().size() * 100 / entry1.getKey().getMaxLessons()))
                .forEach(entry -> {
                    Tutor tutor = entry.getKey();
                    allTutors.remove(tutor); // Remove this tutor from the set of all tutors (as they are used)
                    int timeslotCount = entry.getValue().size();
                    int maxLessons = tutor.getMaxLessons();
                    int percentageUsed = (int) ((double) timeslotCount / maxLessons * 100);
                    System.out.println(
                            tutor.getName() + ": " + percentageUsed + "% (" + timeslotCount + " / " + maxLessons + ")");
                });

        // Print tutors not used at all
        if (!allTutors.isEmpty()) {
            System.out.println("\nTutors not used:");
            allTutors.forEach(tutor -> System.out.println(
                    tutor.getName() + ": 0 timeslots allocated out of " + tutor.getMaxLessons() + " max timeslots"));
        }
        System.out.println("");
    }

    public static void printTutorUtilisationSummary(TimeTable solution) {
        System.out.println("Tutor Utilisation Summary:");

        Map<Tutor, Set<String>> tutorClassesMap = new HashMap<>();
        Map<Tutor, Map<String, Set<String>>> tutorCohortsMap = new HashMap<>();
        Map<Tutor, Integer> tutorCohortsCountMap = new HashMap<>();

        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            String timeslot = abbreviateDay(assignment.getTimeslot().getDayOfWeek().toString()) + " "
                    + assignment.getTimeslot().getStartTime().toString();
            String cohortLabel = assignment.getStudent().getCohort().getLabel();
            String classInfo = cohortLabel + " - " + timeslot;

            tutorClassesMap.computeIfAbsent(tutor, k -> new HashSet<>()).add(classInfo);
            tutorCohortsMap.computeIfAbsent(tutor, k -> new HashMap<>())
                    .computeIfAbsent(cohortLabel, k -> new HashSet<>())
                    .add(timeslot);
        }

        // Count unique cohorts for each tutor
        tutorCohortsMap.forEach((tutor, cohorts) -> tutorCohortsCountMap.put(tutor, cohorts.size()));

        printTutorSummary(tutorClassesMap, tutorCohortsMap, tutorCohortsCountMap, false);

        // Print summary for tutors teaching more than 4 cohorts
        System.out.println("Tutors teaching more than 4 cohorts:");
        printTutorSummary(tutorClassesMap, tutorCohortsMap, tutorCohortsCountMap, true);
    }

    private static void printTutorSummary(Map<Tutor, Set<String>> tutorClassesMap,
            Map<Tutor, Map<String, Set<String>>> tutorCohortsMap,
            Map<Tutor, Integer> tutorCohortsCountMap,
            boolean filterMoreThanFour) {
        tutorClassesMap.forEach((tutor, classes) -> {
            if (!filterMoreThanFour || tutorCohortsCountMap.get(tutor) > 4) {
                System.out.println(tutor.getName() + ":");
                System.out.println("  Classes: " + String.join(", ", classes));

                int totalCohorts = tutorCohortsCountMap.get(tutor);
                String cohortSummary = tutorCohortsMap.get(tutor).entrySet().stream()
                        .map(entry -> entry.getKey() + " (" + entry.getValue().size() + ")")
                        .collect(Collectors.joining(", "));
                System.out.println("  Cohorts: " + totalCohorts + " -- " + cohortSummary);
                System.out.println();
            }
        });
    }

    private static String abbreviateDay(String day) {
        return day.substring(0, 3).toUpperCase();
    }

    public static void printTimeslotSummary(TimeTable solution) {
        System.out.println("");
        System.out.println("TIMESLOT SUMMARY");
        // Assuming Timeslot has methods getDayOfWeek() and getStartTime()
        Map<Timeslot, Map<Tutor, Set<Student>>> timeslotTutorStudentMap = new HashMap<>();
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            Student student = assignment.getStudent();
            timeslotTutorStudentMap.computeIfAbsent(timeslot, k -> new HashMap<>())
                    .computeIfAbsent(tutor, k -> new HashSet<>())
                    .add(student);
        }

        // Prepare grid data
        Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
        Set<LocalTime> times = new TreeSet<>(Comparator.comparing(LocalTime::toString));
        times.addAll(solution.getTimeslotList().stream().map(Timeslot::getStartTime).collect(Collectors.toSet()));

        // Initialize counts
        Map<DayOfWeek, Map<LocalTime, Integer>> classCounts = new HashMap<>();
        Map<DayOfWeek, Map<LocalTime, Integer>> studentCounts = new HashMap<>();
        Map<DayOfWeek, Integer> totalClassCountsPerDay = new HashMap<>();
        Map<DayOfWeek, Integer> totalStudentCountsPerDay = new HashMap<>();
        for (DayOfWeek day : days) {
            classCounts.put(day, new HashMap<>());
            studentCounts.put(day, new HashMap<>());
            totalClassCountsPerDay.put(day, 0);
            totalStudentCountsPerDay.put(day, 0);
            for (LocalTime time : times) {
                classCounts.get(day).put(time, 0);
                studentCounts.get(day).put(time, 0);
            }
        }

        // Populate grid data
        for (Map.Entry<Timeslot, Map<Tutor, Set<Student>>> timeslotEntry : timeslotTutorStudentMap.entrySet()) {
            DayOfWeek day = timeslotEntry.getKey().getDayOfWeek();
            LocalTime time = timeslotEntry.getKey().getStartTime();
            int classCount = timeslotEntry.getValue().size();
            int studentCount = timeslotEntry.getValue().values().stream().mapToInt(Set::size).sum();
            classCounts.get(day).put(time, classCounts.get(day).get(time) + classCount);
            studentCounts.get(day).put(time, studentCounts.get(day).get(time) + studentCount);
            totalClassCountsPerDay.put(day, totalClassCountsPerDay.get(day) + classCount);
            totalStudentCountsPerDay.put(day, totalStudentCountsPerDay.get(day) + studentCount);
        }

        // Print the grid
        System.out.print("\t");
        days.forEach(day -> System.out.print(day.toString().substring(0, 3) + "\t"));
        System.out.println("TOTAL");
        for (LocalTime time : times) {
            System.out.print(time + "\t");
            int totalClasses = 0, totalStudents = 0;
            for (DayOfWeek day : days) {
                int classes = classCounts.get(day).get(time);
                int students = studentCounts.get(day).get(time);
                totalClasses += classes;
                totalStudents += students;
                System.out.print("(" + classes + " classes, " + students + " students)\t");
            }
            System.out.println("(" + totalClasses + " classes, " + totalStudents + " students)");
        }

        int totalStudentsPlaced = 0;
        // Print totals per day
        System.out.print("Total\t");
        for (DayOfWeek day : days) {
            int classes = totalClassCountsPerDay.get(day);
            int students = totalStudentCountsPerDay.get(day);
            totalStudentsPlaced += students;
            System.out.print("(" + classes + " classes, " + students + " students)\t");
        }
        System.out.println();

        // Print total number of "Classrooms" being run
        int totalClassrooms = timeslotTutorStudentMap.values().stream()
                .mapToInt(dayMap -> new HashSet<>(dayMap.values()).size()).sum();
        System.out.println("Total number of classrooms being run: " + totalClassrooms);
        System.out.println("Total number of students placed: " + totalStudentsPlaced);
        double averageStudentsPerClass = Math.round((double) totalStudentsPlaced / totalClassrooms * 100.0) / 100.0;
        System.out.println("Average students per class: " + averageStudentsPerClass);
        System.out.println("");
    }

    public static void printEmptyClassesSummary(TimeTable solution) {
        System.out.println("");
        System.out.println("EMPTY AVAILABLE CLASSES");
        // Initialize data structures
        Map<Tutor, Set<Timeslot>> tutorAvailabilityMap = new HashMap<>();
        for (Tutor tutor : solution.getTutorList()) {
            // Assume getAvailability returns a Map<Timeslot, Boolean> where true indicates
            // availability
            Set<Timeslot> availableTimeslots = tutor.getAvailability().entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            tutorAvailabilityMap.put(tutor, availableTimeslots);
        }

        Map<Timeslot, Set<Tutor>> assignedTutorsMap = new HashMap<>();
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Timeslot timeslot = assignment.getTimeslot();
            Tutor tutor = assignment.getTutor();
            assignedTutorsMap.computeIfAbsent(timeslot, k -> new HashSet<>()).add(tutor);
        }

        // Prepare grid data
        Set<DayOfWeek> days = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY);
        Set<LocalTime> times = new TreeSet<>(Comparator.comparing(LocalTime::toString));
        solution.getTimeslotList().forEach(timeslot -> times.add(timeslot.getStartTime()));

        // Initialize counts
        Map<DayOfWeek, Map<LocalTime, Integer>> emptyClassCounts = new HashMap<>();
        for (DayOfWeek day : days) {
            emptyClassCounts.put(day, new HashMap<>());
            for (LocalTime time : times) {
                emptyClassCounts.get(day).put(time, 0);
            }
        }

        // Calculate empty class counts
        for (Map.Entry<Tutor, Set<Timeslot>> entry : tutorAvailabilityMap.entrySet()) {
            Tutor tutor = entry.getKey();
            for (Timeslot timeslot : entry.getValue()) {
                if (!assignedTutorsMap.getOrDefault(timeslot, Collections.emptySet()).contains(tutor)) {
                    DayOfWeek day = timeslot.getDayOfWeek();
                    LocalTime time = timeslot.getStartTime();
                    emptyClassCounts.get(day).put(time, emptyClassCounts.get(day).get(time) + 1);
                }
            }
        }

        // Print the grid
        System.out.print("\t");
        days.forEach(day -> System.out.print(day.toString().substring(0, 3) + "\t"));
        System.out.println("TOTAL");
        for (LocalTime time : times) {
            System.out.print(time + "\t");
            int totalEmptyClasses = 0;
            for (DayOfWeek day : days) {
                int emptyClasses = emptyClassCounts.get(day).get(time);
                totalEmptyClasses += emptyClasses;
                System.out.print(emptyClasses + "\t");
            }
            System.out.println(totalEmptyClasses);
        }

        // Print total number of empty "Classrooms" being run
        int totalEmptyClassrooms = emptyClassCounts.values().stream()
                .flatMap(map -> map.values().stream())
                .mapToInt(Integer::intValue).sum();
        System.out.println("Total number of empty classrooms: " + totalEmptyClassrooms);
        System.out.println("");
    }

    public static void printCohortSummary(TimeTable solution) {
        System.out.println("\nCOHORT SUMMARY");

        // Map to hold cohort-wise counts of classrooms and students
        Map<Cohort, Map<String, List<Student>>> cohortClassroomStudentMap = new HashMap<>();

        // Populate the map
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Cohort cohort = assignment.getStudent().getCohort();
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            Student student = assignment.getStudent();

            String classroomKey = tutor.getName() + "-" + timeslot.toString();
            cohortClassroomStudentMap.computeIfAbsent(cohort, k -> new HashMap<>())
                    .computeIfAbsent(classroomKey, k -> new ArrayList<>())
                    .add(student);
        }

        // Process and print the summary for each cohort
        for (Map.Entry<Cohort, Map<String, List<Student>>> entry : cohortClassroomStudentMap.entrySet()) {
            Cohort cohort = entry.getKey();
            Map<String, List<Student>> classroomStudentMap = entry.getValue();

            int totalClassrooms = classroomStudentMap.size();
            int totalStudents = classroomStudentMap.values().stream().mapToInt(List::size).sum();
            List<Integer> classSizes = classroomStudentMap.values().stream().map(List::size)
                    .collect(Collectors.toList());
            double averageClassSize = totalStudents / (double) totalClassrooms;

            System.out.print(
                    cohort.getLabel() + ": \t" + totalClassrooms + " classrooms, \t" + totalStudents + " students: \t");
            System.out.printf("at %.2f students per classroom\t", averageClassSize);
            System.out.println(classSizes);
        }
        System.out.println();
    }

    public static void printStudentsWithImpossibleSlots(TimeTable solution) {
        System.out.println("");
        System.out.println("Students with impossible slots:");
        // Map to hold students with impossible timeslots, grouped by cohort
        Map<Cohort, List<String>> studentsByCohort = new HashMap<>();
        int totalStudentsInImpossibleSlots = 0;

        // Iterate over all student assignments
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Student student = assignment.getStudent();
            Timeslot timeslot = assignment.getTimeslot();
            Integer score = student.getAvailability().get(timeslot);

            // Check if the timeslot is impossible for the student
            if (score != null && (score == 0)) {
                totalStudentsInImpossibleSlots++;
                String difficulty = "impossible";
                String studentInfo = student.getId() + ": " + difficulty;

                // Group students by their cohort
                studentsByCohort.computeIfAbsent(student.getCohort(), k -> new ArrayList<>()).add(studentInfo);
            }
        }

        // Print students grouped by cohort
        studentsByCohort.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Cohort::getLabel)))
                .forEach(entry -> {
                    Cohort cohort = entry.getKey();
                    List<String> students = entry.getValue();

                    System.out.println("------------------ " + cohort.getLabel() + " ------------------");
                    students.forEach(System.out::println);
                    System.out.println("");
                });

        System.out.println("Total students in impossible slots: " + totalStudentsInImpossibleSlots);
        System.out.println("");
    }

    public static void printImpossibleTimeslotsForTutors(TimeTable solution) {
        System.out.println("Impossible timeslots for tutors:");
        System.out.println(" ");
        // Map to keep track of tutors with impossible timeslots
        Map<Tutor, List<String>> tutorsWithImpossibleTimeslots = new HashMap<>();

        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            Cohort cohort = assignment.getStudent().getCohort();

            if (!tutor.isAvailable(timeslot)) {
                String classInfo = cohort.getLabel() + " @ " + timeslot.toString();
                tutorsWithImpossibleTimeslots.computeIfAbsent(tutor, k -> new ArrayList<>()).add(classInfo);
            }
        }

        tutorsWithImpossibleTimeslots.forEach((tutor, impossibleClasses) -> {
            System.out.println("- - - " + tutor.getName());
            impossibleClasses.forEach(System.out::println);
        });
        System.out.println(" ");
    }

    public static void printClassroomsOutsideSizeRange(TimeTable solution) {
        System.out.println("");
        System.out.println("Classes that are too big or too small:");
        // Map to hold the count of students in each classroom (Tutor + Timeslot
        // combination)
        Map<Tutor, Map<Timeslot, Integer>> classroomStudentCounts = new HashMap<>();

        // Populate the map with student counts
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            classroomStudentCounts.computeIfAbsent(tutor, k -> new HashMap<>())
                    .merge(timeslot, 1, Integer::sum);
        }

        // Print classrooms that have less than 3 or more than 7 students
        classroomStudentCounts.forEach((tutor, timeslotCounts) -> {
            timeslotCounts.forEach((timeslot, count) -> {
                if (count < 3 || count > 7) {
                    System.out.println(tutor.getName() + ", " + timeslot.toString() + ": " + count + " students");
                }
            });
        });
        System.out.println("");
    }

    public static void printUnqualifiedTutorClassrooms(TimeTable solution) {
        System.out.println("");
        System.out.println("Unqualified tutor assignments:");
        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            Student student = assignment.getStudent();
            Cohort cohort = student.getCohort();
            Level level = cohort.getLevel();
            Platform platform = cohort.getPlatform();

            if (!tutor.canTeach(level)) {
                System.out.println(tutor.getName() + ", " + timeslot.toString() +
                        ": Tutor cannot teach " + level);
            }
            if (!tutor.canTeachOnPlatform(platform)) {
                System.out.println(tutor.getName() + ", " + timeslot.toString() +
                        ": Tutor cannot teach on " + platform);
            }
        }
        System.out.println("");
    }

    public static void printMixedCohortClassrooms(TimeTable solution) {
        System.out.println("");
        System.out.println("Mixed cohorts:");
        Map<String, Set<String>> classroomCohorts = new HashMap<>();

        for (StudentAssignment assignment : solution.getStudentAssignmentList()) {
            Tutor tutor = assignment.getTutor();
            Timeslot timeslot = assignment.getTimeslot();
            String cohortLabel = assignment.getStudent().getCohort().getLabel();

            String classroomKey = tutor.getName() + ", " + timeslot.toString();
            classroomCohorts.computeIfAbsent(classroomKey, k -> new HashSet<>()).add(cohortLabel);
        }

        classroomCohorts.forEach((classroomKey, cohorts) -> {
            if (cohorts.size() > 1) {
                System.out.println(classroomKey + ": " + cohorts);
            }
        });
        System.out.println("");
    }
}
