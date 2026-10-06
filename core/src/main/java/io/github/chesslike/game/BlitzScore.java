package io.github.chesslike.game;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class BlitzScore {
    public static class Component {
        public final String label;
        public final int points;
        public Component(String label, int points) {
            this.label = label;
            this.points = points;
        }
    }

    public static class Result {
        public final int points;
        public final int basePoints;
        public final int multiplier;
        public final String label;
        public final boolean multiplierUp;
        public final List<Component> components;

        public Result(int points, int basePoints, int multiplier, String label, boolean multiplierUp) {
            this(points, basePoints, multiplier, label, multiplierUp, new ArrayList<Component>());
        }

        public Result(int points, int basePoints, int multiplier, String label,
                      boolean multiplierUp, List<Component> components) {
            this.points = points;
            this.basePoints = basePoints;
            this.multiplier = multiplier;
            this.label = label;
            this.multiplierUp = multiplierUp;
            this.components = components == null ? new ArrayList<Component>() : components;
        }
    }

    private int score, streak, multiplier, bestStreak, permanentMultiplierBonus, permanentBaseBonus;
    private int roomCaptures, totalCaptures, hungryBonusPercent, cardsPlayed;
    private final Set<BlitzRelic> relics = EnumSet.noneOf(BlitzRelic.class);

    public BlitzScore() { reset(); }

    public void reset() {
        score = 0;
        streak = 0;
        multiplier = 1;
        bestStreak = 0;
        permanentMultiplierBonus = 0;
        permanentBaseBonus = 0;
        roomCaptures = 0;
        totalCaptures = 0;
        hungryBonusPercent = 0;
        cardsPlayed = 0;
    }

    public void resetRoom() {
        roomCaptures = 0;
    }

    public void setRelics(Iterable<BlitzRelic> activeRelics) {
        relics.clear();
        if (activeRelics != null)
            for (BlitzRelic r : activeRelics)
                if (r != null) relics.add(r);
    }

    private boolean has(BlitzRelic r) { return relics.contains(r); }

    private void add(List<Component> list, String label, int points) {
        if (points != 0) list.add(new Component(label, points));
    }

    public Result addCapture(boolean cursed, String movement, boolean assassination,
                             int enemyHealthPercent, int gold, int room, float timeRemaining) {
        totalCaptures++;
        roomCaptures++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);

        int oldMultiplier = multiplier;
        updateMultiplier();

        List<Component> parts = new ArrayList<Component>();
        int base = 100 + permanentBaseBonus;
        add(parts, "BASE CAPTURE", 100);

        if (permanentBaseBonus > 0) {
            add(parts, "PERMANENT BONUS", permanentBaseBonus);
        }

        if (has(BlitzRelic.ENDLESS_FANG) && totalCaptures % 10 == 0) {
            permanentBaseBonus += 25;
            base += 25;
            add(parts, "ENDLESS FANG", 25);
        }

        if (has(BlitzRelic.BLOODIED_CROWN)) {
            int bonus = Math.round(base * Math.min(100, streak * 5) / 100f);
            base += bonus;
            add(parts, "BLOODIED CROWN", bonus);
        }

        if (has(BlitzRelic.HUNGRY_BLADE) && hungryBonusPercent > 0) {
            int bonus = Math.round(base * hungryBonusPercent / 100f);
            base += bonus;
            add(parts, "HUNGRY BLADE", bonus);
        }

        if (has(BlitzRelic.GOLD_CLOCK)) {
            int bonus = (Math.max(0, gold) / 50) * 25;
            base += bonus;
            add(parts, "GOLD CLOCK", bonus);
        }

        if (has(BlitzRelic.GOLDEN_FANG)) {
            int bonus = (Math.max(0, gold) / 100) * 50;
            base += bonus;
            add(parts, "GOLDEN FANG", bonus);
        }

        if (has(BlitzRelic.GOLDEN_MOMENT)) {
            int bonus = (Math.max(0, gold) / 100) * 100;
            base += bonus;
            add(parts, "GOLDEN MOMENT", bonus);
        }

        int percent = 0;
        if (has(BlitzRelic.BLACK_KNIGHT) && "KNIGHT".equals(movement)) {
            percent += 100;
            add(parts, "BLACK KNIGHT +100%", Math.round(base));
        }
        if (has(BlitzRelic.ROYAL_SEAL) &&
            ("QUEEN".equals(movement) || "ROOK".equals(movement) || "BISHOP".equals(movement))) {
            percent += 75;
            add(parts, "ROYAL SEAL +75%", Math.round(base * .75f));
        }
        if (has(BlitzRelic.EXECUTIONERS_EYE) && enemyHealthPercent > 0 && enemyHealthPercent < 50) {
            percent += 50;
            add(parts, "EXECUTIONER'S EYE +50%", Math.round(base * .50f));
        }
        if (has(BlitzRelic.ASSASSINS_VEIL) && assassination) {
            percent += 100;
            add(parts, "ASSASSINATION +100%", Math.round(base));
        }
        if (has(BlitzRelic.VOID_MIRROR) && assassination) {
            percent += 50;
            add(parts, "VOID MIRROR +50%", Math.round(base * .50f));
        }
        if (has(BlitzRelic.COMBO_ENGINE) && streak >= 3) {
            percent += 25;
            add(parts, "COMBO ENGINE +25%", Math.round(base * .25f));
        }
        if (has(BlitzRelic.OVERDRIVE) && multiplier >= 4) {
            percent += 50;
            add(parts, "OVERDRIVE +50%", Math.round(base * .50f));
        }
        if (has(BlitzRelic.MERCILESS_EDGE) && multiplier >= 3) {
            percent += 30;
            add(parts, "MERCILESS EDGE +30%", Math.round(base * .30f));
        }
        if (has(BlitzRelic.BROKEN_CROWN) && multiplier >= 5) {
            percent += 25;
            add(parts, "BROKEN CROWN +25%", Math.round(base * .25f));
        }
        if (has(BlitzRelic.LAST_STAND) && timeRemaining <= 15f) {
            percent += 100;
            add(parts, "LAST STAND +100%", Math.round(base));
        }

        if (cursed) {
            if (has(BlitzRelic.CURSED_COIN)) {
                base += 300;
                add(parts, "CURSED COIN", 300);
            }
            if (has(BlitzRelic.WAR_DRUM)) {
                base += 150;
                add(parts, "WAR DRUM", 150);
            }
        }

        if (assassination) add(parts, "ASSASSINATION", 400);
        if (enemyHealthPercent > 0 && enemyHealthPercent < 25) add(parts, "EXECUTION", 300);
        else if (enemyHealthPercent > 0 && enemyHealthPercent < 50) add(parts, "LOW HP", 150);

        if ("KNIGHT".equals(movement)) add(parts, "KNIGHT CAPTURE", 100);
        else if ("BISHOP".equals(movement)) add(parts, "BISHOP CAPTURE", 100);
        else if ("ROOK".equals(movement)) add(parts, "ROOK CAPTURE", 125);
        else if ("QUEEN".equals(movement)) add(parts, "QUEEN CAPTURE", 175);

        if (roomCaptures == 1) add(parts, "FIRST BLOOD", 500);
        if (timeRemaining <= 10f) add(parts, "LAST 10 SECONDS", 1000);
        else if (timeRemaining <= 20f) add(parts, "LAST 20 SECONDS", 500);
        if (streak >= 10) add(parts, "RAMPAGE CHAIN", 500);
        else if (streak >= 5) add(parts, "CHAIN BONUS", 250);

        if (has(BlitzRelic.GLASS_DAGGER) && roomCaptures == 1) {
            base += 500;
            add(parts, "GLASS DAGGER", 500);
        }
        if (has(BlitzRelic.CHAIN_LINK) && totalCaptures % 4 == 0) {
            base += 300;
            add(parts, "CHAIN LINK", 300);
        }
        if (has(BlitzRelic.CRITICAL_MASS) && totalCaptures % 6 == 0) {
            base += 500;
            add(parts, "CRITICAL MASS", 500);
        }

        // Execution bonuses above are additive score components. Relic percentage
        // bonuses modify the subtotal, matching the old relic behavior.
        int additive = 0;
        for (Component c : parts) {
            if (!c.label.contains("%")) additive += c.points;
        }
        int percentBonus = Math.round((base) * percent / 100f);
        int pre = additive + percentBonus;
        int points = pre * Math.max(1, multiplier);

        if (has(BlitzRelic.ECHO_STONE) && totalCaptures % 3 == 0) {
            int echo = Math.round(points * .5f);
            points += echo;
            add(parts, "ECHO STONE", echo);
        }

        if (has(BlitzRelic.HUNGRY_BLADE))
            hungryBonusPercent = Math.min(500, hungryBonusPercent + 10);

        String label = "CAPTURE";
        if (assassination) label = "EXECUTION";
        else if (cursed) label = "CURSED";
        else if (roomCaptures == 1) label = "FIRST BLOOD";
        else if (streak >= 10) label = "RAMPAGE";
        else if (streak >= 5) label = "CHAIN";

        score += points;
        return new Result(points, pre, multiplier, label, multiplier > oldMultiplier, parts);
    }

    public Result addCapture(boolean cursed, String movement, boolean assassination,
                             int enemyHealthPercent, int gold, int room) {
        return addCapture(cursed, movement, assassination, enemyHealthPercent, gold, room, 999f);
    }

    public int addCapture() {
        return addCapture(false, "", false, 100, 0, 0, 999f).points;
    }

    public int addSpecialCapture(int bonus) {
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        updateMultiplier();
        int p = (100 + bonus) * multiplier;
        score += p;
        return p;
    }

    public int addRoomClear() {
        int p = 500 * multiplier;
        if (has(BlitzRelic.HASTE_CORE)) p += 250;
        score += p;
        return p;
    }

    public int addSpell() {
        int p = 75 * multiplier;
        score += p;
        return p;
    }

    public int addCardPlay() {
        cardsPlayed++;
        int p = 25 * multiplier;
        score += p;
        return p;
    }

    public void breakStreak() {
        streak = 0;
        multiplier = 1 + permanentMultiplierBonus;
        hungryBonusPercent = 0;
    }

    private void updateMultiplier() {
        if (streak >= 15) multiplier = 5;
        else if (streak >= 10) multiplier = 4;
        else if (streak >= 6) multiplier = 3;
        else if (streak >= 3) multiplier = 2;
        else multiplier = 1;

        multiplier += permanentMultiplierBonus;

        if (has(BlitzRelic.MOMENTUM_CORE) && streak % 5 == 0) {
            permanentMultiplierBonus++;
            multiplier++;
        }
    }

    public int getScore() { return score; }
    public int getStreak() { return streak; }
    public int getMultiplier() { return multiplier; }
    public int getBestStreak() { return bestStreak; }
    public int getTotalCaptures() { return totalCaptures; }
    public int getRoomCaptures() { return roomCaptures; }
    public int getCardsPlayed() { return cardsPlayed; }
    public void addBonusScore(int points) { score += Math.max(0, points); }
}
