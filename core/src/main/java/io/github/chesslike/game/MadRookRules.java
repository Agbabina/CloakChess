package io.github.chesslike.game;

import java.util.Random;

public class MadRookRules implements MovementRules {
    private final Random random = new Random();

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetX >= boardSize || targetY < 0 || targetY >= boardSize) {
            return false;
        }
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        if (dx == 0 && dy == 0) return false;

        boolean isHorizontal = (dy == 0 && dx > 0);
        boolean isVertical   = (dx == 0 && dy > 0);
        boolean isDiagonal   = (dx == dy);

        int roll = random.nextInt(100);

        if (isHorizontal && roll < 40) return true;
        if (isVertical && roll >= 40 && roll < 60) return true;
        if (isDiagonal && roll >= 60 && roll < 70) return true;

        return false;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
