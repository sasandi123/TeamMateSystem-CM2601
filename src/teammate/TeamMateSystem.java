// TeamMateSystem.java
package teammate;

import java.util.*;

public class TeamMateSystem {
    private ParticipantManager participantManager;
    private TeamBuilder teamBuilder;

    public TeamMateSystem() {
        this.participantManager = new ParticipantManager();
        this.teamBuilder = new TeamBuilder();

        // CRITICAL: Load existing unassigned participants pool at startup
        try {
            participantManager.loadInitialUnassignedPool();
        } catch (Exception e) {
            System.out.println("System Initialization Warning: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // PARTICIPANT PORTAL
    // -----------------------------------------------------------------

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

            System.out.print("Enter Preferred Role (e.g., Strategist, Attacker, Defender, Supporter, Coordinator): ");
            String preferredRole = scanner.nextLine().trim();

            // CRITICAL CHANGE: Calculate personality score via survey
            int personalityScore = runPersonalitySurvey(scanner);

            String personalityType = PersonalityClassifier.classifyPersonality(personalityScore);

            // Create new participant object
            Participant newParticipant = new Participant(name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            // Add to manager. Manager handles duplicate checks.
            participantManager.addParticipant(newParticipant);

            // Save new participant state immediately to the unassigned pool file
            participantManager.saveUnassignedParticipantsPool();

            System.out.println("\n✓ Survey submitted successfully!");
            System.out.println("Your ID is: " + newParticipant.getId());
            System.out.println("Calculated Personality Score: " + personalityScore);
            System.out.println("Predicted Personality: " + personalityType + " (" +
                    PersonalityClassifier.getDescription(personalityType) + ")");

        } catch (TeamMateException.InvalidInputException e) {
            System.out.println("Submission Failed: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Submission Failed (Error): " + e.getMessage());
        }
    }

    /**
     * Runs the 5-question survey and calculates the final personality score (20-100).
     */
    private int runPersonalitySurvey(Scanner scanner) throws TeamMateException.InvalidInputException {
        System.out.println("\n--- PERSONALITY SURVEY (Rate 1-5) ---");
        System.out.println("1 = Strongly Disagree, 5 = Strongly Agree");

        String[] questions = {
                "Q1: I enjoy taking the lead and guiding others during group activities.",
                "Q2: I prefer analyzing situations and coming up with strategic solutions.",
                "Q3: I work well with others and enjoy collaborative teamwork.",
                "Q4: I am calm under pressure and can help maintain team morale.",
                "Q5: I like making quick decisions and adapting in dynamic situations."
        };
        int totalRawScore = 0;

        for (int i = 0; i < questions.length; i++) {
            totalRawScore += getUserIntInput(scanner, questions[i] + " [1-5]: ", 1, 5);
        }

        // Final Score = Raw Score (5-25) * 4 to scale to 100 (20-100)
        int personalityScore = totalRawScore * 4;
        System.out.println("Raw Survey Score: " + totalRawScore + "/25");
        return personalityScore;
    }

    // ... (checkMyTeam and getUserIntInput methods remain the same) ...

    private void checkMyTeam(Scanner scanner) {
        System.out.print("Enter your Participant ID or Email: ");
        String searchKey = scanner.nextLine().trim();

        Participant p = participantManager.findParticipant(searchKey);

        if (p == null) {
            System.out.println("Error: Participant not found.");
            return;
        }

        if (p.getStatus().equals("Available")) {
            System.out.println("Status: You are currently **unassigned** and available for team formation.");
            return;
        }

        Team team = teamBuilder.findTeamForParticipant(p);

        if (team != null) {
            System.out.println("\n--- YOUR TEAM ---");
            System.out.println("ID: " + p.getId() + ", Name: " + p.getName());
            team.displayTeamInfo();
        } else {
            System.out.println("Status: " + p.getStatus() + ". Team generated but not currently loaded in memory or exported.");
        }
    }

    private int getUserIntInput(Scanner scanner, String prompt, int min, int max) throws TeamMateException.InvalidInputException {
        while (true) {
            System.out.print(prompt);
            try {
                String input = scanner.nextLine().trim();
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                } else {
                    throw new TeamMateException.InvalidInputException("Value must be between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                throw new TeamMateException.InvalidInputException("Invalid input. Please enter a valid number.");
            }
        }
    }

    // -----------------------------------------------------------------
    // ORGANIZER PORTAL (Workflow Control remains the same)
    // -----------------------------------------------------------------

    public void organizerPortal(Scanner scanner) {
        while (true) {
            System.out.println("\n--- ORGANIZER PORTAL ---");
            System.out.println("1. Load Participant Data (CSV)");
            System.out.println("2. Generate Teams (Temporary assignment only)");
            System.out.println("3. View Current Generated Teams");
            System.out.println("4. Export Teams to CSV (**Finalizes Assignments**)");
            System.out.println("5. Back to Main Menu (Saves AVAILABLE Pool)");
            System.out.print("Enter choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        loadParticipantData(scanner);
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
                        // Warn if teams are in a temporary state
                        if (teamBuilder.getTeamCount() > 0) {
                            System.out.println("Warning: " + teamBuilder.getTeamCount() + " teams were generated but not exported/finalized. They will be lost.");
                        }
                        // Always save the current state of AVAILABLE participants before exiting
                        participantManager.saveUnassignedParticipantsPool();
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

    private void loadParticipantData(Scanner scanner) throws TeamMateException.FileReadException {
        System.out.print("Enter CSV filename (or full path) to load: ");
        String filename = scanner.nextLine().trim();

        // Pass the filename to the manager, which handles loading and avoiding duplicates
        participantManager.loadParticipantsFromCSV(filename);

        System.out.println("Successfully loaded new participants and updated the system pool.");
    }

    private void generateTeams(Scanner scanner) throws Exception {

        // 1. Clear any previously generated teams from memory
        teamBuilder.clearTeams();

        // Get the list of participants where status == "Available"
        List<Participant> currentAvailablePool = participantManager.getAvailableParticipants();
        int availableCount = currentAvailablePool.size();

        if (availableCount == 0) {
            System.out.println("No unassigned participants available in the system pool. Please load a CSV first (Option 1).");
            return;
        }

        System.out.print("Enter desired team size (e.g., 5): ");
        int teamSize = Integer.parseInt(scanner.nextLine().trim());

        if (teamSize < 3) {
            throw new TeamMateException.InvalidInputException("Team size must be at least 3");
        }

        if (availableCount < teamSize) {
            System.out.println("Insufficient participants (" + availableCount +
                    ") to form a team of size " + teamSize + ". Need at least " + teamSize + ".");
            return;
        }

        System.out.println("\nGenerating teams from the persistent pool of " + availableCount + " available participants...");

        // Start team building concurrently
        TeamBuildingTask task = new TeamBuildingTask(currentAvailablePool, teamSize, teamBuilder);

        Thread buildThread = new Thread(task);
        buildThread.start();
        buildThread.join(); // Wait for completion

        System.out.println("✓ Teams generated successfully! (In Memory)");
        System.out.println("Total teams formed: " + teamBuilder.getTeamCount());

        if (teamBuilder.getTeamCount() > 0) {
            System.out.println("\nNote: Teams are generated but NOT FINALIZED. Select Option 4 (Export Teams) to finalize assignments.");
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
            System.out.println("No teams to export. Generate teams first (Option 2).");
            return;
        }

        System.out.print("Enter output filename (e.g., formed_teams.csv): ");
        String filename = scanner.nextLine().trim();

        if (!filename.endsWith(".csv")) {
            filename += ".csv";
        }

        // 1. Export the teams (WRITE the file)
        teamBuilder.exportTeamsToCSV(filename);

        // 2. COMMIT/FINALIZE: Mark all exported participants as "Assigned"
        for (Team team : teamBuilder.getTeams()) {
            for (Participant p : team.getMembers()) {
                p.setStatus("Assigned");
            }
        }

        // 3. SAVE STATE: Update the unassigned_participants.csv file
        // This saves all participants with status 'Available' (the remaining ones)
        participantManager.saveUnassignedParticipantsPool();

        // 4. CLEAR: Clear the current in-memory teams after finalization
        teamBuilder.clearTeams();

        System.out.println("✓ Teams successfully exported and **assignments finalized** to " + filename);
        System.out.println("The unassigned participant pool has been updated.");
    }
}