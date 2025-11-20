// Participant.java
package teammate;

public class Participant {
    private String id;
    private String name;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private String preferredRole;
    private int personalityScore;
    private String personalityType;
    private String status; // CRITICAL: Tracks assignment status
    private static int participantCounter = 0;

    // Constructor for new participants (e.g., from survey)
    public Participant(String name, String email, String preferredGame, int skillLevel,
                       String preferredRole, int personalityScore, String personalityType) {
        this.id = generateId();
        this.name = name;
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
        this.status = "Available";
    }

    // Constructor for loading from CSV (including status)
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       String preferredRole, int personalityScore, String personalityType, String status) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
        this.status = status;
        updateCounter(id);
    }

    private String generateId() {
        participantCounter++;
        return "P" + String.format("%03d", participantCounter);
    }

    private void updateCounter(String id) {
        try {
            int num = Integer.parseInt(id.substring(1));
            if (num > participantCounter) {
                participantCounter = num;
            }
        } catch (Exception e) {
            // Ignore invalid IDs
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPreferredGame() { return preferredGame; }
    public int getSkillLevel() { return skillLevel; }
    public String getPreferredRole() { return preferredRole; }
    public int getPersonalityScore() { return personalityScore; }
    public String getPersonalityType() { return personalityType; }
    public String getStatus() { return status; }

    // Setters
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("%s - %s (%s) | Game: %s | Skill: %d | Role: %s | Type: %s | Status: %s",
                id, name, email, preferredGame, skillLevel, preferredRole, personalityType, status);
    }

    public String toCSVString() {
        return String.format("%s,%s,%s,%s,%d,%s,%d,%s,%s",
                id, name, email, preferredGame, skillLevel, preferredRole,
                personalityScore, personalityType, status);
    }
}