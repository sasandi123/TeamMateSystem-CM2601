// TeamBuildingTask.java
package teammate;

import java.util.List;

/**
 * Runnable task for building teams concurrently
 */
public class TeamBuildingTask implements Runnable {
    private List<Participant> participants;
    private int teamSize;
    private TeamBuilder teamBuilder;

    public TeamBuildingTask(List<Participant> participants, int teamSize, TeamBuilder teamBuilder) {
        this.participants = participants;
        this.teamSize = teamSize;
        this.teamBuilder = teamBuilder;
    }

    @Override
    public void run() {
        try {
            System.out.println("[Thread " + Thread.currentThread().getId() + "] Starting team formation...");

            // Simulate processing time
            Thread.sleep(500);

            // Build teams
            teamBuilder.buildTeams(participants, teamSize);

            System.out.println("[Thread " + Thread.currentThread().getId() + "] Team formation completed!");

        } catch (InterruptedException e) {
            System.err.println("Team building interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error during team building: " + e.getMessage());
        }
    }
}