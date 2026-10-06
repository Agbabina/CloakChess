package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class BlitzManager {
    private static final float STARTING_TIME = 180f;
    private final BlitzScore score = new BlitzScore();
    private final Preferences save = Gdx.app.getPreferences("CloakChessBlitz");
    private float timeRemaining;
    private boolean active;

    public BlitzManager() { reset(); }

    public void start() { reset(); active = true; }

    public void update(float delta) {
        if (!active) return;
        timeRemaining -= delta;
        if (timeRemaining <= 0f) {
            timeRemaining = 0f;
            endRun();
        }
    }

    public void endRun() {
        if (!active) return;
        active = false;
        if (score.getScore() > getBestScore()) {
            save.putInteger("bestScore", score.getScore());
            save.flush();
        }
    }

    public void reset() {
        score.reset();
        timeRemaining = STARTING_TIME;
        active = false;
    }

    public int capture() { return score.addCapture(); }
    public int specialCapture(int bonus) { return score.addSpecialCapture(bonus); }
    public int roomClear() { return score.addRoomClear(); }
    public int spellCast() { return score.addSpell(); }
    public int cardPlayed() { return score.addCardPlay(); }
    public void breakStreak() { score.breakStreak(); }

    public int getScore() { return score.getScore(); }
    public int getStreak() { return score.getStreak(); }
    public int getMultiplier() { return score.getMultiplier(); }
    public int getBestStreak() { return score.getBestStreak(); }
    public float getTimeRemaining() { return timeRemaining; }
    public boolean isActive() { return active; }
    public int getBestScore() { return save.getInteger("bestScore", 0); }
    public BlitzScore getScoreSystem() { return score; }
}
