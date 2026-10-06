package io.github.chesslike.game;

public enum BlitzRelic {
    BLOODIED_CROWN("Bloodied Crown", "Each consecutive capture gains +5% score."),
    GOLDEN_FANG("Golden Fang", "Every 100 gold earned grants +50 score."),
    EXECUTIONERS_EYE("Executioner's Eye", "Capturing an enemy below half HP gives +50% score."),
    BLACK_KNIGHT("Black Knight", "Knight captures give +100% score."),
    MOMENTUM_CORE("Momentum Core", "Every 5 streak permanently adds +1 multiplier."),
    GLASS_DAGGER("Glass Dagger", "First capture in each room grants +500 score."),
    HUNGRY_BLADE("Hungry Blade", "Each capture makes the next capture worth 10% more."),
    CURSED_COIN("Cursed Coin", "Cursed captures give +300 score and extra momentum."),
    ECHO_STONE("Echo Stone", "Every 3rd capture repeats 50% of its score."),
    TIME_SHARD("Time Shard", "Special captures restore 2 seconds."),
    BLOOD_CLOCK("Blood Clock", "Every capture restores 1 second."),
    ROYAL_SEAL("Royal Seal", "Queen, Rook and Bishop captures gain +75% score."),
    ASSASSINS_VEIL("Assassin's Veil", "Cloak/assassination captures deal double score."),
    BROKEN_CROWN("Broken Crown", "At x5 multiplier, all capture scores gain +25%."),
    ENDLESS_FANG("Endless Fang", "Every 10th capture permanently adds +25 base score."),

    CHRONO_COG("Chrono Cog", "Every 5th capture restores 3 seconds."),
    OVERTIME_GEM("Overtime Gem", "Captures under 20 seconds restore 3 seconds."),
    SUN_DIAL("Sun Dial", "Clearing a room restores 6 seconds."),
    WAR_DRUM("War Drum", "Cursed captures restore 2 seconds and gain +150 score."),
    VOID_MIRROR("Void Mirror", "Assassinations gain +50% score and 1 second."),
    COMBO_ENGINE("Combo Engine", "At 3+ streak, captures gain +25% score."),
    OVERDRIVE("Overdrive", "At x4+, captures gain +50% score."),
    CHAIN_LINK("Chain Link", "Every 4th capture adds +300 score."),
    LAST_STAND("Last Stand", "Under 15 seconds, captures gain +100% score and 1 second."),
    GOLD_CLOCK("Gold Clock", "Every 50 gold held adds +25 capture score."),
    SPELL_ENGINE("Spell Engine", "Casting a spell restores 2 seconds."),
    CARD_CLOCK("Card Clock", "Every 5 cards played restores 2 seconds."),
    RELIC_ENGINE("Relic Engine", "Buying a relic grants +1,000 score and 5 seconds."),
    MERCILESS_EDGE("Merciless Edge", "Captures at x3+ gain +30% score."),
    HASTE_CORE("Haste Core", "Room clears restore 3 seconds and grant +250 score."),
    EXECUTION_CLOCK("Execution Clock", "Every 7th capture restores 5 seconds."),
    CRITICAL_MASS("Critical Mass", "Every 6th capture gains +500 score."),
    GOLDEN_MOMENT("Golden Moment", "Every 100 gold held grants +100 capture score."),
    TIME_BLOOM("Time Bloom", "First capture of each room restores 4 seconds."),
    FRENZY_REACTOR("Frenzy Reactor", "At 5+ streak, every capture restores 1 second.");

    private final String name;
    private final String description;

    BlitzRelic(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
