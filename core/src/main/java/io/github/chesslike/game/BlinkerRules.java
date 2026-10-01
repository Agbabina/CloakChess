package io.github.chesslike.game;

import java.util.Random;

public class BlinkerRules implements MovementRules {
    private final Random random = new Random();

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetX >= boardSize || targetY < 0 || targetY >= boardSize) {
            return false;
        }
        if (startX == targetX && startY == targetY) return false;

        // 40% chance to allow teleportation
        return random.nextInt(100) < 40;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
