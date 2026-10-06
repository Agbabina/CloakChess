package io.github.chesslike.game;

public enum BlitzOmen {
    BLOOD_MOON("Blood Moon", "Captures are worth +50%, but damage taken breaks your streak."),
    FORTUNES_EYE("Fortune's Eye", "Every 5th capture grants a bonus reward."),
    VENOM_MIST("Venom Mist", "Poisoned enemies give double score."),
    MANA_TIDE("Mana Tide", "Start with +2 mana; spells give +50% score."),
    IRON_TIDE("Iron Tide", "Start with +1 DEF; blocking damage preserves streak."),
    FAMINE("Famine", "Gold rewards are reduced, but room clears give +75% score."),
    ECLIPSE("Eclipse", "Cloak lasts longer and assassination kills give huge bonus score."),
    DREAD("Dread", "Enemies spawn with a higher chance of curses."),
    WITHERING("Withering", "Every room gains one extra cursed enemy."),
    HUNTERS_MOON("Hunter's Moon", "Hunter enemies are more common and worth +100% score."),
    GREED("Greed", "Gold earned also becomes Blitz score."),
    OVERCHARGE("Overcharge", "The first spell each room costs 0 mana."),
    GLASS_BOARD("Glass Board", "You deal more damage, but taking damage ends the run's streak."),
    BLACK_MARKET("Black Market", "Shop rerolls cost less and rare rewards appear more often."),
    CHAIN_REACTION("Chain Reaction", "Every third consecutive capture gives a burst of bonus score.");

    private final String name;
    private final String description;

    BlitzOmen(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
