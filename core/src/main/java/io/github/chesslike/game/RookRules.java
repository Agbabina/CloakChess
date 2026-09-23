package io.github.chesslike.game;

public class RookRules implements MovementRules {

    @Override
    public boolean isValidMove(
        int startX,
        int startY,
        int targetX,
        int targetY,
        int boardSize
    ) {

        // Same column
        boolean sameColumn = startX == targetX;

        // Same row
        boolean sameRow = startY == targetY;

        // Must actually move
        boolean actuallyMoved =
            startX != targetX || startY != targetY;

        return (sameColumn || sameRow) && actuallyMoved;
    }
}
