package io.github.chesslike.game;

/** Combat archetypes layered on top of an enemy's chess movement. */
public enum EnemyVariant {
    NORMAL("Normal", "A standard enemy with no special variant ability.", 1, 1.0f),
    ARMORED("Armored", "Takes at least 1 damage, but reduces each hit by 1.", 2, 0.85f),
    PHANTOM("Phantom", "Changes its movement pattern each turn, making its approach harder to predict.", 1, 1.15f),
    BERSERKER("Berserker", "When reduced to half health or less, moves twice per turn.", 1, 1.0f),
    PLAGUEBEARER("Plaguebearer", "Leaves a poisonous tile when defeated.", 1, 1.0f),
    BOSS("FLOOR BREAKER", "A powerful floor boss with high health and two moves per turn.", 12, 0.85f);

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

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getBonusHealth() {
        return bonusHealth;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }
}
