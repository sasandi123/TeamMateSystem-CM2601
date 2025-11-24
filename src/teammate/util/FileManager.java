package teammate.util;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.service.ParticipantManager;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

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
            String line = br.readLine();

            if (line == null) {
                return participants;
            }

            while ((line = br.readLine()) != null) {
                try {
                    String[] parts = line.split(",");
                    if (parts.length < 9) continue;

                    Participant participant = new Participant(
                            parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(),
                            Integer.parseInt(parts[4].trim()), parts[5].trim(),
                            Integer.parseInt(parts[6].trim()), parts[7].trim(), parts[8].trim()
                    );

                    participants.add(participant);
                } catch (Exception e) {
                    // Skip invalid lines
                }
            }

        } catch (FileNotFoundException e) {
            // File doesn't exist yet
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

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Could not append to cumulative file: " + e.getMessage());
        }
    }

    /**
     * Search for a team and display FULL details including member information
     */
    public static void searchTeamById(String teamId, ParticipantManager participantManager) {
        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine(); // Skip header

            boolean found = false;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5 && parts[1].trim().equalsIgnoreCase(teamId)) {
                    // Found the team - display full details
                    String timestamp = parts[0];
                    String tId = parts[1];
                    String teamSize = parts[2];
                    String avgSkill = parts[3];
                    String memberIds = parts[4];

                    System.out.println("\n" + "=".repeat(60));
                    System.out.println("TEAM DETAILS");
                    System.out.println("=".repeat(60));
                    System.out.println("Team ID: " + tId);
                    System.out.println("Formation Date: " + timestamp);
                    System.out.println("Team Size: " + teamSize);
                    System.out.println("Average Skill: " + avgSkill);
                    System.out.println("=".repeat(60));

                    // Get member details
                    String[] ids = memberIds.split(";");
                    System.out.println("\nTeam Members:");

                    Map<String, Integer> roleCount = new HashMap<>();
                    Map<String, Integer> personalityCount = new HashMap<>();
                    Map<String, Integer> gameCount = new HashMap<>();
                    int leaderCount = 0;
                    int thinkerCount = 0;
                    int balancedCount = 0;

                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i].trim();
                        Participant p = participantManager.findParticipant(id);

                        if (p != null) {
                            System.out.printf("%d. %s - %s | Game: %s | Skill: %d | Role: %s | Type: %s\n",
                                    (i + 1), p.getId(), p.getName(), p.getPreferredGame(),
                                    p.getSkillLevel(), p.getPreferredRole(), p.getPersonalityType());

                            // Count distributions
                            roleCount.put(p.getPreferredRole(),
                                    roleCount.getOrDefault(p.getPreferredRole(), 0) + 1);
                            gameCount.put(p.getPreferredGame(),
                                    gameCount.getOrDefault(p.getPreferredGame(), 0) + 1);

                            String type = p.getPersonalityType();
                            personalityCount.put(type, personalityCount.getOrDefault(type, 0) + 1);

                            if (type.equals("Leader")) leaderCount++;
                            else if (type.equals("Thinker")) thinkerCount++;
                            else if (type.equals("Balanced")) balancedCount++;
                        } else {
                            System.out.printf("%d. %s (Details not available)\n", (i + 1), id);
                        }
                    }

                    // Display distributions
                    System.out.println("\nRole Distribution:");
                    roleCount.forEach((role, count) ->
                            System.out.println("  " + role + ": " + count));

                    System.out.println("\nPersonality Distribution:");
                    System.out.println("  Leaders: " + leaderCount);
                    System.out.println("  Thinkers: " + thinkerCount);
                    System.out.println("  Balanced: " + balancedCount);

                    System.out.println("\nGame Distribution:");
                    gameCount.forEach((game, count) ->
                            System.out.println("  " + game + ": " + count));

                    System.out.println("=".repeat(60));
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("\n✗ Team ID '" + teamId + "' not found in records.");
            }

        } catch (FileNotFoundException e) {
            System.out.println("\n✗ No team records found.");
        } catch (IOException e) {
            System.out.println("\n✗ Error reading team records: " + e.getMessage());
        }
    }

    /**
     * Find which team a participant belongs to
     */
    public static void findParticipantTeamInCumulative(String participantId, ParticipantManager participantManager) {
        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String memberIds = parts[4];
                    if (memberIds.contains(participantId)) {
                        // Use the same detailed display
                        searchTeamById(parts[1].trim(), participantManager);
                        return;
                    }
                }
            }

            System.out.println("\n✗ No team assignment found in historical records.");

        } catch (FileNotFoundException e) {
            System.out.println("\n✗ No team records found.");
        } catch (IOException e) {
            System.out.println("\n✗ Error reading team records: " + e.getMessage());
        }
    }
}
