package io.github.chesslike.game;

import java.util.Random;

public final class BlitzEnemyCurses {
    private static final Random RANDOM = new Random();

    private BlitzEnemyCurses() {}

    public static EnemyCurse roll(int room, BlitzOmen omen) {
        int chance = 12 + Math.min(38, room * 3);
        if (omen == BlitzOmen.DREAD || omen == BlitzOmen.WITHERING) chance += 15;
        if (RANDOM.nextInt(100) >= chance) return null;

        EnemyCurse[] pool = {
            EnemyCurse.ARMORED,
            EnemyCurse.WARDED,
            EnemyCurse.VENGEFUL,
            EnemyCurse.VOLATILE,
            EnemyCurse.ANCHORED,
            EnemyCurse.GREEDY,
            EnemyCurse.SAPPING,
            EnemyCurse.HASTY,
            EnemyCurse.THORNY,
            EnemyCurse.BRUTAL,
            EnemyCurse.CORROSIVE,
            EnemyCurse.CRIPPLING,
            EnemyCurse.RELENTLESS,
            EnemyCurse.HUNTER,
            EnemyCurse.JUGGERNAUT,
            EnemyCurse.MARTYR,
            EnemyCurse.MAIMING
        };

        return pool[RANDOM.nextInt(pool.length)];
    }
}
