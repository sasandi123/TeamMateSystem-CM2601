package teammate.concurrent;

import teammate.entity.Participant;
import teammate.service.TeamBuilder;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Concurrent task for building teams using Callable interface
 */
public class TeamBuildingTask implements Callable<Integer> {
    private List<Participant> participants;
    private int teamSize;
    private TeamBuilder teamBuilder;

    public TeamBuildingTask(List<Participant> participants, int teamSize, TeamBuilder teamBuilder) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.teamBuilder = teamBuilder;
    }

    @Override
    public Integer call() throws Exception {
        try {
            System.out.println("[Thread " + Thread.currentThread().getId() + "] Starting team formation...");

            Thread.sleep(500); // Simulate processing

            teamBuilder.buildTeams(participants, teamSize);

            System.out.println("[Thread " + Thread.currentThread().getId() + "] Team formation completed!");

            return teamBuilder.getTeamCount();

        } catch (InterruptedException e) {
            System.err.println("Team building interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            System.err.println("Error during team building: " + e.getMessage());
            throw e;
        }
    }
}