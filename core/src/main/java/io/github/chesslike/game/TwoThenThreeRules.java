package io.github.chesslike.game;

public class TwoThenThreeRules implements MovementRules {

    private static final int[][] DIRECTIONS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetY < 0 || targetX >= boardSize || targetY >= boardSize) return false;

        // First jump exactly 2 tiles, then exactly 3 tiles from that landing square.
        for (int[] first : DIRECTIONS) {
            int midX = startX + first[0] * 2;
            int midY = startY + first[1] * 2;

            if (midX < 0 || midY < 0 || midX >= boardSize || midY >= boardSize) continue;

            for (int[] second : DIRECTIONS) {
                int endX = midX + second[0] * 3;
                int endY = midY + second[1] * 3;

                if (endX == targetX && endY == targetY) return true;
            }
        }

        return false;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, Player.BOARD_SIZE);
    }
}
