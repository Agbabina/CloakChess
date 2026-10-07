package io.github.chesslike.game;

/**
 * The Corruption system lets a run gradually break the rules of chess.
 *
 * Corruption is intentionally deterministic: hitting a threshold unlocks
 * a specific rule break instead of randomly making the game unreadable.
 *
 * 0   = normal chess
 * 20  = Pawn can move backward
 * 35  = Knight gets a second jump
 * 50  = Rook can bend once
 * 65  = Bishop can bend once
 * 80  = Dash can pass through enemies
 * 90  = Queen can chain a second capture
 * 100 = THE BOARD BREAKS
 */
public class Corruption {

    public static final int MAX = 100;

    public enum Effect {
        NONE,
        BACKWARD_PAWN,
        DOUBLE_KNIGHT,
        BENDING_ROOK,
        BENDING_BISHOP,
        PHASE_DASH,
        DOUBLE_QUEEN_CAPTURE,
        BOARD_BREAKS
    }

    private int level;

    public Corruption() {
        this(0);
    }

    public Corruption(int level) {
        this.level = clamp(level);
    }

    public int getLevel() {
        return level;
    }

    public float getProgress() {
        return level / (float) MAX;
    }

    public boolean isCorrupted() {
        return level > 0;
    }

    public boolean isFullyCorrupted() {
        return level >= MAX;
    }

    public void add(int amount) {
        level = clamp(level + Math.max(0, amount));
    }

    public void reduce(int amount) {
        level = clamp(level - Math.max(0, amount));
    }

    public void set(int amount) {
        level = clamp(amount);
    }

    public void reset() {
        level = 0;
    }

    public boolean has(int threshold) {
        return level >= threshold;
    }

    public boolean allowsBackwardPawn() {
        return level >= 20;
    }

    public boolean allowsDoubleKnightJump() {
        return level >= 35;
    }

    public boolean allowsBendingRook() {
        return level >= 50;
    }

    public boolean allowsBendingBishop() {
        return level >= 65;
    }

    public boolean allowsDashThroughEnemies() {
        return level >= 80;
    }

    public boolean allowsDoubleQueenCapture() {
        return level >= 90;
    }

    public boolean boardIsBroken() {
        return level >= MAX;
    }

    /**
     * Returns the strongest rule-breaking effect currently unlocked.
     */
    public Effect getCurrentEffect() {
        if (level >= 100) return Effect.BOARD_BREAKS;
        if (level >= 90) return Effect.DOUBLE_QUEEN_CAPTURE;
        if (level >= 80) return Effect.PHASE_DASH;
        if (level >= 65) return Effect.BENDING_BISHOP;
        if (level >= 50) return Effect.BENDING_ROOK;
        if (level >= 35) return Effect.DOUBLE_KNIGHT;
        if (level >= 20) return Effect.BACKWARD_PAWN;
        return Effect.NONE;
    }

    /**
     * Human-readable name for the HUD/tutorial.
     */
    public String getStatusText() {
        if (level >= 100) return "THE BOARD BREAKS";
        if (level >= 90) return "QUEENS CAPTURE TWICE";
        if (level >= 80) return "DASH PHASES THROUGH ENEMIES";
        if (level >= 65) return "BISHOPS CAN BEND";
        if (level >= 50) return "ROOKS CAN BEND";
        if (level >= 35) return "KNIGHTS JUMP TWICE";
        if (level >= 20) return "PAWNS MOVE BACKWARD";
        return "THE BOARD IS STABLE";
    }

    /**
     * Returns the next threshold the player can reach.
     */
    public int getNextThreshold() {
        if (level < 20) return 20;
        if (level < 35) return 35;
        if (level < 50) return 50;
        if (level < 65) return 65;
        if (level < 80) return 80;
        if (level < 90) return 90;
        return 100;
    }

    public int getUntilNextThreshold() {
        return Math.max(0, getNextThreshold() - level);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(MAX, value));
    }
}
