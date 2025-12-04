package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.ValidationUtil;
import teammate.util.FileManager;
import teammate.util.SystemLogger;

import static teammate.Main.centerText;

// Handles participant-specific operations including survey submission and team status checking
public class ParticipantPortalService extends PortalService {

    public ParticipantPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        super(participantManager, teamBuilder);
    }

    @Override
    protected void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("|" + centerText("PARTICIPANT PORTAL", 58) + "|");
        System.out.println("=".repeat(60));
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n+" + "-".repeat(58) + "+");
        System.out.println("|  [1] Submit Survey" + " ".repeat(39) + "|");
        System.out.println("|  [2] Check My Team Status" + " ".repeat(32) + "|");
        System.out.println("|  [3] Return to Main Menu" + " ".repeat(33) + "|");
        System.out.println("+" + "-".repeat(58) + "+");
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

    // Collects survey data from participant and processes using concurrent threads
    private void submitSurvey() {
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");

            String id = collectAndValidateParticipantId();
            if (id == null) return;

            String email = collectAndValidateEmail();
            if (email == null) return;

            // Check for duplicate ID or email
            String validationResult = participantManager.validateParticipantCredentials(id, email);

            if (validationResult.equals("ID_EXISTS")) {
                System.out.println("[X] Registration Failed: A participant with ID '" + id + "' already exists.");
                return;
            }

            if (validationResult.equals("EMAIL_EXISTS")) {
                System.out.println("[X] Registration Failed: A participant with email '" + email + "' already exists.");
                return;
            }

            System.out.println("[OK] ID and Email validated successfully.\n");

            String name = collectAndValidateName();
            String preferredGame = collectAndValidateGame();

            int skillLevel = getUserIntInput("Enter Skill Level (1-10): ", 1, 10);

            String preferredRole = collectAndValidateRole();
            int personalityScore = runPersonalitySurvey();

            // Process survey using concurrent processor
            SurveyDataProcessor.SurveyResult result = SurveyDataProcessor.processIndividualSurvey(
                    id, name, email, preferredGame, skillLevel, preferredRole, personalityScore
            );

            if (!result.isSuccess()) {
                System.out.println("\n[X] Survey Failed: " + result.getErrorMessage());
                System.out.println("   Please retake the survey and provide more accurate responses.");
                return;
            }

            // Add participant to system
            participantManager.addParticipant(result.getParticipant());

            // Save participant data to file immediately
            participantManager.saveAllParticipants();

            System.out.println("\n[OK] Survey submitted successfully!");
            System.out.println("Your Participant ID: " + result.getParticipant().getId());
            System.out.println("Your Email: " + result.getParticipant().getEmail());
            System.out.println("Calculated Personality Type: " + result.getPersonalityType());
            System.out.println("\nYou can now check your team status using option 2.");

        } catch (Exception e) {
            System.out.println("Submission Failed: " + e.getMessage());
            SystemLogger.logException("Survey submission error", e);
        }
    }

    // Checks and displays participant's team assignment status
    private void checkMyTeam() {
        System.out.println("\n--- CHECK TEAM STATUS ---");

        String searchKey = getNonEmptyInput("Enter your Participant ID, Email, or Full Name: ");

        Participant p = participantManager.findParticipant(searchKey);

        if (p == null) {
            System.out.println("[X] Error: Participant not found.");
            return;
        }

        System.out.println("\n[OK] Found Participant:");
        System.out.println("   System ID: " + p.getId());
        System.out.println("   Name: " + p.getName());
        System.out.println("   Email: " + p.getEmail());
        System.out.println("   Status: " + p.getStatus());

        if (p.getStatus().equals("Available")) {
            System.out.println("\n   You are currently unassigned and available for team formation.");
            return;
        }

        // Search for participant's team in finalized records
        System.out.println("\n   Searching team records...");
        FileManager.findMostRecentParticipantTeam(p.getId(), participantManager);
    }

    // Collects and validates personality survey responses
    private int runPersonalitySurvey() throws TeamMateException.InvalidInputException {
        System.out.println("\n--- PERSONALITY SURVEY (Rate 1-5) ---");
        int totalScore = 0;

        totalScore += getUserIntInput("Q1: I enjoy taking the lead and guiding others. ", 1, 5);
        totalScore += getUserIntInput("Q2: I prefer analyzing situations and strategic solutions. ", 1, 5);
        totalScore += getUserIntInput("Q3: I work well with others and enjoy collaborative teamwork. ", 1, 5);
        totalScore += getUserIntInput("Q4: I am calm under pressure and can maintain team morale. ", 1, 5);
        totalScore += getUserIntInput("Q5: I like making quick decisions and adapting in dynamic situations. ", 1, 5);

        return totalScore * 4;
    }

    // Validation helper methods with retry loops

    private String collectAndValidateParticipantId() {
        while (true) {
            String id = getNonEmptyInput("Enter Your Participant ID (must start with 'P'): ");

            if (!ValidationUtil.isValidParticipantId(id)) {
                System.out.println("[X] Invalid ID format. ID must start with 'P' (e.g., P0001, P1234)");
                continue;
            }

            return id;
        }
    }

    private String collectAndValidateEmail() {
        while (true) {
            String email = getNonEmptyInput("Enter Your Email: ");

            if (!ValidationUtil.isValidEmail(email)) {
                System.out.println("[X] Invalid email format. Please try again.");
                continue;
            }

            return email;
        }
    }

    private String collectAndValidateName() {
        while (true) {
            String name = getNonEmptyInput("Enter Full Name: ");

            if (!ValidationUtil.isValidName(name)) {
                System.out.println("[X] Invalid name format. Name must contain only letters and spaces.");
                System.out.println("   Examples: John Smith, Mary-Jane, O'Brien");
                continue;
            }

            return name;
        }
    }

    private String collectAndValidateGame() {
        while (true) {
            String game = getNonEmptyInput("Enter Preferred Game: ");

            if (!ValidationUtil.isValidGameName(game)) {
                System.out.println("[X] Invalid game name format.");
                System.out.println("   Examples: Valorant, Call of Duty, FIFA 23");
                continue;
            }

            return game;
        }
    }

    private String collectAndValidateRole() {
        System.out.println("Valid Roles: Strategist, Attacker, Defender, Supporter, Coordinator");

        while (true) {
            String role = getNonEmptyInput("Enter Preferred Role: ");

            if (!ValidationUtil.isValidRole(role)) {
                System.out.println("[X] Invalid role. Please choose from: Strategist, Attacker, Defender, Supporter, Coordinator");
                continue;
            }

            return role;
        }
    }
}