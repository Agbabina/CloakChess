package io.github.chesslike.game;

public class DashRules implements MovementRules{
    @Override
    public boolean isValidMove(int startX,int startY, int targetX, int targetY, int boardSize){
        boolean sameRow=startY==targetY;
        boolean actuallyMoved = startX != targetX || startY != targetY;
        return (sameRow) && actuallyMoved;
    }
    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }

}
