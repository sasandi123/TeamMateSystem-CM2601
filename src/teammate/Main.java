package teammate;

import teammate.service.ParticipantPortalService;
import teammate.service.OrganizerPortalService;
import teammate.service.ParticipantManager;
import teammate.service.TeamBuilder;
import java.util.Scanner;

/**
 * TeamMate System - Main Entry Point
 * Intelligent Team Formation System for Esports
 */
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Initialize shared components
        ParticipantManager participantManager = new ParticipantManager();
        TeamBuilder sharedTeamBuilder = new TeamBuilder();

        // Create portals with shared components
        ParticipantPortalService participantPortal =
                new ParticipantPortalService(participantManager, sharedTeamBuilder);
        OrganizerPortalService organizerPortal =
                new OrganizerPortalService(participantManager, sharedTeamBuilder);

        // CRITICAL: Link organizer portal to participant manager
        // This allows participant manager to call displayAssignedParticipantsDetails
        participantManager.setOrganizerPortal(organizerPortal);

        displayWelcomeScreen();

        while (true) {
            displayMainMenu();

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                switch (choice) {
                    case 1:
                        participantPortal.showPortal(scanner);
                        break;
                    case 2:
                        organizerPortal.showPortal(scanner);
                        break;
                    case 3:
                        System.out.println("\n" + "=".repeat(60));
                        System.out.println(centerText("THANK YOU FOR USING TEAMMATE SYSTEM", 60));
                        System.out.println("=".repeat(60));
                        System.out.println("Saving data...");
                        participantManager.saveAllParticipants();
                        System.out.println("Data saved successfully.");
                        System.out.println("=".repeat(60) + "\n");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("\n[!] Invalid choice. Please select 1, 2, or 3.\n");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[!] Invalid input. Please enter a number.\n");
            } catch (Exception e) {
                System.out.println("\n[!] Error: " + e.getMessage() + "\n");
            }
        }
    }

    private static void displayWelcomeScreen() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("|" + " ".repeat(58) + "|");
        System.out.println("|" + centerText("T E A M M A T E   S Y S T E M", 58) + "|");
        System.out.println("|" + centerText("Intelligent Esports Team Formation", 58) + "|");
        System.out.println("|" + " ".repeat(58) + "|");
        System.out.println("=".repeat(60) + "\n");
    }

    private static void displayMainMenu() {
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.println("|" + centerText("MAIN MENU", 58) + "|");
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.println("|  [1] Participant Portal" + " ".repeat(34) + "|");
        System.out.println("|  [2] Organizer Portal" + " ".repeat(36) + "|");
        System.out.println("|  [3] Exit System" + " ".repeat(41) + "|");
        System.out.println("+" + "-".repeat(58) + "+");
        System.out.print("\nYour choice: ");
    }

    public static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        int rightPadding = width - padding - text.length();
        return " ".repeat(Math.max(0, padding)) + text + " ".repeat(Math.max(0, rightPadding));
    }
}