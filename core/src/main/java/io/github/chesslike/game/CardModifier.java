package io.github.chesslike.game;

import java.util.Random;

public enum CardModifier {

    // Blessings
    LEECH("Leech", "Capture with it: heal 1 HP", false),
    FRUGAL("Frugal", "50% chance a play is free", false),

    // Curses
    BRITTLE("Brittle", "Each play costs 2 uses", true),
    BLOODPRICE("Bloodprice", "Each play costs 1 HP", true);

    private final String label;
    private final String description;
    private final boolean curse;

    CardModifier(String label, String description, boolean curse) {
        this.label = label;
        this.description = description;
        this.curse = curse;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCurse() {
        return curse;
    }

    public static CardModifier random(Random random) {
        CardModifier[] all = values();
        return all[random.nextInt(all.length)];
    }
}
