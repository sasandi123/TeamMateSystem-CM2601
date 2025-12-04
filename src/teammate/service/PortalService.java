package teammate.service;

import teammate.exception.TeamMateException;
import java.util.Scanner;

// Abstract base class providing common functionality for both participant and organizer portals
public abstract class PortalService {
    protected Scanner scanner;
    protected ParticipantManager participantManager;
    protected TeamBuilder teamBuilder;

    public PortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        this.participantManager = participantManager;
        this.teamBuilder = teamBuilder;
    }

    // Main portal flow controlling the user interface loop
    public final void showPortal(Scanner scanner) {
        this.scanner = scanner;

        while (true) {
            displayWelcomeMessage();
            displayMenu();

            int choice = getMenuChoice();

            if (choice == getExitOption()) {
                displayGoodbyeMessage();
                break;
            }

            handleMenuChoice(choice);
        }
    }

    // Abstract methods to be implemented by subclasses
    protected abstract void displayWelcomeMessage();
    protected abstract void displayMenu();
    protected abstract void handleMenuChoice(int choice);
    protected abstract int getExitOption();
    protected abstract void displayGoodbyeMessage();

    // Gets menu choice from user with input validation
    protected int getMenuChoice() {
        System.out.print("Enter choice: ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return -1;
        }
    }

    // Gets validated integer input within specified range
    protected int getUserIntInput(String prompt, int min, int max)
            throws TeamMateException.InvalidInputException {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please enter a number between " +
                        min + " and " + max + ".");
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

    // Gets non-empty string input from user
    protected String getNonEmptyInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (!input.isEmpty()) {
                return input;
            }

            System.out.println("Input cannot be empty. Please try again.");
        }
    }

    // Displays a separator line for visual organization
    protected void displaySeparator() {
        System.out.println("=".repeat(50));
    }

    // Pauses execution until user presses Enter
    protected void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}