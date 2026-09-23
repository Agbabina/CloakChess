package io.github.chesslike.game;

public class BishopRules implements MovementRules {

    @Override
    public boolean isValidMove(
        int startX,
        int startY,
        int targetX,
        int targetY,
        int boardSize
    ) {

        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        // Bishop moves diagonally.
        return dx == dy && dx != 0;
    }
}
