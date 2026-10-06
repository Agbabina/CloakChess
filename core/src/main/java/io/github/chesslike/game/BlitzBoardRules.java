package io.github.chesslike.game;

public final class BlitzBoardRules {
    public static final int BOARD_SIZE = Board.SIZE;
    public static final float TILE_SIZE = Board.TILE_SIZE;

    private BlitzBoardRules() {}

    public static int enemyCount(int room) {
        return Math.min(2 + (room / 3), 7);
    }

    public static int curseCount(int room, BlitzOmen omen) {
        int base = room < 3 ? 0 : 1 + room / 4;
        if (omen == BlitzOmen.WITHERING) base++;
        if (omen == BlitzOmen.DREAD) base++;
        return Math.min(base, 5);
    }

    public static int roomScore(int room) {
        return 500 + room * 100;
    }

    public static float timeBonus(int room) {
        return Math.min(30f, 5f + room * 1.5f);
    }
}
