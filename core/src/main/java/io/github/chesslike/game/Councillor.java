package io.github.chesslike.game;

public class Councillor implements MovementRules {

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        // Zebra: 3 squares in one direction, 2 squares in the other
        return (dx == 3 && dy == 2) || (dx == 2 && dy == 3);
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
