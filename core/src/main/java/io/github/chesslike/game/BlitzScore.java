package io.github.chesslike.game;

import java.util.EnumSet;
import java.util.Set;

public class BlitzScore {
    public static class Result {
        public final int points;
        public final int basePoints;
        public final int multiplier;
        public final String label;
        public final boolean multiplierUp;

        public Result(int points, int basePoints, int multiplier, String label, boolean multiplierUp) {
            this.points = points;
            this.basePoints = basePoints;
            this.multiplier = multiplier;
            this.label = label;
            this.multiplierUp = multiplierUp;
        }
    }

    private int score;
    private int streak;
    private int multiplier;
    private int bestStreak;
    private int permanentMultiplierBonus;
    private int permanentBaseBonus;
    private int roomCaptures;
    private int totalCaptures;
    private int hungryBonusPercent;
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
    }

    public void setRelics(Iterable<BlitzRelic> activeRelics) {
        relics.clear();
        if (activeRelics != null) {
            for (BlitzRelic relic : activeRelics) {
                if (relic != null) relics.add(relic);
            }
        }
    }

    public Result addCapture(boolean cursed, String movement, boolean assassination, int enemyHealthPercent, int gold, int room) {
        totalCaptures++;
        roomCaptures++;
        streak++;
        bestStreak = Math.max(bestStreak, streak);

        int oldMultiplier = multiplier;
        if (streak >= 15) multiplier = 5;
        else if (streak >= 10) multiplier = 4;
        else if (streak >= 6) multiplier = 3;
        else if (streak >= 3) multiplier = 2;
        else multiplier = 1;
        multiplier += permanentMultiplierBonus;

        if (relics.contains(BlitzRelic.MOMENTUM_CORE) && streak % 5 == 0) {
            permanentMultiplierBonus++;
            multiplier++;
        }

        int base = 100 + permanentBaseBonus;
        if (relics.contains(BlitzRelic.ENDLESS_FANG) && totalCaptures % 10 == 0) {
            permanentBaseBonus += 25;
            base += 25;
        }

        if (relics.contains(BlitzRelic.BLOODIED_CROWN))
            base += Math.round(base * Math.min(100, streak * 5) / 100f);

        if (relics.contains(BlitzRelic.HUNGRY_BLADE))
            base += Math.round(base * hungryBonusPercent / 100f);

        int percent = 0;
        String label = "CAPTURE";
        if (relics.contains(BlitzRelic.BLACK_KNIGHT) && "KNIGHT".equals(movement)) percent += 100;
        if (relics.contains(BlitzRelic.ROYAL_SEAL) &&
            ("QUEEN".equals(movement) || "ROOK".equals(movement) || "BISHOP".equals(movement))) percent += 75;
        if (relics.contains(BlitzRelic.EXECUTIONERS_EYE) && enemyHealthPercent > 0 && enemyHealthPercent < 50) percent += 50;
        if (relics.contains(BlitzRelic.ASSASSINS_VEIL) && assassination) {
            percent += 100;
            label = "ASSASSINATION";
        }
        if (relics.contains(BlitzRelic.BROKEN_CROWN) && multiplier >= 5) percent += 25;
        if (cursed && relics.contains(BlitzRelic.CURSED_COIN)) {
            base += 300;
            label = "CURSED EXECUTION";
        }

        int preMultiplier = base + Math.round(base * percent / 100f);
        preMultiplier += Math.max(0, gold) / 100 * (relics.contains(BlitzRelic.GOLDEN_FANG) ? 50 : 0);

        if (relics.contains(BlitzRelic.GLASS_DAGGER) && roomCaptures == 1) {
            preMultiplier += 500;
            label = "FIRST BLOOD";
        }

        int points = preMultiplier * Math.max(1, multiplier);

        if (relics.contains(BlitzRelic.ECHO_STONE) && totalCaptures % 3 == 0) {
            points += Math.round(points * .5f);
            label = "ECHO CHAIN";
        }

        score += points;
        if (relics.contains(BlitzRelic.HUNGRY_BLADE))
            hungryBonusPercent = Math.min(500, hungryBonusPercent + 10);

        return new Result(points, preMultiplier, multiplier, label, multiplier > oldMultiplier);
    }

    public void resetRoom() {
        roomCaptures = 0;
        hungryBonusPercent = 0;
    }

    public int addCapture() {
        return addCapture(false, "", false, 100, 0, 0).points;
    }

    public int addSpecialCapture(int bonus) {
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        updateMultiplier();
        int points = (100 + bonus) * multiplier;
        score += points;
        return points;
    }

    public int addRoomClear() {
        int points = 500 * multiplier;
        score += points;
        return points;
    }

    public int addSpell() {
        int points = 75 * multiplier;
        score += points;
        return points;
    }

    public int addCardPlay() {
        int points = 25 * multiplier;
        score += points;
        return points;
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
    }

    public int getScore() { return score; }
    public int getStreak() { return streak; }
    public int getMultiplier() { return multiplier; }
    public int getBestStreak() { return bestStreak; }
    public int getTotalCaptures() { return totalCaptures; }
}
