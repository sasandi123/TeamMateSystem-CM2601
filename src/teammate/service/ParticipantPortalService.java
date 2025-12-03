package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.ValidationUtil;
import teammate.util.FileManager;
import teammate.util.SystemLogger;

/**
 * Participant Portal Service - extends PortalService
 * Demonstrates: Inheritance and Polymorphism
 *
 * This class inherits common portal functionality from PortalService
 * and implements participant-specific behavior through method overriding.
 */
public class ParticipantPortalService extends PortalService {

    /**
     * Constructor - calls parent constructor
     * Demonstrates: Inheritance (using super keyword)
     */
    public ParticipantPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        super(participantManager, teamBuilder);
    }

    /**
     * OVERRIDDEN ABSTRACT METHODS FROM PortalService
     * Demonstrates: Polymorphism (runtime method binding)
     */

    @Override
    protected void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("PARTICIPANT PORTAL");
        System.out.println("=".repeat(50));
    }

    @Override
    protected void displayMenu() {
        System.out.println("1. Submit Survey");
        System.out.println("2. Check My Team");
        System.out.println("3. Back to Main Menu");
    }

    @Override
    protected void handleMenuChoice(int choice) {
        try {
            switch (choice) {
                case 1:
                    submitSurvey();
                    break;
                case 2:
                    checkMyTeam();
                    break;
                case -1:
                    // Invalid input already handled by parent class
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    @Override
    protected int getExitOption() {
        return 3;
    }

    @Override
    protected void displayGoodbyeMessage() {
        System.out.println("Returning to main menu...");
    }

    /**
     * PARTICIPANT-SPECIFIC PRIVATE METHODS
     * These methods implement the core functionality of the participant portal
     */

    /**
     * Submit survey with proper validation
     * This method handles the complete survey submission process
     */
    private void submitSurvey() {
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");

            // Get and validate ID using inherited helper method
            String id = getValidParticipantId();
            if (id == null) return;

            // Get and validate email using inherited helper method
            String email = getValidEmail();
            if (email == null) return;

            // Validate credentials (check duplicates)
            String validationResult = participantManager.validateParticipantCredentials(id, email);

            if (validationResult.equals("ID_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with ID '" + id + "' already exists.");
                return;
            }

            if (validationResult.equals("EMAIL_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with email '" + email + "' already exists.");
                return;
            }

            System.out.println("✓ ID and Email validated successfully.\n");

            // Get personal details with validation
            String name = getValidName();
            String preferredGame = getValidGameName();

            // Use inherited getUserIntInput method from PortalService
            int skillLevel = getUserIntInput("Enter Skill Level (1-10): ", 1, 10);

            String preferredRole = getValidRole();
            int personalityScore = runPersonalitySurvey();

            // Process survey using concurrent processor
            SurveyDataProcessor.SurveyResult result = SurveyDataProcessor.processIndividualSurvey(
                    id, name, email, preferredGame, skillLevel, preferredRole, personalityScore
            );

            if (!result.isSuccess()) {
                System.out.println("\n✗ Survey Failed: " + result.getErrorMessage());
                System.out.println("   Please retake the survey and provide more accurate responses.");
                return;
            }

            // Add participant to system
            participantManager.addParticipant(result.getParticipant());

            System.out.println("\n✓ Survey submitted successfully!");
            System.out.println("Your Participant ID: " + result.getParticipant().getId());
            System.out.println("Your Email: " + result.getParticipant().getEmail());
            System.out.println("Calculated Personality Type: " + result.getPersonalityType());
            System.out.println("\nYou can now check your team status using option 2.");

        } catch (Exception e) {
            System.out.println("Submission Failed: " + e.getMessage());
            SystemLogger.logException("Survey submission error", e);
        }
    }

    /**
     * Check team assignment status
     * Allows participants to view their team information
     */
    private void checkMyTeam() {
        System.out.println("\n--- CHECK TEAM STATUS ---");

        // Use inherited getNonEmptyInput method from PortalService
        String searchKey = getNonEmptyInput("Enter your Participant ID, Email, or Full Name: ");

        Participant p = participantManager.findParticipant(searchKey);

        if (p == null) {
            System.out.println("✗ Error: Participant not found.");
            return;
        }

        System.out.println("\n✓ Found Participant:");
        System.out.println("   System ID: " + p.getId());
        System.out.println("   Name: " + p.getName());
        System.out.println("   Email: " + p.getEmail());
        System.out.println("   Status: " + p.getStatus());

        if (p.getStatus().equals("Available")) {
            System.out.println("\n   You are currently unassigned and available for team formation.");
            return;
        }

        Team team = teamBuilder.findTeamForParticipant(p);

        if (team != null) {
            System.out.println("\n   You are assigned to a team!");
            team.displayTeamInfo();
        } else {
            System.out.println("\n   Searching team records...");
            FileManager.findParticipantTeamInCumulative(p.getId(), participantManager);
        }
    }

    /**
     * Runs personality survey questions
     * Uses inherited getUserIntInput method for validated input
     */
    private int runPersonalitySurvey() throws TeamMateException.InvalidInputException {
        System.out.println("\n--- PERSONALITY SURVEY (Rate 1-5) ---");
        int totalScore = 0;

        // All questions use the inherited getUserIntInput method
        totalScore += getUserIntInput("Q1: I enjoy taking the lead and guiding others. ", 1, 5);
        totalScore += getUserIntInput("Q2: I prefer analyzing situations and strategic solutions. ", 1, 5);
        totalScore += getUserIntInput("Q3: I work well with others and enjoy collaborative teamwork. ", 1, 5);
        totalScore += getUserIntInput("Q4: I am calm under pressure and can maintain team morale. ", 1, 5);
        totalScore += getUserIntInput("Q5: I like making quick decisions and adapting in dynamic situations. ", 1, 5);

        return totalScore * 4;
    }

    /**
     * VALIDATION HELPER METHODS
     * These methods provide specific validation for participant data
     */

    /**
     * Get valid participant ID with format validation
     */
    private String getValidParticipantId() {
        while (true) {
            // Uses inherited getNonEmptyInput method
            String id = getNonEmptyInput("Enter Your Participant ID (must start with 'P'): ");

            if (!ValidationUtil.isValidParticipantId(id)) {
                System.out.println("✗ Invalid ID format. ID must start with 'P' (e.g., P0001, P1234)");
                continue;
            }

            return id;
        }
    }

    /**
     * Get valid email address
     */
    private String getValidEmail() {
        while (true) {
            // Uses inherited getNonEmptyInput method
            String email = getNonEmptyInput("Enter Your Email: ");

            if (!ValidationUtil.isValidEmail(email)) {
                System.out.println("✗ Invalid email format. Please try again.");
                continue;
            }

            return email;
        }
    }

    /**
     * Get valid name - must contain only letters and spaces
     */
    private String getValidName() {
        while (true) {
            // Uses inherited getNonEmptyInput method
            String name = getNonEmptyInput("Enter Full Name: ");

            if (!ValidationUtil.isValidName(name)) {
                System.out.println("✗ Invalid name format. Name must contain only letters and spaces.");
                System.out.println("   Examples: John Smith, Mary-Jane, O'Brien");
                continue;
            }

            return name;
        }
    }

    /**
     * Get valid game name - must be appropriate format
     */
    private String getValidGameName() {
        while (true) {
            // Uses inherited getNonEmptyInput method
            String game = getNonEmptyInput("Enter Preferred Game: ");

            if (!ValidationUtil.isValidGameName(game)) {
                System.out.println("✗ Invalid game name format.");
                System.out.println("   Examples: Valorant, Call of Duty, FIFA 23");
                continue;
            }

            return game;
        }
    }

    /**
     * Get valid role from predefined list
     */
    private String getValidRole() {
        System.out.println("Valid Roles: Strategist, Attacker, Defender, Supporter, Coordinator");

        while (true) {
            // Uses inherited getNonEmptyInput method
            String role = getNonEmptyInput("Enter Preferred Role: ");

            if (!ValidationUtil.isValidRole(role)) {
                System.out.println("✗ Invalid role. Please choose from: Strategist, Attacker, Defender, Supporter, Coordinator");
                continue;
            }

            return role;
        }
    }
}