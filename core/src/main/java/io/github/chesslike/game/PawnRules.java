package io.github.chesslike.game;

public class PawnRules implements MovementRules {

    @Override
    public boolean isValidMove(
        int startX,
        int startY,
        int targetX,
        int targetY,
        int boardSize
    ) {

        int dx = targetX - startX;
        int dy = targetY - startY;

        // Pawn moves one square forward.
        return dx == 0 && dy == 1;
    }
}
