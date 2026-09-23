package io.github.chesslike.game;

public interface MovementRules {
    boolean isValidMove(
        int startX,
        int startY,
        int targetX,
        int targetY,
        int boardSize
    );
}
