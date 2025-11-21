package teammate.service;

import teammate.entity.Participant;
import teammate.exception.TeamMateException;
import teammate.util.FileManager;
import java.io.*;
import java.util.*;

/**
 * Service class for managing participants
 */
public class ParticipantManager {
    private List<Participant> participants;

    public ParticipantManager() {
        this.participants = new ArrayList<>();
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

    public List<Participant> processExternalCSV(String filename) throws Exception {
        List<Participant> newlyAdded = new ArrayList<>();
        int addedCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line = br.readLine();

            if (line == null) throw new TeamMateException.FileReadException("External CSV file is empty.");

            String[] header = line.split(",");
            boolean hasIdColumn = header[0].trim().equalsIgnoreCase("ID");

            System.out.println("Processing external file: " + filename);

            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = line.split(",");
                    int startIndex = hasIdColumn ? 1 : 0;

                    if (parts.length < (hasIdColumn ? 9 : 8)) {
                        System.out.println("Warning: Skipping line " + lineNumber);
                        continue;
                    }

                    String email = parts[startIndex + 1].trim();

                    boolean duplicate = participants.stream()
                            .anyMatch(p -> p.getEmail().equalsIgnoreCase(email));

                    if (duplicate) {
                        System.out.println("Warning: Skipping duplicate email: " + email);
                        continue;
                    }

                    Participant newParticipant = new Participant(
                            parts[startIndex].trim(),
                            email,
                            parts[startIndex + 2].trim(),
                            Integer.parseInt(parts[startIndex + 3].trim()),
                            parts[startIndex + 4].trim(),
                            Integer.parseInt(parts[startIndex + 5].trim()),
                            parts[startIndex + 6].trim()
                    );

                    participants.add(newParticipant);
                    newlyAdded.add(newParticipant);
                    addedCount++;

                } catch (Exception e) {
                    System.out.println("Warning: Error on line " + lineNumber + " - " + e.getMessage());
                }
            }

            if (addedCount > 0) {
                System.out.println("Successfully processed " + addedCount + " new participants");
                saveAllParticipants();
            }

            return newlyAdded;

        } catch (FileNotFoundException e) {
            throw new TeamMateException.FileReadException("File not found: " + filename);
        }
    }

    public void addParticipant(Participant participant) throws TeamMateException.DuplicateParticipantException {
        for (Participant p : participants) {
            if (p.getEmail().equalsIgnoreCase(participant.getEmail())) {
                throw new TeamMateException.DuplicateParticipantException("Email already exists");
            }
        }
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
