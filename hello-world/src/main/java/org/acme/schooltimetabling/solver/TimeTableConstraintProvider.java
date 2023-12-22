package org.acme.schooltimetabling.solver;

import org.acme.schooltimetabling.domain.StudentAssignment;
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
                studentAvailability(constraintFactory),
                sameCohortInClassroom(constraintFactory),
                tutorAvailability(constraintFactory),
                tutorLevelProficiency(constraintFactory),
                tutorPlatformProficiency(constraintFactory),
                minimumStudentsInClassroom(constraintFactory),
                maximumStudentsInClassroom(constraintFactory),

                // Soft constraints
                goodNumberOfStudentsInClassroom(constraintFactory),
                // teacherTimeEfficiency(constraintFactory),
                // studentGroupSubjectVariety(constraintFactory)
        };
    }

    Constraint sameCohortInClassroom(ConstraintFactory constraintFactory) {
        // A classroom is defined by a unique combination of tutor and timeslot.
        // All students in the same classroom must have the same cohort.
        return constraintFactory.forEachUniquePair(StudentAssignment.class,
                Joiners.equal(StudentAssignment::getTutor),
                Joiners.equal(StudentAssignment::getTimeslot))
                .filter((assignment1,
                        assignment2) -> !assignment1.getStudent().getCohort()
                                .equals(assignment2.getStudent().getCohort()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Students in classroom must be from the same cohort");
    }

    Constraint minimumStudentsInClassroom(ConstraintFactory constraintFactory) {
        // There must be a minimum of 3 students associated with the same Tutor and
        // Timeslot.
        return constraintFactory.forEach(StudentAssignment.class)
                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot, ConstraintCollectors.count())
                // Filter for groups that have less than the minimum required student count
                // Empty classrooms are absolutely fine.
                .filter((tutor, timeslot, count) -> count == 1 || count == 2)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Minimum 3 students in a classroom");
    }

    Constraint maximumStudentsInClassroom(ConstraintFactory constraintFactory) {
        // There must be a maximum of 7 students associated with the same Tutor and
        // Timeslot.
        return constraintFactory.forEach(StudentAssignment.class)
                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot, ConstraintCollectors.count())
                // Filter for groups that have more than the maximum allowed student count
                .filter((tutor, timeslot, count) -> count > 7)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Maximum 7 students in a classroom");
    }

    Constraint tutorAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(StudentAssignment.class)
                .filter(assignment -> !assignment.getTutor().isAvailable(assignment.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Tutor must be available to teach in the timeslot");
    }

    Constraint tutorLevelProficiency(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(StudentAssignment.class)
                .filter(assignment -> !assignment.getTutor().canTeach(assignment.getStudent().getCohort().getLevel()))
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

    Constraint studentAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(StudentAssignment.class)
                .filter(assignment -> assignment.getStudent().getAvailability().getOrDefault(assignment.getTimeslot(),
                        4) == 4)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student must be available for the timeslot");
    }

    Constraint goodNumberOfStudentsInClassroom(ConstraintFactory constraintFactory) {
        // Ideally, there are 5 or 6 students in a classroom.
        return constraintFactory.forEach(StudentAssignment.class)
                // Group by Tutor and Timeslot to define a conceptual "Classroom"
                .groupBy(StudentAssignment::getTutor, StudentAssignment::getTimeslot, ConstraintCollectors.count())
                // Filter for groups that have more than the maximum allowed student count
                .filter((tutor, timeslot, count) -> count == 4)
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("This class of 4 students is a bit small.");
    }

    Constraint studentAvailabilityScore(ConstraintFactory constraintFactory) {
        // Penalize assigning students to timeslots with low availability scores.
        return constraintFactory.forEach(StudentAssignment.class)
                // Check the availability score for the assigned timeslot.
                .filter(assignment -> {
                    Integer score = assignment.getStudent().getAvailability().get(assignment.getTimeslot());
                    // Only penalize if the score is 2 (doable, but not first prize) or 3
                    // (difficult).
                    return score != null && (score == 2 || score == 3);
                })
                // Apply different penalties for scores of 1 and 2.
                .penalize("Student availability score penalty", HardSoftScore.ONE_SOFT,
                        assignment -> {
                            Integer score = assignment.getStudent().getAvailability().get(assignment.getTimeslot());
                            return (score != null && score == 1) ? 2 : 1; // Higher penalty for score of 1.
                        });
    }

}
