package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.concurrent.SurveyDataProcessor;
import teammate.util.FileManager;
import teammate.util.ValidationUtil;
import teammate.util.SystemLogger;
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
     * Process external CSV with CONCURRENT processing using threads
     * Uses SurveyDataProcessor for separation of concerns
     */
    public Map<String, Object> processExternalCSV(String filename, Scanner scanner)
            throws Exception {

        Map<String, Object> result = new HashMap<>();

        SystemLogger.info("Starting CSV upload: " + filename);

        // Use SurveyDataProcessor for CSV processing
        SurveyDataProcessor processor = new SurveyDataProcessor();
        SurveyDataProcessor.ProcessingResult processingResult =
                processor.processCSVConcurrently(filename, participants);

        List<Participant> newlyAdded = processingResult.getNewParticipants();
        List<String> duplicateAssigned = processingResult.getDuplicateAssigned();
        List<String> duplicateAvailable = processingResult.getDuplicateAvailable();
        List<String> invalidRecords = processingResult.getInvalidRecords();

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
        if (!newlyAdded.isEmpty()) {
            synchronized (participants) {
                participants.addAll(newlyAdded);
            }
            saveAllParticipants();
            System.out.println("\n✓ Successfully added " + newlyAdded.size() +
                    " new participants");
            SystemLogger.success("CSV upload complete: " + newlyAdded.size() + " added");
        }

        result.put("cancelled", false);
        result.put("newlyAdded", newlyAdded);
        result.put("duplicateAvailable", duplicateAvailable.size());
        result.put("duplicateAssigned", duplicateAssigned.size());
        result.put("invalidRecords", invalidRecords.size());

        return result;
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
}