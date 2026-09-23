package io.github.chesslike.game;

public class KnightRules implements MovementRules {

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

        // Knight moves:
        // 2 squares in one direction
        // 1 square in the other

        return (dx == 2 && dy == 1)
            || (dx == 1 && dy == 2);
    }
}
