package teammate.concurrent;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.service.TeamBuilder;
import teammate.util.SystemLogger;
import java.util.*;
import java.util.concurrent.*;

/**
 * Unified engine for team formation with automatic mode selection
 * Handles both sequential and parallel processing intelligently
 * CRITICAL: Calculates GLOBAL target skill for consistent team balancing
 */
public class TeamFormationEngine {
    private TeamBuilder teamBuilder;
    private static final int PARALLEL_THRESHOLD = 30; // Participants
    private static final int OPTIMAL_THREADS = 4;

    public TeamFormationEngine(TeamBuilder teamBuilder) {
        this.teamBuilder = teamBuilder;
    }

    /**
     * Builds teams automatically selecting best processing mode
     */
    public int buildTeams(List<Participant> participants, int teamSize)
            throws InterruptedException, ExecutionException {

        SystemLogger.info("Team formation started: " + participants.size() + " participants");

        if (participants.size() < teamSize) {
            SystemLogger.warning("Insufficient participants");
            return 0;
        }

        boolean useParallel = participants.size() >= PARALLEL_THRESHOLD;

        if (useParallel) {
            SystemLogger.info("Using PARALLEL processing mode");
            System.out.println("Using parallel processing...");
            return buildTeamsParallel(participants, teamSize);
        } else {
            SystemLogger.info("Using SEQUENTIAL processing mode");
            System.out.println("Using sequential processing...");
            return buildTeamsSequential(participants, teamSize);
        }
    }

    /**
     * Sequential processing for small datasets
     */
    private int buildTeamsSequential(List<Participant> participants, int teamSize)
            throws InterruptedException, ExecutionException {

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<Integer> future = executor.submit(() -> {
                teamBuilder.buildTeams(participants, teamSize);
                return teamBuilder.getTeamCount();
            });

            int teamCount = future.get();
            SystemLogger.success("Sequential formation complete: " + teamCount + " teams");
            return teamCount;

        } finally {
            executor.shutdown();
        }
    }

    /**
     * Parallel processing for large datasets
     * CRITICAL: Calculates GLOBAL target skill to ensure all teams balance consistently
     */
    private int buildTeamsParallel(List<Participant> participants, int teamSize)
            throws InterruptedException, ExecutionException {

        // CRITICAL FIX: Calculate GLOBAL target skill for ALL participants
        int totalSkill = 0;
        for (Participant p : participants) {
            totalSkill += p.getSkillLevel();
        }
        int expectedTeams = Math.max(1, participants.size() / teamSize);
        double globalTargetSkill = (double) totalSkill / (expectedTeams * teamSize);

        // Show global target skill to user
        System.out.println("Target average skill per team: " + String.format("%.2f", globalTargetSkill));
        SystemLogger.info("Global target skill: " + String.format("%.2f", globalTargetSkill));

        // CRITICAL: Set target skill in TeamBuilder so it displays when viewing teams
        teamBuilder.setOverallAverageSkill(globalTargetSkill);

        int actualThreads = Math.min(OPTIMAL_THREADS,
                Math.max(2, participants.size() / (teamSize * 3)));

        System.out.println("Processing with " + actualThreads + " concurrent threads...");

        ExecutorService executor = Executors.newFixedThreadPool(actualThreads);
        List<Future<List<Team>>> futures = new ArrayList<>();

        try {
            int batchSize = Math.max(teamSize * 3, participants.size() / actualThreads);
            List<List<Participant>> batches = divideToBatches(participants, batchSize);

            for (List<Participant> batch : batches) {
                // Pass GLOBAL target skill to ensure consistency across all batches
                BatchProcessor task = new BatchProcessor(batch, teamSize, globalTargetSkill);
                futures.add(executor.submit(task));
            }

            List<Team> allTeams = new ArrayList<>();
            for (Future<List<Team>> future : futures) {
                allTeams.addAll(future.get());
            }

            // CRITICAL STEP: Teams are added sequentially here to ensure
            // the finalizeTeamId() call in TeamBuilder is thread-safe.
            for (Team team : allTeams) {
                teamBuilder.addTeam(team);
            }

            SystemLogger.success("Parallel formation complete: " + allTeams.size() + " teams");
            return allTeams.size();

        } finally {
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private List<List<Participant>> divideToBatches(List<Participant> participants, int batchSize) {
        List<List<Participant>> batches = new ArrayList<>();

        for (int i = 0; i < participants.size(); i += batchSize) {
            int end = Math.min(i + batchSize, participants.size());
            batches.add(new ArrayList<>(participants.subList(i, end)));
        }

        return batches;
    }
}