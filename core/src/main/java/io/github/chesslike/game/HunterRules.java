package io.github.chesslike.game;

/**
 * Hunter movement: a short-range predator that can step one tile in any
 * direction or make a knight jump. It pressures the King without having
 * the unrestricted reach of a queen.
 */
public class HunterRules implements MovementRules {
    @Override
    public boolean isValidMove(int sx, int sy, int tx, int ty, int boardSize) {
        int dx = Math.abs(tx - sx);
        int dy = Math.abs(ty - sy);
        if (dx == 0 && dy == 0) return false;
        boolean kingStep = dx <= 1 && dy <= 1;
        boolean knightJump = (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        return kingStep || knightJump;
    }

    @Override
    public boolean isValidMove(int sx, int sy, int tx, int ty, Board board) {
        return isValidMove(sx, sy, tx, ty, board.getSize());
    }
}
