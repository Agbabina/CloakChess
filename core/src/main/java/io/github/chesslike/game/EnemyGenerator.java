package io.github.chesslike.game;

import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class EnemyGenerator {

    private final Random random = new Random();

    private static final int BOARD_SIZE = 5;

    // How many enemies can spawn initially
    private static final int MIN_ENEMIES = 1;
    private static final int MAX_ENEMIES = 3;

    /**
     * Generates a random group of enemies.
     *
     * difficulty controls how nasty the room can become.
     */
    public Array<Enemy> generateEnemies(int difficulty) {

        Array<Enemy> enemies = new Array<>();

        int enemyCount = getEnemyCount(difficulty);

        int attempts = 0;

        while (
            enemies.size < enemyCount
                && attempts < 100
        ) {

            attempts++;

            int x = random.nextInt(BOARD_SIZE);
            int y = random.nextInt(BOARD_SIZE);

            // Keep enemies away from the player's starting area
            if (isTooCloseToPlayer(x, y)) {
                continue;
            }

            // Don't stack enemies on the same tile
            if (isOccupied(enemies, x, y)) {
                continue;
            }

            Card.MovementType type =
                generateEnemyType(difficulty);

            Enemy enemy =
                createEnemy(
                    x,
                    y,
                    type
                );

            enemies.add(enemy);
        }

        return enemies;
    }

    // =========================================
    // ENEMY COUNT
    // =========================================

    private int getEnemyCount(
        int difficulty
    ) {

        int max =
            Math.min(
                MAX_ENEMIES + difficulty / 2,
                6
            );

        return MIN_ENEMIES
            + random.nextInt(
            max - MIN_ENEMIES + 1
        );
    }

    // =========================================
    // ENEMY TYPE
    // =========================================

    private Card.MovementType generateEnemyType(
        int difficulty
    ) {

        int roll = random.nextInt(100);

        // Early rooms are mostly simple pieces

        if (difficulty <= 1) {

            if (roll < 50) {
                return Card.MovementType.PAWN;
            }

            if (roll < 75) {
                return Card.MovementType.KNIGHT;
            }

            return Card.MovementType.BISHOP;
        }

        // Medium difficulty

        if (difficulty <= 3) {

            if (roll < 30) {
                return Card.MovementType.PAWN;
            }

            if (roll < 55) {
                return Card.MovementType.KNIGHT;
            }

            if (roll < 75) {
                return Card.MovementType.BISHOP;
            }

            if (roll < 90) {
                return Card.MovementType.ROOK;
            }

            return Card.MovementType.QUEEN;
        }

        // Hard rooms

        if (roll < 20) {
            return Card.MovementType.PAWN;
        }

        if (roll < 40) {
            return Card.MovementType.KNIGHT;
        }

        if (roll < 60) {
            return Card.MovementType.BISHOP;
        }

        if (roll < 80) {
            return Card.MovementType.ROOK;
        }

        return Card.MovementType.QUEEN;
    }

    // =========================================
    // CREATE ENEMY
    // =========================================

    private Enemy createEnemy(
        int x,
        int y,
        Card.MovementType type
    ) {

        String texture =
            getTexture(type);

        return new Enemy(
            x,
            y,
            type,
            texture
        );
    }

    // =========================================
    // TEXTURE
    // =========================================

    private String getTexture(
        Card.MovementType type
    ) {

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

            default:
                return "b_pawn_png_256px.png";
        }
    }

    // =========================================
    // POSITION CHECK
    // =========================================

    private boolean isOccupied(
        Array<Enemy> enemies,
        int x,
        int y
    ) {

        for (Enemy enemy : enemies) {

            if (!enemy.isAlive()) {
                continue;
            }

            if (
                enemy.getX() == x
                    && enemy.getY() == y
            ) {
                return true;
            }
        }

        return false;
    }

    // =========================================
    // PLAYER DISTANCE
    // =========================================

    private boolean isTooCloseToPlayer(
        int x,
        int y
    ) {

        // Player starts around the center.
        int playerX = 2;
        int playerY = 2;

        int distance =
            Math.abs(x - playerX)
                + Math.abs(y - playerY);

        return distance <= 1;
    }
}
