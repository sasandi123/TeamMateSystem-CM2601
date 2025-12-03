package teammate.concurrent;

import teammate.entity.Participant;
import teammate.util.PersonalityClassifier;
import java.util.concurrent.*;

/**
 * Concurrent processor for individual survey submissions
 * Handles personality calculation using threads
 */
public class SurveyDataProcessor {

    /**
     * Process individual survey submission using threads
     * Uses concurrent processing to calculate personality type
     */
    public static SurveyResult processIndividualSurvey(String id, String name, String email,
                                                       String preferredGame, int skillLevel,
                                                       String preferredRole, int personalityScore) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Calculate personality using thread
            Future<String> personalityTask = executor.submit(() -> {
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

            return SurveyResult.success(participant, personalityType);

        } catch (ExecutionException e) {
            return SurveyResult.failure(e.getCause().getMessage());
        } catch (Exception e) {
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
