package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Enemy {

    private int x;
    private int y;

    private final Texture texture;

    private final Card.MovementType movementType;
    private MovementRules movementRules;

    private static final int BOARD_SIZE = 5;
    private static final int TILE_SIZE = 100;

    private boolean alive = true;

    private final Random random = new Random();

    // HEALTH
    private final int maxHealth;
    private int health;

    // CURSE
    private EnemyCurse curse = null;

    // MOVE / PUSH ANIMATION
    private static final float MOVE_DURATION = 0.25f;
    private static final float PUSH_DURATION = 0.15f;

    private boolean moving = false;

    private float renderX;
    private float renderY;

    private float startX;
    private float startY;

    private float targetX;
    private float targetY;

    private float moveTimer = 0f;
    private float activeDuration = MOVE_DURATION;

    // A pushed enemy loses its next move (turn logic, not animation state)
    private boolean skipNextMove = false;

    // FALL-IN ANIMATION
    private static final float FALL_DURATION = 0.45f;
    private static final float FALL_HEIGHT = 500f;

    private boolean falling = false;
    private float fallDelay = 0f;
    private float fallTimer = 0f;
    private boolean landed = false;

    // CONSTRUCTORS
    public Enemy(int x, int y, Card.MovementType movementType, String texturePath) {
        this(x, y, movementType, texturePath, 1, 0.5f);
    }

    public Enemy(int x, int y, Card.MovementType movementType, String texturePath, int room, float difficulty) {
        this.x = x;
        this.y = y;

        this.movementType = movementType;
        this.movementRules = createMovementRules(movementType);

        this.maxHealth = rollMaxHealth(room, difficulty);
        this.health = maxHealth;

        texture = new Texture(texturePath);

        renderX = getBoardX() + x * TILE_SIZE;
        renderY = getBoardY() + y * TILE_SIZE;
    }

    // HEALTH ROLL
    private int rollMaxHealth(int room, float difficulty) {
        if (room > 10) {
            float chanceOfFour = Math.max(0f, Math.min(1f, difficulty));
            return random.nextFloat() < chanceOfFour ? 4 : 3;
        }
        if (room > 5) {
            return 2;
        }
        return 1;
    }

    // CURSE
    public EnemyCurse getCurse() {
        return curse;
    }

    public void setCurse(EnemyCurse curse) {
        this.curse = curse;
    }

    public boolean hasCurse() {
        return curse != null;
    }

    public boolean hasCurse(EnemyCurse curse) {
        return this.curse == curse;
    }

    // CREATE RULES
    private MovementRules createMovementRules(Card.MovementType type) {
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
            case JESTER:
                return new JesterRules();
            case BLINKER:
                return new BlinkerRules();
            case MADROOK:
                return new MadRookRules();
            case CHAMELEON:
                return randomChameleonRules();
            default:
                throw new IllegalArgumentException("Unknown enemy type: " + type);
        }
    }

    // CHAMELEON: borrows a different piece's movement every turn
    private MovementRules randomChameleonRules() {
        switch (random.nextInt(3)) {
            case 0:
                return new KnightRules();
            case 1:
                return new BishopRules();
            default:
                return new RookRules();
        }
    }

    // FALL-IN
    public void startFall(float delay) {
        falling = true;
        fallDelay = delay;
        fallTimer = 0f;
        landed = false;
    }

    public boolean isFalling() {
        return falling;
    }

    public boolean consumeLanded() {
        if (landed) {
            landed = false;
            return true;
        }
        return false;
    }

    private void updateFall() {
        if (!falling) {
            return;
        }

        float delta = Gdx.graphics.getDeltaTime();

        if (fallDelay > 0f) {
            fallDelay -= delta;
            return;
        }

        fallTimer += delta;

        if (fallTimer >= FALL_DURATION) {
            falling = false;
            landed = true;
        }
    }

    private float getFallOffset() {
        if (!falling) {
            return 0f;
        }

        if (fallDelay > 0f) {
            return FALL_HEIGHT;
        }

        float t = Math.min(1f, fallTimer / FALL_DURATION);
        return FALL_HEIGHT * (1f - t * t);
    }

    // SHADOW
    public void renderShadow(ShapeRenderer shapes) {
        if (!alive || !falling) {
            return;
        }

        float t = fallDelay > 0f ? 0f : Math.min(1f, fallTimer / FALL_DURATION);

        float w = TILE_SIZE * 0.85f * (0.3f + 0.7f * t);
        float h = w * 0.45f;

        float cx = renderX + TILE_SIZE / 2f;
        float cy = renderY + TILE_SIZE / 2f - TILE_SIZE * 0.25f;

        shapes.setColor(0.85f, 0.2f, 0.2f, 0.15f + 0.45f * t);
        shapes.ellipse(cx - w / 2f, cy - h / 2f, w, h);
    }

    // RENDER
    public void render(SpriteBatch batch) {
        if (!alive) {
            return;
        }

        updateFall();

        if (falling && fallDelay > 0f) {
            return;
        }

        updateAnimation();

        float pieceSize = Player.TILE_SIZE * 0.90f;
        float offset = (Player.TILE_SIZE - pieceSize) / 2f;

        batch.draw(
            texture,
            renderX + offset,
            renderY + offset + getFallOffset(),
            pieceSize,
            pieceSize
        );
    }

    // MOVEMENT TOWARDS PLAYER
    public boolean moveTowards(int playerX, int playerY) {
        if (!alive || falling) {
            return false;
        }

        // Pushed enemies lose exactly one move
        if (skipNextMove) {
            skipNextMove = false;
            return false;
        }

        // Chameleon changes its movement rules every turn
        if (movementType == Card.MovementType.CHAMELEON) {
            movementRules = randomChameleonRules();
        }

        List<int[]> legalMoves = new ArrayList<>();

        for (int tx = 0; tx < BOARD_SIZE; tx++) {
            for (int ty = 0; ty < BOARD_SIZE; ty++) {
                if (movementRules.isValidMove(x, y, tx, ty, BOARD_SIZE)) {
                    legalMoves.add(new int[] {tx, ty});
                }
            }
        }

        if (legalMoves.isEmpty()) {
            return false;
        }

        int[] bestMove = null;
        int bestDistance = Integer.MAX_VALUE;

        for (int[] move : legalMoves) {
            int distance = Math.abs(move[0] - playerX) + Math.abs(move[1] - playerY);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestMove = move;
            }
        }

        if (bestMove == null) {
            return false;
        }

        moveTo(bestMove[0], bestMove[1], MOVE_DURATION);

        return true;
    }

    // PUSH
    public void pushTo(int newX, int newY) {
        if (!alive) {
            return;
        }

        skipNextMove = true;
        moveTo(newX, newY, PUSH_DURATION);
    }

    // MOVE
    private void moveTo(int tileX, int tileY, float duration) {
        startX = renderX;
        startY = renderY;

        x = tileX;
        y = tileY;

        targetX = getBoardX() + x * TILE_SIZE;
        targetY = getBoardY() + y * TILE_SIZE;

        activeDuration = duration;
        moveTimer = 0f;
        moving = true;
    }

    // ANIMATION
    private void updateAnimation() {
        if (!moving) {
            return;
        }

        moveTimer += Gdx.graphics.getDeltaTime();

        float progress = moveTimer / activeDuration;

        if (progress >= 1f) {
            progress = 1f;
            moving = false;

            renderX = targetX;
            renderY = targetY;

            return;
        }

        float smooth = progress * progress * (3f - 2f * progress);

        renderX = startX + (targetX - startX) * smooth;
        renderY = startY + (targetY - startY) * smooth;
    }

    // CLICK
    public boolean isClicked(float screenX, float screenY) {
        if (!alive) {
            return false;
        }

        float enemyX = getBoardX() + x * TILE_SIZE;
        float enemyY = getBoardY() + y * TILE_SIZE;

        return screenX >= enemyX
            && screenX <= enemyX + TILE_SIZE
            && screenY >= enemyY
            && screenY <= enemyY + TILE_SIZE;
    }

    // GETTERS
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

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    // DAMAGE / DEATH
    public boolean takeDamage(int amount) {
        if (!alive) {
            return false;
        }

        health = Math.max(0, health - amount);

        if (health == 0) {
            alive = false;
            return true;
        }

        return false;
    }

    public void kill() {
        health = 0;
        alive = false;
    }

    // BOARD POSITION
    private float getBoardX() {
        return Player.getBoardX();
    }

    private float getBoardY() {
        return Player.getBoardY();
    }

    // CLEANUP
    public void dispose() {
        texture.dispose();
    }
}
