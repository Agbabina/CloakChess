package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BlitzManager {
    private static final float STARTING_TIME = 180f, MAX_TIME = 300f;
    private static final float GAMBLE_COST = 10f, GAMBLE_DURATION = 8f, GAMBLE_COOLDOWN = 18f;

    private final BlitzScore score = new BlitzScore();
    private final Preferences save = Gdx.app.getPreferences("CloakChessBlitz");
    private final Random random = new Random();
    private final List<BlitzRelic> activeRelics = new ArrayList<>();
    private final List<BlitzRelic> shopOffers = new ArrayList<>();

    private float timeRemaining;
    private boolean active;
    private BlitzOmen currentOmen;
    private BlitzRelic lastUnlockedRelic;
    private float lastTimeDelta;
    private String lastTimeReason = "";

    private float gambleRemaining;
    private float gambleCooldown;

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

        if (gambleCooldown > 0f)
            gambleCooldown = Math.max(0f, gambleCooldown - delta);

        if (gambleRemaining > 0f) {
            gambleRemaining -= delta;
            if (gambleRemaining <= 0f)
                gambleRemaining = 0f;
        }

        if (timeRemaining <= 0f) {
            timeRemaining = 0f;
            gambleRemaining = 0f;
            endRun();
        }
    }

    public void endRun() {
        if (!active) return;
        active = false;
        gambleRemaining = 0f;
        unlockRelic();
        if (score.getScore() > getBestScore())
            save.putInteger("bestScore", score.getScore());
        save.flush();
    }

    public void reset() {
        score.reset();
        timeRemaining = STARTING_TIME;
        active = false;
        currentOmen = null;
        lastUnlockedRelic = null;
        lastTimeDelta = 0f;
        lastTimeReason = "";
        gambleRemaining = 0f;
        gambleCooldown = 0f;
        activeRelics.clear();
        shopOffers.clear();
    }

    private void loadActiveRelic() {
        activeRelics.clear();
        int unlocked = Math.max(1, Math.min(save.getInteger("unlockedRelics", 0), BlitzRelic.values().length));
        BlitzRelic[] all = BlitzRelic.values();
        activeRelics.add(all[random.nextInt(unlocked)]);
        score.setRelics(activeRelics);
    }

    private void unlockRelic() {
        BlitzRelic[] all = BlitzRelic.values();
        int unlocked = Math.min(save.getInteger("unlockedRelics", 0), all.length);
        if (unlocked < all.length) {
            lastUnlockedRelic = all[unlocked];
            save.putInteger("unlockedRelics", unlocked + 1);
        } else {
            lastUnlockedRelic = all[random.nextInt(all.length)];
        }
    }

    public void addTime(float seconds, String reason) {
        if (!active || seconds == 0f) return;
        float before = timeRemaining;
        timeRemaining = Math.max(0f, Math.min(MAX_TIME, before + seconds));
        lastTimeDelta += timeRemaining - before;
        lastTimeReason = reason == null ? "" : reason;
    }

    public float consumeLastTimeDelta() {
        float d = lastTimeDelta;
        lastTimeDelta = 0f;
        lastTimeReason = "";
        return d;
    }

    public String getLastTimeReason() { return lastTimeReason; }

    public boolean activateTimeGamble() {
        if (!active || gambleRemaining > 0f || gambleCooldown > 0f || timeRemaining <= GAMBLE_COST)
            return false;

        addTime(-GAMBLE_COST, "TIME GAMBLE");
        gambleRemaining = GAMBLE_DURATION;
        gambleCooldown = GAMBLE_DURATION + GAMBLE_COOLDOWN;
        return true;
    }

    public boolean isTimeGambleActive() { return gambleRemaining > 0f; }
    public float getGambleRemaining() { return gambleRemaining; }
    public float getGambleCooldown() { return gambleCooldown; }
    public float getGambleCost() { return GAMBLE_COST; }

    public BlitzScore.Result capture(boolean cursed, String movement, boolean assassination,
                                     int enemyHealthPercent, int gold, int room) {
        BlitzScore.Result result = score.addCapture(
            cursed, movement, assassination, enemyHealthPercent, gold, room, timeRemaining
        );

        String originalLabel = result.label;
        if (isTimeGambleActive()) {
            int bonus = result.points;
            score.addBonusScore(bonus);
            ArrayList<BlitzScore.Component> parts = new ArrayList<>(result.components);
            parts.add(new BlitzScore.Component("TIME GAMBLE x2", bonus));
            result = new BlitzScore.Result(
                result.points + bonus,
                result.basePoints + bonus,
                result.multiplier,
                "GAMBLE " + result.label,
                result.multiplierUp,
                parts
            );
        }

        addTime(1f, "CAPTURE");
        if (activeRelics.contains(BlitzRelic.BLOOD_CLOCK)) addTime(1f, "BLOOD CLOCK");
        if (activeRelics.contains(BlitzRelic.TIME_SHARD) &&
            (originalLabel.contains("ASSASSINATION") || originalLabel.contains("FIRST BLOOD") || originalLabel.contains("CURSED")))
            addTime(2f, "TIME SHARD");
        if (activeRelics.contains(BlitzRelic.CHRONO_COG) && score.getTotalCaptures() % 5 == 0)
            addTime(3f, "CHRONO COG");
        if (activeRelics.contains(BlitzRelic.OVERTIME_GEM) && timeRemaining <= 20f)
            addTime(3f, "OVERTIME GEM");
        if (activeRelics.contains(BlitzRelic.WAR_DRUM) && cursed)
            addTime(2f, "WAR DRUM");
        if (activeRelics.contains(BlitzRelic.VOID_MIRROR) && assassination)
            addTime(1f, "VOID MIRROR");
        if (activeRelics.contains(BlitzRelic.LAST_STAND) && timeRemaining <= 15f)
            addTime(1f, "LAST STAND");
        if (activeRelics.contains(BlitzRelic.EXECUTION_CLOCK) && score.getTotalCaptures() % 7 == 0)
            addTime(5f, "EXECUTION CLOCK");
        if (activeRelics.contains(BlitzRelic.TIME_BLOOM) && score.getRoomCaptures() == 1)
            addTime(4f, "TIME BLOOM");
        if (activeRelics.contains(BlitzRelic.FRENZY_REACTOR) && score.getStreak() >= 5)
            addTime(1f, "FRENZY REACTOR");

        return result;
    }

    public int capture() { return score.addCapture(); }
    public int specialCapture(int bonus) { return score.addSpecialCapture(bonus); }

    public int roomClear() {
        int p = score.addRoomClear();
        addTime(5f, "ROOM CLEAR");
        if (activeRelics.contains(BlitzRelic.SUN_DIAL)) addTime(6f, "SUN DIAL");
        if (activeRelics.contains(BlitzRelic.HASTE_CORE)) addTime(3f, "HASTE CORE");
        return p;
    }

    public int spellCast() {
        int p = score.addSpell();
        if (activeRelics.contains(BlitzRelic.SPELL_ENGINE)) addTime(2f, "SPELL ENGINE");
        return p;
    }

    public int cardPlayed() {
        int p = score.addCardPlay();
        if (activeRelics.contains(BlitzRelic.CARD_CLOCK) &&
            score.getCardsPlayed() > 0 && score.getCardsPlayed() % 5 == 0)
            addTime(2f, "CARD CLOCK");
        return p;
    }

    public void breakStreak() {
        score.breakStreak();
        if (gambleRemaining > 0f) {
            gambleRemaining = 0f;
            lastTimeReason = "GAMBLE BROKEN";
        }
    }

    public void resetRoom() { score.resetRoom(); }

    public void refreshRelicShop() {
        shopOffers.clear();
        ArrayList<BlitzRelic> pool = new ArrayList<>();
        Collections.addAll(pool, BlitzRelic.values());
        Collections.shuffle(pool, random);
        for (BlitzRelic r : pool) {
            if (!activeRelics.contains(r)) {
                shopOffers.add(r);
                if (shopOffers.size() >= 4) break;
            }
        }
    }

    public List<BlitzRelic> getRelicShopOffers() { return shopOffers; }

    public boolean buyRelic(BlitzRelic relic) {
        if (!active || relic == null || activeRelics.contains(relic)) return false;

        activeRelics.add(relic);
        score.setRelics(activeRelics);
        addTime(4f, "RELIC ACTIVATED");

        if (relic == BlitzRelic.RELIC_ENGINE) {
            score.addBonusScore(1000);
            addTime(5f, "RELIC ENGINE");
        }
        return true;
    }

    public boolean hasRelic(BlitzRelic relic) { return activeRelics.contains(relic); }

    public int getRelicCost(BlitzRelic relic) { return 40 + relic.ordinal() * 6; }

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
