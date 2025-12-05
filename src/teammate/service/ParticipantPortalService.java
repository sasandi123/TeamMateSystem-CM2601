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
    private void submitSurvey() {//sq no.1 of use case submit survey
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");

            String id = collectAndValidateParticipantId();//sq no.1.1 of use case submit survey
            if (id == null) return;

            String email = collectAndValidateEmail();//sq no. 1.2 of use case submit survey
            if (email == null) return;

            // Check for duplicate ID or email
            String validationResult = participantManager.validateParticipantCredentials(id, email); // sq no. 1.3 of submit survey use case

            if (validationResult.equals("ID_EXISTS")) {
                System.out.println("[X] Registration Failed: A participant with ID '" + id + "' already exists.");
                return;
            }

            if (validationResult.equals("EMAIL_EXISTS")) {
                System.out.println("[X] Registration Failed: A participant with email '" + email + "' already exists.");
                return;
            }

            System.out.println("[OK] ID and Email validated successfully.\n");

            String name = collectAndValidateName(); // sq no. 1.4 of submit survey use case
            String preferredGame = collectAndValidateGame();// sq no. 1.5 of submit survey use case

            int skillLevel = getUserIntInput("Enter Skill Level (1-10): ", 1, 10);// sq no. 1.6 of submit survey use case

            String preferredRole = collectAndValidateRole();// sq no. 1.7 of submit survey use case
            int personalityScore = runPersonalitySurvey();// sq no. 1.8 of submitSurvey use case

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
    private void checkMyTeam() {// sq no.1 of check my team use case
        System.out.println("\n--- CHECK TEAM STATUS ---");

        String searchKey = getNonEmptyInput("Enter your Participant ID, Email, or Full Name: ");// sq 1.1 of check my team use case

        Participant p = participantManager.findParticipant(searchKey);// sq 1.2 of check my team use case

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
        FileManager.findMostRecentParticipantTeam(p.getId(), participantManager); // sq no.1.3 of check my team use case
    }

    // Collects and validates personality survey responses
    private int runPersonalitySurvey() throws TeamMateException.InvalidInputException {
        System.out.println("\n--- PERSONALITY SURVEY (Rate 1-5) ---");
        int totalScore = 0;

        totalScore += getUserIntInput("Q1: I enjoy taking the lead and guiding others. ", 1, 5);// sq no.1.8.1 of submit survey use case
        totalScore += getUserIntInput("Q2: I prefer analyzing situations and strategic solutions. ", 1, 5);// sq no.1.8.2 of submit survey use case
        totalScore += getUserIntInput("Q3: I work well with others and enjoy collaborative teamwork. ", 1, 5);// sq no.1.8.3 of submit survey use case
        totalScore += getUserIntInput("Q4: I am calm under pressure and can maintain team morale. ", 1, 5);// sq no.1.8.4 of submit survey use case
        totalScore += getUserIntInput("Q5: I like making quick decisions and adapting in dynamic situations. ", 1, 5);// sq no.1.8.5 of submit survey use case

        return totalScore * 4;
    }

    // Validation helper methods with retry loops

    private String collectAndValidateParticipantId() {
        while (true) {
            String id = getNonEmptyInput("Enter Your Participant ID (must start with 'P'): ");//sq 1.1 of submit survey use case

            if (!ValidationUtil.isValidParticipantId(id)) {//sq 1.2 of submit survey use case
                System.out.println("[X] Invalid ID format. ID must start with 'P' (e.g., P0001, P1234)");
                continue;
            }

            return id;
        }
    }

    private String collectAndValidateEmail() {
        while (true) {
            String email = getNonEmptyInput("Enter Your Email: ");//sq 1.2.1 of submit survey use case

            if (!ValidationUtil.isValidEmail(email)) {// sq no. 1.2.2 of submit survey use case
                System.out.println("[X] Invalid email format. Please try again.");
                continue;
            }

            return email;
        }
    }

    private String collectAndValidateName() {
        while (true) {
            String name = getNonEmptyInput("Enter Full Name: ");//sq no. 1.4.1 of submit survey use case

            if (!ValidationUtil.isValidName(name)) {// sq no. 1.4.2 of submit survey use case
                System.out.println("[X] Invalid name format. Name must contain only letters and spaces.");
                System.out.println("   Examples: John Smith, Mary-Jane, O'Brien");
                continue;
            }

            return name;
        }
    }

    private String collectAndValidateGame() {
        while (true) {
            String game = getNonEmptyInput("Enter Preferred Game: ");// sq no. 1.5.1 of submit survey use case

            if (!ValidationUtil.isValidGameName(game)) {// sq. no. 1.5.2 of submit survey use case
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
            String role = getNonEmptyInput("Enter Preferred Role: ");// sq no. 1.7.1 of submit survey use case

            if (!ValidationUtil.isValidRole(role)) {// sq no. 1.7.2 of submit survey use case
                System.out.println("[X] Invalid role. Please choose from: Strategist, Attacker, Defender, Supporter, Coordinator");
                continue;
            }

            return role;
        }
    }
}