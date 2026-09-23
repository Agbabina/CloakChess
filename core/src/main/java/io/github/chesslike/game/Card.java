package io.github.chesslike.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

public class Card {

    // =========================================
    // MOVEMENT TYPES
    // =========================================

    public enum MovementType {
        KNIGHT,
        ROOK,
        BISHOP,
        QUEEN,
        PAWN
    }

    // =========================================
    // CARD DATA
    // =========================================

    private final String name;
    private final MovementType movementType;
    private final MovementRules movementRules;

    // =========================================
    // CARD SIZE
    // =========================================

    private static final float WIDTH = 115f;
    private static final float HEIGHT = 155f;

    // =========================================
    // POSITION
    // =========================================

    private float screenX;
    private float screenY;

    private float animatedX;
    private float animatedY;

    private boolean initialized = false;

    // =========================================
    // SCALE
    // =========================================

    private float scale = 1f;
    private float layoutScale = 1f;

    private float targetScale = 1f;

    // =========================================
    // ANIMATION
    // =========================================

    private static final float HOVER_LIFT = 14f;
    private static final float SELECTED_LIFT = 24f;

    private static final float HOVER_SCALE = 1.04f;
    private static final float SELECTED_SCALE = 1.09f;

    private static final float ANIMATION_SPEED = 12f;

    // =========================================
    // STATE
    // =========================================

    private boolean hovered;
    private boolean selected;

    // =========================================
    // USES
    // =========================================

    private int usesRemaining;

    private static final int DEFAULT_USES = 2;

    // =========================================
    // CONSTRUCTOR
    // =========================================

    public Card(
        String name,
        MovementType movementType
    ) {

        this(
            name,
            movementType,
            DEFAULT_USES
        );
    }

    public Card(
        String name,
        MovementType movementType,
        int uses
    ) {

        this.name = name;
        this.movementType = movementType;

        this.movementRules =
            createMovementRules(movementType);

        this.usesRemaining =
            Math.max(uses, 0);
    }

    // =========================================
    // MOVEMENT RULES
    // =========================================

