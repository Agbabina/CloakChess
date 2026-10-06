package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlitzManager {
    private static final float STARTING_TIME = 180f;
    private final BlitzScore score = new BlitzScore();
    private final Preferences save = Gdx.app.getPreferences("CloakChessBlitz");
    private final Random random = new Random();
    private final List<BlitzRelic> activeRelics = new ArrayList<>();

    private float timeRemaining;
    private boolean active;
    private BlitzOmen currentOmen;
    private BlitzRelic lastUnlockedRelic;

    public BlitzManager() { reset(); }

    public void start() {
        reset();
        loadActiveRelic();
        score.setRelics(activeRelics);
        BlitzOmen[] omens = BlitzOmen.values();
        currentOmen = omens[random.nextInt(omens.length)];
        active = true;
    }

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
        unlockRelic();
        if (score.getScore() > getBestScore()) {
            save.putInteger("bestScore", score.getScore());
        }
        save.flush();
    }

    public void reset() {
        score.reset();
        timeRemaining = STARTING_TIME;
        active = false;
        currentOmen = null;
        lastUnlockedRelic = null;
        activeRelics.clear();
    }

    private void loadActiveRelic() {
        activeRelics.clear();
        int unlocked = save.getInteger("unlockedRelics", 0);
        if (unlocked <= 0) return;

        ArrayList<BlitzRelic> pool = new ArrayList<>();
        BlitzRelic[] all = BlitzRelic.values();
        for (int i = 0; i < Math.min(unlocked, all.length); i++) pool.add(all[i]);
        if (!pool.isEmpty()) activeRelics.add(pool.get(random.nextInt(pool.size())));
    }

    private void unlockRelic() {
        BlitzRelic[] all = BlitzRelic.values();
        int unlocked = Math.min(save.getInteger("unlockedRelics", 0), all.length);
        if (unlocked >= all.length) {
            lastUnlockedRelic = all[random.nextInt(all.length)];
            return;
        }

        lastUnlockedRelic = all[unlocked];
        save.putInteger("unlockedRelics", unlocked + 1);
    }

    public BlitzScore.Result capture(boolean cursed, String movement, boolean assassination, int enemyHealthPercent, int gold, int room) {
        BlitzScore.Result result = score.addCapture(cursed, movement, assassination, enemyHealthPercent, gold, room);

        if (activeRelics.contains(BlitzRelic.BLOOD_CLOCK)) timeRemaining += 1f;
        if (activeRelics.contains(BlitzRelic.TIME_SHARD) &&
            (result.label.contains("ASSASSINATION") || result.label.contains("FIRST BLOOD") || result.label.contains("CURSED"))) {
            timeRemaining += 2f;
        }
        return result;
    }

    public int capture() { return score.addCapture(); }
    public int specialCapture(int bonus) { return score.addSpecialCapture(bonus); }
    public int roomClear() { return score.addRoomClear(); }
    public int spellCast() { return score.addSpell(); }
    public int cardPlayed() { return score.addCardPlay(); }
    public void breakStreak() { score.breakStreak(); }
    public void resetRoom() { score.resetRoom(); }

    public int getScore() { return score.getScore(); }
    public int getStreak() { return score.getStreak(); }
    public int getMultiplier() { return score.getMultiplier(); }
    public int getBestStreak() { return score.getBestStreak(); }
    public int getTotalCaptures() { return score.getTotalCaptures(); }
    public float getTimeRemaining() { return timeRemaining; }
    public boolean isActive() { return active; }
    public int getBestScore() { return save.getInteger("bestScore", 0); }
    public BlitzScore getScoreSystem() { return score; }
    public BlitzOmen getCurrentOmen() { return currentOmen; }
    public List<BlitzRelic> getActiveRelics() { return activeRelics; }
    public BlitzRelic getLastUnlockedRelic() { return lastUnlockedRelic; }
    public int getUnlockedRelicCount() { return save.getInteger("unlockedRelics", 0); }
}
