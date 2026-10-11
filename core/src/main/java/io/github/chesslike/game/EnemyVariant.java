package io.github.chesslike.game;

/** Combat archetypes layered on top of an enemy's chess movement. */
public enum EnemyVariant {
    NORMAL("Normal", "A standard enemy with no special ability.", 1, 1.0f),
    ARMORED("Armored", "Has extra health and reduces each hit by 1.", 2, 0.85f),
    PHANTOM("Phantom", "Changes its movement pattern each turn.", 1, 1.15f),
    BERSERKER("Berserker", "Moves twice per turn while wounded.", 1, 1.0f),
    PLAGUEBEARER("Plaguebearer", "Leaves a poisonous tile when defeated.", 1, 1.0f),
    MENDER("Mender", "Heals a nearby wounded ally.", 1, 0.9f),
    PYROMANCER("Pyromancer", "Bishop movement; threatens diagonals with fire.", 1, 1.0f),
    CRYOMANCER("Cryomancer", "Rook movement; freezes a card when it lines up with you.", 1, 0.9f),
    NECROMANCER("Necromancer", "Shifts between knight, bishop, and rook movement.", 1, 0.9f),
    STRIKER("Striker", "Uses knight movement to close distance quickly.", 1, 1.2f),
    SUMMONER("Summoner", "Uses queen movement to pressure open lanes.", 1, 0.85f),
    BOSS("FLOOR BREAKER", "A floor boss with a unique health tier and aggressive movement.", 12, 0.85f);

    private final String displayName;
    private final String description;
    private final int bonusHealth;
    private final float movementSpeed;

    EnemyVariant(String displayName, String description, int bonusHealth, float movementSpeed) {
        this.displayName = displayName;
        this.description = description;
        this.bonusHealth = bonusHealth;
        this.movementSpeed = movementSpeed;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public int getBonusHealth() { return bonusHealth; }
    public float getMovementSpeed() { return movementSpeed; }
}
