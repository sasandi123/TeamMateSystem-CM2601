package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.concurrent.TeamFormationEngine;
import teammate.util.FileManager;
import java.io.File;
import java.util.*;

public class OrganizerPortalService {
    private ParticipantManager participantManager;
    private TeamBuilder teamBuilder;
    private List<Participant> currentAssignmentPool;

    public OrganizerPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        this.participantManager = participantManager;
        this.teamBuilder = teamBuilder;
        this.currentAssignmentPool = new ArrayList<>();
    }

    public TeamBuilder getTeamBuilder() {
        return teamBuilder;
    }

    public void showPortal(Scanner scanner) {
        while (true) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("ORGANIZER PORTAL");
            System.out.println("=".repeat(50));
            System.out.println("1. Upload Participant File");
            System.out.println("2. Generate Teams");
            System.out.println("3. View Generated Teams");
            System.out.println("4. Search Team by ID");
            System.out.println("5. Export & Finalize Teams");
            System.out.println("6. Back to Main Menu");
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
                        searchTeam(scanner);
                        break;
                    case 5:
                        exportTeams(scanner);
                        break;
                    case 6:
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
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
            System.out.println("\n--- UPLOAD PARTICIPANT FILE ---");
            System.out.print("Enter the CSV filename: ");
            String filename = scanner.nextLine().trim();

            if (!new File(filename).exists()) {
                System.out.println("✗ File not found.");
                return;
            }

            Map<String, Object> result = participantManager.processExternalCSV(filename, scanner);

            if ((Boolean) result.get("cancelled")) {
                System.out.println("\n✗ Upload cancelled.");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Participant> newlyAdded = (List<Participant>) result.get("newlyAdded");

            currentAssignmentPool = newlyAdded;

            System.out.println("\n" + "=".repeat(50));
            System.out.println("UPLOAD SUMMARY");
            System.out.println("=".repeat(50));
            System.out.println("New participants: " + newlyAdded.size());
            System.out.println("Duplicates skipped: " + result.get("duplicateAvailable"));
            System.out.println("Already assigned: " + result.get("duplicateAssigned"));
            System.out.println("Invalid records: " + result.get("invalidRecords"));
            System.out.println("=".repeat(50));

            if (!newlyAdded.isEmpty()) {
                System.out.println("\n✓ " + newlyAdded.size() + " participants ready for team generation.");
            }

        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
        }
    }

    private void generateTeams(Scanner scanner) throws Exception {
        teamBuilder.resetCurrentGenerationStatus();

        List<Participant> participantsToUse;

        if (!currentAssignmentPool.isEmpty()) {
            participantsToUse = currentAssignmentPool;
            System.out.println("\nUsing uploaded participants (" + participantsToUse.size() + " total).");
        } else {
            System.out.println("\nNo file uploaded.");
            System.out.print("Use available participants from system? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("Y")) {
                participantManager.loadAllParticipants();
                participantsToUse = participantManager.getAvailableParticipants();

                if (participantsToUse.isEmpty()) {
                    System.out.println("\n" + "=".repeat(50));
                    System.out.println("✗ NO AVAILABLE PARTICIPANTS");
                    System.out.println("=".repeat(50));
                    System.out.println("All participants are already assigned to teams.");
                    System.out.println("Please upload a new participant file to continue.");
                    System.out.println("=".repeat(50));
                    return;
                }

                System.out.println("Found " + participantsToUse.size() + " available participants.");
            } else {
                System.out.println("Team generation cancelled.");
                return;
            }
        }

        currentAssignmentPool = new ArrayList<>(participantsToUse);

        if (participantsToUse.size() < 3) {
            System.out.println("\n✗ Not enough participants (minimum 3 required).");
            return;
        }

        System.out.print("\nEnter team size (minimum 3): ");
        int teamSize = getUserIntInput(scanner, "", 3, participantsToUse.size());

        System.out.println("\nStarting team formation...");
        System.out.println("Please wait...\n");

        long startTime = System.currentTimeMillis();

        TeamFormationEngine engine = new TeamFormationEngine(teamBuilder);
        int teamsFormed = engine.buildTeams(participantsToUse, teamSize);

        long endTime = System.currentTimeMillis();

        int assignedCount = teamsFormed * teamSize;
        int unassignedCount = participantsToUse.size() - assignedCount;

        System.out.println("\n" + "=".repeat(50));
        System.out.println("✓ TEAM FORMATION COMPLETE");
        System.out.println("=".repeat(50));
        System.out.println("Teams formed: " + teamsFormed);
        System.out.println("Participants assigned: " + assignedCount);
        System.out.println("Participants unassigned: " + unassignedCount);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        System.out.println("=".repeat(50));

        if (teamsFormed > 0) {
            System.out.println("\n⚠ Teams generated but NOT finalized yet.");
            System.out.println("Team IDs are temporary. Proceed to [5. Export & Finalize Teams] to:");
            System.out.println("  - Assign permanent team IDs");
            System.out.println("  - Save teams to files");
            System.out.println("  - Update participant statuses");
        } else {
            System.out.println("\n⚠ No teams could be formed. Try adjusting team size.");
        }
    }

    private void viewAllTeams() {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("\nNo teams have been generated yet.");
            return;
        }

        teamBuilder.displayAllTeams();
    }

    private void searchTeam(Scanner scanner) {
        System.out.println("\n--- SEARCH TEAM ---");
        System.out.print("Enter Team ID (e.g., TEAM0001): ");
        String teamId = scanner.nextLine().trim();

        FileManager.searchTeamById(teamId, participantManager);
    }

    private void exportTeams(Scanner scanner) throws Exception {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("\nNo teams to export. Please generate teams first.");
            return;
        }

        List<Participant> assignedInRun = teamBuilder.getAllParticipantsInTeams();
        List<Participant> unassignedInRun = new ArrayList<>(currentAssignmentPool);
        unassignedInRun.removeAll(assignedInRun);

        int assignedCount = assignedInRun.size();
        int unassignedCount = unassignedInRun.size();

        System.out.print("\nEnter filename for export (e.g., Teams_Nov2024.csv): ");
        String snapshotFilename = scanner.nextLine().trim();

        System.out.println("\nExporting teams...");

        // Export teams to files
        teamBuilder.exportTeamsSnapshot(snapshotFilename);
        teamBuilder.appendTeamsToCumulative();

        // NOW save the counter to file (only when finalized)
        Team.saveTeamCounterToFile();

        // Mark participants as assigned
        teamBuilder.markParticipantsAssigned();
        System.out.println("✓ " + assignedCount + " participants assigned to teams.");
        System.out.println("✓ Assigned participant statuses updated in memory.");

        if (unassignedCount > 0) {
            System.out.println("\n" + unassignedCount + " participants remain unassigned.");

            System.out.println("\nWhat would you like to do with unassigned participants?");
            System.out.println("1. Keep for future team formation");
            System.out.println("2. Remove from system");
            System.out.print("Enter choice: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice == 2) {
                    participantManager.removeParticipants(unassignedInRun);
                    System.out.println("✓ Removed " + unassignedCount + " unassigned participants.");
                } else {
                    System.out.println("✓ Unassigned participants kept for future use.");
                }
            } catch (NumberFormatException e) {
                System.out.println("✓ Keeping unassigned participants by default.");
            }
        }

        participantManager.saveAllParticipants();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("✓ EXPORT COMPLETE");
        System.out.println("=".repeat(50));
        System.out.println("Snapshot saved: " + snapshotFilename);
        System.out.println("Historical records updated");
        System.out.println("Participant statuses saved");
        System.out.println("Team counter saved to file");
        System.out.println("All changes saved successfully");
        System.out.println("=".repeat(50));

        teamBuilder.clearTeams();
        currentAssignmentPool.clear();
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
                    System.out.println("Please enter a number between " + min + " and " + max + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }
}