package io.github.chesslike.game;

public enum EnemyCurse {

    ARMORED("ARMORED", "+1 HP"),
    VENGEFUL("VENGEFUL", "Hurts you when captured");

    private final String label;
    private final String description;

    EnemyCurse(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
