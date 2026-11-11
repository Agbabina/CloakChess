package io.github.chesslike.game;

/**
 * Mad Rook follows a visible three-step movement cycle instead of rolling
 * random movement rules: horizontal, vertical, then diagonal.
 */
public class MadRookRules implements MovementRules {
    private int phase = 0;

    public void advanceTurn() {
        phase = (phase + 1) % 3;
    }

    public int getPhase() {
        return phase;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetX >= boardSize || targetY < 0 || targetY >= boardSize) return false;
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);
        if (dx == 0 && dy == 0) return false;

        switch (phase) {
            case 0: return dy == 0 && dx > 0;             // horizontal
            case 1: return dx == 0 && dy > 0;             // vertical
            default: return dx == dy;                    // diagonal
        }
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
