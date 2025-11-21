package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.exception.TeamMateException;
import teammate.util.FileManager;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for building teams
 */
public class TeamBuilder {
    private List<Team> teams;
    private double overallAverageSkill;

    public TeamBuilder() {
        this.teams = new ArrayList<>();
        this.overallAverageSkill = 0;
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
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

    public void resetTeamMembersStatus() {
        if (teams.isEmpty()) return;

        int count = 0;
        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Available");
                count++;
            }
        }

        if (count > 0) {
            System.out.println("Pre-generation Cleanup: " + count + " assignments reverted to 'Available'.");
        }

        this.teams.clear();
    }

    public void markParticipantsAssigned() {
        if (teams.isEmpty()) return;

        int count = 0;
        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Assigned");
                count++;
            }
        }

        System.out.println("✓ Updated status for " + count + " participants to 'Assigned'.");
    }

    public void buildTeams(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty()) {
            System.out.println("No participants available for team formation");
            return;
        }

        List<Participant> available = new ArrayList<>(participants);

        if (available.isEmpty()) {
            System.out.println("No available participants to form teams.");
            return;
        }

        // Calculate target skill level
        int totalSkill = available.stream().mapToInt(Participant::getSkillLevel).sum();
        int expectedTeams = Math.max(1, available.size() / teamSize);
        this.overallAverageSkill = (double) totalSkill / (expectedTeams * teamSize);

        System.out.println("Target average skill per participant: " + String.format("%.2f", overallAverageSkill));

        Collections.shuffle(available);

        int maxAttempts = 50;
        int attempts = 0;
        double skillTolerance = 0.15; // Start with 15% tolerance

        while (available.size() >= teamSize && attempts < maxAttempts) {
            Team team = buildSingleTeam(available, teamSize, skillTolerance);

            if (team != null && isTeamValid(team)) {
                teams.add(team);

                // Remove assigned members
                for (Participant member : team.getMembers()) {
                    available.remove(member);
                }

                attempts = 0; // Reset attempts on success
            } else {
                attempts++;

                // Gradually relax constraints
                if (attempts % 10 == 0 && skillTolerance < 0.35) {
                    skillTolerance += 0.05;
                    System.out.println("--- Relaxing skill tolerance to " +
                            String.format("%.0f%%", skillTolerance * 100) + " ---");
                    Collections.shuffle(available);
                }
            }
        }

        System.out.println("\nTeam Formation Complete:");
        System.out.println("  Teams formed: " + teams.size());
        System.out.println("  Participants assigned: " + (participants.size() - available.size()));
        System.out.println("  Participants unassigned: " + available.size());
    }

    private Team buildSingleTeam(List<Participant> candidates, int teamSize, double skillTolerance) {
        if (candidates.size() < teamSize) return null;

        Team team = new Team(teamSize);
        List<Participant> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool);

        List<Participant> selected = new ArrayList<>();

        // STEP 1: Find exactly 1 Leader (MANDATORY)
        Participant leader = pool.stream()
                .filter(p -> p.getPersonalityType().equals("Leader"))
                .findFirst()
                .orElse(null);

        if (leader == null) {
            return null; // Cannot form team without a leader
        }

        selected.add(leader);
        pool.remove(leader);

        // STEP 2: Find 1-2 Thinkers
        List<Participant> thinkers = pool.stream()
                .filter(p -> p.getPersonalityType().equals("Thinker"))
                .limit(2)
                .collect(Collectors.toList());

        if (thinkers.isEmpty()) {
            return null; // Must have at least 1 thinker
        }

        selected.addAll(thinkers);
        pool.removeAll(thinkers);

        // STEP 3: Fill remaining spots, ensuring role and game diversity
        while (selected.size() < teamSize && !pool.isEmpty()) {
            Participant candidate = pool.remove(0);

            // Check if adding this candidate maintains diversity
            selected.add(candidate);

            // Temporarily create team to check constraints
            Team tempTeam = new Team(teamSize);
            for (Participant p : selected) {
                tempTeam.addMember(p);
            }

            // If constraints violated, remove and try next
            if (!meetsBasicConstraints(tempTeam)) {
                selected.remove(candidate);
                continue;
            }
        }

        // Only return team if it's full and meets all constraints
        if (selected.size() == teamSize) {
            for (Participant p : selected) {
                team.addMember(p);
            }

            // Final skill check
            double teamAvg = team.getAverageSkill();
            double lowerBound = overallAverageSkill * (1 - skillTolerance);
            double upperBound = overallAverageSkill * (1 + skillTolerance);

            if (teamAvg >= lowerBound && teamAvg <= upperBound) {
                return team;
            }
        }

        return null;
    }

    private boolean meetsBasicConstraints(Team team) {
        List<Participant> members = team.getMembers();

        // Check game diversity - max 2 from same game
        Map<String, Long> gameCounts = members.stream()
                .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));

        if (gameCounts.values().stream().anyMatch(count -> count > 2)) {
            return false;
        }

        // Check role diversity - at least 3 different roles
        long distinctRoles = members.stream()
                .map(Participant::getPreferredRole)
                .distinct()
                .count();

        return distinctRoles >= 3;
    }

    private boolean isTeamValid(Team team) {
        List<Participant> members = team.getMembers();

        // 1. Check personality mix: Exactly 1 Leader
        long leaders = members.stream()
                .filter(p -> p.getPersonalityType().equals("Leader"))
                .count();

        if (leaders != 1) return false;

        // 2. Check personality mix: 1-2 Thinkers
        long thinkers = members.stream()
                .filter(p -> p.getPersonalityType().equals("Thinker"))
                .count();

        if (thinkers < 1 || thinkers > 2) return false;

        // 3. Check game diversity: Max 2 from same game
        Map<String, Long> gameCounts = members.stream()
                .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));

        if (gameCounts.values().stream().anyMatch(count -> count > 2)) return false;

        // 4. Check role diversity: At least 3 different roles
        long distinctRoles = members.stream()
                .map(Participant::getPreferredRole)
                .distinct()
                .count();

        if (distinctRoles < 3) return false;

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

    public void exportTeamsSnapshot(String filename) throws Exception {
        FileManager.exportTeamsSnapshot(teams, filename);
    }

    public void appendTeamsToCumulative() throws Exception {
        FileManager.appendTeamsToCumulative(teams);
    }
}