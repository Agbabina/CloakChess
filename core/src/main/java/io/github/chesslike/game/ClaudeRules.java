package io.github.chesslike.game;

// Claude: a knight jump followed by a free 1-tile step in any direction.
// Valid targets = every tile within 1 step of a knight landing tile.
public class ClaudeRules implements MovementRules {

    private static final int[][] KNIGHT = {
        {1, 2}, {2, 1}, {-1, 2}, {-2, 1},
        {1, -2}, {2, -1}, {-1, -2}, {-2, -1}
    };

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetY < 0 || targetX >= boardSize || targetY >= boardSize) return false;
        if (targetX == startX && targetY == startY) return false;

        int dx = targetX - startX;
        int dy = targetY - startY;

        for (int[] k : KNIGHT) {
            if (Math.abs(dx - k[0]) <= 1 && Math.abs(dy - k[1]) <= 1) return true;
        }
        return false;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, Player.BOARD_SIZE);
    }
}
