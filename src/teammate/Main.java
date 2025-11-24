package teammate;

import teammate.service.ParticipantPortalService;
import teammate.service.OrganizerPortalService;
import teammate.service.ParticipantManager;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        ParticipantManager participantManager = new ParticipantManager();
        ParticipantPortalService participantPortal = new ParticipantPortalService(participantManager);
        OrganizerPortalService organizerPortal = new OrganizerPortalService(participantManager);

        System.out.println("=================================");
        System.out.println("  TEAMMATE SYSTEM");
        System.out.println("  Intelligent Team Formation");
        System.out.println("=================================\n");

        while (true) {
            System.out.println("\nSelect Portal:");
            System.out.println("1. Participant Portal");
            System.out.println("2. Organizer Portal");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");

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
                        System.out.println("Thank you for using TeamMate System! Saving final state...");
                        participantManager.saveAllParticipants();
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
    }
}