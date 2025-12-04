package teammate.util;

// Classifies participants into personality types based on survey scores
public class PersonalityClassifier {

    // Classifies personality type based on total personality score
    public static String classifyPersonality(int score) {
        if (score >= 90 && score <= 100) {
            return "Leader";
        } else if (score >= 70 && score <= 89) {
            return "Balanced";
        } else if (score >= 50 && score <= 69) {
            return "Thinker";
        } else {
            throw new IllegalArgumentException(
                    "Insufficient personality score (" + score + ") to classify into a personality type. " +
                            "Score must be between 50 and 100.");
        }
    }

    // Validates if personality score is within acceptable range
    public static boolean isValidScore(int score) {
        return score >= 50 && score <= 100;
    }
}