package io.github.chesslike.game;

public enum EnemyCurse {
    ARMORED("Armored", "Starts with 2 shield."),
    WARDED("Warded", "Immune to spells."),
    VENGEFUL("Vengeful", "Hurts you for 1 extra when captured or when it hits you."),
    VOLATILE("Volatile", "Explodes on death, hitting everything adjacent."),
    ANCHORED("Anchored", "Cannot be pushed."),
    GREEDY("Greedy", "Steals 10 gold when it hits you."),
    SAPPING("Sapping", "Drains 2 mana when it hits you."),
    HASTY("Hasty", "Moves twice per turn."),
    THORNY("Thorny", "Capturing it costs you 1 HP."),
    FRAIL("Frail", "Takes +1 damage from its next hit."),
    BRUTAL("Brutal", "Hits you for 1 extra damage."),
    CORROSIVE("Corrosive", "Capturing it strips all of your DEF."),
    CRIPPLING("Crippling", "When it hits you, one of your cards loses a use."),
    RELENTLESS("Relentless", "Moves twice per turn while you are at half HP or less."),
    HUNTER("Hunter", "Sees through Cloak and keeps moving."),
    JUGGERNAUT("Juggernaut", "Starts with 3 shield and cannot be pushed."),
    MARTYR("Martyr", "When it dies, every other enemy gains 1 shield."),
    MAIMING("Maiming", "When it hits you, you lose 1 max HP.");

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
