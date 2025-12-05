package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.FileManager;
import teammate.util.ValidationUtil;
import teammate.util.SystemLogger;
import java.io.*;
import java.util.*;

// Manages participant data including loading, saving, and CSV processing
public class ParticipantManager {
    private List<Participant> participants;
    private OrganizerPortalService organizerPortal;

    public ParticipantManager() {
        this.participants = Collections.synchronizedList(new ArrayList<>());
        loadAllParticipants();
    }

    // Sets organizer portal reference for displaying participant details
    public void setOrganizerPortal(OrganizerPortalService portal) {
        this.organizerPortal = portal;
    }

    // Loads all participants from file on system startup
    public void loadAllParticipants() {
        try {
            this.participants = Collections.synchronizedList(
                    FileManager.loadAllParticipants());
            SystemLogger.success("Loaded " + participants.size() + " participants");
        } catch (TeamMateException.FileReadException e) {
            SystemLogger.error("Error loading participants: " + e.getMessage());
            System.err.println("Error loading participants: " + e.getMessage());
        }
    }

    // Saves all participants to file for persistence
    public void saveAllParticipants() {
        try {
            synchronized (participants) {
                FileManager.saveAllParticipants(new ArrayList<>(participants));
                SystemLogger.success("Saved " + participants.size() + " participants");
            }
        } catch (TeamMateException.FileWriteException e) {
            SystemLogger.error("Failed to save participants: " + e.getMessage());
            System.out.println("Warning: " + e.getMessage());
        }
    }

    // Validates participant credentials for uniqueness
    public String validateParticipantCredentials(String id, String email) {
        if (!ValidationUtil.isValidParticipantId(id)) { // sq no. 1.3.1 of submit survey use case
            return "INVALID_ID_FORMAT";
        }

        synchronized (participants) {
            for (Participant p : participants) {
                if (p.getId().equalsIgnoreCase(id)) {
                    return "ID_EXISTS";
                }
            }

            for (Participant p : participants) {
                if (p.getEmail().equalsIgnoreCase(email)) {
                    return "EMAIL_EXISTS";
                }
            }
        }

        return "VALID";
    }

    // Processes external CSV file with participant data for bulk upload
    public Map<String, Object> processExternalCSV(String filename, Scanner scanner)
            throws Exception {

        Map<String, Object> result = new HashMap<>();
        SystemLogger.info("Starting CSV upload: " + filename);

        List<String> lines = readAllLines(filename); // sq no. 1.2.2 of upload csv use case

        if (lines.isEmpty()) {
            throw new Exception("CSV file is empty");
        }

        lines.remove(0);

        // Categorize participants by their current status in system
        List<Participant> sessionParticipants = new ArrayList<>();
        List<Participant> trulyNewParticipants = new ArrayList<>();
        List<String> duplicateAvailable = new ArrayList<>();
        List<String> duplicateAssigned = new ArrayList<>();
        Map<String, String> assignedDetails = new HashMap<>();
        List<String> invalidRecords = new ArrayList<>();

        // Process each CSV line and categorize participants
        synchronized (participants) {
            for (String line : lines) {
                ParticipantRecord record = parseLine(line);

                if (record.isValid()) {
                    Participant existing = findByIdOrEmail(record.getParticipant().getId(), // sq no. 1.2.4 of upload csv use case
                            record.getParticipant().getEmail());

                    if (existing != null) {
                        if (existing.getStatus().equals("Available")) {
                            sessionParticipants.add(existing);
                            duplicateAvailable.add(record.getParticipant().getId() +
                                    " (" + record.getParticipant().getEmail() + ")");
                        } else if (existing.getStatus().equals("Assigned")) {
                            String assignmentInfo = FileManager.findLatestAssignment(
                                    record.getParticipant().getId());

                            duplicateAssigned.add(record.getParticipant().getId() +
                                    " - " + record.getParticipant().getName() +
                                    " (" + record.getParticipant().getEmail() + ")");
                            assignedDetails.put(record.getParticipant().getId(), assignmentInfo);
                        }
                    } else {
                        sessionParticipants.add(record.getParticipant());
                        trulyNewParticipants.add(record.getParticipant());
                    }
                } else {
                    invalidRecords.add(record.getErrorMessage());
                }
            }
        }

        int assignedIncludedCount = 0;
        boolean statusChangesOccurred = false;

        // Handle participants already assigned to previous tournaments
        if (!duplicateAssigned.isEmpty()) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("[!] " + duplicateAssigned.size() +
                    " participants already in previous tournaments");
            System.out.println("=".repeat(60));

            System.out.print("View their details? (Y/N): ");
            String viewDetails = scanner.nextLine().trim().toUpperCase();

            if (viewDetails.equals("Y") && organizerPortal != null) {
                organizerPortal.displayAssignedParticipantsDetails(duplicateAssigned, assignedDetails);
            }

            System.out.print("\nInclude them in this NEW tournament? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("Y")) {
                synchronized (participants) {
                    for (String dup : duplicateAssigned) {
                        String participantId = dup.split(" - ")[0];
                        Participant existing = findByIdOrEmail(participantId, null);

                        if (existing != null) {
                            sessionParticipants.add(existing);
                            assignedIncludedCount++;
                            SystemLogger.info("Reusing participant " + participantId);
                        }
                    }
                }

                duplicateAssigned.clear();

            } else if (!choice.equals("N")) {
                SystemLogger.info("CSV upload cancelled by user");
                result.put("cancelled", true);
                result.put("reason", "Invalid choice - upload cancelled");
                return result;
            }
        }

        // Add new participants to master list
        if (!trulyNewParticipants.isEmpty()) {
            synchronized (participants) {
                participants.addAll(trulyNewParticipants);
            }
            SystemLogger.info("Added " + trulyNewParticipants.size() + " new participants to system");
        }

