package teammate.util;

/**
 * Validation utilities for TeamMate System
 * Provides input validation for IDs, emails, names, games, and other fields
 */
public class ValidationUtil {

    /**
     * Validates participant ID format (must start with 'P' or 'p')
     */
    public static boolean isValidParticipantId(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }

        String trimmedId = id.trim();
        return trimmedId.toLowerCase().startsWith("p") && trimmedId.length() > 1;
    }

    /**
     * Validates email format (basic validation)
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Validates skill level range
     */
    public static boolean isValidSkillLevel(int skillLevel) {
        return skillLevel >= 1 && skillLevel <= 10;
    }

    /**
     * Validates role
     */
    public static boolean isValidRole(String role) {
        String[] validRoles = {"Strategist", "Attacker", "Defender", "Supporter", "Coordinator"};
        for (String validRole : validRoles) {
            if (validRole.equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validates name format - must contain only letters and spaces
     * No numbers or special characters allowed
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        String trimmedName = name.trim();

        // Must be at least 2 characters
        if (trimmedName.length() < 2) {
            return false;
        }

        // Must contain only letters, spaces, hyphens, and apostrophes
        // Allows names like "Mary-Jane" or "O'Brien"
        return trimmedName.matches("^[a-zA-Z][a-zA-Z\\s'-]*[a-zA-Z]$");
    }

    /**
     * Validates game name format - must contain only letters, numbers, spaces, and basic punctuation
     * No special characters that could cause file/database issues
     */
    public static boolean isValidGameName(String game) {
        if (game == null || game.trim().isEmpty()) {
            return false;
        }

        String trimmedGame = game.trim();

        // Must be at least 2 characters
        if (trimmedGame.length() < 2) {
            return false;
        }

        // Allows letters, numbers, spaces, hyphens, apostrophes, and colons
        return trimmedGame.matches("^[a-zA-Z0-9][a-zA-Z0-9\\s:'-]*[a-zA-Z0-9]$");
    }
}