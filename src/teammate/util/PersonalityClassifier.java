package teammate.util;

public class PersonalityClassifier {

    public static String classifyPersonality(int score) {
        if (score >= 90 && score <= 100) {
            return "Leader";
        } else if (score >= 70 && score <= 89) {
            return "Balanced";
        } else if (score >= 50 && score <= 69) {
            return "Thinker";
        } else {
            throw new IllegalArgumentException("Invalid personality score: " + score);
        }
    }

    public static boolean isValidScore(int score) {
        return score >= 50 && score <= 100;
    }

    public static String getDescription(String type) {
        switch (type) {
            case "Leader":
                return "Confident, decision-maker, naturally takes charge";
            case "Balanced":
                return "Adaptive, communicative, team-oriented";
            case "Thinker":
                return "Observant, analytical, prefers planning before action";
            default:
                return "Unknown type";
        }
    }
}