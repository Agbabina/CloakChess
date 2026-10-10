package io.github.chesslike.game;

/** Enemy archetypes: each archetype has its own sprite, movement style, and combat trait. */
public enum EnemyVariant {
    NORMAL("Normal", 1, 1.0f),
    ARMORED("Armored", 2, 0.85f),
    PHANTOM("Phantom", 1, 1.15f),
    BERSERKER("Berserker", 1, 1.0f),
    PLAGUEBEARER("Plaguebearer", 1, 1.0f),
    MENDER("Mender", 1, 0.9f),
    PYROMANCER("Pyromancer", 1, 1.0f),
    CRYOMANCER("Cryomancer", 1, 0.9f),
    NECROMANCER("Necromancer", 1, 0.9f),
    STRIKER("Striker", 1, 1.2f),
    SUMMONER("Summoner", 1, 0.85f),
    BOSS("FLOOR BREAKER", 12, 0.85f);

    private final String displayName;
    private final int bonusHealth;
    private final float movementSpeed;

    EnemyVariant(String displayName, int bonusHealth, float movementSpeed) {
        this.displayName = displayName;
        this.bonusHealth = bonusHealth;
        this.movementSpeed = movementSpeed;
    }

    public String getDisplayName() { return displayName; }
    public int getBonusHealth() { return bonusHealth; }
    public float getMovementSpeed() { return movementSpeed; }
}
