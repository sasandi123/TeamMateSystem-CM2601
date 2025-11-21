package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.util.PersonalityClassifier;
import teammate.util.FileManager;
import teammate.concurrent.TeamBuildingTask;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main service class coordinating the TeamMate System
 */
public class TeamMateSystem {
    private ParticipantManager participantManager;
    private TeamBuilder teamBuilder;
    private List<Participant> currentAssignmentPool;

    public TeamMateSystem() {
        this.participantManager = new ParticipantManager();
        this.teamBuilder = new TeamBuilder();
        this.currentAssignmentPool = new ArrayList<>();
        participantManager.loadAllParticipants();
    }

    public ParticipantManager getParticipantManager() {
        return participantManager;
    }

    // ============================================
    // PARTICIPANT PORTAL
    // ============================================

    public void participantPortal(Scanner scanner) {
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

            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter Email: ");
            String email = scanner.nextLine().trim();

            System.out.print("Enter Preferred Game: ");
            String preferredGame = scanner.nextLine().trim();

            int skillLevel = getUserIntInput(scanner, "Enter Skill Level (1-10): ", 1, 10);

            System.out.print("Enter Preferred Role (Strategist, Attacker, Defender, Supporter, Coordinator): ");
            String preferredRole = scanner.nextLine().trim();

            int personalityScore = runPersonalitySurvey(scanner);
            String personalityType = PersonalityClassifier.classifyPersonality(personalityScore);

            Participant newParticipant = new Participant(name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            participantManager.addParticipant(newParticipant);

            System.out.println("\n✓ Survey submitted successfully!");
            System.out.println("Your UNIQUE Participant ID is: " + newParticipant.getId());
            System.out.println("Please use this ID to check your team status later.");
            System.out.println("Calculated Personality Type: " + personalityType);

        } catch (TeamMateException.DuplicateParticipantException e) {
            System.out.println("Submission Failed: " + e.getMessage());
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
            // Search in historical cumulative file
            System.out.println("\n   Searching historical team records...");
            FileManager.findParticipantTeamInCumulative(p.getId());
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

    // ============================================
    // ORGANIZER PORTAL
    // ============================================

    public void organizerPortal(Scanner scanner) {
        while (true) {
            System.out.println("\n--- ORGANIZER PORTAL ---");
            System.out.println("1. Upload External Participant CSV");
            System.out.println("2. Generate Teams");
            System.out.println("3. View Generated Teams");
            System.out.println("4. Export/Finalize Teams");
            System.out.println("5. Back to Main Menu");
            System.out.print("Enter choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        uploadCSV(scanner);
                        break;
                    case 2:
                        generateTeams(scanner);
                        break;
                    case 3:
                        viewAllTeams();
                        break;
                    case 4:
                        exportTeams(scanner);
                        break;
                    case 5:
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

    private void uploadCSV(Scanner scanner) {
        try {
            System.out.println("\n--- UPLOAD EXTERNAL CSV ---");
            System.out.print("Enter the filename of the CSV to upload: ");
            String filename = scanner.nextLine().trim();

            if (!new File(filename).exists()) {
                System.out.println("✗ Error: File not found.");
                return;
            }

            currentAssignmentPool = participantManager.processExternalCSV(filename);

            System.out.println("✓ Upload complete. " + currentAssignmentPool.size() +
                    " unique participants ready for team generation.");

        } catch (Exception e) {
            System.out.println("Upload Failed: " + e.getMessage());
        }
    }

    private void generateTeams(Scanner scanner) throws Exception {
        // Reset previous team assignments
        teamBuilder.resetTeamMembersStatus();

        List<Participant> participantsToUse;

        if (!currentAssignmentPool.isEmpty()) {
            participantsToUse = currentAssignmentPool;
            System.out.println("\nUsing recently uploaded group (" + participantsToUse.size() + " participants).");
        } else {
            System.out.println("\nNo new file uploaded.");
            System.out.println("Use currently Available participants? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("Y")) {
                participantsToUse = participantManager.getAvailableParticipants();
                System.out.println("Using " + participantsToUse.size() + " available participants.");
            } else {
                System.out.println("Team generation cancelled.");
                return;
            }
        }

        if (participantsToUse.size() < 3) {
            System.out.println("Not enough participants to form teams.");
            return;
        }

        System.out.print("Enter desired Team Size (Min 3): ");
        int teamSize = getUserIntInput(scanner, "", 3, participantsToUse.size());

        System.out.println("\nGenerating teams...");

        // Use Callable for concurrent team building
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TeamBuildingTask task = new TeamBuildingTask(participantsToUse, teamSize, teamBuilder);

        try {
            Future<Integer> future = executor.submit(task);
            Integer teamsFormed = future.get(); // Wait for completion

            System.out.println("✓ Teams generated successfully!");
            System.out.println("Total teams formed: " + teamsFormed);

            if (teamsFormed > 0) {
                teamBuilder.markParticipantsAssigned();
            }
        } finally {
            executor.shutdown();
        }
    }

    private void viewAllTeams() {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("No teams have been generated yet.");
            return;
        }

        teamBuilder.displayAllTeams();
    }

    private void exportTeams(Scanner scanner) throws Exception {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("No teams to export. Please generate teams first.");
            return;
        }

        System.out.print("Enter filename for snapshot (e.g., Run_1_Nov.csv): ");
        String snapshotFilename = scanner.nextLine().trim();

        // Export teams
        teamBuilder.exportTeamsSnapshot(snapshotFilename);
        teamBuilder.appendTeamsToCumulative();

        // Handle unassigned participants
        List<Participant> unassigned = participantManager.getAvailableParticipants();

        if (!unassigned.isEmpty()) {
            System.out.println("\n" + unassigned.size() + " participants remain unassigned.");
            System.out.println("Options:");
            System.out.println("1. Keep them as 'Available' for future team formation");
            System.out.println("2. Remove them from the participant pool");
            System.out.print("Enter choice (1 or 2): ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 2) {
                    participantManager.removeParticipants(unassigned);
                    System.out.println("✓ Removed " + unassigned.size() + " unassigned participants.");
                } else {
                    System.out.println("✓ Unassigned participants kept as 'Available'.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Keeping participants as 'Available'.");
            }
        }

        // Save final state
        participantManager.saveAllParticipants();
        System.out.println("✓ All changes saved successfully!");

        // Clear the current assignment pool
        currentAssignmentPool.clear();
    }
}