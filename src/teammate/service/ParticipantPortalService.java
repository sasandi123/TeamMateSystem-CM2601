package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.util.PersonalityClassifier;
import teammate.util.ValidationUtil;
import teammate.util.FileManager;
import java.util.Scanner;

public class ParticipantPortalService {
    private ParticipantManager participantManager;
    private TeamBuilder teamBuilder;

    public ParticipantPortalService(ParticipantManager participantManager) {
        this.participantManager = participantManager;
        this.teamBuilder = new TeamBuilder();
    }

    public void setTeamBuilder(TeamBuilder teamBuilder) {
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

    private void submitSurvey(Scanner scanner) {
        try {
            System.out.println("\n--- SUBMIT SURVEY ---");

            System.out.print("Enter Your Participant ID (must start with 'P'): ");
            String id = scanner.nextLine().trim();

            if (!ValidationUtil.isValidParticipantId(id)) {
                System.out.println("✗ Invalid ID format. ID must start with 'P' (e.g., P0001, P1234)");
                return;
            }

            System.out.print("Enter Your Email: ");
            String email = scanner.nextLine().trim();

            if (!ValidationUtil.isValidEmail(email)) {
                System.out.println("✗ Invalid email format.");
                return;
            }

            String validationResult = participantManager.validateParticipantCredentials(id, email);

            if (validationResult.equals("ID_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with ID '" + id + "' already exists.");
                System.out.println("   Please use your existing ID to check your status.");
                return;
            }

            if (validationResult.equals("EMAIL_EXISTS")) {
                System.out.println("✗ Registration Failed: A participant with email '" + email + "' already exists.");
                System.out.println("   Please check your team status using option 2.");
                return;
            }

            System.out.println("✓ ID and Email validated successfully. Please complete the survey.\n");

            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter Preferred Game: ");
            String preferredGame = scanner.nextLine().trim();

            int skillLevel = getUserIntInput(scanner, "Enter Skill Level (1-10): ", 1, 10);

            System.out.print("Enter Preferred Role (Strategist, Attacker, Defender, Supporter, Coordinator): ");
            String preferredRole = scanner.nextLine().trim();

            if (!ValidationUtil.isValidRole(preferredRole)) {
                System.out.println("⚠ Warning: Role '" + preferredRole + "' is not standard. Proceeding anyway...");
            }

            int personalityScore = runPersonalitySurvey(scanner);
            String personalityType = PersonalityClassifier.classifyPersonality(personalityScore);

            Participant newParticipant = new Participant(id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            participantManager.addParticipant(newParticipant);

            System.out.println("\n✓ Survey submitted successfully!");
            System.out.println("Your Participant ID: " + newParticipant.getId());
            System.out.println("Your Email: " + newParticipant.getEmail());
            System.out.println("Calculated Personality Type: " + personalityType);
            System.out.println("\nYou can now check your team status using option 2.");

        } catch (Exception e) {
            System.out.println("Submission Failed: " + e.getMessage());
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

        // Check current memory first
        Team team = teamBuilder.findTeamForParticipant(p);

        if (team != null) {
            System.out.println("\n   You are assigned to a team!");
            team.displayTeamInfo();
        } else {
            // Search in historical cumulative file with full details
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

    private int getUserIntInput(Scanner scanner, String prompt, int min, int max)
            throws TeamMateException.InvalidInputException {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    System.out.println("Input must be between " + min + " and " + max + ". Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}