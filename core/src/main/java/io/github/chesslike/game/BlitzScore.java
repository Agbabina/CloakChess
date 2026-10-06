package io.github.chesslike.game;

public class BlitzScore {
    private int score;
    private int streak;
    private int multiplier;
    private int bestStreak;

    public BlitzScore() { reset(); }

    public void reset() {
        score = 0;
        streak = 0;
        multiplier = 1;
        bestStreak = 0;
    }

    public int addCapture() {
        streak++;
        bestStreak = Math.max(bestStreak, streak);
        updateMultiplier();
        int points = 100 * multiplier;
        score += points;
        return points;
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
        multiplier = 1;
    }

    private void updateMultiplier() {
        if (streak >= 15) multiplier = 5;
        else if (streak >= 10) multiplier = 4;
        else if (streak >= 6) multiplier = 3;
        else if (streak >= 3) multiplier = 2;
        else multiplier = 1;
    }

    public int getScore() { return score; }
    public int getStreak() { return streak; }
    public int getMultiplier() { return multiplier; }
    public int getBestStreak() { return bestStreak; }
}
