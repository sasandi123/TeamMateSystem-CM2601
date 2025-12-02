package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.FileManager;
import teammate.util.ValidationUtil;
import teammate.util.SystemLogger;
import java.io.*;
import java.util.*;

/**
 * Manages participant data with thread-safe operations
 */
public class ParticipantManager {
    private List<Participant> participants;

    public ParticipantManager() {
        this.participants = Collections.synchronizedList(new ArrayList<>());
        loadAllParticipants();
    }

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

    /**
     * Validates if ID and Email already exist
     */
    public String validateParticipantCredentials(String id, String email) {
        if (!ValidationUtil.isValidParticipantId(id)) {
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

    /**
     * Process external CSV file
     * Simple sequential processing with validation
     */
    public Map<String, Object> processExternalCSV(String filename, Scanner scanner)
            throws Exception {

        Map<String, Object> result = new HashMap<>();

        SystemLogger.info("Starting CSV upload: " + filename);

        // Read all lines from CSV
        List<String> lines = readAllLines(filename);

        if (lines.isEmpty()) {
            throw new Exception("CSV file is empty");
        }

        // Remove header
        lines.remove(0);

        System.out.println("\n[System] Processing " + lines.size() + " records...");

        // Categorize results
        List<Participant> newParticipants = new ArrayList<>();
        List<String> duplicateAvailable = new ArrayList<>();
        List<String> duplicateAssigned = new ArrayList<>();
        List<String> invalidRecords = new ArrayList<>();

        // Process each line sequentially
        synchronized (participants) {
            for (String line : lines) {
                ParticipantRecord record = parseLine(line);

                if (record.isValid()) {
                    // Check for duplicates
                    Participant existing = findByIdOrEmail(record.getParticipant().getId(),
                            record.getParticipant().getEmail());

                    if (existing != null) {
                        if (existing.getStatus().equals("Available")) {
                            duplicateAvailable.add(record.getParticipant().getId() +
                                    " (" + record.getParticipant().getEmail() + ")");
                        } else if (existing.getStatus().equals("Assigned")) {
                            duplicateAssigned.add(record.getParticipant().getId() +
                                    " - " + record.getParticipant().getName() +
                                    " (" + record.getParticipant().getEmail() + ")");
                        }
                    } else {
                        newParticipants.add(record.getParticipant());
                    }
                } else {
                    invalidRecords.add(record.getErrorMessage());
                }
            }
        }

        // Handle assigned duplicates
        if (!duplicateAssigned.isEmpty()) {
            System.out.println("\n⚠ WARNING: Found " + duplicateAssigned.size() +
                    " participants already assigned to teams:");
            for (String dup : duplicateAssigned) {
                System.out.println("  - " + dup);
            }

            System.out.print("\nThese participants are already in teams. " +
                    "Continue with remaining participants? (Y/N): ");
            String choice = scanner.nextLine().trim().toUpperCase();

            if (!choice.equals("Y")) {
                SystemLogger.info("CSV upload cancelled by user");
                result.put("cancelled", true);
                result.put("reason", "Upload cancelled due to assigned participants");
                return result;
            }
        }

        // Report results
        if (!duplicateAvailable.isEmpty()) {
            System.out.println("\n⚠ Skipped " + duplicateAvailable.size() +
                    " duplicate participants (already available):");
            for (String dup : duplicateAvailable) {
                System.out.println("  - " + dup);
            }
        }

        if (!invalidRecords.isEmpty()) {
            System.out.println("\n⚠ Skipped " + invalidRecords.size() +
                    " invalid records:");
            for (String inv : invalidRecords) {
                System.out.println("  - " + inv);
            }
        }

        // Add new participants to main list
        if (!newParticipants.isEmpty()) {
            synchronized (participants) {
                participants.addAll(newParticipants);
            }
            saveAllParticipants();
            System.out.println("\n✓ Successfully added " + newParticipants.size() +
                    " new participants");
            SystemLogger.success("CSV upload complete: " + newParticipants.size() + " added");
        }

        result.put("cancelled", false);
        result.put("newlyAdded", newParticipants);
        result.put("duplicateAvailable", duplicateAvailable.size());
        result.put("duplicateAssigned", duplicateAssigned.size());
        result.put("invalidRecords", invalidRecords.size());

        return result;
    }

    /**
     * Read all lines from CSV file
     */
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

    /**
     * Parse a single CSV line to Participant
     */
    private ParticipantRecord parseLine(String line) {
        try {
            String[] parts = line.split(",");

            if (parts.length < 8) {
                return ParticipantRecord.invalid("Missing fields");
            }

            String id = parts[0].trim();
            String name = parts[1].trim();
            String email = parts[2].trim();
            String preferredGame = parts[3].trim();
            int skillLevel = Integer.parseInt(parts[4].trim());
            String preferredRole = parts[5].trim();
            int personalityScore = Integer.parseInt(parts[6].trim());
            String personalityType = parts[7].trim();

            if (!ValidationUtil.isValidParticipantId(id)) {
                return ParticipantRecord.invalid("Invalid ID format: " + id);
            }

            Participant participant = new Participant(
                    id, name, email, preferredGame, skillLevel,
                    preferredRole, personalityScore, personalityType);

            return ParticipantRecord.valid(participant);

        } catch (NumberFormatException e) {
            return ParticipantRecord.invalid("Invalid number format");
        } catch (Exception e) {
            return ParticipantRecord.invalid(e.getMessage());
        }
    }

    /**
     * Find participant by ID or email
     */
    private Participant findByIdOrEmail(String id, String email) {
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id) || p.getEmail().equalsIgnoreCase(email)) {
                return p;
            }
        }
        return null;
    }

    public void addParticipant(Participant participant)
            throws TeamMateException.DuplicateParticipantException {
        synchronized (participants) {
            participants.add(participant);
        }
        saveAllParticipants();
        SystemLogger.info("Participant added: " + participant.getId());
    }

    public Participant findParticipant(String searchKey) {
        synchronized (participants) {
            for (Participant p : participants) {
                if (p.getId().equalsIgnoreCase(searchKey) ||
                        p.getName().equalsIgnoreCase(searchKey) ||
                        p.getEmail().equalsIgnoreCase(searchKey)) {
                    return p;
                }
            }
        }
        return null;
    }

    public List<Participant> getAvailableParticipants() {
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

    public List<Participant> getAllParticipants() {
        synchronized (participants) {
            return new ArrayList<>(participants);
        }
    }

    public void removeParticipants(List<Participant> toRemove) {
        synchronized (participants) {
            participants.removeAll(toRemove);
        }
        saveAllParticipants();
        SystemLogger.info("Removed " + toRemove.size() + " participants");
    }

    /**
     * Internal class to hold parsing results
     */
    private static class ParticipantRecord {
        private Participant participant;
        private String errorMessage;
        private boolean valid;

        private ParticipantRecord(Participant participant, String errorMessage, boolean valid) {
            this.participant = participant;
            this.errorMessage = errorMessage;
            this.valid = valid;
        }

        static ParticipantRecord valid(Participant p) {
            return new ParticipantRecord(p, null, true);
        }

        static ParticipantRecord invalid(String msg) {
            return new ParticipantRecord(null, msg, false);
        }

        boolean isValid() { return valid; }
        Participant getParticipant() { return participant; }
        String getErrorMessage() { return errorMessage; }
    }
}
