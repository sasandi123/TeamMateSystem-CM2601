package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.concurrent.TeamFormationEngine;
import teammate.util.FileManager;
import java.io.File;
import java.util.*;

import static teammate.Main.centerText;

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
                System.out.println("[X] File not found.");
                return;
            }

            Map<String, Object> result = participantManager.processExternalCSV(filename, scanner);

            if ((Boolean) result.get("cancelled")) {
                System.out.println("\n[X] Upload cancelled.");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Participant> newlyAdded = (List<Participant>) result.get("newlyAdded");

            // CRITICAL FIX: Store the uploaded participants for THIS session
            // This ensures team generation uses ONLY these participants
            currentAssignmentPool = new ArrayList<>(newlyAdded);

        } catch (Exception e) {
            System.out.println("Upload failed: " + e.getMessage());
        }
    }

    /**
     * Generate teams from available participants
     * Uses TeamFormationEngine with concurrent processing
     * FIXED: Now strictly uses uploaded file participants only
     */
    private void generateTeams() throws Exception {
        teamBuilder.resetCurrentGenerationStatus();

        List<Participant> participantsToUse;

        // CRITICAL FIX: If file uploaded, use ONLY those participants
        if (!currentAssignmentPool.isEmpty()) {
            participantsToUse = new ArrayList<>(currentAssignmentPool);
            System.out.println("\nUsing uploaded file participants (" + participantsToUse.size() + " total).");
            System.out.println("[System] These participants are from your current upload session.");
        } else {
            // No file uploaded - offer to use system's available participants
            System.out.println("\nNo file uploaded.");
            System.out.print("Use available participants from system? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("Y")) {
                participantManager.loadAllParticipants();
                participantsToUse = participantManager.getAvailableParticipants();

                if (participantsToUse.isEmpty()) {
                    System.out.println("\n[X] NO AVAILABLE PARTICIPANTS");
                    System.out.println("All participants are already assigned to teams.");
                    System.out.println("Please upload a new participant file to continue.");
                    return;
                }

                System.out.println("Found " + participantsToUse.size() + " available participants.");

                // Store these for this session
                currentAssignmentPool = new ArrayList<>(participantsToUse);
            } else {
                System.out.println("Team generation cancelled.");
                return;
            }
        }

        if (participantsToUse.size() < 3) {
            System.out.println("\n[X] Not enough participants (minimum 3 required).");
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
        System.out.println("[OK] TEAM FORMATION COMPLETE");
        displaySeparator();
        System.out.println("Teams formed: " + teamsFormed);
        System.out.println("Participants assigned: " + assignedCount);
        System.out.println("Participants unassigned: " + unassignedCount);
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        displaySeparator();

        if (teamsFormed > 0) {
            System.out.println("\n[OK] Teams generated but NOT finalized yet.");
            System.out.println("Proceed to [5. Export & Finalize Teams] to save permanently.");
        } else {
            System.out.println("\n[!] No teams could be formed. Try adjusting team size.");
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
        System.out.println("[OK] " + assignedCount + " participants assigned to teams.");

        if (unassignedCount > 0) {
            System.out.println("\n[!] " + unassignedCount + " participants remain unassigned.");

            // Ask if user wants to view unassigned participants
            System.out.print("\nView unassigned participants? (Y/N): ");
            String viewChoice = scanner.nextLine().trim().toUpperCase();

            if (viewChoice.equals("Y")) {
                displayUnassignedParticipants(unassignedInRun);
            }

            System.out.println("\nWhat would you like to do with unassigned participants?");
            System.out.println("1. Keep for future team formation");
            System.out.println("2. Remove from system");

            // Use inherited getMenuChoice method from PortalService
            int choice = getMenuChoice();

            if (choice == 2) {
                participantManager.removeParticipants(unassignedInRun);
                System.out.println("[OK] Removed " + unassignedCount + " unassigned participants.");
            } else {
                System.out.println("[OK] Unassigned participants kept for future use.");
            }
        }

        participantManager.saveAllParticipants();

        // Use inherited displaySeparator method from PortalService
        displaySeparator();
        System.out.println("[OK] EXPORT COMPLETE");
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

    /**
     * Display unassigned participants in a clean format
     * Helper method to reduce noise in main flow
     */
    private void displayUnassignedParticipants(List<Participant> unassignedParticipants) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("UNASSIGNED PARTICIPANTS (" + unassignedParticipants.size() + ")");
        System.out.println("=".repeat(60));

        for (int i = 0; i < unassignedParticipants.size(); i++) {
            Participant p = unassignedParticipants.get(i);
            System.out.printf("%d. %s - %s | Game: %s | Skill: %d | Role: %s | Type: %s\n",
                    (i + 1), p.getId(), p.getName(), p.getPreferredGame(),
                    p.getSkillLevel(), p.getPreferredRole(), p.getPersonalityType());
        }

        System.out.println("=".repeat(60));
    }

    /**
     * Display assigned participants details
     * Shows details only when organizer wants to see them
     */
    void displayAssignedParticipantsDetails(List<String> duplicateAssigned,
                                            Map<String, String> assignedDetails) {
        System.out.println("\n╔" + "═".repeat(78) + "╗");
        System.out.println("║" + centerText("PREVIOUSLY ASSIGNED PARTICIPANTS", 78) + "║");
        System.out.println("╠" + "═".repeat(78) + "╣");

        System.out.println("\n┌" + "─".repeat(78) + "┐");
        System.out.println("│" + centerText("PARTICIPANT DETAILS", 78) + "│");
        System.out.println("├" + "─".repeat(78) + "┤");

        for (int i = 0; i < duplicateAssigned.size(); i++) {
            String dup = duplicateAssigned.get(i);
            String[] parts = dup.split(" - ");
            String participantId = parts[0];
            String nameAndEmail = parts.length > 1 ? parts[1] : "";

            // Extract name and email
            String name = "";
            String email = "";
            if (!nameAndEmail.isEmpty()) {
                int emailStart = nameAndEmail.lastIndexOf('(');
                if (emailStart != -1) {
                    name = nameAndEmail.substring(0, emailStart).trim();
                    email = nameAndEmail.substring(emailStart + 1, nameAndEmail.length() - 1).trim();
                }
            }

            // Get participant details from system
            Participant p = participantManager.findParticipant(participantId);

            System.out.println("│");
            System.out.println("│ " + (i + 1) + ". Participant ID: " + participantId);
            System.out.println("│    Name         : " + (name.isEmpty() && p != null ? p.getName() : name));

            // Show assignment history
            String info = assignedDetails.get(participantId);
            if (info != null) {
                System.out.println("│    Assignment   : " + info);
            }

            if (i < duplicateAssigned.size() - 1) {
                System.out.println("│" + " ".repeat(78) + "│");
                System.out.println("│" + "·".repeat(78) + "│");
            }
        }

        System.out.println("│");
        System.out.println("└" + "─".repeat(78) + "┘");
    }
}