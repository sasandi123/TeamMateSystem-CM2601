package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.concurrent.TeamFormationEngine;
import teammate.util.FileManager;
import java.io.File;
import java.util.*;

import static teammate.Main.centerText;

// Handles organizer-specific operations including team generation and export
public class OrganizerPortalService extends PortalService {
    private List<Participant> currentAssignmentPool;

    public OrganizerPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        super(participantManager, teamBuilder);
        this.currentAssignmentPool = new ArrayList<>();
    }

    @Override
    protected void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("|" + centerText("ORGANIZER PORTAL", 58) + "|");
        System.out.println("=".repeat(60));
    }

    @Override
    protected void displayMenu() {
        System.out.println("\n+" + "-".repeat(58) + "+");
        System.out.println("|  [1] Upload Participant File" + " ".repeat(29) + "|");
        System.out.println("|  [2] Generate Teams" + " ".repeat(38) + "|");
        System.out.println("|  [3] View Generated Teams" + " ".repeat(32) + "|");
        System.out.println("|  [4] Search Team by ID" + " ".repeat(35) + "|");
        System.out.println("|  [5] Export & Finalize Teams" + " ".repeat(29) + "|");
        System.out.println("|  [6] Return to Main Menu" + " ".repeat(33) + "|");
        System.out.println("+" + "-".repeat(58) + "+");
    }

    @Override
    protected void handleMenuChoice(int choice) {
        try {
            switch (choice) {
                case 1:
                    uploadCSV();
                    break;
                case 2:
                    generateTeams();
                    break;
                case 3:
                    viewAllTeams();
                    break;
                case 4:
                    searchTeam();
                    break;
                case 5:
                    exportTeams();
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
        return 6;
    }

    @Override
    protected void displayGoodbyeMessage() {
        System.out.println("Returning to main menu...");
    }

    // Uploads CSV file with participant data for tournament
    private void uploadCSV() throws Exception {
        System.out.println("\n--- UPLOAD PARTICIPANT FILE ---");

        String filename = getNonEmptyInput("Enter CSV filename: ");

        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("[X] File not found: " + filename);
            return;
        }

        // Process CSV and handle results
        Map<String, Object> result = participantManager.processExternalCSV(filename, scanner);

        if ((Boolean) result.get("cancelled")) {
            System.out.println("Upload cancelled.");
            return;
        }

        // Store participants for this session's team formation
        currentAssignmentPool = (List<Participant>) result.get("newlyAdded");

        System.out.println("\nParticipants ready for team generation.");
    }

    // Generates teams using automatic mode selection (sequential/parallel)
    private void generateTeams() throws Exception {
        System.out.println("\n--- GENERATE TEAMS ---");

        List<Participant> participantsToUse;

        // Use uploaded participants or available system participants
        if (currentAssignmentPool.isEmpty()) {
            System.out.print("\nNo participants uploaded. Use available participants from system? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("Y")) {
                participantsToUse = participantManager.getAvailableParticipants();

                if (participantsToUse.isEmpty()) {
                    System.out.println("[X] No available participants in system.");
                    return;
                }

                System.out.println("[OK] Found " + participantsToUse.size() + " available participants.");
            } else {
                System.out.println("Team generation cancelled.");
                return;
            }
        } else {
            participantsToUse = new ArrayList<>(currentAssignmentPool);
        }

        int teamSize = getUserIntInput("\nEnter team size (minimum 3): ", 3, 10);

        if (participantsToUse.size() < teamSize) {
            System.out.println("\n[X] Not enough participants. Need at least " + teamSize + " participants.");
            return;
        }

        // Reset any previous generation
        teamBuilder.resetCurrentGenerationStatus();
        Team.resetTeamCounter();

        // Use engine for automatic mode selection
        TeamFormationEngine engine = new TeamFormationEngine(teamBuilder);
        int teamsFormed = engine.buildTeams(participantsToUse, teamSize);

        if (teamsFormed > 0) {
            System.out.println("\n[OK] Successfully formed " + teamsFormed + " teams!");
            System.out.println("Use option 3 to view teams or option 5 to export them.");
        } else {
            System.out.println("\n[X] Could not form any teams with current participants.");
        }
    }

    // Displays all generated teams with full details
    private void viewAllTeams() {
        System.out.println("\n--- VIEW GENERATED TEAMS ---");

        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("\nNo teams have been generated yet.");
            System.out.println("Use option 2 to generate teams first.");
            return;
        }

        teamBuilder.displayAllTeams();
    }

    // Searches for a specific team by ID in historical records
    private void searchTeam() {
        System.out.println("\n--- SEARCH TEAM ---");

        String teamId = getNonEmptyInput("Enter Team ID (e.g., TEAM0001): ");

        FileManager.searchTeamById(teamId, participantManager);
    }

    // Exports teams to files and finalizes participant assignments
    private void exportTeams() throws Exception {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("\nNo teams to export. Please generate teams first.");
            return;
        }

        List<Participant> assignedInRun = teamBuilder.getAllParticipantsInTeams();
        List<Participant> unassignedInRun = new ArrayList<>(currentAssignmentPool);
        unassignedInRun.removeAll(assignedInRun);

        int assignedCount = assignedInRun.size();
        int unassignedCount = unassignedInRun.size();

        String snapshotFilename = getNonEmptyInput("Enter filename for export (e.g., Teams_Nov2024.csv): ");

        System.out.println("\nExporting teams...");

        // Export to both snapshot and cumulative files
        teamBuilder.exportTeamsSnapshot(snapshotFilename);
        teamBuilder.appendTeamsToCumulative();

        // Save team counter for sequential ID management
        Team.saveTeamCounterToFile();

        // Update participant status
        teamBuilder.markParticipantsAssigned();
        System.out.println("[OK] " + assignedCount + " participants assigned to teams.");

        // Handle unassigned participants
        if (unassignedCount > 0) {
            System.out.println("\n[!] " + unassignedCount + " participants remain unassigned.");

            System.out.print("\nView unassigned participants? (Y/N): ");
            String viewChoice = scanner.nextLine().trim().toUpperCase();

            if (viewChoice.equals("Y")) {
                System.out.println("\nUnassigned Participants:");
                for (int i = 0; i < unassignedInRun.size(); i++) {
                    Participant p = unassignedInRun.get(i);
                    System.out.println((i + 1) + ". " + p.getId() + " - " + p.getName() +
                            " | Game: " + p.getPreferredGame() + " | Skill: " + p.getSkillLevel());
                }
            }

            System.out.print("\nKeep unassigned for future tournaments or remove? (1=Keep / 2=Remove): ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("2")) {
                participantManager.removeParticipants(unassignedInRun);
                System.out.println("[OK] Removed " + unassignedCount + " unassigned participants.");
            } else {
                System.out.println("[OK] Keeping " + unassignedCount + " participants as available.");
            }
        }

        // Save changes and clear temporary data
        participantManager.saveAllParticipants();
        teamBuilder.clearTeams();
        currentAssignmentPool.clear();

        System.out.println("\n[OK] Export complete! Teams saved to:");
        System.out.println("  - Snapshot: " + snapshotFilename);
        System.out.println("  - Cumulative: formed_teams_cumulative.csv");
    }

    // Displays details of previously assigned participants
    public void displayAssignedParticipantsDetails(List<String> participantIds, Map<String, String> assignedDetails) {
        System.out.println("\nPreviously Assigned Participants:");
        System.out.println("=".repeat(70));

        for (int i = 0; i < participantIds.size(); i++) {
            String fullInfo = participantIds.get(i);
            String id = fullInfo.split(" - ")[0];

            System.out.println((i + 1) + ". " + fullInfo);
            System.out.println("   " + assignedDetails.get(id));
        }

        System.out.println("=".repeat(70));
    }
}