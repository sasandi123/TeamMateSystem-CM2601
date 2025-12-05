package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.util.FileManager;
import teammate.util.TeamFormationHelper;
import java.util.*;

// Manages team formation and maintains team collection
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

    // Sets the target average skill level for all teams
    public void setOverallAverageSkill(double skill) {
        this.overallAverageSkill = skill;
    }

    public double getOverallAverageSkill() {
        return overallAverageSkill;
    }

    // Adds a team and assigns it a unique sequential ID
    public void addTeam(Team team) {
        if (team != null) {
            team.setTargetSkillLevel(overallAverageSkill);
            team.finalizeTeamId();
            this.teams.add(team);
        }
    }

    // Displays all teams with their details and target skill level
    public void displayAllTeams() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("GENERATED TEAMS (" + teams.size() + " teams)");
        System.out.println("=".repeat(60));

        if (overallAverageSkill > 0) {
            System.out.println("Target Average Skill Level: " +
                    String.format("%.2f", overallAverageSkill));
            System.out.println("=".repeat(60));
        }

        for (Team team : teams) {
            team.displayTeamInfo();// sq no. 1.1.1 of view teams use case
            System.out.println();
        }
    }

    // Resets teams and reverts participant status for regeneration
    public void resetCurrentGenerationStatus() {
        if (teams.isEmpty()) return;

        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Available");
            }
        }

        this.teams.clear();
        this.overallAverageSkill = 0;
    }

    // Updates participant status to assigned after team finalization
    public void markParticipantsAssigned() {
        if (teams.isEmpty()) return;

        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Assigned");
            }
        }
    }

    // Builds teams sequentially using greedy algorithm with balancing constraints
    public void buildTeams(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty()) {
            System.out.println("No participants available for team formation");
            return;
        }

        List<Participant> available = new ArrayList<>(participants);

        // Calculate target skill level for balancing
        int totalSkill = 0;
        for (Participant p : available) {
            totalSkill += p.getSkillLevel();
        }

        int expectedTeams = Math.max(1, available.size() / teamSize);
        this.overallAverageSkill = (double) totalSkill / (expectedTeams * teamSize);

        Collections.shuffle(available);

        int attempts = 0;
        double skillTolerance = 0.10;

        // Continue forming teams while enough participants remain
        while (available.size() >= teamSize && attempts < 50) {
            Team team = TeamFormationHelper.buildSingleTeam(available, teamSize,
                    overallAverageSkill, skillTolerance);

            if (team != null && TeamFormationHelper.isTeamValid(team)) {
                addTeam(team);

                // Remove assigned members from available pool
                for (Participant member : team.getMembers()) {
                    available.remove(member);
                }

                attempts = 0;
            } else {
                attempts++;

                // Gradually increase tolerance if struggling to form teams
                if (attempts % 10 == 0 && skillTolerance < 0.15) {
                    skillTolerance += 0.02;
                    Collections.shuffle(available);
                }
            }
        }
    }

    // Finds the team containing a specific participant
    public Team findTeamForParticipant(Participant participant) {
        for (Team team : teams) {
            if (team.hasMember(participant)) {
                return team;
            }
        }
        return null;
    }

    // Exports current teams to snapshot file
    public void exportTeamsSnapshot(String filename) throws Exception {
        FileManager.exportTeamsSnapshot(teams, filename);//sq no. 1.3.1 of export teams use case
    }

    // Appends teams to cumulative records with timestamp
    public void appendTeamsToCumulative() throws Exception {
        FileManager.appendTeamsToCumulative(teams);
    }

    // Returns all participants currently assigned to teams
    public List<Participant> collectAssignedParticipants() {
        List<Participant> assigned = new ArrayList<>();
        for (Team team : teams) {
            assigned.addAll(team.getMembers());
        }
        return assigned;
    }

    // Clears all teams from memory
    public void clearTeams() {
        this.teams.clear();
        this.overallAverageSkill = 0;
    }
}