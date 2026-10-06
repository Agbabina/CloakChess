package io.github.chesslike.game;

import java.util.Random;

public class Relic {

    public enum Blessing {

        IRON_HEART("Iron Heart", "+1 max HP"),
        SECOND_WIND("Second Wind", "Heal 1 HP after each room"),
        GOLD_MAGNET("Gold Magnet", "+10 gold per room"),
        SHOCKWAVE("Shockwave", "Ground pound also deals 1 damage"),
        ARCANE_WELL("Arcane Well", "+2 max mana"),
        MANA_SPRING("Mana Spring", "Playing a card restores 1 extra mana"),
        THRIFTY_MAGE("Thrifty Mage", "Spells cost 1 less mana"),
        LUCKY_DRAW("Lucky Draw", "+25% chance to receive a card"),
        HEARTY_BREW("Hearty Brew", "Potions heal 1 extra HP");

        private final String label;
        private final String description;

        Blessing(String label, String description) {
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

    public enum Curse {

        FRAGILE("Fragile", "-1 max HP"),
        TOLL("Toll", "-10 gold per room"),
        HARDENED("Hardened Foes", "Enemies have +1 HP"),
        DRAINED("Drained", "-2 max mana"),
        MANA_LEAK("Mana Leak", "Start each room with half mana"),
        COSTLY_MAGIC("Costly Magic", "Spells cost 1 more mana"),
        STINGY_DECK("Stingy Deck", "-25% chance to receive a card");

        private final String label;
        private final String description;

        Curse(String label, String description) {
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

    private static final float CURSE_CHANCE = 0.75f;

    private static final int PRICE = 60;
    private static final int CURSED_PRICE = 40;

    private final Blessing blessing;
    private final Curse curse; // may be null

    public Relic(Blessing blessing, Curse curse) {
        this.blessing = blessing;
        this.curse = curse;
    }

    public static Relic random(Random random) {

        Blessing[] blessings = Blessing.values();
        Curse[] curses = Curse.values();

        Blessing blessing = blessings[random.nextInt(blessings.length)];

        Curse curse = random.nextFloat() < CURSE_CHANCE
            ? curses[random.nextInt(curses.length)]
            : null;

        return new Relic(blessing, curse);
    }

    public Blessing getBlessing() {
        return blessing;
    }

    public Curse getCurse() {
        return curse;
    }

    public boolean isCursed() {
        return curse != null;
    }

    public String getName() {
        return curse == null
            ? blessing.getLabel()
            : blessing.getLabel() + " (" + curse.getLabel() + ")";
    }

    public String getDescription() {
        return curse == null
            ? blessing.getDescription()
            : blessing.getDescription() + "  CURSE: " + curse.getDescription();
    }

    // Cursed relics are cheaper
    public int getPrice() {
        return curse == null ? PRICE : CURSED_PRICE;
    }
}
