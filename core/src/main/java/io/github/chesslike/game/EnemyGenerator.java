package io.github.chesslike.game;

import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class EnemyGenerator {

    private final Random random = new Random();

    private static final int BOARD_SIZE = Board.SIZE;

    // How many enemies can spawn initially
    private static final int MIN_ENEMIES = 1;
    private static final int MAX_ENEMIES = 3;

    /**
     * Generates a random group of enemies.
     *
     * difficulty controls how nasty the room can become.
     */
    public Array<Enemy> generateEnemies(int difficulty, int playerX, int playerY) {
        return generateEnemies(difficulty, playerX, playerY, null);
    }

    /** Blitz-aware generation. Curses are assigned automatically using the active omen. */
    public Array<Enemy> generateEnemies(int difficulty, int playerX, int playerY, BlitzOmen omen) {

        Array<Enemy> enemies = new Array<>();

        // Guaranteed boss encounters: Floor 3, Floor 5, and Floor 8.
        if (omen == null && FloorProgression.isBossRoom(difficulty)) {
            int bx = BOARD_SIZE / 2;
            int by = BOARD_SIZE - 1;
            int bossNumber = FloorProgression.bossNumberForRoom(difficulty);
            String bossTexture;
            switch (bossNumber) {
                case 1:
                    bossTexture = "boss_hollow_king_p1_idle.png";
                    break;
                case 2:
                    bossTexture = "boss_hollow_king_p2_idle.png";
                    break;
                default:
                    bossTexture = "boss_hollow_king_p3_idle.png";
                    break;
            }
            enemies.add(new Enemy(
                bx, by, Card.MovementType.QUEEN, bossTexture,
                difficulty, 1f, EnemyVariant.BOSS
            ));
            return enemies;
        }

        int enemyCount = omen == null
            ? getEnemyCount(difficulty)
            : BlitzBoardRules.enemyCount(difficulty);

        int attempts = 0;

        while (enemies.size < enemyCount && attempts < 100) {

            attempts++;

            int x = random.nextInt(BOARD_SIZE);
            int y = random.nextInt(BOARD_SIZE);

            // Keep enemies away from the player's starting area
            if (isTooCloseToPlayer(x, y, playerX, playerY)) {
                continue;
            }

            // Don't stack enemies on the same tile
            if (isOccupied(enemies, x, y)) {
                continue;
            }

            Card.MovementType type = generateEnemyType(difficulty);

            EnemyVariant variant = rollVariant(difficulty);
            Enemy enemy = createEnemy(x, y, type, difficulty, variant);

            if (omen != null) {
                enemy.setCurse(BlitzEnemyCurses.roll(difficulty, omen));
            }

            enemies.add(enemy);
        }

        return enemies;
    }


    // =========================================
    // ENEMY COUNT
    // =========================================

    private int getEnemyCount(int difficulty) {

        int max = Math.min(MAX_ENEMIES + difficulty / 2, 6);

        return MIN_ENEMIES + random.nextInt(max - MIN_ENEMIES + 1);
    }

    // =========================================
    // ENEMY TYPE
    // =========================================

    private Card.MovementType generateEnemyType(int difficulty) {

        int roll = random.nextInt(100);

        // Early rooms are mostly simple pieces
        if (difficulty <= 1) {

            if (roll < 50) return Card.MovementType.PAWN;
            if (roll < 75) return Card.MovementType.KNIGHT;
            return Card.MovementType.BISHOP;
        }

        // Medium difficulty (rooms 2-4)
        if (difficulty <= 4) {

            if (roll < 20) return Card.MovementType.PAWN;
            if (roll < 40) return Card.MovementType.MADROOK;
            if (roll < 60) return Card.MovementType.BISHOP;
            if (roll < 80) return Card.MovementType.ROOK;
            return Card.MovementType.JESTER;
        }

        // High difficulty: the full enemy-piece roster begins appearing.
        if (difficulty <= 6) {
            if (roll < 12) return Card.MovementType.PAWN;
            if (roll < 24) return Card.MovementType.BLINKER;
            if (roll < 36) return Card.MovementType.MADROOK;
            if (roll < 48) return Card.MovementType.CHAMELEON;
            if (roll < 60) return Card.MovementType.JESTER;
            if (roll < 70) return Card.MovementType.LEAPER;
            if (roll < 80) return Card.MovementType.CLAUDE;
            if (roll < 90) return Card.MovementType.ARCHBISHOP;
            return Card.MovementType.COUNCILLOR;
        }

        if (roll < 10) return Card.MovementType.PAWN;
        if (roll < 20) return Card.MovementType.BLINKER;
        if (roll < 30) return Card.MovementType.MADROOK;
        if (roll < 40) return Card.MovementType.CHAMELEON;
        if (roll < 50) return Card.MovementType.JESTER;
        if (roll < 60) return Card.MovementType.LEAPER;
        if (roll < 70) return Card.MovementType.CLAUDE;
        if (roll < 80) return Card.MovementType.ARCHBISHOP;
        if (roll < 90) return Card.MovementType.COUNCILLOR;
        return Card.MovementType.DASH;
    }

    // =========================================
    // CREATE ENEMY
    // =========================================

    private Enemy createEnemy(int x, int y, Card.MovementType type, int room, EnemyVariant variant) {

        String texture = getTexture(type);

        // Rooms 6-10 = 2 HP, rooms 11+ roll 3 or 4.
        // The chance of 4 HP climbs 10% per room after room 10.
        float chanceOfFour = Math.max(0f, Math.min(1f, (room - 10) / 10f));

        return new Enemy(x, y, type, texture, room, chanceOfFour, variant);
    }


    private EnemyVariant rollVariant(int room) {
        int roll = random.nextInt(100);
        if (room >= 8 && roll < 12) return EnemyVariant.PLAGUEBEARER;
        if (room >= 6 && roll < 27) return EnemyVariant.BERSERKER;
        if (room >= 4 && roll < 42) return EnemyVariant.PHANTOM;
        if (room >= 3 && roll < 58) return EnemyVariant.ARMORED;
        return EnemyVariant.NORMAL;
    }

    // =========================================
    // TEXTURE
    // =========================================

    private String getTexture(Card.MovementType type) {

        switch (type) {

            case KNIGHT:
                return "b_knight_png_256px.png";

            case BISHOP:
                return "b_bishop_png_256px.png";

            case ROOK:
                return "b_rook_png_256px.png";

            case QUEEN:
                return "b_queen_png_256px.png";

            case PAWN:
                return "b_pawn_png_256px.png";

            case JESTER:
                return "jester.png";

            case BLINKER:
                return "blinker.png";

            case MADROOK:
                return "mad_rook.png";

            case CHAMELEON:
                return "chameleon.png";

            // Reuse existing art until dedicated enemy sprites are drawn.
            case DASH:
            case PAWN:
            case SHIFTER:
                return "b_pawn_png_256px.png";
            case LEAPER:
            case CLAUDE:
                return "b_knight_png_256px.png";
            case ARCHBISHOP:
                return "b_bishop_png_256px.png";
            case COUNCILLOR:
                return "b_queen_png_256px.png";

            default:
                return "b_pawn_png_256px.png";
        }
    }

    // =========================================
    // POSITION CHECK
    // =========================================

    private boolean isOccupied(Array<Enemy> enemies, int x, int y) {

        for (int i = 0; i < enemies.size; i++) {

            Enemy enemy = enemies.get(i);

            if (!enemy.isAlive()) {
                continue;
            }

            if (enemy.getX() == x && enemy.getY() == y) {
                return true;
            }
        }

        return false;
    }

    // =========================================
    // PLAYER DISTANCE
    // =========================================

    private boolean isTooCloseToPlayer(int x, int y, int playerX, int playerY) {

        int distance = Math.abs(x - playerX) + Math.abs(y - playerY);

        return distance <= 1;
    }
}
