package teammate.concurrent;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.service.TeamBuilder;
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

        if (participants.size() < teamSize) {
            return 0;
        }

        boolean useParallel = participants.size() >= PARALLEL_THRESHOLD;

        System.out.println("Processing " + participants.size() + " participants...");

        if (useParallel) {
            return buildTeamsParallel(participants, teamSize);
        } else {
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

            return future.get();
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

        System.out.println("[System] Global target skill: " +
                String.format("%.2f", globalTargetSkill));

        int actualThreads = Math.min(OPTIMAL_THREADS,
                Math.max(2, participants.size() / (teamSize * 3)));

        ExecutorService executor = Executors.newFixedThreadPool(actualThreads);
        List<Future<List<Team>>> futures = new ArrayList<>();

        try {
            int batchSize = Math.max(teamSize * 3, participants.size() / actualThreads);
            List<List<Participant>> batches = divideToBatches(participants, batchSize);

            System.out.println("[System] Using " + actualThreads + " threads for " +
                    batches.size() + " batches");

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