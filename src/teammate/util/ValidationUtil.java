package teammate.util;

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
}
