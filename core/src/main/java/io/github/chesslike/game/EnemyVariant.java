package io.github.chesslike.game;

/** Enemy archetypes layered on top of an enemy's chess movement. */
public enum EnemyVariant {
    NORMAL("Normal", "A standard enemy with no special ability.", 1, 1.0f),
    ARMORED("Armored", "Has +1 max HP and reduces each hit by 1 (minimum 1 damage).", 2, 0.85f),
    PHANTOM("Phantom", "Randomly changes its movement pattern each turn.", 1, 1.15f),
    BERSERKER("Berserker", "Moves twice per turn while at half health or less.", 1, 1.0f),
    PLAGUEBEARER("Plaguebearer", "Leaves a poisonous tile when defeated.", 1, 1.0f),
    MENDER("Mender", "Heals the most wounded nearby ally by 1 HP each turn.", 1, 0.9f),
    PYROMANCER("Pyromancer", "Uses diagonal bishop movement.", 1, 1.0f),
    CRYOMANCER("Cryomancer", "Uses straight-line rook movement.", 1, 0.9f),
    NECROMANCER("Necromancer", "Shifts between knight, bishop, and rook movement.", 1, 0.9f),
    STRIKER("Striker", "Uses knight movement to close distance quickly.", 1, 1.2f),
    SUMMONER("Summoner", "Uses queen movement to pressure open lanes.", 1, 0.85f),
    BOSS("FLOOR BREAKER", "A powerful floor boss with 12 HP and two moves per turn.", 12, 0.85f);

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
