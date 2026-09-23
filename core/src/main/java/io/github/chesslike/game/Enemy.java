package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enemy {

    private int x;
    private int y;

    private final Texture texture;

    private final Card.MovementType movementType;
    private final MovementRules movementRules;

    private static final int BOARD_SIZE = 5;
    private static final int TILE_SIZE = 100;

    private boolean alive = true;

    private final Random random = new Random();

    // -----------------------------------------
    // ANIMATION
    // -----------------------------------------

    private boolean moving = false;

    private float renderX;
    private float renderY;

    private float startX;
    private float startY;

    private float targetX;
    private float targetY;

    private float moveTimer = 0f;

    private final float moveDuration = 0.25f;

    // -----------------------------------------
    // CONSTRUCTOR
    // -----------------------------------------

    public Enemy(
        int x,
        int y,
        Card.MovementType movementType,
        String texturePath
    ) {

        this.x = x;
        this.y = y;

        this.movementType = movementType;
        this.movementRules =
            createMovementRules(movementType);

        texture = new Texture(texturePath);

        renderX =
            getBoardX()
                + x * TILE_SIZE;

        renderY =
            getBoardY()
                + y * TILE_SIZE;
    }

    // -----------------------------------------
    // CREATE RULES
    // -----------------------------------------

    private MovementRules createMovementRules(
        Card.MovementType type
    ) {

        switch (type) {

            case KNIGHT:
                return new KnightRules();

            case BISHOP:
                return new BishopRules();

            case ROOK:
                return new RookRules();

            case QUEEN:
                return new QueenRules();

            case PAWN:
                return new PawnRules();

            default:
                throw new IllegalArgumentException(
                    "Unknown enemy type: " + type
                );
        }
    }

    // -----------------------------------------
    // RENDER
    // -----------------------------------------

    public void render(SpriteBatch batch) {

        if (!alive) {
            return;
        }

        updateAnimation();

        float pieceSize =
            Player.TILE_SIZE * 0.90f;

        float offset =
            (Player.TILE_SIZE - pieceSize) / 2f;

        batch.draw(
            texture,
            renderX + offset,
            renderY + offset,
            pieceSize,
            pieceSize
        );
    }


    // -----------------------------------------
    // RANDOM MOVEMENT
    // -----------------------------------------

    public boolean moveTowards(
        int playerX,
        int playerY
    ) {

        if (!alive || moving) {
            return false;
        }

        List<int[]> legalMoves =
            new ArrayList<>();

        // Find all legal moves
        for (int targetX = 0;
             targetX < BOARD_SIZE;
             targetX++) {

            for (int targetY = 0;
                 targetY < BOARD_SIZE;
                 targetY++) {

                if (movementRules.isValidMove(
                    x,
                    y,
                    targetX,
                    targetY,
                    BOARD_SIZE
                )) {

                    legalMoves.add(
                        new int[] {
                            targetX,
                            targetY
                        }
                    );
                }
            }
        }

        if (legalMoves.isEmpty()) {
            return false;
        }

        // Find the move that gets closest to player
        int[] bestMove = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int[] move : legalMoves) {

            int distance =
                Math.abs(move[0] - playerX)
                    + Math.abs(move[1] - playerY);

            if (distance < bestDistance) {

                bestDistance = distance;
                bestMove = move;
            }
        }

        if (bestMove == null) {
            return false;
        }

        moveTo(
            bestMove[0],
            bestMove[1]
        );

        return true;
    }


    // -----------------------------------------
    // MOVE
    // -----------------------------------------

    private void moveTo(
        int targetX,
        int targetY
    ) {

        startX = renderX;
        startY = renderY;

        x = targetX;
        y = targetY;

        targetX =
            (int) (getBoardX()
                            + x * TILE_SIZE);

        targetY =
            (int) (getBoardY()
                            + y * TILE_SIZE);

        this.targetX = targetX;
        this.targetY = targetY;

        moveTimer = 0f;
        moving = true;
    }

    // -----------------------------------------
    // ANIMATION
    // -----------------------------------------

    private void updateAnimation() {

        if (!moving) {
            return;
        }

        moveTimer +=
            Gdx.graphics.getDeltaTime();

        float progress =
            moveTimer / moveDuration;

        if (progress >= 1f) {

            progress = 1f;
            moving = false;

            renderX = targetX;
            renderY = targetY;

            return;
        }

        // Smooth movement
        float smooth =
            progress * progress
                * (3f - 2f * progress);

        renderX =
            startX
                + (targetX - startX)
                * smooth;

        renderY =
            startY
                + (targetY - startY)
                * smooth;
    }

    // -----------------------------------------
    // CLICK
    // -----------------------------------------

    public boolean isClicked(
        float screenX,
        float screenY
    ) {

        if (!alive) {
            return false;
        }

        float enemyX =
            getBoardX()
                + x * TILE_SIZE;

        float enemyY =
            getBoardY()
                + y * TILE_SIZE;

        return screenX >= enemyX
            && screenX <= enemyX + TILE_SIZE
            && screenY >= enemyY
            && screenY <= enemyY + TILE_SIZE;
    }

    // -----------------------------------------
    // GETTERS
    // -----------------------------------------

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Card.MovementType getMovementType() {
        return movementType;
    }

    public MovementRules getMovementRules() {
        return movementRules;
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean isMoving() {
        return moving;
    }

    // -----------------------------------------
    // DEATH
    // -----------------------------------------

    public void kill() {
        alive = false;
    }

    // -----------------------------------------
    // BOARD POSITION
    // -----------------------------------------

    private float getBoardX() {
        return Player.getBoardX();
    }

    private float getBoardY() {
        return Player.getBoardY();
    }


    // -----------------------------------------
    // CLEANUP
    // -----------------------------------------

    public void dispose() {
        texture.dispose();
    }
}
