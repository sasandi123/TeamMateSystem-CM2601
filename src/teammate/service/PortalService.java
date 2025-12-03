package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import java.util.Scanner;

/**
 * Abstract base class for portal services

 * This class provides common functionality for both Participant and Organizer portals,
 * while allowing subclasses to implement their specific behavior.
 */
public abstract class PortalService {
    protected Scanner scanner;
    protected ParticipantManager participantManager;
    protected TeamBuilder teamBuilder;

    /**
     * Constructor - initializes shared components
     */
    public PortalService(ParticipantManager participantManager, TeamBuilder teamBuilder) {
        this.participantManager = participantManager;
        this.teamBuilder = teamBuilder;
    }

    /**
     * Template method - defines the overall portal flow
     * This method controls the structure but delegates specific behavior to subclasses

     */
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

    /**
     * Abstract methods - must be implemented by subclasses
     * Demonstrates: Abstraction (defines contract without implementation)
     */
    protected abstract void displayWelcomeMessage();
    protected abstract void displayMenu();
    protected abstract void handleMenuChoice(int choice);
    protected abstract int getExitOption();
    protected abstract void displayGoodbyeMessage();

    /**
     * Concrete helper methods - shared by all subclasses
     * Demonstrates: Code reuse through inheritance
     */

    /**
     * Gets menu choice from user with error handling
     */
    protected int getMenuChoice() {
        System.out.print("Enter choice: ");
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
            return -1;
        }
    }

    /**
     * Gets validated integer input within range
     */
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

    /**
     * Gets non-empty string input from user
     */
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

    /**
     * Displays a separator line
     */
    protected void displaySeparator() {
        System.out.println("=".repeat(50));
    }

    /**
     * Pauses execution until user presses Enter
     */
    protected void pressEnterToContinue() {
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }
}