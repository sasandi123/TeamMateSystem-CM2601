package teammate.concurrent;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.service.TeamBuilder;
import teammate.util.SystemLogger;
import java.util.*;
import java.util.concurrent.*;

// Manages team formation with automatic selection between sequential and parallel processing
public class TeamFormationEngine {
    private TeamBuilder teamBuilder;
    private static final int PARALLEL_THRESHOLD = 30;
    private static final int OPTIMAL_THREADS = 4;

    public TeamFormationEngine(TeamBuilder teamBuilder) {
        this.teamBuilder = teamBuilder;
    }

    // Builds teams using the most efficient processing mode based on participant count
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
            return buildTeamsParallel(participants, teamSize);
        } else {
            SystemLogger.info("Using SEQUENTIAL processing mode");
            return buildTeamsSequential(participants, teamSize);
        }
    }

    // Processes team formation sequentially for small datasets
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

    // Processes team formation in parallel for large datasets
    private int buildTeamsParallel(List<Participant> participants, int teamSize)
            throws InterruptedException, ExecutionException {

        // Calculate global target skill for consistent team balancing across all batches
        int totalSkill = 0;
        for (Participant p : participants) {
            totalSkill += p.getSkillLevel();
        }
        int expectedTeams = Math.max(1, participants.size() / teamSize);
        double globalTargetSkill = (double) totalSkill / (expectedTeams * teamSize);

        SystemLogger.info("Global target skill: " + String.format("%.2f", globalTargetSkill));

        // Set target skill in TeamBuilder for display purposes
        teamBuilder.setOverallAverageSkill(globalTargetSkill);

        int actualThreads = Math.min(OPTIMAL_THREADS,
                Math.max(2, participants.size() / (teamSize * 3)));

        ExecutorService executor = Executors.newFixedThreadPool(actualThreads);
        List<Future<List<Team>>> futures = new ArrayList<>();

        try {
            int batchSize = Math.max(teamSize * 3, participants.size() / actualThreads);
            List<List<Participant>> batches = divideToBatches(participants, batchSize);

            // Submit batch processing tasks
            for (List<Participant> batch : batches) {
                BatchProcessor task = new BatchProcessor(batch, teamSize, globalTargetSkill);
                futures.add(executor.submit(task));
            }

            // Collect results from all batches
            List<Team> allTeams = new ArrayList<>();
            for (Future<List<Team>> future : futures) {
                allTeams.addAll(future.get());
            }

            // Add teams sequentially to ensure thread-safe ID assignment
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

    // Divides participants into batches for parallel processing
    private List<List<Participant>> divideToBatches(List<Participant> participants, int batchSize) {
        List<List<Participant>> batches = new ArrayList<>();

        for (int i = 0; i < participants.size(); i += batchSize) {
            int end = Math.min(i + batchSize, participants.size());
            batches.add(new ArrayList<>(participants.subList(i, end)));
        }

        return batches;
    }
}