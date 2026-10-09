package io.github.chesslike.game;

/** Four rooms per floor. Bosses appear on floors 3, 5, and 8. */
public final class FloorProgression {
    public static final int ROOMS_PER_FLOOR = 4;
    public static final int FIRST_BOSS_FLOOR = 3;
    public static final int SECOND_BOSS_FLOOR = 5;
    public static final int FINAL_BOSS_FLOOR = 8;

    private FloorProgression() {}

    public static int floorForRoom(int room) {
        return Math.max(1, (Math.max(1, room) - 1) / ROOMS_PER_FLOOR + 1);
    }

    public static int roomWithinFloor(int room) {
        return (Math.max(1, room) - 1) % ROOMS_PER_FLOOR + 1;
    }

    public static int bossNumberForRoom(int room) {
        if (!isBossRoom(room)) return 0;
        int floor = floorForRoom(room);
        if (floor == FIRST_BOSS_FLOOR) return 1;
        if (floor == SECOND_BOSS_FLOOR) return 2;
        if (floor == FINAL_BOSS_FLOOR) return 3;
        return 0;
    }

    public static boolean isBossRoom(int room) {
        if (roomWithinFloor(room) != ROOMS_PER_FLOOR) return false;
        int floor = floorForRoom(room);
        return floor == FIRST_BOSS_FLOOR
            || floor == SECOND_BOSS_FLOOR
            || floor == FINAL_BOSS_FLOOR;
    }

    public static boolean isFinalBossRoom(int room) {
        return bossNumberForRoom(room) == 3;
    }
}