        // Save changes to file if needed
        if (!trulyNewParticipants.isEmpty() || statusChangesOccurred) {
            saveAllParticipants();
        }

        // Display upload summary
        System.out.println("\n" + "=".repeat(60));
        System.out.println("UPLOAD COMPLETE");
        System.out.println("=".repeat(60));
        System.out.println("Ready for team formation: " + sessionParticipants.size() + " participants");

        if (trulyNewParticipants.size() > 0) {
            System.out.println("  - New participants: " + trulyNewParticipants.size());
        }
        if (duplicateAvailable.size() > 0) {
            System.out.println("  - From previous uploads: " + duplicateAvailable.size());
        }
        if (assignedIncludedCount > 0) {
            System.out.println("  - From past tournaments: " + assignedIncludedCount);
        }
        if (invalidRecords.size() > 0) {
            System.out.println("  - Skipped (invalid): " + invalidRecords.size());
        }
        System.out.println("=".repeat(60));

        // Prepare result map for caller
        result.put("cancelled", false);
        result.put("newlyAdded", sessionParticipants);
        result.put("duplicateAvailable", duplicateAvailable.size());
        result.put("duplicateAssigned", duplicateAssigned.size());
        result.put("invalidRecords", invalidRecords.size());
        result.put("statusChangesOccurred", statusChangesOccurred);

        return result;
    }

    // Reads all lines from CSV file
    private List<String> readAllLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    // Parses CSV line and validates participant data using concurrent processing
    private ParticipantRecord parseLine(String line) {
        try {
            String[] parts = line.split(",");
            if (parts.length < 7) {
                return ParticipantRecord.invalid("Insufficient columns in CSV");
            }

            String id = parts[0].trim();
            String name = parts[1].trim();
            String email = parts[2].trim();
            String game = parts[3].trim();
            int skill = Integer.parseInt(parts[4].trim());
            String role = parts[5].trim();
            int personalityScore = Integer.parseInt(parts[6].trim());

            // Validate all input fields
            if (!ValidationUtil.isValidParticipantId(id)) {
                return ParticipantRecord.invalid("Invalid ID format: " + id);
            }
            if (!ValidationUtil.isValidEmail(email)) {
                return ParticipantRecord.invalid("Invalid email: " + email);
            }
            if (!ValidationUtil.isValidSkillLevel(skill)) {
                return ParticipantRecord.invalid("Invalid skill level: " + skill);
            }
            if (!ValidationUtil.isValidRole(role)) {
                return ParticipantRecord.invalid("Invalid role: " + role);
            }

            // Process personality using concurrent processor
            SurveyDataProcessor.SurveyResult result = SurveyDataProcessor.processIndividualSurvey(
                    id, name, email, game, skill, role, personalityScore
            );

            if (result.isSuccess()) {
                return ParticipantRecord.valid(result.getParticipant());
            } else {
                return ParticipantRecord.invalid("Personality score too low: " + personalityScore);
            }

        } catch (NumberFormatException e) {
            return ParticipantRecord.invalid("Invalid number format in CSV");
        } catch (Exception e) {
            return ParticipantRecord.invalid("Processing error: " + e.getMessage());
        }
    }

    // Finds participant by ID or email
    private Participant findByIdOrEmail(String id, String email) {
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id)) {
                return p;
            }
            if (email != null && p.getEmail().equalsIgnoreCase(email)) {
                return p;
            }
        }
        return null;
    }

    // Adds a new participant to the system
    public void addParticipant(Participant participant) throws TeamMateException.DuplicateParticipantException {
        if (participant == null) {
            throw new IllegalArgumentException("Participant cannot be null");
        }

        synchronized (participants) {
            for (Participant p : participants) {
                if (p.getId().equalsIgnoreCase(participant.getId())) {
                    throw new TeamMateException.DuplicateParticipantException(
                            "Participant with ID " + participant.getId() + " already exists");
                }
            }

            participants.add(participant);
            SystemLogger.info("Added new participant: " + participant.getId());
        }
    }

    // Searches for participant by ID, email, or name
    public Participant findParticipant(String searchKey) {
        synchronized (participants) {
            for (Participant p : participants) {
                if (p.getId().equalsIgnoreCase(searchKey) ||
                        p.getEmail().equalsIgnoreCase(searchKey) ||
                        p.getName().equalsIgnoreCase(searchKey)) {
                    return p;
                }
            }
        }
        return null;
    }

    // Returns all participants with Available status
    public List<Participant> findAvailableParticipants() {
        List<Participant> available = new ArrayList<>();
        synchronized (participants) {
            for (Participant p : participants) {
                if (p.getStatus().equals("Available")) {
                    available.add(p);
                }
            }
        }
        return available;
    }

    // Returns all participants in the system
    public List<Participant> getAllParticipants() {
        synchronized (participants) {
            return new ArrayList<>(participants);
        }
    }

    // Removes specified participants from the system
    public void removeParticipants(List<Participant> toRemove) {
        synchronized (participants) {
            participants.removeAll(toRemove);
            SystemLogger.info("Removed " + toRemove.size() + " participants");
        }
    }

    // Helper class for CSV parsing results
    private static class ParticipantRecord {
        private Participant participant;
        private String errorMessage;
        private boolean valid;

        static ParticipantRecord valid(Participant p) {
            ParticipantRecord record = new ParticipantRecord();
            record.participant = p;
            record.valid = true;
            return record;
        }

        static ParticipantRecord invalid(String error) {
            ParticipantRecord record = new ParticipantRecord();
            record.errorMessage = error;
            record.valid = false;
            return record;
        }

        boolean isValid() { return valid; }
        Participant getParticipant() { return participant; }
        String getErrorMessage() { return errorMessage; }
    }
}