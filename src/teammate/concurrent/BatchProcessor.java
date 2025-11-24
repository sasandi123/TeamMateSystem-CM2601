package teammate.concurrent;

import teammate.entity.Participant;
import teammate.entity.Team;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Callable task for forming teams within a batch in parallel processing.
 */
public class BatchProcessor implements Callable<List<Team>> {
    private List<Participant> participants;
    private int teamSize;

    public BatchProcessor(List<Participant> participants, int teamSize) {
        this.participants = participants;
        this.teamSize = teamSize;
    }

    @Override
    public List<Team> call() {
        List<Team> teams = new ArrayList<>();
        List<Participant> available = new ArrayList<>(participants);

        double targetSkill = available.stream()
                .mapToInt(Participant::getSkillLevel)
                .average()
                .orElse(5.0);

        Collections.shuffle(available);

        int attempts = 0;
        double skillTolerance = 0.15;

        while (available.size() >= teamSize && attempts < 40) {
            Team team = buildSingleTeam(available, teamSize, targetSkill, skillTolerance);

            if (team != null && isTeamValid(team)) {
                // IMPORTANT: The ID is *NOT* finalized here. It is finalized
                // sequentially by the TeamBuilder when all batches are merged.
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

    private Team buildSingleTeam(List<Participant> candidates, int teamSize,
                                 double targetSkill, double skillTolerance) {
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

            if (!meetsBasicConstraints(selected)) {
                selected.remove(candidate);
                continue;
            }
        }

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

    private boolean meetsBasicConstraints(List<Participant> members) {
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
}