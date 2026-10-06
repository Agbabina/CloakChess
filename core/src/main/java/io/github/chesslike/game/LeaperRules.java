package io.github.chesslike.game;

public class LeaperRules implements MovementRules {

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetY < 0 || targetX >= boardSize || targetY >= boardSize) return false;

        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        // A leaper jumps exactly two tiles in one direction.
        return (dx == 2 && dy == 0) || (dx == 0 && dy == 2)
            || (dx == 2 && dy == 2);
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, Player.BOARD_SIZE);
    }
}
