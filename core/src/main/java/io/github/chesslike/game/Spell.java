package io.github.chesslike.game;

public enum Spell {
    LIGHTNING("Lightning", "2 damage to target, 1 to enemies next to it", 45),
    POISON("Poison", "Target loses 1 HP each turn for 4 turns", 35),
    BURN("Burn", "Ignites target and neighbours: 1 damage a turn for 2 turns", 40),
    CONFUSE("Confuse", "Confuses the selected enemy for 1 turn", 50),
    CLOAK("Cloak", "Invisible for 2 turns: enemies hold still", 55),
    STUN("Stun", "Selected enemy cannot move for 2 turns", 40),
    ICE("Ice", "Selected enemy cannot move for 1 turn", 35),
    FREEZE("Freeze", "All enemies cannot move for 1 turn", 55),
    GLASSING("Glassing", "Encases the target: cannot move for 2 turns and takes +1 damage", 65),
    SHIELD("Shield", "Gain 2 DEF immediately", 45),
    CLEANSE("Cleanse", "Remove your poison, burn, confusion and negative status", 40);

    private final String label;
    private final String description;
    private final int cost;

    Spell(String label, String description, int cost) {
        this.label = label;
        this.description = description;
        this.cost = cost;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public int getCost() {
        return cost;
    }
}
