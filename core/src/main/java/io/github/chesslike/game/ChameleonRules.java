package io.github.chesslike.game;

public class ChameleonRules implements MovementRules {
    private Card.MovementType copiedType = Card.MovementType.PAWN;

    public void setCopiedType(Card.MovementType type) {
        this.copiedType = type;
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, int boardSize) {
        if (targetX < 0 || targetX >= boardSize || targetY < 0 || targetY >= boardSize) {
            return false;
        }
        int dx = Math.abs(targetX - startX);
        int dy = Math.abs(targetY - startY);

        if (dx == 0 && dy == 0) return false;

        switch (copiedType) {
            case ROOK:
                return (dx == 0 && dy > 0) || (dx > 0 && dy == 0);
            case BISHOP:
                return dx == dy;
            case KNIGHT:
                return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
            default:
                return dx <= 1 && dy <= 1;
        }
    }

    @Override
    public boolean isValidMove(int startX, int startY, int targetX, int targetY, Board board) {
        return isValidMove(startX, startY, targetX, targetY, board.getSize());
    }
}
