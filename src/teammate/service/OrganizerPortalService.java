package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.concurrent.TeamFormationEngine;
import teammate.util.FileManager;
import teammate.util.SystemLogger;
import java.io.File;
import java.util.*;

/**
 * Organizer Portal Service - extends PortalService
 * Demonstrates: Inheritance and Polymorphism
 *
 * This class inherits common portal functionality from PortalService
 * and implements organizer-specific behavior through method overriding.
 */
public class OrganizerPortalService extends PortalService {
    private List<Participant> currentAssignmentPool;

    /**
     * Constructor - calls parent constructor and initializes organizer-specific data
     * Demonstrates: Inheritance (using super keyword) and Encapsulation
     */
    public OrganizerPortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        super(participantManager, teamBuilder);
        this.currentAssignmentPool = new ArrayList<>();
    }

    /**
     * OVERRIDDEN ABSTRACT METHODS FROM PortalService
     * Demonstrates: Polymorphism (runtime method binding)
     */

    @Override
    protected void displayWelcomeMessage() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("ORGANIZER PORTAL");
        System.out.println("=".repeat(50));
    }

    @Override
    protected void displayMenu() {
        System.out.println("1. Upload Participant File");
        System.out.println("2. Generate Teams");
        System.out.println("3. View Generated Teams");
        System.out.println("4. Search Team by ID");
        System.out.println("5. Export & Finalize Teams");
        System.out.println("6. Back to Main Menu");
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
        return 6;
    }

    @Override
    protected void displayGoodbyeMessage() {
        System.out.println("Returning to main menu...");
    }

    /**
     * ORGANIZER-SPECIFIC PRIVATE METHODS
     * These methods implement the core functionality of the organizer portal
     */

    /**
     * Upload CSV file with participant data
     * Processes external CSV files and validates participant information
     */
    private void uploadCSV() {
        try {
            System.out.println("\n--- UPLOAD PARTICIPANT FILE ---");

            // Use inherited getNonEmptyInput method from PortalService
            String filename = getNonEmptyInput("Enter the CSV filename: ");

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

            // Use inherited displaySeparator method from PortalService
            displaySeparator();
            System.out.println("UPLOAD SUMMARY");
            displaySeparator();
            System.out.println("New participants: " + newlyAdded.size());
            System.out.println("Duplicates skipped: " + result.get("duplicateAvailable"));
            System.out.println("Already assigned: " + result.get("duplicateAssigned"));
            System.out.println("Invalid records: " + result.get("invalidRecords"));
            displaySeparator();

            if (!newlyAdded.isEmpty()) {
                System.out.println("\n✓ " + newlyAdded.size() + " participants ready for team generation.");
            }

        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Generate teams from available participants
     * Uses TeamFormationEngine with concurrent processing
     */
    private void generateTeams() throws Exception {
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
                    System.out.println("\n✗ NO AVAILABLE PARTICIPANTS");
                    System.out.println("All participants are already assigned to teams.");
                    System.out.println("Please upload a new participant file to continue.");
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

        // Use inherited getUserIntInput method from PortalService
        int teamSize = getUserIntInput("Enter team size (minimum 3): ", 3, participantsToUse.size());

        System.out.println("\nStarting team formation...");

        long startTime = System.currentTimeMillis();

        // Use TeamFormationEngine for concurrent team formation
        TeamFormationEngine engine = new TeamFormationEngine(teamBuilder);
        int teamsFormed = engine.buildTeams(participantsToUse, teamSize);

        long endTime = System.currentTimeMillis();

        int assignedCount = teamsFormed * teamSize;
        int unassignedCount = participantsToUse.size() - assignedCount;

        // Use inherited displaySeparator method from PortalService
        displaySeparator();
        System.out.println("✓ TEAM FORMATION COMPLETE");
        displaySeparator();
        System.out.println("Teams formed: " + teamsFormed);
        System.out.println("Participants assigned: " + assignedCount);
        System.out.println("Participants unassigned: " + unassignedCount);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        displaySeparator();

        if (teamsFormed > 0) {
            System.out.println("\n✓ Teams generated but NOT finalized yet.");
            System.out.println("Proceed to [5. Export & Finalize Teams] to save permanently.");
        } else {
            System.out.println("\n⚠ No teams could be formed. Try adjusting team size.");
        }
    }

    /**
     * View all generated teams
     * Displays comprehensive information about all formed teams
     */
    private void viewAllTeams() {
        if (teamBuilder.getTeamCount() == 0) {
            System.out.println("\nNo teams have been generated yet.");
            return;
        }

        teamBuilder.displayAllTeams();
    }

    /**
     * Search for a specific team by ID
     * Retrieves and displays detailed team information
     */
    private void searchTeam() {
        System.out.println("\n--- SEARCH TEAM ---");

        // Use inherited getNonEmptyInput method from PortalService
        String teamId = getNonEmptyInput("Enter Team ID (e.g., TEAM0001): ");

        FileManager.searchTeamById(teamId, participantManager);
    }

    /**
     * Export and finalize teams
     * Saves teams to files and marks participants as assigned
     */
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

        // Use inherited getNonEmptyInput method from PortalService
        String snapshotFilename = getNonEmptyInput("Enter filename for export (e.g., Teams_Nov2024.csv): ");

        System.out.println("\nExporting teams...");

        // Export teams to files
        teamBuilder.exportTeamsSnapshot(snapshotFilename);
        teamBuilder.appendTeamsToCumulative();

        // Save the counter to file (for sequential numbering)
        Team.saveTeamCounterToFile();

        // Mark participants as assigned
        teamBuilder.markParticipantsAssigned();
        System.out.println("✓ " + assignedCount + " participants assigned to teams.");

        if (unassignedCount > 0) {
            System.out.println("\n⚠ " + unassignedCount + " participants remain unassigned.");

            System.out.println("\nWhat would you like to do with unassigned participants?");
            System.out.println("1. Keep for future team formation");
            System.out.println("2. Remove from system");

            // Use inherited getMenuChoice method from PortalService
            int choice = getMenuChoice();

            if (choice == 2) {
                participantManager.removeParticipants(unassignedInRun);
                System.out.println("✓ Removed " + unassignedCount + " unassigned participants.");
            } else {
                System.out.println("✓ Unassigned participants kept for future use.");
            }
        }

        participantManager.saveAllParticipants();

        // Use inherited displaySeparator method from PortalService
        displaySeparator();
        System.out.println("✓ EXPORT COMPLETE");
        displaySeparator();
        System.out.println("Snapshot saved: " + snapshotFilename);
        displaySeparator();

        teamBuilder.clearTeams();
        currentAssignmentPool.clear();
    }

    /**
     * Getter for TeamBuilder (if needed externally)
     * Provides access to the team builder instance
     */
    public TeamBuilder getTeamBuilder() {
        return teamBuilder;
    }
}