package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Player {

    private int x;
    private int y;

    private final Texture texture;

    public static final int BOARD_SIZE = 6;
    public static final float TILE_SIZE = 100f;


    private boolean selected = false;

    private Corruption corruption;

    // =========================================
    // HEALTH
    // =========================================

    private int maxHealth = 5;
    private int health = 5;

    // =========================================
    // ARROWS
    // =========================================

    private static final int DEFAULT_ARROWS = 2;
    private int arrowsRemaining = DEFAULT_ARROWS;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;

        texture = new Texture("w_king_png_256px.png");
    }

    // =========================================
    // HEALTH
    // =========================================

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = Math.max(1, maxHealth);
        health = Math.min(health, this.maxHealth);
    }

    public void increaseMaxHealth(int amount) {
        maxHealth = Math.max(1, maxHealth + amount);
        health = Math.min(health, maxHealth);
    }

    public void heal(int amount) {
        if (amount <= 0) {
            return;
        }

        health = Math.min(maxHealth, health + amount);
    }

    public boolean takeDamage(int amount) {
        if (amount <= 0) {
            return false;
        }

        health = Math.max(0, health - amount);

        return health <= 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public void resetHealth() {
        health = maxHealth;
    }

    // =========================================
    // BOARD POSITION
    // =========================================

    public static float getBoardX() {
        return (Gdx.graphics.getWidth() - BOARD_SIZE * TILE_SIZE) / 2f;
    }

    public static float getBoardY() {
        return (Gdx.graphics.getHeight() - BOARD_SIZE * TILE_SIZE) / 2f;
    }

    // =========================================
    // RENDER
    // =========================================

    public void render(SpriteBatch batch) {
        float boardX = getBoardX();
        float boardY = getBoardY();

        float pieceSize = TILE_SIZE * 0.90f;
        float offset = (TILE_SIZE - pieceSize) / 2f;

        batch.draw(
            texture,
            boardX + x * TILE_SIZE + offset,
            boardY + y * TILE_SIZE + offset,
            pieceSize,
            pieceSize
        );
    }

    // =========================================
    // CLICK DETECTION
    // =========================================

    public boolean isClicked(float screenX, float screenY) {
        float boardX = getBoardX();
        float boardY = getBoardY();

        float playerX = boardX + x * TILE_SIZE;
        float playerY = boardY + y * TILE_SIZE;

        return screenX >= playerX
            && screenX <= playerX + TILE_SIZE
            && screenY >= playerY
            && screenY <= playerY + TILE_SIZE;
    }

    // =========================================
    // SELECTION
    // =========================================

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    // =========================================
    // POSITION MANAGEMENT
    // =========================================

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // =========================================
    // MOVEMENT
    // =========================================

    public boolean isValidMove(
        int targetX,
        int targetY,
        MovementRules rules
    ) {
        if (!isInsideBoard(targetX, targetY)) {
            return false;
        }

        return rules.isValidMove(
            x,
            y,
            targetX,
            targetY,
            BOARD_SIZE
        );
    }

    public boolean isValidMove(
        int targetX,
        int targetY,
        MovementRules rules,
        Board board
    ) {
        if (!isInsideBoard(targetX, targetY)) {
            return false;
        }

        if (corruption != null) {
            int dx = targetX - x;
            int dy = targetY - y;
            int ax = Math.abs(dx);
            int ay = Math.abs(dy);

            // Corruption bends the rules without replacing normal movement.
            if (rules instanceof PawnRules && corruption.allowsBackwardPawn()) {
                if (dx == 0 && Math.abs(dy) == 1) return true;
            }

            if (rules instanceof KnightRules && corruption.allowsDoubleKnightJump()) {
                if ((ax == 4 && ay == 2) || (ax == 2 && ay == 4)) return true;
            }

            // A bent rook can turn once: horizontal + vertical = any tile.
            if (rules instanceof RookRules && corruption.allowsBendingRook()) {
                if (dx != 0 || dy != 0) return true;
            }

            // A bent bishop can change diagonal direction once.
            // Two diagonal segments preserve square colour.
            if (rules instanceof BishopRules && corruption.allowsBendingBishop()) {
                if ((ax + ay) % 2 == 0 && (ax != 0 || ay != 0)) return true;
            }

            // At full corruption, the board has stopped respecting piece movement.
            if (corruption.boardIsBroken()) {
                if (dx != 0 || dy != 0) return true;
            }
        }

        return rules.isValidMove(
            x,
            y,
            targetX,
            targetY,
            board.getSize()
        );
    }

    public void setCorruption(Corruption corruption) {
        this.corruption = corruption;
    }

    public Corruption getCorruption() {
        return corruption;
    }

    public boolean moveTo(
        int targetX,
        int targetY,
        MovementRules rules,
        Board board
    ) {
        if (!isValidMove(targetX, targetY, rules, board)) {
            return false;
        }

        x = targetX;
        y = targetY;

        return true;
    }

    // =========================================
    // LEGAL MOVES RENDERING
    // =========================================

    public void renderLegalMoves(
        ShapeRenderer shape,
        Card activeCard,
        Board board
    ) {
        if (!selected || activeCard == null) {
            return;
        }

        if (activeCard.hasTeleportationInfusion()) {
            renderTeleportationMoves(shape);
        } else {
            renderLegalMoves(
                shape,
                activeCard.getMovementRules(),
                board
            );
        }
    }

    public void renderLegalMoves(
        ShapeRenderer shape,
        MovementRules rules,
        Board board
    ) {
        if (!selected) {
            return;
        }

        float boardX = getBoardX();
        float boardY = getBoardY();

        shape.setColor(
            new Color(
                0.20f,
                0.70f,
                1f,
                0.45f
            )
        );

        for (int targetX = 0; targetX < BOARD_SIZE; targetX++) {
            for (int targetY = 0; targetY < BOARD_SIZE; targetY++) {

                if (isValidMove(
                    targetX,
                    targetY,
                    rules,
                    board
                )) {
                    shape.rect(
                        boardX + targetX * TILE_SIZE + 15,
                        boardY + targetY * TILE_SIZE + 15,
                        TILE_SIZE - 30,
                        TILE_SIZE - 30
                    );
                }
            }
        }

        shape.setColor(
            new Color(
                1f,
                0.75f,
                0.15f,
                1f
            )
        );

        shape.rect(
            boardX + x * TILE_SIZE + 4,
            boardY + y * TILE_SIZE + 4,
            TILE_SIZE - 8,
            TILE_SIZE - 8
        );
    }

    public void renderTeleportationMoves(
        ShapeRenderer shape
    ) {
        if (!selected) {
            return;
        }

        float boardX = getBoardX();
        float boardY = getBoardY();

        shape.setColor(
            new Color(
                0.70f,
                0.20f,
                1f,
                0.45f
            )
        );

        for (int targetX = 0; targetX < BOARD_SIZE; targetX++) {
            for (int targetY = 0; targetY < BOARD_SIZE; targetY++) {

                if (targetX != x || targetY != y) {
                    shape.rect(
                        boardX + targetX * TILE_SIZE + 15,
                        boardY + targetY * TILE_SIZE + 15,
                        TILE_SIZE - 30,
                        TILE_SIZE - 30
                    );
                }
            }
        }

        shape.setColor(
            new Color(
                1f,
                0.75f,
                0.15f,
                1f
            )
        );

        shape.rect(
            boardX + x * TILE_SIZE + 4,
            boardY + y * TILE_SIZE + 4,
            TILE_SIZE - 8,
            TILE_SIZE - 8
        );
    }

    // =========================================
    // ARROW SHOOTING
    // =========================================

    public boolean hasArrows() {
        return arrowsRemaining > 0;
    }

    public int getArrowsRemaining() {
        return arrowsRemaining;
    }

    public void useArrow() {
        if (arrowsRemaining > 0) {
            arrowsRemaining--;
        }
    }

    public void resetArrows() {
        arrowsRemaining = DEFAULT_ARROWS;
    }

    public boolean isValidArrowTarget(
        int targetX,
        int targetY
    ) {
        if (!isInsideBoard(targetX, targetY)) {
            return false;
        }

        if (targetX == x && targetY == y) {
            return false;
        }

        boolean straight =
            targetX == x || targetY == y;

        boolean diagonal =
            Math.abs(targetX - x)
                == Math.abs(targetY - y);

        return straight || diagonal;
    }

    public void renderArrowRange(
        ShapeRenderer shape
    ) {
        if (!selected) {
            return;
        }

        float boardX = getBoardX();
        float boardY = getBoardY();

        shape.setColor(
            new Color(
                1f,
                0.35f,
                0.25f,
                0.45f
            )
        );

        int[][] directions = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1},
            {1, 1},
            {1, -1},
            {-1, 1},
            {-1, -1}
        };

        for (int[] dir : directions) {

            int tx = x + dir[0];
            int ty = y + dir[1];

            while (isInsideBoard(tx, ty)) {

                shape.rect(
                    boardX + tx * TILE_SIZE + 15,
                    boardY + ty * TILE_SIZE + 15,
                    TILE_SIZE - 30,
                    TILE_SIZE - 30
                );

                tx += dir[0];
                ty += dir[1];
            }
        }
    }

    // =========================================
    // BOARD BOUNDS
    // =========================================

    private boolean isInsideBoard(
        int targetX,
        int targetY
    ) {
        return targetX >= 0
            && targetX < BOARD_SIZE
            && targetY >= 0
            && targetY < BOARD_SIZE;
    }

    // =========================================
    // CLEANUP
    // =========================================

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
