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

    public static final int BOARD_SIZE = 5;
    public static final float TILE_SIZE = 100f;

    private boolean selected = false;

    public Player(int x, int y) {

        this.x = x;
        this.y = y;

        texture =
            new Texture("w_king_png_256px.png");
    }

    // =========================================
    // BOARD POSITION
    // =========================================

    public static float getBoardX() {

        return (
            Gdx.graphics.getWidth()
                - BOARD_SIZE * TILE_SIZE
        ) / 2f;
    }

    public static float getBoardY() {

        return (
            Gdx.graphics.getHeight()
                - BOARD_SIZE * TILE_SIZE
        ) / 2f;
    }

    // =========================================
    // RENDER
    // =========================================

    public void render(SpriteBatch batch) {

        float boardX = getBoardX();
        float boardY = getBoardY();

        float pieceSize = TILE_SIZE * 0.90f;

        float offset =
            (TILE_SIZE - pieceSize) / 2f;

        batch.draw(
            texture,

            boardX
                + x * TILE_SIZE
                + offset,

            boardY
                + y * TILE_SIZE
                + offset,

            pieceSize,
            pieceSize
        );
    }

    // =========================================
    // CLICK
    // =========================================

    public boolean isClicked(
        float screenX,
        float screenY
    ) {

        float boardX = getBoardX();
        float boardY = getBoardY();

        float playerX =
            boardX + x * TILE_SIZE;

        float playerY =
            boardY + y * TILE_SIZE;

        return screenX >= playerX
            && screenX <= playerX + TILE_SIZE
            && screenY >= playerY
            && screenY <= playerY + TILE_SIZE;
    }

    // =========================================
    // SELECTION
    // =========================================

    public void setSelected(
        boolean selected
    ) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    // =========================================
    // POSITION
    // =========================================

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // =========================================
    // MOVEMENT
    // =========================================

    public boolean isValidMove(
        int targetX,
        int targetY,
        MovementRules rules
    ) {

        if (!isInsideBoard(
            targetX,
            targetY
        )) {
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

    public boolean moveTo(
        int targetX,
        int targetY,
        MovementRules rules
    ) {

        if (!isValidMove(
            targetX,
            targetY,
            rules
        )) {
            return false;
        }

        x = targetX;
        y = targetY;

        return true;
    }

    // =========================================
    // LEGAL MOVES
    // =========================================

    public void renderLegalMoves(
        ShapeRenderer shape,
        MovementRules rules
    ) {

        if (!selected) {
            return;
        }

        float boardX = getBoardX();
        float boardY = getBoardY();

        // -----------------------------------------
        // DESTINATION SQUARES
        // -----------------------------------------

        shape.setColor(
            new Color(
                0.20f,
                0.70f,
                1f,
                0.45f
            )
        );

        for (
            int targetX = 0;
            targetX < BOARD_SIZE;
            targetX++
        ) {

            for (
                int targetY = 0;
                targetY < BOARD_SIZE;
                targetY++
            ) {

                if (isValidMove(
                    targetX,
                    targetY,
                    rules
                )) {

                    shape.rect(
                        boardX
                            + targetX * TILE_SIZE
                            + 15,

                        boardY
                            + targetY * TILE_SIZE
                            + 15,

                        TILE_SIZE - 30,
                        TILE_SIZE - 30
                    );
                }
            }
        }

        // -----------------------------------------
        // SELECTED BORDER
        // -----------------------------------------

        shape.setColor(
            new Color(
                1f,
                0.75f,
                0.15f,
                1f
            )
        );

        shape.rect(
            boardX
                + x * TILE_SIZE
                + 4,

            boardY
                + y * TILE_SIZE
                + 4,

            TILE_SIZE - 8,
            TILE_SIZE - 8
        );
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
        texture.dispose();
    }
}
