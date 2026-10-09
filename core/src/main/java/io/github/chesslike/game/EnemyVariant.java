package io.github.chesslike.game;

/** Combat archetypes layered on top of an enemy's chess movement. */
public enum EnemyVariant {
    NORMAL("Normal", 1, 1.0f),
    ARMORED("Armored", 2, 0.85f),
    PHANTOM("Phantom", 1, 1.15f),
    BERSERKER("Berserker", 1, 1.0f),
    PLAGUEBEARER("Plaguebearer", 1, 1.0f),
    BOSS("FLOOR BREAKER", 12, 0.85f);

    private final String displayName;
    private final int bonusHealth;
    private final float movementSpeed;

    EnemyVariant(String displayName, int bonusHealth, float movementSpeed) {
        this.displayName = displayName;
        this.bonusHealth = bonusHealth;
        this.movementSpeed = movementSpeed;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBonusHealth() {
        return bonusHealth;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }
}
