// ParticipantManager.java
package teammate;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ParticipantManager {
    private List<Participant> participants;
    private static final String UNASSIGNED_CSV_FILENAME = "unassigned_participants.csv";

    public ParticipantManager() {
        this.participants = new ArrayList<>();
    }

    public void addParticipant(Participant participant) throws Exception {
        // Check for duplicate email across the current in-memory list
        for (Participant p : participants) {
            if (p.getEmail().equalsIgnoreCase(participant.getEmail())) {
                throw new TeamMateException.DuplicateParticipantException("A participant with this email already exists");
            }
        }
        participants.add(participant);
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

    /**
     * Returns ONLY those participants whose status is "Available" for team formation.
     */
    public List<Participant> getAvailableParticipants() {
        return this.participants.stream()
                .filter(p -> p.getStatus().equalsIgnoreCase("Available"))
                .collect(Collectors.toList());
    }

    public List<Participant> getParticipants() {
        return this.participants;
    }

    /**
     * Loads the persistent pool of unassigned participants at system startup.
     */
    public void loadInitialUnassignedPool() {
        try {
            loadParticipantsFromCSV(UNASSIGNED_CSV_FILENAME);
            System.out.println("Loaded " + participants.size() + " existing participants from unassigned pool.");
        } catch (TeamMateException.FileReadException e) {
            System.out.println("No existing unassigned pool file found. Starting fresh.");
        }
    }

    /**
     * Saves the remaining 'Available' participants back to the dedicated file.
     */
    public void saveUnassignedParticipantsPool() throws TeamMateException.FileWriteException {
        // Filter the entire list for only 'Available' ones
        List<Participant> unassigned = getAvailableParticipants();

        if (unassigned.isEmpty()) {
            // Delete the file if empty to signal a clean start next time
            File file = new File(UNASSIGNED_CSV_FILENAME);
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        try (FileWriter fw = new FileWriter(UNASSIGNED_CSV_FILENAME, false); // Overwrite (false)
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // Write header
            out.println("ID,Name,Email,Game,SkillLevel,Role,PersonalityScore,PersonalityType,Status");

            for (Participant participant : unassigned) {
                out.println(participant.toCSVString());
            }
            System.out.println("Saved " + unassigned.size() + " unassigned participants to " + UNASSIGNED_CSV_FILENAME);

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Error writing unassigned pool to file: " + e.getMessage());
        }
    }

    /**
     * Centralized CSV loading method that handles file reading and parsing.
     */
    public void loadParticipantsFromCSV(String filename) throws TeamMateException.FileReadException {

        int participantsAdded = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;
            br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                lineNumber++;
                String[] data = line.split(",");

                if (data.length < 9) {
                    continue;
                }

                try {
                    String id = data[0];
                    String name = data[1];
                    String email = data[2];
                    String preferredGame = data[3];
                    int skillLevel = Integer.parseInt(data[4].trim());
                    String preferredRole = data[5];
                    int personalityScore = Integer.parseInt(data[6].trim());
                    String personalityType = data[7];
                    String status = data[8];

                    Participant participant = new Participant(id, name, email, preferredGame, skillLevel, preferredRole,
                            personalityScore, personalityType, status);

                    // Only add if not already in the main participant list (avoids duplicates)
                    if (findParticipant(email) == null) {
                        this.participants.add(participant);
                        participantsAdded++;
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Warning: Invalid number format on line " + lineNumber);
                } catch (Exception e) {
                    System.out.println("Warning: Error on line " + lineNumber + " - " + e.getMessage());
                }
            }

        } catch (FileNotFoundException e) {
            // Re-throw custom exception for clean error handling in TeamMateSystem
            throw new TeamMateException.FileReadException("File not found: " + filename);
        } catch (IOException e) {
            throw new TeamMateException.FileReadException("Error reading file: " + e.getMessage());
        }
    }
}