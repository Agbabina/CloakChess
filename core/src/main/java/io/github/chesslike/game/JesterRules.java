package io.github.chesslike.game;

public class JesterRules implements MovementRules {
    private int turnCounter = 1;

    public void advanceTurn() {
        turnCounter = (turnCounter % 4) + 1;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetX >= boardSize || targetY < 0 || targetY >= boardSize) {
            return false;
        }
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        if (dx == 0 && dy == 0) return false;

        switch (turnCounter) {
            case 1: // Turn 1: Diagonal
                return dx == dy;
            case 2: // Turn 2: Knight-like
                return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
            case 3: // Turn 3: Straight (orthogonal)
                return (dx == 0 && dy > 0) || (dx > 0 && dy == 0);
            case 4: // Turn 4: Any adjacent tile (1 step)
            default:
                return dx <= 1 && dy <= 1;
        }
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }

    public int getTurnCounter() {
        return turnCounter;
    }
}
