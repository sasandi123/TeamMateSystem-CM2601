package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.util.FileManager;
import java.util.*;
import java.util.stream.Collectors;

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

    public void addTeam(Team team) {
        if (team != null) {
            team.finalizeTeamId();
            this.teams.add(team);
        }
    }

    public void displayAllTeams() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GENERATED TEAMS (" + teams.size() + " teams)");
        System.out.println("=".repeat(60));

        for (Team team : teams) {
            team.displayTeamInfo();
            System.out.println();
        }
    }

    /**
     * Resets the status of participants in the current in-memory teams back to "Available"
     * and clears the teams list for a new generation attempt.
     */
    public void resetCurrentGenerationStatus() {
        if (teams.isEmpty()) return;

        // Only reset participants who are in the current in-memory teams
        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Available");
            }
        }

        // Clear the teams list for new generation
        this.teams.clear();
    }

    /**
     * Marks all participants in the currently generated teams as "Assigned" in memory.
     * The count is printed by the calling service class (OrganizerPortalService).
     */
    public void markParticipantsAssigned() {
        if (teams.isEmpty()) return;

        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Assigned");
            }
        }
        // Removed System.out.println statement here.
    }

    public void buildTeams(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty()) {
            System.out.println("No participants available for team formation");
            return;
        }

        List<Participant> available = new ArrayList<>(participants);

        int totalSkill = available.stream().mapToInt(Participant::getSkillLevel).sum();
        int expectedTeams = Math.max(1, available.size() / teamSize);
        this.overallAverageSkill = (double) totalSkill / (expectedTeams * teamSize);

        Collections.shuffle(available);

        int attempts = 0;
        double skillTolerance = 0.15;

        while (available.size() >= teamSize && attempts < 50) {
            Team team = buildSingleTeam(available, teamSize, skillTolerance);

            if (team != null && isTeamValid(team)) {
                teams.add(team);

                for (Participant member : team.getMembers()) {
                    available.remove(member);
                }

                attempts = 0;
            } else {
                attempts++;

                if (attempts % 10 == 0 && skillTolerance < 0.35) {
                    skillTolerance += 0.05;
                    Collections.shuffle(available);
                }
            }
        }
    }

    private Team buildSingleTeam(List<Participant> candidates, int teamSize, double skillTolerance) {
        if (candidates.size() < teamSize) return null;

        Team team = new Team(teamSize);
        List<Participant> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool);

        List<Participant> selected = new ArrayList<>();

        Participant leader = pool.stream()
                .filter(p -> p.getPersonalityType().equals("Leader"))
                .findFirst()
                .orElse(null);

        if (leader == null) return null;

        selected.add(leader);
        pool.remove(leader);

        List<Participant> thinkers = pool.stream()
                .filter(p -> p.getPersonalityType().equals("Thinker"))
                .limit(2)
                .collect(Collectors.toList());

        if (thinkers.isEmpty()) return null;

        selected.addAll(thinkers);
        pool.removeAll(thinkers);

        while (selected.size() < teamSize && !pool.isEmpty()) {
            Participant candidate = pool.remove(0);
            selected.add(candidate);

            Team tempTeam = new Team(teamSize);
            for (Participant p : selected) {
                tempTeam.addMember(p);
            }

            if (!meetsBasicConstraints(tempTeam)) {
                selected.remove(candidate);
                continue;
            }
        }

        if (selected.size() == teamSize) {
            for (Participant p : selected) {
                team.addMember(p);
            }

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

        Map<String, Long> gameCounts = members.stream()
                .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));

        if (gameCounts.values().stream().anyMatch(count -> count > 2)) {
            return false;
        }

        long distinctRoles = members.stream()
                .map(Participant::getPreferredRole)
                .distinct()
                .count();

        return distinctRoles >= 3;
    }

    private boolean isTeamValid(Team team) {
        List<Participant> members = team.getMembers();

        long leaders = members.stream()
                .filter(p -> p.getPersonalityType().equals("Leader"))
                .count();
        if (leaders != 1) return false;

        long thinkers = members.stream()
                .filter(p -> p.getPersonalityType().equals("Thinker"))
                .count();
        if (thinkers < 1 || thinkers > 2) return false;

        Map<String, Long> gameCounts = members.stream()
                .collect(Collectors.groupingBy(Participant::getPreferredGame, Collectors.counting()));
        if (gameCounts.values().stream().anyMatch(count -> count > 2)) return false;

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

    /**
     * Gets a combined list of all participants in the currently generated teams.
     */
    public List<Participant> getAllParticipantsInTeams() {
        List<Participant> assigned = new ArrayList<>();
        for (Team team : teams) {
            assigned.addAll(team.getMembers());
        }
        return assigned;
    }

    /**
     * Clears the temporary list of generated teams after export.
     */
    public void clearTeams() {
        this.teams.clear();
    }
}