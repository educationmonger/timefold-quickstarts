package org.acme.schooltimetabling.domain;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVReaderUtility {

    public static List<Cohort> readCohorts(String filePath) throws IOException {
        List<Cohort> cohorts = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            reader.readLine(); // skip the header row

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String label = tokens[0].trim();
                Level level = Level.valueOf(tokens[2].trim().toUpperCase());
                Platform platform = Platform.valueOf(tokens[3].trim().toUpperCase());
                cohorts.add(new Cohort(label, level, platform));
            }
        }
        return cohorts;
    }

    public static List<Tutor> readTutors(String filePath, List<Timeslot> timeslots) throws IOException {
        List<Tutor> tutors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            reader.readLine(); // Skip header line

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String name = tokens[0].trim();

                EnumSet<Level> levelProficiencies = parseLevelProficiencies(tokens, 6, 10);
                EnumSet<Platform> platformProficiencies = parsePlatformProficiencies(tokens, 3, 5);
                Map<Timeslot, Boolean> availability = parseAvailability(tokens, timeslots, 15, 26);

                int idealLessons = Integer.parseInt(tokens[1].trim());
                int maxLessons = Integer.parseInt(tokens[2].trim());

                tutors.add(new Tutor(name, levelProficiencies, platformProficiencies, availability, idealLessons,
                        maxLessons));
            }
        }
        return tutors;
    }

    private static EnumSet<Level> parseLevelProficiencies(String[] tokens, int start, int end) {
        EnumSet<Level> proficiencies = EnumSet.noneOf(Level.class);
        // Assume Level enum order matches CSV column order
        Level[] levels = Level.values();
        for (int i = start; i <= end; i++) {
            if (Boolean.parseBoolean(tokens[i].trim())) {
                proficiencies.add(levels[i - start]);
            }
        }
        return proficiencies;
    }

    private static EnumSet<Platform> parsePlatformProficiencies(String[] tokens, int start, int end) {
        EnumSet<Platform> proficiencies = EnumSet.noneOf(Platform.class);
        // Assume Platform enum order matches CSV column order
        Platform[] platforms = Platform.values();
        for (int i = start; i <= end; i++) {
            if (Boolean.parseBoolean(tokens[i].trim())) {
                proficiencies.add(platforms[i - start]);
            }
        }
        return proficiencies;
    }

    private static Map<Timeslot, Boolean> parseAvailability(String[] tokens, List<Timeslot> timeslots, int start,
            int end) {
        Map<Timeslot, Boolean> availability = new HashMap<>();
        for (int i = start; i <= end; i++) {
            availability.put(timeslots.get(i - start), Boolean.parseBoolean(tokens[i].trim()));
        }
        return availability;
    }

    public static List<Student> readStudents(String filePath, List<Timeslot> timeslots)
            throws IOException {
        List<Student> students = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {

            String line;
            reader.readLine(); // skip the header row

            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String studentId = tokens[0].trim();
                String cohortLabel = tokens[14].trim();
                Cohort cohort = Cohort.getCohort(cohortLabel);
                Map<Timeslot, Integer> availability = new HashMap<>();

                for (int i = 0; i < timeslots.size(); i++) {
                    int score = Integer.parseInt(tokens[i + 1].trim()); // +1 to start at the right column
                    availability.put(timeslots.get(i), score);
                }

                if (cohort != null) {
                    students.add(new Student(studentId, cohort, availability));
                }
            }
        }
        return students;
    }

}
