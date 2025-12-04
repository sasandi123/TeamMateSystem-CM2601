package teammate.util;

import teammate.entity.Participant;
import teammate.entity.Team;
import java.util.*;

// Provides utility methods for team formation logic and validation
public class TeamFormationHelper {

    // Builds a single balanced team from available candidates
    public static Team buildSingleTeam(List<Participant> candidates, int teamSize,
                                       double targetSkill, double skillTolerance) {
        if (candidates.size() < teamSize) return null;

        Team team = new Team(teamSize);
        List<Participant> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool);

        List<Participant> selected = new ArrayList<>();

        // Select exactly 1 Leader
        Participant leader = null;
        for (Participant p : pool) {
            if (p.getPersonalityType().equals("Leader")) {
                leader = p;
                break;
            }
        }
        if (leader == null) return null;

        selected.add(leader);
        pool.remove(leader);

        // Select 1-2 Thinkers
        List<Participant> thinkers = new ArrayList<>();
        int thinkerCount = 0;
        for (Participant p : pool) {
            if (p.getPersonalityType().equals("Thinker")) {
                thinkers.add(p);
                thinkerCount++;
                if (thinkerCount >= 2) break;
            }
        }
        if (thinkers.isEmpty()) return null;

        selected.addAll(thinkers);
        pool.removeAll(thinkers);

        // Fill remaining slots while checking constraints
        while (selected.size() < teamSize && !pool.isEmpty()) {
            Participant candidate = pool.remove(0);
            selected.add(candidate);

            if (!meetsBasicConstraints(selected)) {
                selected.remove(candidate);
                continue;
            }
        }

        // Validate team meets all requirements including skill balance
        if (selected.size() == teamSize) {
            for (Participant p : selected) {
                team.addMember(p);
            }

            double teamAvg = team.getAverageSkill();
            double lowerBound = targetSkill * (1 - skillTolerance);
            double upperBound = targetSkill * (1 + skillTolerance);

            if (teamAvg >= lowerBound && teamAvg <= upperBound) {
                return team;
            }
        }

        return null;
    }

    // Checks if team meets basic constraints (game and role diversity)
    public static boolean meetsBasicConstraints(List<Participant> members) {
        Map<String, Integer> gameCounts = new HashMap<>();
        for (Participant p : members) {
            String game = p.getPreferredGame();
            gameCounts.put(game, gameCounts.getOrDefault(game, 0) + 1);
        }

        // Maximum 2 players per game
        for (Integer count : gameCounts.values()) {
            if (count > 2) return false;
        }

        // Minimum 3 unique roles
        Set<String> uniqueRoles = new HashSet<>();
        for (Participant p : members) {
            uniqueRoles.add(p.getPreferredRole());
        }

        return uniqueRoles.size() >= 3;
    }

    // Validates team meets all formation rules
    public static boolean isTeamValid(Team team) {
        List<Participant> members = team.getMembers();

        // Exactly 1 Leader
        int leaders = 0;
        for (Participant p : members) {
            if (p.getPersonalityType().equals("Leader")) {
                leaders++;
            }
        }
        if (leaders != 1) return false;

        // 1-2 Thinkers
        int thinkers = 0;
        for (Participant p : members) {
            if (p.getPersonalityType().equals("Thinker")) {
                thinkers++;
            }
        }
        if (thinkers < 1 || thinkers > 2) return false;

        // Maximum 2 players per game
        Map<String, Integer> gameCounts = new HashMap<>();
        for (Participant p : members) {
            String game = p.getPreferredGame();
            gameCounts.put(game, gameCounts.getOrDefault(game, 0) + 1);
        }
        for (Integer count : gameCounts.values()) {
            if (count > 2) return false;
        }

        // Minimum 3 unique roles
        Set<String> uniqueRoles = new HashSet<>();
        for (Participant p : members) {
            uniqueRoles.add(p.getPreferredRole());
        }
        if (uniqueRoles.size() < 3) return false;

        return true;
    }
}