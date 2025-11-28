package teammate.concurrent;

import teammate.entity.Participant;
import teammate.util.ValidationUtil;
import teammate.util.PersonalityClassifier;
import teammate.util.SystemLogger;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Concurrent processor for survey data
 * Handles both CSV file processing AND individual survey submissions
 */
public class SurveyDataProcessor {
    private static final int THREAD_POOL_SIZE = 4;

    // ==================== CSV PROCESSING ====================

    /**
     * Process CSV file concurrently, validating and parsing participant data
     */
    public ProcessingResult processCSVConcurrently(String filename,
                                                   List<Participant> existingParticipants)
            throws Exception {

        SystemLogger.info("Starting CSV processing: " + filename);

        List<String> lines = readAllLines(filename);

        if (lines.isEmpty()) {
            SystemLogger.error("CSV file is empty");
            throw new Exception("CSV file is empty");
        }

        // Remove header
        lines.remove(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<ParticipantResult>> futures = new ArrayList<>();

        System.out.println("\n[System] Processing " + lines.size() +
                " records using " + THREAD_POOL_SIZE + " threads...");

        // Submit all lines for concurrent processing
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            final int lineNumber = i + 2;

            futures.add(executor.submit(() ->
                    processLine(line, lineNumber, existingParticipants)));
        }

        // Collect results
        ProcessingResult result = new ProcessingResult();

        for (Future<ParticipantResult> future : futures) {
            try {
                ParticipantResult pr = future.get();

                if (pr.isValid()) {
                    result.addNewParticipant(pr.getParticipant());
                } else if (pr.isDuplicateAvailable()) {
                    result.addDuplicateAvailable(pr.getErrorMessage());
                } else if (pr.isDuplicateAssigned()) {
                    result.addDuplicateAssigned(pr.getErrorMessage());
                } else {
                    result.addInvalidRecord(pr.getErrorMessage());
                }

            } catch (Exception e) {
                SystemLogger.logException("CSV processing error", e);
                result.addInvalidRecord("Processing error: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        SystemLogger.success("CSV processing complete: " + result.getNewCount() + " new");
        System.out.println("[System] Concurrent processing complete.");

        return result;
    }

    private ParticipantResult processLine(String line, int lineNumber,
                                          List<Participant> existingParticipants) {
        try {
            String[] parts = line.split(",");

            if (parts.length < 8) {
                return ParticipantResult.invalid(
                        "Line " + lineNumber + ": Missing fields");
            }

            String id = parts[0].trim();
            String name = parts[1].trim();
            String email = parts[2].trim();
            String preferredGame = parts[3].trim();
            int skillLevel = Integer.parseInt(parts[4].trim());
            String preferredRole = parts[5].trim();
            int personalityScore = Integer.parseInt(parts[6].trim());
            String personalityType = parts[7].trim();

            if (!ValidationUtil.isValidParticipantId(id)) {
                return ParticipantResult.invalid(
                        "Line " + lineNumber + ": Invalid ID format (must start with 'P')");
            }

            synchronized (existingParticipants) {
                Participant existing = findByIdOrEmail(existingParticipants, id, email);

                if (existing != null) {
                    if (existing.getStatus().equals("Available")) {
                        return ParticipantResult.duplicateAvailable(
                                id + " (" + email + ")");
                    } else if (existing.getStatus().equals("Assigned")) {
                        return ParticipantResult.duplicateAssigned(
                                id + " - " + name + " (" + email + ")");
                    }
                }
            }

            Participant newParticipant = new Participant(
                    id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            return ParticipantResult.valid(newParticipant);

        } catch (NumberFormatException e) {
            return ParticipantResult.invalid(
                    "Line " + lineNumber + ": Invalid number format");
        } catch (Exception e) {
            return ParticipantResult.invalid(
                    "Line " + lineNumber + ": " + e.getMessage());
        }
    }

    private List<String> readAllLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private Participant findByIdOrEmail(List<Participant> participants, String id, String email) {
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id) || p.getEmail().equalsIgnoreCase(email)) {
                return p;
            }
        }
        return null;
    }

    // ==================== INDIVIDUAL SURVEY PROCESSING ====================

    /**
     * Process individual survey submission using threads
     */
    public static SurveyResult processIndividualSurvey(String id, String name, String email,
                                                       String preferredGame, int skillLevel,
                                                       String preferredRole, int personalityScore) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            SystemLogger.info("Processing survey for: " + id);

            // Calculate personality using thread
            Future<String> personalityTask = executor.submit(() -> {
                SystemLogger.info("Thread calculating personality - score: " + personalityScore);
                try {
                    return PersonalityClassifier.classifyPersonality(personalityScore);
                } catch (IllegalArgumentException e) {
                    throw new Exception(e.getMessage());
                }
            });

            String personalityType = personalityTask.get();

            Participant participant = new Participant(id, name, email, preferredGame,
                    skillLevel, preferredRole,
                    personalityScore, personalityType);

            SystemLogger.success("Survey processed: " + id + " - " + personalityType);

            return SurveyResult.success(participant, personalityType);

        } catch (ExecutionException e) {
            SystemLogger.error("Survey processing failed: " + e.getCause().getMessage());
            return SurveyResult.failure(e.getCause().getMessage());
        } catch (Exception e) {
            SystemLogger.logException("Survey processing error", e);
            return SurveyResult.failure("Processing error: " + e.getMessage());
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Result for CSV line processing
     */
    private static class ParticipantResult {
        private Participant participant;
        private String errorMessage;
        private ResultType type;

        private enum ResultType {
            VALID, DUPLICATE_AVAILABLE, DUPLICATE_ASSIGNED, INVALID
        }

        private ParticipantResult(Participant participant, String errorMessage, ResultType type) {
            this.participant = participant;
            this.errorMessage = errorMessage;
            this.type = type;
        }

        static ParticipantResult valid(Participant p) {
            return new ParticipantResult(p, null, ResultType.VALID);
        }

        static ParticipantResult duplicateAvailable(String msg) {
            return new ParticipantResult(null, msg, ResultType.DUPLICATE_AVAILABLE);
        }

        static ParticipantResult duplicateAssigned(String msg) {
            return new ParticipantResult(null, msg, ResultType.DUPLICATE_ASSIGNED);
        }

        static ParticipantResult invalid(String msg) {
            return new ParticipantResult(null, msg, ResultType.INVALID);
        }

        boolean isValid() { return type == ResultType.VALID; }
        boolean isDuplicateAvailable() { return type == ResultType.DUPLICATE_AVAILABLE; }
        boolean isDuplicateAssigned() { return type == ResultType.DUPLICATE_ASSIGNED; }
        Participant getParticipant() { return participant; }
        String getErrorMessage() { return errorMessage; }
    }

    /**
     * Aggregated result for CSV processing
     */
    public static class ProcessingResult {
        private List<Participant> newParticipants;
        private List<String> duplicateAvailable;
        private List<String> duplicateAssigned;
        private List<String> invalidRecords;

        public ProcessingResult() {
            this.newParticipants = new ArrayList<>();
            this.duplicateAvailable = new ArrayList<>();
            this.duplicateAssigned = new ArrayList<>();
            this.invalidRecords = new ArrayList<>();
        }

        synchronized void addNewParticipant(Participant p) { newParticipants.add(p); }
        synchronized void addDuplicateAvailable(String msg) { duplicateAvailable.add(msg); }
        synchronized void addDuplicateAssigned(String msg) { duplicateAssigned.add(msg); }
        synchronized void addInvalidRecord(String msg) { invalidRecords.add(msg); }

        public List<Participant> getNewParticipants() { return new ArrayList<>(newParticipants); }
        public List<String> getDuplicateAvailable() { return new ArrayList<>(duplicateAvailable); }
        public List<String> getDuplicateAssigned() { return new ArrayList<>(duplicateAssigned); }
        public List<String> getInvalidRecords() { return new ArrayList<>(invalidRecords); }

        public int getNewCount() { return newParticipants.size(); }
        public int getDuplicateAvailableCount() { return duplicateAvailable.size(); }
        public int getDuplicateAssignedCount() { return duplicateAssigned.size(); }
        public int getInvalidCount() { return invalidRecords.size(); }
    }

    /**
     * Result for individual survey processing
     */
    public static class SurveyResult {
        private boolean success;
        private Participant participant;
        private String personalityType;
        private String errorMessage;

        private SurveyResult(boolean success, Participant participant,
                             String personalityType, String errorMessage) {
            this.success = success;
            this.participant = participant;
            this.personalityType = personalityType;
            this.errorMessage = errorMessage;
        }

        public static SurveyResult success(Participant participant, String personalityType) {
            return new SurveyResult(true, participant, personalityType, null);
        }

        public static SurveyResult failure(String errorMessage) {
            return new SurveyResult(false, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public Participant getParticipant() { return participant; }
        public String getPersonalityType() { return personalityType; }
        public String getErrorMessage() { return errorMessage; }
    }
}