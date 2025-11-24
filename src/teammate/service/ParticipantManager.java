package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.util.FileManager;
import teammate.util.ValidationUtil;
import java.io.*;
import java.util.*;

public class ParticipantManager {
    private List<Participant> participants;

    public ParticipantManager() {
        this.participants = new ArrayList<>();
        loadAllParticipants();
    }

    public void loadAllParticipants() {
        try {
            this.participants = FileManager.loadAllParticipants();
        } catch (TeamMateException.FileReadException e) {
            System.err.println("Error loading participants: " + e.getMessage());
        }
    }

    public void saveAllParticipants() {
        try {
            FileManager.saveAllParticipants(participants);
        } catch (TeamMateException.FileWriteException e) {
            System.out.println("Warning: " + e.getMessage());
        }
    }

    /**
     * Validates if ID and Email already exist
     */
    public String validateParticipantCredentials(String id, String email) {
        // Validate ID format
        if (!ValidationUtil.isValidParticipantId(id)) {
            return "INVALID_ID_FORMAT";
        }

        // Check for existing ID
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id)) {
                return "ID_EXISTS";
            }
        }

        // Check for existing email
        for (Participant p : participants) {
            if (p.getEmail().equalsIgnoreCase(email)) {
                return "EMAIL_EXISTS";
            }
        }

        return "VALID";
    }

    /**
     * Process external CSV with duplicate checking
     */
    public Map<String, Object> processExternalCSV(String filename, Scanner scanner) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Participant> newlyAdded = new ArrayList<>();
        List<String> duplicateAvailable = new ArrayList<>();
        List<String> duplicateAssigned = new ArrayList<>();
        List<String> invalidRecords = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();

            if (line == null) {
                throw new TeamMateException.FileReadException("External CSV file is empty.");
            }

            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = line.split(",");

                    if (parts.length < 8) {
                        invalidRecords.add("Line " + lineNumber + ": Missing fields");
                        continue;
                    }

                    String id = parts[0].trim();
                    String name = parts[1].trim();
                    String email = parts[2].trim();
                    String preferredGame = parts[3].trim();
                    int skillLevel = Integer.parseInt(parts[4].trim());
                    String preferredRole = parts[5].trim();
                    int personalityScore = Integer.parseInt(parts[6].trim());
                    String personalityType = parts[7].trim();

                    // Validate ID format
                    if (!ValidationUtil.isValidParticipantId(id)) {
                        invalidRecords.add("Line " + lineNumber + ": Invalid ID format (must start with 'P')");
                        continue;
                    }

                    // Check for duplicates
                    Participant existing = findByIdOrEmail(id, email);

                    if (existing != null) {
                        if (existing.getStatus().equals("Available")) {
                            duplicateAvailable.add(id + " (" + email + ")");
                            continue;
                        } else if (existing.getStatus().equals("Assigned")) {
                            duplicateAssigned.add(id + " - " + name + " (" + email + ")");
                            continue;
                        }
                    }

                    // Valid new participant
                    Participant newParticipant = new Participant(id, name, email, preferredGame,
                            skillLevel, preferredRole, personalityScore, personalityType);

                    participants.add(newParticipant);
                    newlyAdded.add(newParticipant);

                } catch (NumberFormatException e) {
                    invalidRecords.add("Line " + lineNumber + ": Invalid number format");
                } catch (Exception e) {
                    invalidRecords.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }

            // Handle assigned duplicates
            if (!duplicateAssigned.isEmpty()) {
                System.out.println("\n⚠ WARNING: Found " + duplicateAssigned.size() +
                        " participants already assigned to teams:");
                for (String dup : duplicateAssigned) {
                    System.out.println("  - " + dup);
                }

                System.out.print("\nThese participants are already in teams. Continue with remaining participants? (Y/N): ");
                String choice = scanner.nextLine().trim().toUpperCase();

                if (!choice.equals("Y")) {
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
                System.out.println("\n⚠ Skipped " + invalidRecords.size() + " invalid records:");
                for (String inv : invalidRecords) {
                    System.out.println("  - " + inv);
                }
            }

            if (!newlyAdded.isEmpty()) {
                saveAllParticipants();
                System.out.println("\n✓ Successfully added " + newlyAdded.size() + " new participants");
            }

            result.put("cancelled", false);
            result.put("newlyAdded", newlyAdded);
            result.put("duplicateAvailable", duplicateAvailable.size());
            result.put("duplicateAssigned", duplicateAssigned.size());
            result.put("invalidRecords", invalidRecords.size());

            return result;

        } catch (FileNotFoundException e) {
            throw new TeamMateException.FileReadException("File not found: " + filename);
        }
    }

    private Participant findByIdOrEmail(String id, String email) {
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(id) || p.getEmail().equalsIgnoreCase(email)) {
                return p;
            }
        }
        return null;
    }

    public void addParticipant(Participant participant) throws TeamMateException.DuplicateParticipantException {
        participants.add(participant);
        saveAllParticipants();
    }

    public Participant findParticipant(String searchKey) {
        for (Participant p : participants) {
            if (p.getId().equalsIgnoreCase(searchKey) ||
                    p.getName().equalsIgnoreCase(searchKey) ||
                    p.getEmail().equalsIgnoreCase(searchKey)) {
                return p;
            }
        }
        return null;
    }

    public List<Participant> getAvailableParticipants() {
        List<Participant> available = new ArrayList<>();
        for (Participant p : participants) {
            if (p.getStatus().equals("Available")) {
                available.add(p);
            }
        }
        return available;
    }

    public List<Participant> getAllParticipants() {
        return new ArrayList<>(participants);
    }

    public void removeParticipants(List<Participant> toRemove) {
        participants.removeAll(toRemove);
        saveAllParticipants();
    }
}
