package teammate.entity;

// Represents a participant in the esports team formation system
public class Participant {
    private String id;
    private String name;
    private String email;
    private String preferredGame;
    private int skillLevel;
    private String preferredRole;
    private int personalityScore;
    private String personalityType;
    private String status;

    // Constructor for creating new participant with survey data
    public Participant(String id, String name, String email, String preferredGame, int skillLevel,
                       String preferredRole, int personalityScore, String personalityType) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.preferredGame = preferredGame;
        this.skillLevel = skillLevel;
        this.preferredRole = preferredRole;
        this.personalityScore = personalityScore;
        this.personalityType = personalityType;
        this.status = "Available";
    }

    // Constructor for loading participant from file
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

    public void setStatus(String status) { this.status = status; }

    // Converts participant data to CSV format for file storage
    public String toCSVString() {
        return String.format("%s,%s,%s,%s,%d,%s,%d,%s,%s",
                id, name, email, preferredGame, skillLevel, preferredRole,
                personalityScore, personalityType, status);
    }

    @Override
    public String toString() {
        return String.format("%s - %s (%s) | Game: %s | Skill: %d | Role: %s | Type: %s",
                id, name, email, preferredGame, skillLevel, preferredRole, personalityType);
    }
}