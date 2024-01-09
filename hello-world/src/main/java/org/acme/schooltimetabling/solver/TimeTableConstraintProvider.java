package org.acme.schooltimetabling.solver;

import java.time.LocalTime;

import org.acme.schooltimetabling.domain.StudentAssignment;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.Tutor;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
// import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;

public class TimeTableConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
                return new Constraint[] {

                                // Hard constraints
                                sameCohortInClassroom(constraintFactory),
                                tutorAvailability(constraintFactory),
                                tutorLevelProficiency(constraintFactory),
                                tutorPlatformProficiency(constraintFactory),
                                // maximumStudentsInClassroom(constraintFactory),
                                maxTutorLessons(constraintFactory),
                                // minimumStudentsInClassroom(constraintFactory),
                                // limitTotalNumberOfClassrooms(constraintFactory),
                                // studentAvailabilityHard(constraintFactory),
                                // limitExcessStudentsInImpossibleTimeslots(constraintFactory),
                                // ensureMinimumStudentsAtEachStartTime(constraintFactory),

                                // Soft constraints
                                studentAvailabilitySoft(constraintFactory),
                                goodNumberOfStudentsInClassroom(constraintFactory),
                                // idealTutorLessons(constraintFactory),
                                // studentAvailabilityScore(constraintFactory),
                                // minTutorLessons(constraintFactory),
                                rewardAssignmentsAt1530(constraintFactory),
                                limitTutorCohorts(constraintFactory),
                };
        }

        // HARD CONSTRAINTS

        Constraint sameCohortInClassroom(ConstraintFactory constraintFactory) {
                // A classroom is defined by a unique combination of tutor and timeslot.
                // All students in the same classroom must have the same cohort.
                return constraintFactory.forEachUniquePair(StudentAssignment.class,
                                Joiners.equal(StudentAssignment::getTutor),
                                Joiners.equal(StudentAssignment::getTimeslot))
                                .filter((assignment1,
                                                assignment2) -> !assignment1.getStudent().getCohort()
                                                                .equals(assignment2.getStudent().getCohort()))
                                .penalize(HardSoftScore.ofHard(4))
                                .asConstraint("Students in classroom must be from the same cohort");
        }

        Constraint maximumStudentsInClassroom(ConstraintFactory constraintFactory) {
                // There must be a maximum of 7 students associated with the same Tutor and
                // Timeslot.
                final int maxStudentsPerClassroom = 7;

                return constraintFactory.forEach(StudentAssignment.class)
                                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot,
                                                ConstraintCollectors.count())
                                // Filter for groups that have more than the maximum allowed student count
                                .filter((tutor, timeslot, count) -> count > maxStudentsPerClassroom)
                                // Penalize by the number of students exceeding the maximum limit
                                .penalize("Maximum 7 students in a classroom", HardSoftScore.ONE_HARD,
                                                (tutor, timeslot, count) -> count - maxStudentsPerClassroom);
        }

        Constraint tutorAvailability(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> !assignment.getTutor().isAvailable(assignment.getTimeslot()))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Tutor must be available to teach in the timeslot");
        }

        Constraint tutorLevelProficiency(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> !assignment.getTutor()
                                                .canTeach(assignment.getStudent().getCohort().getLevel()))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Tutor must be proficient at the cohort's level");
        }

        Constraint tutorPlatformProficiency(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> !assignment.getTutor()
                                                .canTeachOnPlatform(assignment.getStudent().getCohort().getPlatform()))
                                .penalize(HardSoftScore.ONE_HARD)
                                .asConstraint("Tutor must be proficient at the cohort's platform");
        }

        Constraint maxTutorLessons(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                // Group by Tutor and count the number of lessons (assignments) they are
                                // teaching
                                .groupBy(StudentAssignment::getTutor, ConstraintCollectors.count())
                                // Apply a filter to only include cases where the count exceeds the tutor's
                                // maximum limit
                                .filter((tutor, count) -> count > tutor.getMaxLessons())
                                // Penalize by the number of lessons exceeding the tutor's maximum limit
                                .penalize("Tutor maximum lessons exceeded", HardSoftScore.ONE_HARD,
                                                (tutor, count) -> count - tutor.getMaxLessons());
        }

        Constraint minimumStudentsInClassroom(ConstraintFactory constraintFactory) {
                // Define the minimum number of students per classroom
                final int minStudentsPerClassroom = 3;

                return constraintFactory.forEach(StudentAssignment.class)
                                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot,
                                                ConstraintCollectors.count())
                                // Filter for groups that have less than the minimum required student count
                                // Note: Empty classrooms are not penalized
                                .filter((tutor, timeslot, count) -> count > 0 && count < minStudentsPerClassroom)
                                // Penalize by the number of students below the minimum limit
                                .penalize("Minimum 4 students in a classroom", HardSoftScore.ONE_HARD,
                                                (tutor, timeslot, count) -> minStudentsPerClassroom - count);
        }

        public Constraint limitTotalNumberOfClassrooms(ConstraintFactory constraintFactory) {
                final int maxClassrooms = 90;

                return constraintFactory.forEach(StudentAssignment.class)
                                .groupBy((assignment) -> assignment.getTutor().getName() + "-"
                                                + assignment.getTimeslot().toString(),
                                                ConstraintCollectors.count())
                                .filter((tutorTimeslotKey, count) -> count > 0)
                                .penalize("Limit total number of classrooms", HardSoftScore.ONE_HARD,
                                                (tutorTimeslotKey, count) -> calculatePenaltyForExcessClassrooms(count,
                                                                maxClassrooms));
        }

        private int calculatePenaltyForExcessClassrooms(int count, int maxClassrooms) {
                // Penalize for each classroom beyond the max limit
                return 4 * Math.max(0, count - maxClassrooms);
        }

        public Constraint limitExcessStudentsInImpossibleTimeslots(ConstraintFactory constraintFactory) {
                final int maxAllowedStudentsInImpossibleSlots = 50;

                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> assignment.getStudent().getAvailability()
                                                .get(assignment.getTimeslot()) == 0)
                                .groupBy(ConstraintCollectors.count())
                                .penalize("Limit excess students in impossible timeslots", HardSoftScore.ONE_HARD,
                                                (Integer count) -> {
                                                        int excessCount = count - maxAllowedStudentsInImpossibleSlots;
                                                        return excessCount > 0 ? excessCount : 0;
                                                });
        }

        public Constraint ensureMinimumStudentsAtEachStartTime(ConstraintFactory constraintFactory) {
                final int minStudentsPerStartTime = 100;

                return constraintFactory.forEach(StudentAssignment.class)
                                .groupBy(assignment -> assignment.getTimeslot().getStartTime(),
                                                ConstraintCollectors.count())
                                .filter((startTime, count) -> count < minStudentsPerStartTime)
                                .penalize("Minimum students at each start time", HardSoftScore.ONE_HARD,
                                                (startTime, count) -> minStudentsPerStartTime - count);
        }

        Constraint studentAvailabilityHard(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> assignment.getStudent().getAvailability().getOrDefault(
                                                assignment.getTimeslot(),
                                                0) == 0)
                                .penalize(HardSoftScore.ofHard(2))
                                .asConstraint("Student must be available for the timeslot");
        }

        // SOFT CONSTRAINTS

        Constraint studentAvailabilitySoft(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .filter(assignment -> assignment.getStudent().getAvailability().getOrDefault(
                                                assignment.getTimeslot(),
                                                0) == 0)
                                .penalize(HardSoftScore.ofSoft(50))
                                .asConstraint("Student must be available for the timeslot");
        }

        Constraint minTutorLessons(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .groupBy(StudentAssignment::getTutor, ConstraintCollectors.count())
                                .filter((tutor, count) -> count < 3)
                                .penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("This tutor is too few classes.");
        }

        Constraint idealTutorLessons(ConstraintFactory constraintFactory) {
                return constraintFactory.forEach(StudentAssignment.class)
                                .groupBy(StudentAssignment::getTutor, ConstraintCollectors.count())
                                .filter((tutor, count) -> count > tutor.getIdealLessons())
                                .penalize(HardSoftScore.ONE_SOFT)
                                .asConstraint("This tutor is teaching more than thier ideal load");
        }

        Constraint goodNumberOfStudentsInClassroom(ConstraintFactory constraintFactory) {
                // The optimal class size range is 6 or 7 students
                final int optimalLowerBound = 6;
                final int optimalUpperBound = 7;
                final int classSizeWeighting = 20;

                return constraintFactory.forEach(StudentAssignment.class)
                                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot,
                                                ConstraintCollectors.count())
                                // Apply a penalty based on the distance from the optimal class size range
                                .penalize("Optimal class size range penalty", HardSoftScore.ONE_SOFT,
                                                (tutor, timeslot, count) -> {
                                                        if (count >= optimalLowerBound && count <= optimalUpperBound) {
                                                                return 0; // No penalty for class sizes within the
                                                                          // optimal range
                                                        } else {
                                                                return classSizeWeighting
                                                                                * Math.abs(count - optimalUpperBound); // Penalty
                                                                // increases
                                                                // the
                                                                // further
                                                                // away from
                                                                // 7
                                                        }
                                                });
        }

        Constraint studentAvailabilityScore(ConstraintFactory constraintFactory) {
                // Penalize assigning students to timeslots with low availability scores.
                return constraintFactory.forEach(StudentAssignment.class)
                                // Check the availability score for the assigned timeslot.
                                .filter(assignment -> {
                                        Integer score = assignment.getStudent().getAvailability()
                                                        .get(assignment.getTimeslot());
                                        // Only penalize if the score is 2 (doable) or 1
                                        // (doable, but not first prize).
                                        return score != null && (score == 2 || score == 1);
                                })
                                // Apply different penalties for scores of 1 and 2.
                                .penalize("Student availability score penalty", HardSoftScore.ONE_SOFT,
                                                assignment -> {
                                                        Integer score = assignment.getStudent().getAvailability()
                                                                        .get(assignment.getTimeslot());
                                                        return (score != null && score == 1) ? 2 : 1; // Higher penalty
                                                                                                      // for score of 1.
                                                });
        }

        // Soft constraint to encourage assignments in the 15:30 timeslots
        public Constraint rewardAssignmentsAt1530(ConstraintFactory constraintFactory) {
                LocalTime targetStartTime = LocalTime.of(15, 30);

                return constraintFactory.forEach(StudentAssignment.class)
                                // Filter for assignments at the 15:30 timeslot
                                .filter(assignment -> assignment.getTimeslot().getStartTime().equals(targetStartTime))
                                // Reward each assignment in this timeslot
                                .reward("Reward assignments at 15:30", HardSoftScore.ofSoft(10));
        }

        // Soft constraint to discourage tutors from teaching too many unique cohorts
        Constraint limitTutorCohorts(ConstraintFactory constraintFactory) {
                final int noPenaltyCohortUpperLimit = 3;
                final int numCohortsWeighting = 500;

                return constraintFactory.forEach(StudentAssignment.class)
                                // Group by tutor and count unique timeslots to define unique "classrooms"
                                .groupBy(StudentAssignment::getTutor,
                                                ConstraintCollectors.toSet((assignment) -> assignment.getTimeslot()),
                                                ConstraintCollectors.toSet(
                                                                (assignment) -> assignment.getStudent().getCohort()))
                                .filter((tutor, uniqueTimeslots, cohorts) -> cohorts.size() > noPenaltyCohortUpperLimit
                                                && cohorts.size() > uniqueTimeslots.size() / 2)
                                .penalize("Limit tutor cohorts", HardSoftScore.ONE_SOFT,
                                                (tutor, uniqueTimeslots, cohorts) -> numCohortsWeighting
                                                                * (cohorts.size() - Math.max(noPenaltyCohortUpperLimit,
                                                                                uniqueTimeslots.size() / 2)));
        }

}
