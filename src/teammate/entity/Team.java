package teammate.entity;

import java.util.*;
import java.io.*;

public class Team {
    private String teamId;
    private List<Participant> members;
    private int maxSize;
    private double targetSkillLevel;
    private static int teamCounter = -1;
    private static final String COUNTER_FILE = "team_counter.dat";
    private static boolean counterModified = false;

    public Team(int maxSize) {
        if (teamCounter == -1) {
            loadTeamCounter();
        }
        this.teamId = "TEMP_ID";
        this.members = new ArrayList<>();
        this.maxSize = maxSize;
        this.targetSkillLevel = 0;
    }

    /**
     * Assigns final team ID with counter increment
     * Counter is incremented but NOT saved to file yet
     */
    public void finalizeTeamId() {
        teamCounter++;
        counterModified = true;  // Mark that counter was changed
        this.teamId = "TEAM" + String.format("%04d", teamCounter);
    }

    /**
     * Save team counter to file
     * Should be called only when teams are exported/finalized
     */
    public static synchronized void saveTeamCounterToFile() {
        if (!counterModified) {
            return;  // No changes, don't save
        }

        try (PrintWriter pw = new PrintWriter(new FileWriter(COUNTER_FILE))) {
            pw.println(teamCounter);
            pw.flush();
            counterModified = false;  // Reset flag after saving
            System.out.println("[System] Team counter saved: Next team will be TEAM" +
                    String.format("%04d", teamCounter + 1));
        } catch (IOException e) {
            System.err.println("[System] Warning: Could not save team counter");
        }
    }

    /**
     * Reset counter to previous value if teams are not finalized
     * Call this when generation is cancelled or not exported
     */
    public static synchronized void resetCounter(int previousValue) {
        teamCounter = previousValue;
        counterModified = false;
    }

    private static synchronized void loadTeamCounter() {
        File file = new File(COUNTER_FILE);

        if (!file.exists()) {
            teamCounter = 0;
            System.out.println("[System] Starting fresh - First team will be TEAM0001");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(COUNTER_FILE))) {
            String line = br.readLine();
            if (line != null && !line.trim().isEmpty()) {
                teamCounter = Integer.parseInt(line.trim());
                System.out.println("[System] Loaded team counter - Next team: TEAM" +
                        String.format("%04d", teamCounter + 1));
            } else {
                teamCounter = 0;
            }
        } catch (Exception e) {
            System.out.println("[System] Could not load counter, starting from TEAM0001");
            teamCounter = 0;
        }
    }

    public void setTargetSkillLevel(double target) {
        this.targetSkillLevel = target;
    }

    public double getTargetSkillLevel() {
        return targetSkillLevel;
    }

    public boolean addMember(Participant participant) {
        if (members.size() < maxSize) {
            members.add(participant);
            return true;
        }
        return false;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }

    public String getTeamId() {
        return teamId;
    }

    public boolean hasMember(Participant participant) {
        for (Participant member : members) {
            if (member.getId().equals(participant.getId())) {
                return true;
            }
        }
        return false;
    }

    public double getAverageSkill() {
        if (members.isEmpty()) return 0;

        int totalSkill = 0;
        for (Participant p : members) {
            totalSkill += p.getSkillLevel();
        }

        return (double) totalSkill / members.size();
    }

    public Map<String, Integer> getRoleDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Participant p : members) {
            distribution.put(p.getPreferredRole(),
                    distribution.getOrDefault(p.getPreferredRole(), 0) + 1);
        }
        return distribution;
    }

    public Map<String, Integer> getPersonalityDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Participant p : members) {
            distribution.put(p.getPersonalityType(),
                    distribution.getOrDefault(p.getPersonalityType(), 0) + 1);
        }
        return distribution;
    }

    public Map<String, Integer> getGameDistribution() {
        Map<String, Integer> distribution = new HashMap<>();
        for (Participant p : members) {
            distribution.put(p.getPreferredGame(),
                    distribution.getOrDefault(p.getPreferredGame(), 0) + 1);
        }
        return distribution;
    }

    public void displayTeamInfo() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Team ID: " + teamId);
        System.out.println("Team Size: " + members.size() + "/" + maxSize);
        System.out.println("Average Skill Level: " + String.format("%.2f", getAverageSkill()));
        System.out.println("=".repeat(50));

        System.out.println("\nMembers:");
        for (int i = 0; i < members.size(); i++) {
            Participant p = members.get(i);
            System.out.printf("%d. %s\n", (i + 1), p.toString());
        }

        System.out.println("\nRole Distribution:");
        getRoleDistribution().forEach((role, count) ->
                System.out.println("  " + role + ": " + count));

        System.out.println("\nPersonality Distribution:");
        getPersonalityDistribution().forEach((type, count) ->
                System.out.println("  " + type + ": " + count));

        System.out.println("\nGame Distribution:");
        getGameDistribution().forEach((game, count) ->
                System.out.println("  " + game + ": " + count));
    }

    public String toCSVString() {
        StringBuilder sb = new StringBuilder();
        sb.append(teamId).append(",");
        sb.append(members.size()).append(",");
        sb.append(String.format("%.2f", getAverageSkill())).append(",");

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