    private MovementRules createMovementRules(
        MovementType type
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
                    "Unsupported movement type: " + type
                );
        }
    }

    // =========================================
    // GETTERS
    // =========================================

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementRules getMovementRules() {
        return movementRules;
    }

    public String getName() {
        return name;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    public boolean isUsed() {
        return usesRemaining <= 0;
    }

    // =========================================
    // USE CARD
    // =========================================

    public boolean use() {

        if (isUsed()) {
            return false;
        }

        usesRemaining--;

        selected = false;
        hovered = false;

        return true;
    }

    // =========================================
    // POSITION
    // =========================================

    public void setPosition(
        float x,
        float y
    ) {

        screenX = x;
        screenY = y;

        // First placement is instant.
        // Later positioning is animated.
        if (!initialized) {

            animatedX = x;
            animatedY = y;

            initialized = true;
        }
    }

    // =========================================
    // LAYOUT SCALE
    // =========================================

    public void setLayoutScale(float scale) {

        layoutScale =
            MathUtils.clamp(
                scale,
                0.45f,
                1f
            );
    }

    // =========================================
    // HOVER
    // =========================================

    public void setHovered(boolean hovered) {

        if (isUsed()) {
            this.hovered = false;
            return;
        }

        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    // =========================================
    // SELECTED
    // =========================================

    public void setSelected(boolean selected) {

        if (isUsed()) {
            this.selected = false;
            return;
        }

        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    // =========================================
    // UPDATE ANIMATION
    // =========================================

    public void update(float delta) {

        float targetX = screenX;
        float targetY = screenY;

        // -----------------------------------------
        // VERTICAL LIFT
        // -----------------------------------------

        if (!isUsed()) {

            if (selected) {

                targetY += SELECTED_LIFT;

            } else if (hovered) {

                targetY += HOVER_LIFT;
            }
        }

        // -----------------------------------------
        // SCALE
        // -----------------------------------------

        if (isUsed()) {

            targetScale =
                layoutScale * 0.88f;

        } else if (selected) {

            targetScale =
                layoutScale * SELECTED_SCALE;

        } else if (hovered) {

            targetScale =
                layoutScale * HOVER_SCALE;

        } else {

            targetScale =
                layoutScale;
        }

        float amount =
            Math.min(
                delta * ANIMATION_SPEED,
                1f
            );

        // -----------------------------------------
        // POSITION
        // -----------------------------------------

        animatedX =
            MathUtils.lerp(
                animatedX,
                targetX,
                amount
            );

        animatedY =
            MathUtils.lerp(
                animatedY,
                targetY,
                amount
            );

        // -----------------------------------------
        // SCALE
        // -----------------------------------------

        scale =
            MathUtils.lerp(
                scale,
                targetScale,
                amount
            );
    }

    // =========================================
    // DRAW SHAPE
    // =========================================

    public void drawShape(
        ShapeRenderer shape
    ) {

        float scaledWidth =
            WIDTH * scale;

        float scaledHeight =
            HEIGHT * scale;

        float drawX =
            animatedX
                + (WIDTH - scaledWidth) / 2f;

        float drawY =
            animatedY
                + (HEIGHT - scaledHeight) / 2f;

        boolean used = isUsed();

        // -----------------------------------------
        // SHADOW
        // -----------------------------------------

        shape.setColor(
            new Color(
                0f,
                0f,
                0f,
                used
                    ? 0.15f
                    : selected || hovered
                    ? 0.60f
                    : 0.45f
            )
        );

        shape.rect(
            drawX + 5,
            drawY - 5,
            scaledWidth,
            scaledHeight
        );

        // -----------------------------------------
        // CARD
        // -----------------------------------------

        if (used) {

            shape.setColor(
                new Color(
                    0.055f,
                    0.06f,
                    0.075f,
                    1f
                )
            );

        } else {

            shape.setColor(
                new Color(
                    0.10f,
                    0.12f,
                    0.16f,
                    1f
                )
            );
        }

        shape.rect(
            drawX,
            drawY,
            scaledWidth,
            scaledHeight
        );

        // -----------------------------------------
        // HEADER
        // -----------------------------------------

        shape.setColor(
            used
                ? new Color(
                0.08f,
                0.09f,
                0.11f,
                1f
            )
                : new Color(
                0.16f,
                0.19f,
                0.25f,
                1f
            )
        );

        shape.rect(
            drawX,
            drawY + scaledHeight - 48f * scale,
            scaledWidth,
            48f * scale
        );

        // -----------------------------------------
        // BORDER
        // -----------------------------------------

        if (used) {

            shape.setColor(
                new Color(
                    0.20f,
                    0.21f,
                    0.24f,
                    1f
                )
            );

        } else if (selected) {

            shape.setColor(
                new Color(
                    0.95f,
                    0.75f,
                    0.25f,
                    1f
                )
            );

        } else if (hovered) {

            shape.setColor(
                new Color(
                    0.40f,
                    0.75f,
                    1f,
                    1f
                )
            );

        } else {

            shape.setColor(
                new Color(
                    0.30f,
                    0.65f,
                    0.95f,
                    1f
                )
            );
        }

        float border =
            3f * scale;

        shape.rect(
            drawX,
            drawY,
            scaledWidth,
            border
        );

        shape.rect(
            drawX,
            drawY + scaledHeight - border,
            scaledWidth,
            border
        );

        shape.rect(
            drawX,
            drawY,
            border,
            scaledHeight
        );

        shape.rect(
            drawX + scaledWidth - border,
            drawY,
            border,
            scaledHeight
        );

        // -----------------------------------------
        // MOVEMENT DISPLAY
        // -----------------------------------------

        shape.setColor(
            used
                ? new Color(
                0.045f,
                0.05f,
                0.06f,
                1f
            )
                : new Color(
                0.07f,
                0.09f,
                0.12f,
                1f
            )
        );

        shape.rect(
            drawX + 15f * scale,
            drawY + 45f * scale,
            (WIDTH - 30f) * scale,
            55f * scale
        );
    }

    // =========================================
    // DRAW TEXT
    // =========================================

    public void drawText(
        SpriteBatch batch,
        BitmapFont font
    ) {

        float drawX =
            animatedX
                + (WIDTH - WIDTH * scale) / 2f;

        float drawY =
            animatedY
                + (HEIGHT - HEIGHT * scale) / 2f;

        // -----------------------------------------
        // TITLE
        // -----------------------------------------

        font.getData().setScale(
            0.85f * scale
        );

        font.setColor(
            isUsed()
                ? Color.GRAY
                : Color.WHITE
        );

        font.draw(
            batch,
            name,
            drawX + 15f * scale,
            drawY + HEIGHT * scale - 18f * scale
        );

        // -----------------------------------------
        // USE COUNTER
        // -----------------------------------------

        font.getData().setScale(
            0.55f * scale
        );

        font.setColor(
            isUsed()
                ? Color.DARK_GRAY
                : new Color(
                1f,
                0.80f,
                0.30f,
                1f
            )
        );

        String usesText =
            isUsed()
                ? "USED"
                : "USES: " + usesRemaining;

        font.draw(
            batch,
            usesText,
            drawX + 15f * scale,
            drawY + HEIGHT * scale - 38f * scale
        );

        // -----------------------------------------
        // MOVEMENT TYPE
        // -----------------------------------------

        font.getData().setScale(
            0.65f * scale
        );

        font.setColor(
            isUsed()
                ? Color.DARK_GRAY
                : new Color(
                0.30f,
                0.65f,
                0.95f,
                1f
            )
        );

        font.draw(
            batch,
            movementType.toString(),
            drawX + 20f * scale,
            drawY + 75f * scale
        );

        // -----------------------------------------
        // DESCRIPTION
        // -----------------------------------------

        font.getData().setScale(
            0.55f * scale
        );

        font.setColor(
            isUsed()
                ? Color.DARK_GRAY
                : new Color(
                0.70f,
                0.75f,
                0.82f,
                1f
            )
        );

        font.draw(
            batch,
            getDescription(),
            drawX + 20f * scale,
            drawY + 30f * scale
        );

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // =========================================
    // DESCRIPTION
    // =========================================

    private String getDescription() {

        switch (movementType) {

            case KNIGHT:
                return "L-shaped move";

            case BISHOP:
                return "Diagonal lines";

            case ROOK:
                return "Straight lines";

            case QUEEN:
                return "Any direction";

            case PAWN:
                return "Forward move";

            default:
                return "";
        }
    }

    // =========================================
    // CLICK DETECTION
    // =========================================

    public boolean isClicked(
        float touchX,
        float touchY
    ) {

        if (isUsed()) {
            return false;
        }

        float drawX =
            animatedX
                + (WIDTH - WIDTH * scale) / 2f;

        float drawY =
            animatedY
                + (HEIGHT - HEIGHT * scale) / 2f;

        float drawWidth =
            WIDTH * scale;

        float drawHeight =
            HEIGHT * scale;

        return touchX >= drawX
            && touchX <= drawX + drawWidth
            && touchY >= drawY
            && touchY <= drawY + drawHeight;
    }
}
