package io.github.chesslike.game;

public interface MovementRules {

    /**
     * Check valid move using board size dimension.
     */
    boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize);

    /**
     * Check valid move using Board instance.
     */
    boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board);
}
