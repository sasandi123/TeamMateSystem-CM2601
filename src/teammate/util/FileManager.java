package teammate.util;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.service.ParticipantManager;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

// Handles all file operations for participants and teams
public class FileManager {
    private static final String ALL_REGISTERED_PARTICIPANTS = "all_registered_participants.csv";
    private static final String FORMED_TEAMS_CUMULATIVE = "formed_teams_cumulative.csv";

    // Saves all participants to master CSV file
    public static void saveAllParticipants(List<Participant> participants) throws TeamMateException.FileWriteException {
        try (FileWriter fw = new FileWriter(ALL_REGISTERED_PARTICIPANTS, false);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType,Status");

            for (Participant p : participants) {
                out.println(p.toCSVString());
            }

            SystemLogger.logFileOperation("WRITE", ALL_REGISTERED_PARTICIPANTS, true);

        } catch (IOException e) {
            SystemLogger.logFileOperation("WRITE", ALL_REGISTERED_PARTICIPANTS, false);
            throw new TeamMateException.FileWriteException("Could not save participants: " + e.getMessage());
        }
    }

    // Loads all participants from master CSV file
    public static List<Participant> loadAllParticipants() throws TeamMateException.FileReadException {
        List<Participant> participants = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(ALL_REGISTERED_PARTICIPANTS))) {
            String line = br.readLine();

            if (line == null) {
                SystemLogger.info("Participant file is empty");
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
                    SystemLogger.warning("Skipped invalid participant line: " + e.getMessage());
                }
            }

            SystemLogger.logFileOperation("READ", ALL_REGISTERED_PARTICIPANTS, true);

        } catch (FileNotFoundException e) {
            SystemLogger.info("Participant file not found - starting fresh");
        } catch (IOException e) {
            SystemLogger.logFileOperation("READ", ALL_REGISTERED_PARTICIPANTS, false);
            throw new TeamMateException.FileReadException("Error reading participant file: " + e.getMessage());
        }

        return participants;
    }

    // Retrieves latest team assignment information for a participant
    public static String getLatestAssignment(String participantId) {
        String latestTeam = null;
        String latestTimestamp = null;

        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String timestamp = parts[0];
                    String teamId = parts[1];
                    String memberIds = parts[4];

                    String[] ids = memberIds.split(";");
                    for (String id : ids) {
                        if (id.trim().equalsIgnoreCase(participantId)) {
                            latestTeam = teamId;
                            latestTimestamp = timestamp;
                            break;
                        }
                    }
                }
            }

            if (latestTeam != null) {
                return "Last assigned to: " + latestTeam + " on " + latestTimestamp;
            }

        } catch (FileNotFoundException e) {
            return "No previous assignment found";
        } catch (IOException e) {
            SystemLogger.logException("Error checking assignment history", e);
            return "Could not verify assignment history";
        }

        return "No previous assignment found";
    }

    // Exports current teams to a snapshot file for this tournament
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

            SystemLogger.logFileOperation("EXPORT_SNAPSHOT", filename, true);
            SystemLogger.success("Exported " + teams.size() + " teams to snapshot: " + filename);

        } catch (IOException e) {
            SystemLogger.logFileOperation("EXPORT_SNAPSHOT", filename, false);
            throw new TeamMateException.FileWriteException("Could not save snapshot: " + e.getMessage());
        }
    }

    // Appends teams to cumulative records file with timestamp
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

            SystemLogger.logFileOperation("APPEND_CUMULATIVE", FORMED_TEAMS_CUMULATIVE, true);
            SystemLogger.success("Appended " + teams.size() + " teams to cumulative records");

        } catch (IOException e) {
            SystemLogger.logFileOperation("APPEND_CUMULATIVE", FORMED_TEAMS_CUMULATIVE, false);
            throw new TeamMateException.FileWriteException("Could not append to cumulative file: " + e.getMessage());
        }
    }

    // Searches for and displays full team details for organizers
    public static void searchTeamById(String teamId, ParticipantManager participantManager) {
        SystemLogger.info("Team search requested: " + teamId);

        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine();

            boolean found = false;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5 && parts[1].trim().equalsIgnoreCase(teamId)) {
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

                    String[] ids = memberIds.split(";");
                    System.out.println("\nTeam Members:");

                    Map<String, Integer> roleCount = new HashMap<>();
                    Map<String, Integer> gameCount = new HashMap<>();
                    int leaderCount = 0;
                    int thinkerCount = 0;
                    int balancedCount = 0;

                    // Display each member and calculate distributions
                    for (int i = 0; i < ids.length; i++) {
                        String id = ids[i].trim();
                        Participant p = participantManager.findParticipant(id);

                        if (p != null) {
                            System.out.printf("%d. %s - %s | Game: %s | Skill: %d | Role: %s | Type: %s\n",
                                    (i + 1), p.getId(), p.getName(), p.getPreferredGame(),
                                    p.getSkillLevel(), p.getPreferredRole(), p.getPersonalityType());

                            roleCount.put(p.getPreferredRole(),
                                    roleCount.getOrDefault(p.getPreferredRole(), 0) + 1);
                            gameCount.put(p.getPreferredGame(),
                                    gameCount.getOrDefault(p.getPreferredGame(), 0) + 1);

                            String type = p.getPersonalityType();
                            if (type.equals("Leader")) leaderCount++;
                            else if (type.equals("Thinker")) thinkerCount++;
                            else if (type.equals("Balanced")) balancedCount++;
                        } else {
                            System.out.printf("%d. %s (Details not available)\n", (i + 1), id);
                        }
                    }

                    // Display all distributions
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

                    SystemLogger.success("Team found and displayed: " + teamId);
                    found = true;
                    break;
                }
            }

            if (!found) {
                System.out.println("\n[X] Team ID '" + teamId + "' not found in records.");
                SystemLogger.warning("Team not found: " + teamId);
            }

        } catch (FileNotFoundException e) {
            System.out.println("\n[X] No team records found.");
            SystemLogger.error("Team records file not found");
        } catch (IOException e) {
            System.out.println("\n[X] Error reading team records: " + e.getMessage());
            SystemLogger.logException("Error searching team", e);
        }
    }

    // Finds and displays most recent team assignment for participants
    public static void findMostRecentParticipantTeam(String participantId, ParticipantManager participantManager) {
        SystemLogger.info("Searching most recent team for participant: " + participantId);

        String mostRecentTeamId = null;
        String mostRecentLine = null;

        try (BufferedReader br = new BufferedReader(new FileReader(FORMED_TEAMS_CUMULATIVE))) {
            String line = br.readLine();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5) {
                    String memberIds = parts[4];

                    String[] ids = memberIds.split(";");
                    for (String id : ids) {
                        if (id.trim().equalsIgnoreCase(participantId)) {
                            mostRecentTeamId = parts[1];
                            mostRecentLine = line;
                            break;
                        }
                    }
                }
            }

            if (mostRecentTeamId != null) {
                String[] parts = mostRecentLine.split(",");
                String timestamp = parts[0];
                int teamSize = Integer.parseInt(parts[2]);
                String memberIds = parts[4];

                // Create team object from file data
                Team team = new Team(teamSize);
                team.setTeamId(mostRecentTeamId);

                // Add members to team
                String[] ids = memberIds.split(";");
                for (String id : ids) {
                    Participant p = participantManager.findParticipant(id.trim());
                    if (p != null) {
                        team.addMember(p);
                    }
                }

                System.out.println("\nFormation Date: " + timestamp);

                // Display simplified team info for participants
                team.displayTeamInfoForParticipant();

                SystemLogger.success("Most recent team found: " + mostRecentTeamId);
            } else {
                System.out.println("\n[X] No team assignment found in records.");
                SystemLogger.warning("No team found for participant: " + participantId);
            }

        } catch (FileNotFoundException e) {
            System.out.println("\n[X] No team records found.");
            SystemLogger.error("Team records file not found");
        } catch (IOException e) {
            System.out.println("\n[X] Error reading team records: " + e.getMessage());
            SystemLogger.logException("Error finding participant team", e);
        }
    }
}