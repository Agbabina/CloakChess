package io.github.chesslike.game;

/** Four rooms per floor. Room 20 is the Floor 5 boss encounter. */
public final class FloorProgression {
    public static final int ROOMS_PER_FLOOR = 4;
    public static final int BOSS_FLOOR = 5;

    private FloorProgression() {}

    public static int floorForRoom(int room) {
        return Math.max(1, (Math.max(1, room) - 1) / ROOMS_PER_FLOOR + 1);
    }

    public static int roomWithinFloor(int room) {
        return (Math.max(1, room) - 1) % ROOMS_PER_FLOOR + 1;
    }

    public static boolean isBossRoom(int room) {
        return floorForRoom(room) == BOSS_FLOOR
            && roomWithinFloor(room) == ROOMS_PER_FLOOR;
    }
}
