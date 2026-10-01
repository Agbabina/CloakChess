package io.github.chesslike.game;

public class QueenRules implements MovementRules {

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        boolean straight = startX == targetX || startY == targetY;
        boolean diagonal = dx == dy && dx != 0;

        return straight || diagonal;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
