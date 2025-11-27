package teammate.service;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.util.FileManager;
import teammate.util.TeamFormationHelper;
import java.util.*;

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

    /**
     * Adds a team and assigns it a unique ID
     */
    public void addTeam(Team team) {
        if (team != null) {
            team.setTargetSkillLevel(overallAverageSkill);
            team.finalizeTeamId();
            this.teams.add(team);
        }
    }

    /**
     * Displays all teams with target skill level
     */
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
            team.displayTeamInfo();
            System.out.println();
        }
    }

    /**
     * Resets teams for new generation attempt
     */
    public void resetCurrentGenerationStatus() {
        if (teams.isEmpty()) return;

        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Available");
            }
        }

        this.teams.clear();
    }

    /**
     * Marks all team members as assigned
     */
    public void markParticipantsAssigned() {
        if (teams.isEmpty()) return;

        for (Team team : teams) {
            for (Participant member : team.getMembers()) {
                member.setStatus("Assigned");
            }
        }
    }

    /**
     * Sequential team building algorithm
     */
    public void buildTeams(List<Participant> participants, int teamSize) {
        if (participants == null || participants.isEmpty()) {
            System.out.println("No participants available for team formation");
            return;
        }

        List<Participant> available = new ArrayList<>(participants);

        // Calculate target skill level
        int totalSkill = 0;
        for (Participant p : available) {
            totalSkill += p.getSkillLevel();
        }

        int expectedTeams = Math.max(1, available.size() / teamSize);
        this.overallAverageSkill = (double) totalSkill / (expectedTeams * teamSize);

        Collections.shuffle(available);

        int attempts = 0;
        double skillTolerance = 0.15;

        while (available.size() >= teamSize && attempts < 50) {
            Team team = TeamFormationHelper.buildSingleTeam(available, teamSize,
                    overallAverageSkill, skillTolerance);

            if (team != null && TeamFormationHelper.isTeamValid(team)) {
                addTeam(team);

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

    public List<Participant> getAllParticipantsInTeams() {
        List<Participant> assigned = new ArrayList<>();
        for (Team team : teams) {
            assigned.addAll(team.getMembers());
        }
        return assigned;
    }

    public void clearTeams() {
        this.teams.clear();
    }
}