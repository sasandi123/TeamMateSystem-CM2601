// TeamBuilder.java
package teammate;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class TeamBuilder {
    private List<Team> teams;
    private double overallAverageSkill;

    public TeamBuilder() {
        this.teams = new ArrayList<>();
        this.overallAverageSkill = 0;
    }

    // --- NEW METHODS FOR WORKFLOW CONTROL ---
    public List<Team> getTeams() {
        return new ArrayList<>(this.teams);
    }

    public void clearTeams() {
        this.teams.clear();
    }
    // ----------------------------------------


    /**
     * Builds balanced teams using the matching algorithm.
     */
    public void buildTeams(List<Participant> participants, int teamSize) {

        // Ensure teams from previous runs are cleared before starting a new build
        // Note: TeamMateSystem should call clearTeams() before calling this method.
        // But for safety, we clear the internal list here if not done externally.
        this.teams.clear();

        if (participants.isEmpty()) {
            System.out.println("No participants available for team formation");
            return;
        }

        // --- Calculate the required average skill for balancing ---
        int totalSkill = participants.stream().mapToInt(Participant::getSkillLevel).sum();
        int expectedTeams = participants.size() / teamSize;
        if (expectedTeams == 0) expectedTeams = 1;

        double targetTeamSkill = (double) totalSkill / expectedTeams;
        double targetParticipantSkill = targetTeamSkill / teamSize;
        this.overallAverageSkill = targetParticipantSkill;
        System.out.println("Target average participant skill per team: " + String.format("%.2f", overallAverageSkill));
        // -----------------------------------------------------------

        List<Participant> available = new ArrayList<>(participants); // Mutable copy of the current available pool
        Collections.shuffle(available);

        int attempts = 0;
        double skillToleranceMultiplier = 1.2;
        int initialAvailableSize = available.size();

        while (available.size() >= teamSize) {

            // Graceful Degradation Logic: Relax constraints if struggling
            if (attempts > 20) {
                if (skillToleranceMultiplier < 2.0) {
                    skillToleranceMultiplier += 0.4;
                    attempts = 0;
                    System.out.println("--- RELAXING CONSTRAINTS: Skill tolerance increased to "
                            + String.format("%.0f%%", (skillToleranceMultiplier - 1.0) * 100) + " ---");
                    Collections.shuffle(available);
                } else {
                    System.out.println("Warning: Difficulty forming more valid teams even with relaxed constraints. Stopping.");
                    break;
                }
            }

            attempts++;
            Team team = buildSingleTeam(available, teamSize);

            if (team != null && isTeamValid(team, teamSize)) {

                // Final skill balance validation
                if (team.getAverageSkill() > overallAverageSkill * skillToleranceMultiplier ||
                        team.getAverageSkill() < overallAverageSkill / skillToleranceMultiplier) {

                    System.out.println("Attempt " + attempts + ": Team formed but skill (" + String.format("%.2f", team.getAverageSkill()) +
                            ") too far from target (" + String.format("%.2f", overallAverageSkill) + "). Retrying...");
                    Collections.shuffle(available);
                    continue;
                }

                // Team is valid and balanced
                teams.add(team);

                // CRITICAL CHANGE: DO NOT SET STATUS! ONLY REMOVE FROM THE TEMP AVAILABLE LIST
                for (Participant member : team.getMembers()) {
                    available.remove(member);
                    // member.setStatus("Assigned"); // <-- REMOVED! Assignment status is set upon export.
                }

                attempts = 0;
                skillToleranceMultiplier = 1.2;
            } else if (team != null) {
                Collections.shuffle(available);
            }
        }

        System.out.println("\nTeam Formation Summary:");
        System.out.println("Teams created in this run: " + teams.size());
        int totalAssigned = initialAvailableSize - available.size();
        System.out.println("Participants assigned (in memory): " + totalAssigned);
        System.out.println("Unassigned participants remaining in pool: " + available.size());

        if (teams.size() > 0) {
            double finalAvgSkill = teams.stream().mapToDouble(Team::getAverageSkill).average().orElse(0.0);
            System.out.println("Final average skill across generated teams: " + String.format("%.2f", finalAvgSkill));
        }
    }

    /**
     * Builds a single team from available participants.
     */
    private Team buildSingleTeam(List<Participant> available, int teamSize) {
        // ... (private buildSingleTeam logic remains the same) ...
        if (available.size() < teamSize) {
            return null;
        }

        Team team = new Team(teamSize);
        List<Participant> candidates = new ArrayList<>(available);

        // Separate by personality
        List<Participant> leaders = new ArrayList<>();
        List<Participant> thinkers = new ArrayList<>();
        List<Participant> balanced = new ArrayList<>();
        List<Participant> others = new ArrayList<>();

        for (Participant p : candidates) {
            switch (p.getPersonalityType()) {
                case "Leader": leaders.add(p); break;
                case "Thinker": thinkers.add(p); break;
                case "Balanced": balanced.add(p); break;
                default: others.add(p);
            }
        }

        candidates.clear();
        candidates.addAll(thinkers);
        candidates.addAll(balanced);
        candidates.addAll(others);

        // 1. Add EXACTLY 1 leader (if available)
        if (!leaders.isEmpty()) {
            Participant leader = leaders.get(0);
            team.addMember(leader);
            candidates.remove(leader);
        }

        // 2. Add 1 thinker (if available and team not full)
        if (!team.isFull() && !thinkers.isEmpty()) {
            Participant thinker = thinkers.get(0);
            team.addMember(thinker);
            candidates.remove(thinker);
        }

        // 3. Add 1 balanced (if available and team not full)
        if (!team.isFull() && !balanced.isEmpty()) {
            Participant balancedP = balanced.get(0);
            team.addMember(balancedP);
            candidates.remove(balancedP);
        }

        // Fill remaining slots
        while (!team.isFull() && !candidates.isEmpty()) {
            Participant best = findBestCandidate(team, candidates);
            if (best != null) {
                team.addMember(best);
                candidates.remove(best);
            } else {
                team.addMember(candidates.get(0));
                candidates.remove(0);
            }
        }

        if (team.getCurrentSize() < teamSize) {
            return null;
        }

        return team;
    }

    /**
     * Finds the best candidate that adds diversity to the team
     */
    private Participant findBestCandidate(Team team, List<Participant> candidates) {
        Random random = new Random();

        List<Participant> bestCandidates = candidates.stream()
                .filter(candidate -> {
                    if (candidate.getPersonalityType().equals("Leader")) return false;
                    boolean roleExists = team.getRoleDistribution().containsKey(candidate.getPreferredRole());
                    int gameCount = team.getGameDistribution().getOrDefault(candidate.getPreferredGame(), 0);
                    boolean gameOk = gameCount < 2;
                    return !roleExists && gameOk;
                })
                .collect(Collectors.toList());

        if (!bestCandidates.isEmpty()) {
            return bestCandidates.get(random.nextInt(bestCandidates.size()));
        }

        List<Participant> goodCandidates = candidates.stream()
                .filter(candidate -> {
                    if (candidate.getPersonalityType().equals("Leader")) return false;
                    int gameCount = team.getGameDistribution().getOrDefault(candidate.getPreferredGame(), 0);
                    return gameCount < 2;
                })
                .collect(Collectors.toList());

        if (!goodCandidates.isEmpty()) {
            return goodCandidates.get(random.nextInt(goodCandidates.size()));
        }

        return null;
    }

    /**
     * Validates if a team meets all matching criteria
     */
    private boolean isTeamValid(Team team, int teamSize) {
        if (team.getCurrentSize() != teamSize) return false;
        if (team.getRoleCount() < 3) return false;

        Map<String, Integer> gameDistribution = team.getGameDistribution();
        for (int count : gameDistribution.values()) {
            if (count > 2) return false;
        }

        int leaderCount = team.getLeaderCount();
        int thinkerCount = team.getThinkerCount();

        if (leaderCount > 1) return false;
        if (thinkerCount > 2) return false;

        if (leaderCount == 0) {
            if (thinkerCount == 0 || thinkerCount == team.getCurrentSize()) {
                return false;
            }
        }
        return true;
    }

    public Team findTeamForParticipant(Participant participant) {
        for (Team team : teams) {
            if (team.hasMember(participant)) {
                return team;
            }
        }
        return null;
    }

    public int getTeamCount() {
        return teams.size();
    }

    public void displayAllTeams() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("CURRENT GENERATED TEAMS (" + teams.size() + " teams)");
        System.out.println("=".repeat(60));

        for (Team team : teams) {
            team.displayTeamInfo();
            System.out.println();
        }
    }

    public void exportTeamsToCSV(String filename) throws Exception {
        try (FileWriter fw = new FileWriter(filename);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            // Write header
            out.println("TeamID,ParticipantID,Name,Email,Game,SkillLevel,Role,PersonalityType,PersonalityScore");

            // Write team data
            for (Team team : teams) {
                for (Participant p : team.getMembers()) {
                    out.printf("%s,%s,%s,%s,%s,%d,%s,%s,%d%n",
                            team.getTeamId(),
                            p.getId(),
                            p.getName(),
                            p.getEmail(),
                            p.getPreferredGame(),
                            p.getSkillLevel(),
                            p.getPreferredRole(),
                            p.getPersonalityType(),
                            p.getPersonalityScore());
                }
            }

        } catch (IOException e) {
            throw new TeamMateException.FileWriteException("Error writing to file: " + e.getMessage());
        }
    }
}