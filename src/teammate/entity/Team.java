package teammate.entity;

import java.util.*;

/**
 * Entity class representing a team
 */
public class Team {
    private String teamId;
    private List<Participant> members;
    private int maxSize;
    private static int teamCounter = 0;

    public Team(int maxSize) {
        this.teamId = generateTeamId();
        this.members = new ArrayList<>();
        this.maxSize = maxSize;
    }

    private String generateTeamId() {
        teamCounter++;
        return "TEAM" + String.format("%03d", teamCounter);
    }

    public static void resetTeamCounter() {
        teamCounter = 0;
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

    public int getRoleCount() {
        return getRoleDistribution().size();
    }

    public int getLeaderCount() {
        return (int) members.stream()
                .filter(p -> p.getPersonalityType().equals("Leader"))
                .count();
    }

    public int getThinkerCount() {
        return (int) members.stream()
                .filter(p -> p.getPersonalityType().equals("Thinker"))
                .count();
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

        // Add member IDs
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