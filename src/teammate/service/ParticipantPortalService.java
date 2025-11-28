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
                SystemLogger.logException("Participant Portal Error", e);
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    /**
     * Submit survey - delegates processing to SurveyDataProcessor
     */
    private void submitSurvey(Scanner scanner) {
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");
            SystemLogger.info("Participant started survey submission");

            String id = getValidParticipantId(scanner);
            if (id == null) return;

            String email = getValidEmail(scanner);
            if (email == null) return;

            // Validate credentials
            String validationResult = participantManager.validateParticipantCredentials(id, email);

            if (validationResult.equals("ID_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with ID '" + id + "' already exists.");
                SystemLogger.warning("Survey failed: ID exists - " + id);
                return;
            }

            if (validationResult.equals("EMAIL_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with email '" + email + "' already exists.");
                SystemLogger.warning("Survey failed: Email exists - " + email);
                return;
            }

            System.out.println("✓ ID and Email validated successfully. Please complete the survey.\n");

            String name = getNonEmptyInput(scanner, "Enter Full Name: ");
            String preferredGame = getNonEmptyInput(scanner, "Enter Preferred Game: ");
            int skillLevel = getUserIntInput(scanner, "Enter Skill Level (1-10): ", 1, 10);
            String preferredRole = getValidRole(scanner);
            int personalityScore = runPersonalitySurvey(scanner);

            // Delegate to SurveyDataProcessor (separation of concerns)
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

        SystemLogger.info("Team status check: " + searchKey);

        Participant p = participantManager.findParticipant(searchKey);

        if (p == null) {
            System.out.println("✗ Error: Participant not found.");
            SystemLogger.warning("Participant not found: " + searchKey);
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

    private String getNonEmptyInput(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println("✗ This field cannot be empty. Please try again.");
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