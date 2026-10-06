package io.github.chesslike.game;

public class ChancellorRules implements MovementRules {

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        boolean knight = (dx == 2 && dy == 1) || (dx == 1 && dy == 2);
        boolean rook = (dx == 0) != (dy == 0);

        // Chancellor: rook + knight
        return knight || rook;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
