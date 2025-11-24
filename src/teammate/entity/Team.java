package teammate.entity;

import java.util.*;
import java.io.*;

public class Team {
    private String teamId;
    private List<Participant> members;
    private int maxSize;
    private static int teamCounter = -1; // -1 means not loaded yet
    private static final String COUNTER_FILE = "team_counter.dat";

    public Team(int maxSize) {
        // Load counter on first team creation
        if (teamCounter == -1) {
            loadTeamCounter();
        }
        // 💡 CRITICAL FIX: Use a temporary ID. The final ID will be generated later
        // after the team is confirmed valid by the formation engine.
        this.teamId = "TEMP_ID";
        this.members = new ArrayList<>();
        this.maxSize = maxSize;
    }

    /**
     * 💡 NEW METHOD: Finalizes the team ID by incrementing the counter,
     * saving the state, and assigning the sequential ID.
     * This must be called only when a team is successfully formed and validated.
     */
    public void finalizeTeamId() {
        teamCounter++;
        saveTeamCounter();
        this.teamId = "TEAM" + String.format("%04d", teamCounter);
    }

    // ❌ REMOVED: The private generateTeamId() method that was consuming IDs prematurely.

    /**
     * Load team counter from file
     */
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
                // 💡 FIX: Print the correct NEXT team number (current counter + 1)
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

    /**
     * Save team counter to file immediately
     */
    private static synchronized void saveTeamCounter() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(COUNTER_FILE))) {
            pw.println(teamCounter);
            pw.flush(); // Force write to disk
        } catch (IOException e) {
            System.err.println("[System] Warning: Could not save team counter");
        }
    }

    public static void resetTeamCounter() {
        teamCounter = 0;
        saveTeamCounter();
    }

    public boolean addMember(Participant participant) {
        if (members.size() < maxSize) {
            members.add(participant);
            return true;
        }
        return false;
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    public int getCurrentSize() {
        return members.size();
    }

    public int getMaxSize() {
        return maxSize;
    }

    public List<Participant> getMembers() {
        return new ArrayList<>(members);
    }

    public String getTeamId() {
        // Returns the final ID or "TEMP_ID" if not yet finalized.
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
        return members.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(0);
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
        sb.append(getCurrentSize()).append(",");
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