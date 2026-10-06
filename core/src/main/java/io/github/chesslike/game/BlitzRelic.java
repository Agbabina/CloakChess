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
    ENDLESS_FANG("Endless Fang", "Every 10th capture permanently adds +25 base score.");

    private final String name;
    private final String description;

    BlitzRelic(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
