package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.audio.Sound;

public class Card {

    // =========================================================
    // MOVEMENT TYPES
    // =========================================================

    public enum MovementType {
        KNIGHT,
        ROOK,
        BISHOP,
        QUEEN,
        PAWN,
        DASH,
        MADROOK,
        JESTER,
        BLINKER,
        CHAMELEON
    }

    // =========================================================
    // CARD SIZE
    // =========================================================

    private static final float WIDTH = 115f;
    private static final float HEIGHT = 155f;

    // =========================================================
    // CARD TEXTURES
    // =========================================================

    private static Texture frameStandard;
    private static Texture frameAugmented;

    private static Sound selectSound;
    private static Sound hoverSound;

    public static void loadTextures() {

        if (frameStandard == null) {
            frameStandard = new Texture(
                Gdx.files.internal("card_frame_standard.png")
            );

            frameStandard.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        if (frameAugmented == null) {
            frameAugmented = new Texture(
                Gdx.files.internal("card_frame_augmented.png")
            );

            frameAugmented.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        if (selectSound == null) {
            selectSound = Gdx.audio.newSound(
                Gdx.files.internal(
                    "freesound_community-flipcard-91468.mp3"
                )
            );
        }

        if (hoverSound == null) {
            hoverSound = Gdx.audio.newSound(
                Gdx.files.internal(
                    "666herohero-click-21156.mp3"
                )
            );
        }
    }

    public static void disposeTextures() {

        if (frameStandard != null) {
            frameStandard.dispose();
            frameStandard = null;
        }

        if (frameAugmented != null) {
            frameAugmented.dispose();
            frameAugmented = null;
        }

        if (selectSound != null) {
            selectSound.dispose();
            selectSound = null;
        }

        if (hoverSound != null) {
            hoverSound.dispose();
            hoverSound = null;
        }
    }

    // =========================================================
    // CARD INFORMATION
    // =========================================================

    private final String name;
    private final MovementType movementType;
    private final MovementRules movementRules;

    // =========================================================
    // CARD MODIFIER
    // =========================================================

    private CardModifier modifier;

    // =========================================================
    // POSITION / ANIMATION
    // =========================================================

    private float screenX;
    private float screenY;

    private float animatedX;
    private float animatedY;

    private boolean initialized = false;

    private float scale = 1f;
    private float layoutScale = 1f;
    private float targetScale = 1f;

    private static final float HOVER_LIFT = 14f;
    private static final float SELECTED_LIFT = 24f;

    private static final float HOVER_SCALE = 1.04f;
    private static final float SELECTED_SCALE = 1.09f;

    private static final float ANIMATION_SPEED = 12f;

    private boolean hovered;
    private boolean selected;

    // =========================================================
    // USES
    // =========================================================

    private int usesRemaining;

    private static final int DEFAULT_USES = 2;

    // =========================================================
    // AUGMENTS
    // =========================================================

    private boolean hasRebound;
    private boolean hasPierce;
    private boolean hasDoubleMove;
    private boolean hasTeleportationInfusion;
    private boolean hasBurn;

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

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

    // =========================================================
    // MOVEMENT RULE CREATION
    // =========================================================

    private MovementRules createMovementRules(
        MovementType type
    ) {
        switch (type) {

            case KNIGHT:
                return new KnightRules();

            case ROOK:
                return new RookRules();

            case BISHOP:
                return new BishopRules();

            case QUEEN:
                return new QueenRules();

            case PAWN:
                return new PawnRules();

            case DASH:
                return new DashRules();

            default:
                throw new IllegalArgumentException(
                    "Unsupported movement type: "
                        + type
                );
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getName() {
        return name;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public MovementRules getMovementRules() {
        return movementRules;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    public boolean isUsed() {
        return usesRemaining <= 0;
    }

    // =========================================================
    // CARD MODIFIER
    // =========================================================

    public CardModifier getModifier() {
        return modifier;
    }

    public void setModifier(CardModifier modifier) {
        this.modifier = modifier;
    }

    public boolean hasModifier() {
        return modifier != null;
    }

    // =========================================================
    // CARD USE
    // =========================================================

    public void setSelected(boolean selected) {

        if (isUsed()) {
            this.selected = false;
            return;
        }

        if (!this.selected
            && selected
            && selectSound != null) {

            selectSound.play(1.0f);
        }

        this.selected = selected;
    }

    public boolean use() {

        if (isUsed()) {
            return false;
        }

        usesRemaining--;

        selected = false;
        hovered = false;

        if (selectSound != null) {
            selectSound.play(
                0.8f,
                1.2f,
                0f
            );
        }

        return true;
    }

    // =========================================================
    // AUGMENTS
    // =========================================================

    public void applyAugment(Augment augment) {

        if (augment == null) {
            return;
        }

        switch (augment.getType()) {

            case EXTRA_USES:
                usesRemaining +=
                    augment.getValue();
                break;

            case REBOUND:
                hasRebound = true;
                break;

            case PIERCE:
                hasPierce = true;
                break;

            case DOUBLE_MOVE:
                hasDoubleMove = true;
                break;

            case TELEPORTATION_INFUSION:
                hasTeleportationInfusion = true;
                break;

            case BURN:
                hasBurn = true;
                break;

            default:
                break;
        }
    }

    public boolean hasRebound() {
        return hasRebound;
    }

    public boolean hasPierce() {
        return hasPierce;
    }

    public boolean hasDoubleMove() {
        return hasDoubleMove;
    }

    public boolean hasTeleportationInfusion() {
        return hasTeleportationInfusion;
    }

    public boolean hasBurn() {
        return hasBurn;
    }

    public void consumeBurn() {
        hasBurn = false;
    }

    public boolean hasAnyAugment() {
        return hasRebound
            || hasPierce
            || hasDoubleMove
            || hasTeleportationInfusion
            || hasBurn;
    }

    // =========================================================
    // POSITION & ANIMATION
    // =========================================================

    public void setPosition(
        float x,
        float y
    ) {
        screenX = x;
        screenY = y;

        if (!initialized) {
            animatedX = x;
            animatedY = y;
            initialized = true;
        }
    }

    public void setLayoutScale(float scale) {
        layoutScale =
            MathUtils.clamp(
                scale,
                0.45f,
                1f
            );
    }

    public void setHovered(
        boolean hovered
    ) {

        if (isUsed()) {
            this.hovered = false;
            return;
        }

        if (!this.hovered
            && hovered
            && hoverSound != null) {

            hoverSound.play(0.5f);
        }

        this.hovered = hovered;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isSelected() {
        return selected;
    }

    public void update(float delta) {

        float targetX = screenX;
        float targetY = screenY;

        if (!isUsed()) {

            if (selected) {
                targetY += SELECTED_LIFT;
            } else if (hovered) {
                targetY += HOVER_LIFT;
            }
        }

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
            targetScale = layoutScale;
        }

        float amount =
            Math.min(
                delta * ANIMATION_SPEED,
                1f
            );

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

        scale =
            MathUtils.lerp(
                scale,
                targetScale,
                amount
            );
    }

    // =========================================================
    // DRAW METHODS
    // =========================================================

    public void drawShape(
        SpriteBatch batch
    ) {

        if (
            batch == null
                || frameStandard == null
                || frameAugmented == null
        ) {
            return;
        }

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

        Texture frame =
            hasAnyAugment()
                ? frameAugmented
                : frameStandard;

        if (isUsed()) {

            batch.setColor(
                0.55f,
                0.55f,
                0.55f,
                1f
            );

        } else if (selected) {

            batch.setColor(
                1f,
                0.85f,
                0.55f,
                1f
            );

        } else if (hovered) {

            batch.setColor(
                0.75f,
                0.92f,
                1f,
                1f
            );

        } else {

            batch.setColor(Color.WHITE);
        }

        batch.draw(
            frame,
            drawX,
            drawY,
            scaledWidth,
            scaledHeight
        );

        batch.setColor(Color.WHITE);
    }

    public void drawText(
        SpriteBatch batch,
        BitmapFont font
    ) {

        if (
            batch == null
                || font == null
        ) {
            return;
        }

        float drawX =
            animatedX
                + (WIDTH - WIDTH * scale) / 2f;

        float drawY =
            animatedY
                + (HEIGHT - HEIGHT * scale) / 2f;

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
            drawY
                + HEIGHT * scale
                - 18f * scale
        );

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
            drawY
                + HEIGHT * scale
                - 38f * scale
        );

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

            case DASH:
                return "Dash forward";

            default:
                return "";
        }
    }

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
