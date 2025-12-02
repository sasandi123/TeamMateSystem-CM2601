package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.ValidationUtil;
import teammate.util.FileManager;
import teammate.util.SystemLogger;
import java.util.Scanner;

/**
 * Service for participant portal operations
 * Clean version with proper validation and minimal noise
 */
public class ParticipantPortalService {
    private ParticipantManager participantManager;
    private TeamBuilder teamBuilder;

    public ParticipantPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        this.participantManager = participantManager;
        this.teamBuilder = teamBuilder;
    }

    public void showPortal(Scanner scanner) {
        while (true) {
            System.out.println("\n--- PARTICIPANT PORTAL ---");
            System.out.println("1. Submit Survey");
            System.out.println("2. Check My Team");
            System.out.println("3. Back to Main Menu");
            System.out.print("Enter choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        submitSurvey(scanner);
                        break;
                    case 2:
                        checkMyTeam(scanner);
                        break;
                    case 3:
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Submit survey with proper validation
     */
    private void submitSurvey(Scanner scanner) {
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");

            // Get and validate ID
            String id = getValidParticipantId(scanner);
            if (id == null) return;

            // Get and validate email
            String email = getValidEmail(scanner);
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
            String name = getValidName(scanner);
            String preferredGame = getValidGameName(scanner);
            int skillLevel = getUserIntInput(scanner, "Enter Skill Level (1-10): ", 1, 10);
            String preferredRole = getValidRole(scanner);
            int personalityScore = runPersonalitySurvey(scanner);

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

    private void checkMyTeam(Scanner scanner) {
        System.out.println("\n--- CHECK TEAM STATUS ---");
        System.out.print("Enter your Participant ID, Email, or Full Name: ");
        String searchKey = scanner.nextLine().trim();

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

    private int runPersonalitySurvey(Scanner scanner) throws TeamMateException.InvalidInputException {
        System.out.println("\n--- PERSONALITY SURVEY (Rate 1-5) ---");
        int totalScore = 0;

        totalScore += getUserIntInput(scanner, "Q1: I enjoy taking the lead and guiding others. ", 1, 5);
        totalScore += getUserIntInput(scanner, "Q2: I prefer analyzing situations and strategic solutions. ", 1, 5);
        totalScore += getUserIntInput(scanner, "Q3: I work well with others and enjoy collaborative teamwork. ", 1, 5);
        totalScore += getUserIntInput(scanner, "Q4: I am calm under pressure and can maintain team morale. ", 1, 5);
        totalScore += getUserIntInput(scanner, "Q5: I like making quick decisions and adapting in dynamic situations. ", 1, 5);

        return totalScore * 4;
    }

    private String getValidParticipantId(Scanner scanner) {
        while (true) {
            System.out.print("Enter Your Participant ID (must start with 'P'): ");
            String id = scanner.nextLine().trim();

            if (id.isEmpty()) {
                System.out.println("✗ ID cannot be empty. Please try again.");
                continue;
            }

            if (!ValidationUtil.isValidParticipantId(id)) {
                System.out.println("✗ Invalid ID format. ID must start with 'P' (e.g., P0001, P1234)");
                continue;
            }

            return id;
        }
    }

    private String getValidEmail(Scanner scanner) {
        while (true) {
            System.out.print("Enter Your Email: ");
            String email = scanner.nextLine().trim();

            if (email.isEmpty()) {
                System.out.println("✗ Email cannot be empty. Please try again.");
                continue;
            }

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
    private String getValidName(Scanner scanner) {
        while (true) {
            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            if (name.isEmpty()) {
                System.out.println("✗ Name cannot be empty. Please try again.");
                continue;
            }

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
    private String getValidGameName(Scanner scanner) {
        while (true) {
            System.out.print("Enter Preferred Game: ");
            String game = scanner.nextLine().trim();

            if (game.isEmpty()) {
                System.out.println("✗ Game name cannot be empty. Please try again.");
                continue;
            }

            if (!ValidationUtil.isValidGameName(game)) {
                System.out.println("✗ Invalid game name format.");
                System.out.println("   Examples: Valorant, Call of Duty, FIFA 23");
                continue;
            }

            return game;
        }
    }

    private String getValidRole(Scanner scanner) {
        System.out.println("Valid Roles: Strategist, Attacker, Defender, Supporter, Coordinator");

        while (true) {
            System.out.print("Enter Preferred Role: ");
            String role = scanner.nextLine().trim();

            if (role.isEmpty()) {
                System.out.println("✗ Role cannot be empty. Please try again.");
                continue;
            }

            if (!ValidationUtil.isValidRole(role)) {
                System.out.println("✗ Invalid role. Please choose from: Strategist, Attacker, Defender, Supporter, Coordinator");
                continue;
            }

            return role;
        }
    }

    private int getUserIntInput(Scanner scanner, String prompt, int min, int max)
            throws TeamMateException.InvalidInputException {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a number between " + min + " and " + max + ".");
                continue;
            }

            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println("Please enter a number between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}