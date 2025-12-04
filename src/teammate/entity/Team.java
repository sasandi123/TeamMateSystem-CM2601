package teammate.entity;

import teammate.util.SystemLogger;
import java.io.*;
import java.util.*;

// Represents a team with unique sequential ID and members
public class Team {
    private static final String COUNTER_FILE = "team_counter.dat";
    private static int teamCounter = -1;
    private static int previousValue = -1;
    private static boolean counterModified = false;

    private String teamId;
    private List<Participant> members;
    private int maxSize;
    private double averageSkillLevel;
    private Map<String, Integer> roleDistribution;
    private Map<String, Integer> personalityDistribution;
    private Map<String, Integer> gameDistribution;
    private double targetSkillLevel;

    public Team(int maxSize) {
        this.members = new ArrayList<>();
        this.maxSize = maxSize;
        this.roleDistribution = new HashMap<>();
        this.personalityDistribution = new HashMap<>();
        this.gameDistribution = new HashMap<>();
        this.teamId = "TEMP_ID";
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }

    // Adds a participant to the team if space is available
    public boolean addMember(Participant participant) {
        if (participant != null && members.size() < maxSize) {
            members.add(participant);
            updateStatistics();
            return true;
        }
        return false;
    }

    public void setTargetSkillLevel(double targetSkillLevel) {
        this.targetSkillLevel = targetSkillLevel;
    }

    public double getTargetSkillLevel() {
        return targetSkillLevel;
    }

    // Checks if a participant is a member of this team
    public boolean hasMember(Participant participant) {
        for (Participant member : members) {
            if (member.getId().equals(participant.getId())) {
                return true;
            }
        }
        return false;
    }

    // Assigns unique sequential team ID for permanent storage
    public synchronized void finalizeTeamId() {
        if (teamCounter == -1) {
            loadTeamCounter();
        }

        if (previousValue == -1) {
            previousValue = teamCounter;
        }

        teamCounter++;
        counterModified = true;
        teamId = "TEAM" + String.format("%04d", teamCounter);
    }

    // Resets counter to previous value if team generation is cancelled
    public static synchronized void resetTeamCounter() {
        teamCounter = previousValue;
        counterModified = false;
    }

    // Loads the team counter from file to maintain sequential IDs
    private static synchronized void loadTeamCounter() {
        File file = new File(COUNTER_FILE);

        if (!file.exists()) {
            teamCounter = 0;
            SystemLogger.info("Starting fresh - First team will be TEAM0001");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(COUNTER_FILE))) {
            String line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                teamCounter = Integer.parseInt(line.trim());
                SystemLogger.info("Loaded team counter - Next team: TEAM" +
                        String.format("%04d", teamCounter + 1));
            } else {
                teamCounter = 0;
            }
        } catch (Exception e) {
            SystemLogger.warning("Could not load counter, starting from TEAM0001");
            teamCounter = 0;
        }
    }

    // Saves the current team counter to file for persistence
    public static synchronized void saveTeamCounterToFile() {
        if (!counterModified) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(COUNTER_FILE))) {
            writer.write(String.valueOf(teamCounter));
            SystemLogger.success("Team counter saved: " + teamCounter);
        } catch (IOException e) {
            SystemLogger.error("Failed to save team counter: " + e.getMessage());
        }
    }

    // Calculates team statistics including average skill and distributions
    private void updateStatistics() {
        if (members.isEmpty()) return;

        // Calculate average skill
        int totalSkill = 0;
        for (Participant p : members) {
            totalSkill += p.getSkillLevel();
        }
        averageSkillLevel = (double) totalSkill / members.size();

        // Update distributions
        roleDistribution.clear();
        personalityDistribution.clear();
        gameDistribution.clear();

        for (Participant p : members) {
            roleDistribution.merge(p.getPreferredRole(), 1, Integer::sum);
            personalityDistribution.merge(p.getPersonalityType(), 1, Integer::sum);
            gameDistribution.merge(p.getPreferredGame(), 1, Integer::sum);
        }
    }

    public double getAverageSkill() {
        return averageSkillLevel;
    }

    public double getAverageSkillLevel() {
        return averageSkillLevel;
    }

    public Map<String, Integer> getRoleDistribution() {
        return new HashMap<>(roleDistribution);
    }

    public Map<String, Integer> getPersonalityDistribution() {
        return new HashMap<>(personalityDistribution);
    }

    public Map<String, Integer> getGameDistribution() {
        return new HashMap<>(gameDistribution);
    }

    // Displays comprehensive team information for organizers
    public void displayTeamInfo() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Team ID: " + teamId);
        System.out.println("Team Size: " + members.size() + "/" + maxSize);
        System.out.println("Average Skill Level: " + String.format("%.2f", averageSkillLevel));
        System.out.println("=".repeat(50));

        System.out.println("\nMembers:");
        for (int i = 0; i < members.size(); i++) {
            Participant p = members.get(i);
            System.out.println((i + 1) + ". " + p.getId() + " - " + p.getName() +
                    " (" + p.getEmail() + ") | Game: " + p.getPreferredGame() +
                    " | Skill: " + p.getSkillLevel() + " | Role: " + p.getPreferredRole() +
                    " | Type: " + p.getPersonalityType());
        }

        System.out.println("\nRole Distribution:");
        roleDistribution.forEach((role, count) ->
                System.out.println("  " + role + ": " + count));

        System.out.println("\nPersonality Distribution:");
        personalityDistribution.forEach((type, count) ->
                System.out.println("  " + type + ": " + count));

        System.out.println("\nGame Distribution:");
        gameDistribution.forEach((game, count) ->
                System.out.println("  " + game + ": " + count));
    }

    // Displays simplified team information for participants
    public void displayTeamInfoForParticipant() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Team ID: " + teamId);
        System.out.println("Team Size: " + members.size() + "/" + maxSize);
        System.out.println("Average Skill Level: " + String.format("%.2f", averageSkillLevel));
        System.out.println("=".repeat(50));

        System.out.println("\nYour Teammates:");
        for (int i = 0; i < members.size(); i++) {
            Participant p = members.get(i);
            System.out.println((i + 1) + ". " + p.getName() +
                    " | Game: " + p.getPreferredGame() +
                    " | Skill: " + p.getSkillLevel() +
                    " | Role: " + p.getPreferredRole());
        }
        System.out.println("=".repeat(50));
    }

    // Converts team data to CSV format for file export
    public String toCSVString() {
        StringBuilder sb = new StringBuilder();
        sb.append(teamId).append(",");
        sb.append(members.size()).append(",");
        sb.append(String.format("%.2f", averageSkillLevel)).append(",");

        for (int i = 0; i < members.size(); i++) {
            sb.append(members.get(i).getId());
            if (i < members.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return teamId + " (" + members.size() + " members)";
    }
}