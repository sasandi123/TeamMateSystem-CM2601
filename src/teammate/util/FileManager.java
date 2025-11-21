package teammate.util;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for file operations
 */
public class FileManager {
    private static final String ALL_REGISTERED_PARTICIPANTS = "all_registered_participants.csv";
    private static final String FORMED_TEAMS_CUMULATIVE = "formed_teams_cumulative.csv";

    public static void saveAllParticipants(List<Participant> participants) throws TeamMateException.FileWriteException {
        try (FileWriter fw = new FileWriter(ALL_REGISTERED_PARTICIPANTS, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType,Status");

            for (Participant p : participants) {
                out.println(p.toCSVString());
            }

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Could not save participants: " + e.getMessage());
        }
    }

    public static List<Participant> loadAllParticipants() throws TeamMateException.FileReadException {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ALL_REGISTERED_PARTICIPANTS))) {
            String line = br.readLine(); // Skip header

            if (line == null) {
                System.out.println("System Initialization: Participant file is empty. Starting fresh.");
                return participants;
            }

            int count = 0;
            int lineNumber = 1;
            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    String[] parts = line.split(",");

                    if (parts.length < 9) {
                        System.out.println("Warning: Skipping invalid line " + lineNumber);
                        continue;
                    }

                    Participant participant = new Participant(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(),
                            Integer.parseInt(parts[4].trim()), parts[5].trim(),
                            Integer.parseInt(parts[6].trim()), parts[7].trim(), parts[8].trim()
                    );

                    participants.add(participant);
                    count++;

                } catch (Exception e) {
                    System.out.println("Warning: Error on line " + lineNumber + " - " + e.getMessage());
                }
            }

            if (count > 0) {
                System.out.println("Loaded " + count + " participants from " + ALL_REGISTERED_PARTICIPANTS);
            }

        } catch (FileNotFoundException e) {
            System.out.println("System Initialization: " + ALL_REGISTERED_PARTICIPANTS + " not found. Creating new file.");
        } catch (IOException e) {
            throw new TeamMateException.FileReadException("Error reading participant file: " + e.getMessage());
        }

        return participants;
    }

    public static void exportTeamsSnapshot(List<Team> teams, String filename) throws TeamMateException.FileWriteException {
        try (FileWriter fw = new FileWriter(filename, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            out.println("# Team Formation Snapshot - " + sdf.format(new Date()));
            out.println("TeamID,TeamSize,AvgSkill,MemberIDs");

            for (Team team : teams) {
                out.println(team.toCSVString());
            }

            System.out.println("✓ Snapshot saved to: " + filename);

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Could not save snapshot: " + e.getMessage());
        }
    }

    public static void appendTeamsToCumulative(List<Team> teams) throws TeamMateException.FileWriteException {
        boolean fileExists = new File(FORMED_TEAMS_CUMULATIVE).exists();

        try (FileWriter fw = new FileWriter(FORMED_TEAMS_CUMULATIVE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            if (!fileExists) {
                out.println("Timestamp,TeamID,TeamSize,AvgSkill,MemberIDs");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());

            for (Team team : teams) {
                out.println(timestamp + "," + team.toCSVString());
            }

            System.out.println("✓ Teams appended to: " + FORMED_TEAMS_CUMULATIVE);

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Could not append to cumulative file: " + e.getMessage());
        }
    }

    public static Team findParticipantTeamInCumulative(String participantId) {
        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine(); // Skip header

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String memberIds = parts[4];
                    if (memberIds.contains(participantId)) {
                        // Return basic team info
                        System.out.println("Found in historical records:");
                        System.out.println("  Team ID: " + parts[1]);
                        System.out.println("  Team Size: " + parts[2]);
                        System.out.println("  Average Skill: " + parts[3]);
                        System.out.println("  Formation Date: " + parts[0]);
                        System.out.println("  Team Members: " + memberIds.replace(";", ", "));
                        return null; // We don't reconstruct full team object
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("No historical team records found.");
        } catch (IOException e) {
            System.out.println("Error reading team records: " + e.getMessage());
        }

        return null;
    }
}