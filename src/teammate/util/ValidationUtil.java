package teammate.util;

// Provides input validation utilities for the system
public class ValidationUtil {

    // Validates participant ID format (must start with 'P')
    public static boolean isValidParticipantId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        String trimmedId = id.trim();
        return trimmedId.toLowerCase().startsWith("p") && trimmedId.length() > 1;
    }

    // Validates email format using regex pattern
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // Validates skill level is within acceptable range (1-10)
    public static boolean isValidSkillLevel(int skillLevel) {
        return skillLevel >= 1 && skillLevel <= 10;
    }

    // Validates role matches one of the predefined roles
    public static boolean isValidRole(String role) {
        String[] validRoles = {"Strategist", "Attacker", "Defender", "Supporter", "Coordinator"};
        for (String validRole : validRoles) {
            if (validRole.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    // Validates name contains only letters, spaces, hyphens, and apostrophes
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        if (trimmedName.length() < 2) {
            return false;
        }

        // Allows names like "Mary-Jane" or "O'Brien"
        return trimmedName.matches("^[a-zA-Z][a-zA-Z\\s'-]*[a-zA-Z]$");
    }

    // Validates game name contains only alphanumeric characters and basic punctuation
    public static boolean isValidGameName(String game) {
        if (game == null || game.trim().isEmpty()) {
            return false;
        }

        String trimmedGame = game.trim();

        if (trimmedGame.length() < 2) {
            return false;
        }

        return trimmedGame.matches("^[a-zA-Z0-9][a-zA-Z0-9\\s:'-]*[a-zA-Z0-9]$");
    }
}