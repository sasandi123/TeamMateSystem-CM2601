package teammate.concurrent;

import teammate.entity.Participant;
import teammate.entity.Team;
import teammate.util.TeamFormationHelper;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Processes a batch of participants in parallel
 */
public class BatchProcessor implements Callable<List<Team>> {
    private List<Participant> participants;
    private int teamSize;
    private double globalTargetSkill;

    public BatchProcessor(List<Participant> participants, int teamSize, double globalTargetSkill) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.globalTargetSkill = globalTargetSkill;
    }

    @Override
    public List<Team> call() {
        List<Team> teams = new ArrayList<>();
        List<Participant> available = new ArrayList<>(participants);

        Collections.shuffle(available);

        int attempts = 0;
        double skillTolerance = 0.15;

        while (available.size() >= teamSize && attempts < 40) {
            Team team = TeamFormationHelper.buildSingleTeam(available, teamSize,
                    globalTargetSkill, skillTolerance);

            if (team != null && TeamFormationHelper.isTeamValid(team)) {
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

        return teams;
    }
}