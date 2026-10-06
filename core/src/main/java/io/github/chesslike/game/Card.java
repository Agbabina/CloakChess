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
        CHAMELEON,
        ARCHBISHOP,
        COUNCILLOR,
        CLAUDE,
        SHIFTER,
        LEAPER,
    }

    // Forms the Shifter card can take
    private static final MovementType[] SHIFT_FORMS = {
        MovementType.KNIGHT, MovementType.BISHOP, MovementType.ROOK,
        MovementType.QUEEN, MovementType.PAWN, MovementType.DASH
    };

    // =========================================================
    // DRAGGING
    // =========================================================

    private boolean dragging = false;

    private float dragOffsetX;
    private float dragOffsetY;

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
            frameStandard = new Texture(Gdx.files.internal("card_frame_standard.png"));
            frameStandard.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        if (frameAugmented == null) {
            frameAugmented = new Texture(Gdx.files.internal("card_frame_augmented.png"));
            frameAugmented.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        if (selectSound == null) {
            selectSound = Gdx.audio.newSound(Gdx.files.internal("freesound_community-flipcard-91468.mp3"));
        }

        if (hoverSound == null) {
            hoverSound = Gdx.audio.newSound(Gdx.files.internal("666herohero-click-21156.mp3"));
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
    private MovementRules movementRules;
    private MovementType currentForm;

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

    // Drives the pulsing glow on the Claude / Shifter cards
    private float pulseTime = 0f;

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

    // New augments
    private boolean hasFury;        // +1 capture damage (red tint)
    private boolean hasManaSurge;   // +2 mana on play (blue tint)
    private boolean hasVampiric;    // heal 1 HP on capture
    private boolean hasGilded;      // +8 gold on capture
    private boolean hasAegis;       // +1 DEF on play
    private boolean hasVenom;       // poison nearest enemy on play
    private boolean hasEcho;        // 35% chance not to spend a use

    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public Card(String name, MovementType movementType) {
        this(name, movementType, DEFAULT_USES);
    }

    public Card(String name, MovementType movementType, int uses) {
        this.name = name;
        this.movementType = movementType;
        this.usesRemaining = Math.max(uses, 0);

        if (movementType == MovementType.SHIFTER) {
            this.currentForm = SHIFT_FORMS[MathUtils.random(SHIFT_FORMS.length - 1)];
            this.movementRules = createMovementRules(currentForm);
        } else {
            this.currentForm = movementType;
            this.movementRules = createMovementRules(movementType);
        }
    }

    // =========================================================
    // MOVEMENT RULE CREATION
    // =========================================================

    private MovementRules createMovementRules(MovementType type) {
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

            case MADROOK:
                return new MadRookRules();

            case JESTER:
                return new JesterRules();

            case BLINKER:
                return new BlinkerRules();

            case CHAMELEON:
                return new ChameleonRules();

            case ARCHBISHOP:
                return new ArchbishopRules();

            case COUNCILLOR:
                return new Councillor();

            case CLAUDE:
                return new ClaudeRules();

            case LEAPER:
                return new LeaperRules();

            default:
                throw new IllegalArgumentException("Unsupported movement type: " + type);
        }
    }

    // =========================================================
    // SHIFTER
    // =========================================================

    public boolean isShifter() {
        return movementType == MovementType.SHIFTER;
    }

    /** Current form (for non-shifter cards this is just the movement type). */
    public MovementType getCurrentForm() {
        return currentForm;
    }

    /** Shifter only: switch to a different random form. Call once per turn. */
    public void shift() {
        if (!isShifter()) return;
        MovementType next = currentForm;
        for (int attempt = 0; attempt < 20 && next == currentForm; attempt++) {
            next = SHIFT_FORMS[MathUtils.random(SHIFT_FORMS.length - 1)];
        }
        currentForm = next;
        movementRules = createMovementRules(next);
    }

    private static String formLabel(MovementType t) {
        String s = t.name();
        return s.charAt(0) + s.substring(1).toLowerCase();
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

    private boolean isClaude() {
        return movementType == MovementType.CLAUDE;
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

        if (!this.selected && selected && selectSound != null) {
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
        dragging = false;

        if (selectSound != null) {
            selectSound.play(0.8f, 1.2f, 0f);
        }

        return true;
    }

    // =========================================================
    // DRAGGING
    // =========================================================

    public void startDrag(float touchX, float touchY) {

        if (isUsed()) {
            return;
        }

        dragging = true;

        dragOffsetX = touchX - animatedX;
        dragOffsetY = touchY - animatedY;

        hovered = false;
        selected = true;

        if (selectSound != null) {
            selectSound.play(0.8f);
        }
    }

    public void dragTo(float touchX, float touchY) {

        if (!dragging) {
            return;
        }

        screenX = touchX - dragOffsetX;
        screenY = touchY - dragOffsetY;

        // Make the card immediately follow the finger/mouse while dragging.
        animatedX = screenX;
        animatedY = screenY;
    }

    public void stopDrag() {
        dragging = false;
        selected = false;
    }

    public boolean isDragging() {
        return dragging;
    }

    // =========================================================
    // AUGMENTS
    // =========================================================

    // Switches on the augment type NAME, so this compiles even before the new
    // constants exist in Augment.Type. Add FURY, MANA_SURGE, VAMPIRIC, GILDED,
    // AEGIS, VENOM and ECHO to that enum to make them usable.
    public void applyAugment(Augment augment) {

        if (augment == null) {
            return;
        }

        switch (augment.getType().name()) {

            case "EXTRA_USES":
                usesRemaining += augment.getValue();
                break;

            case "REBOUND":
                hasRebound = true;
                break;

            case "PIERCE":
                hasPierce = true;
                break;

            case "DOUBLE_MOVE":
                hasDoubleMove = true;
                break;

            case "TELEPORTATION_INFUSION":
                hasTeleportationInfusion = true;
                break;

            case "BURN":
                hasBurn = true;
                break;

            case "FURY":
                hasFury = true;
                break;

            case "MANA_SURGE":
                hasManaSurge = true;
                break;

            case "VAMPIRIC":
                hasVampiric = true;
                break;

            case "GILDED":
                hasGilded = true;
                break;

            case "AEGIS":
                hasAegis = true;
                break;

            case "VENOM":
                hasVenom = true;
                break;

            case "ECHO":
                hasEcho = true;
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

    public boolean hasFury() {
        return hasFury;
    }

    public boolean hasManaSurge() {
        return hasManaSurge;
    }

    public boolean hasVampiric() {
        return hasVampiric;
    }

    public boolean hasGilded() {
        return hasGilded;
    }

    public boolean hasAegis() {
        return hasAegis;
    }

    public boolean hasVenom() {
        return hasVenom;
    }

    public boolean hasEcho() {
        return hasEcho;
    }

    public void consumeBurn() {
        hasBurn = false;
    }

    public boolean hasAnyAugment() {
        return hasRebound
            || hasPierce
            || hasDoubleMove
            || hasTeleportationInfusion
            || hasBurn
            || hasFury
            || hasManaSurge
            || hasVampiric
            || hasGilded
            || hasAegis
            || hasVenom
            || hasEcho;
    }

    // ---- Save / load for the new augments (comma separated, e.g. "FURY,ECHO") ----

    public String getExtraAugmentCode() {
        StringBuilder sb = new StringBuilder();
        if (hasFury) sb.append("FURY,");
        if (hasManaSurge) sb.append("MANA_SURGE,");
        if (hasVampiric) sb.append("VAMPIRIC,");
        if (hasGilded) sb.append("GILDED,");
        if (hasAegis) sb.append("AEGIS,");
        if (hasVenom) sb.append("VENOM,");
        if (hasEcho) sb.append("ECHO,");
        return sb.toString();
    }

    public void restoreExtraAugments(String code) {
        if (code == null || code.isEmpty()) return;
        for (String part : code.split(",")) {
            switch (part.trim()) {
                case "FURY": hasFury = true; break;
                case "MANA_SURGE": hasManaSurge = true; break;
                case "VAMPIRIC": hasVampiric = true; break;
                case "GILDED": hasGilded = true; break;
                case "AEGIS": hasAegis = true; break;
                case "VENOM": hasVenom = true; break;
                case "ECHO": hasEcho = true; break;
                default: break;
            }
        }
    }

    // =========================================================
    // POSITION & ANIMATION
    // =========================================================

    public void setPosition(float x, float y) {

        screenX = x;
        screenY = y;

        if (!initialized) {
            animatedX = x;
            animatedY = y;
            initialized = true;
        }
    }

    public void setLayoutScale(float scale) {
        layoutScale = MathUtils.clamp(scale, 0.45f, 1f);
    }

    public void setHovered(boolean hovered) {

        if (isUsed() || dragging) {
            this.hovered = false;
            return;
        }

        if (!this.hovered && hovered && hoverSound != null) {
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

        pulseTime += delta;

        float targetX = screenX;
        float targetY = screenY;

        // While dragging, do not add the normal selected lift
        // because the card is already following the pointer.
        if (!dragging && !isUsed()) {

            if (selected) {
                targetY += SELECTED_LIFT;

            } else if (hovered) {
                targetY += HOVER_LIFT;
            }
        }

        // SCALE
        if (isUsed()) {
            targetScale = layoutScale * 0.88f;

        } else if (dragging) {
            targetScale = layoutScale * SELECTED_SCALE;

        } else if (selected) {
            targetScale = layoutScale * SELECTED_SCALE;

        } else if (hovered) {
            targetScale = layoutScale * HOVER_SCALE;

        } else {
            targetScale = layoutScale;
        }

        // ANIMATION
        float amount = Math.min(delta * ANIMATION_SPEED, 1f);

        // Don't interpolate the dragged card.
        // It should stay directly underneath the finger/mouse.
        if (!dragging) {
            animatedX = MathUtils.lerp(animatedX, targetX, amount);
            animatedY = MathUtils.lerp(animatedY, targetY, amount);
        }

        scale = MathUtils.lerp(scale, targetScale, amount);
    }

    // =========================================================
    // DRAW METHODS
    // =========================================================

    public void drawShape(SpriteBatch batch) {

        if (batch == null || frameStandard == null || frameAugmented == null) {
            return;
        }

        float scaledWidth = WIDTH * scale;
        float scaledHeight = HEIGHT * scale;

        float drawX = animatedX + (WIDTH - scaledWidth) / 2f;
        float drawY = animatedY + (HEIGHT - scaledHeight) / 2f;

        Texture frame = hasAnyAugment() ? frameAugmented : frameStandard;

        if (isUsed()) {

            batch.setColor(0.55f, 0.55f, 0.55f, 1f);

        } else if (dragging) {

            // Dragged card gets a bright highlight.
            if (isClaude()) batch.setColor(1f, 0.72f, 0.30f, 1f);
            else batch.setColor(1f, 0.90f, 0.55f, 1f);

        } else if (selected) {

            if (isClaude()) batch.setColor(1f, 0.70f, 0.28f, 1f);
            else batch.setColor(1f, 0.85f, 0.55f, 1f);

        } else if (hovered) {

            if (isClaude()) batch.setColor(1f, 0.80f, 0.45f, 1f);
            else batch.setColor(0.75f, 0.92f, 1f, 1f);

        } else if (isClaude()) {

            // Special orange tint with a slow pulse
            float pulse = 0.5f + 0.5f * MathUtils.sin(pulseTime * 3f);
            batch.setColor(1f, 0.58f + 0.12f * pulse, 0.22f + 0.08f * pulse, 1f);

        } else if (isShifter()) {

            // Purple tint that shimmers
            float pulse = 0.5f + 0.5f * MathUtils.sin(pulseTime * 2.5f);
            batch.setColor(0.70f + 0.25f * pulse, 0.55f, 1f, 1f);

        } else if (hasFury && hasManaSurge) {

            batch.setColor(0.85f, 0.65f, 1f, 1f);

        } else if (hasFury) {

            // Red tint: extra damage
            batch.setColor(1f, 0.55f, 0.55f, 1f);

        } else if (hasManaSurge) {

            // Blue tint: mana
            batch.setColor(0.55f, 0.72f, 1f, 1f);

        } else {

            batch.setColor(Color.WHITE);
        }

        batch.draw(frame, drawX, drawY, scaledWidth, scaledHeight);

        batch.setColor(Color.WHITE);
    }

    public void drawText(SpriteBatch batch, BitmapFont font) {

        if (batch == null || font == null) {
            return;
        }

        float drawX = animatedX + (WIDTH - WIDTH * scale) / 2f;
        float drawY = animatedY + (HEIGHT - HEIGHT * scale) / 2f;

        // NAME
        font.getData().setScale(1.00f * scale);

        if (isUsed()) font.setColor(Color.GRAY);
        else if (isClaude()) font.setColor(1f, 0.62f, 0.22f, 1f);
        else if (isShifter()) font.setColor(0.82f, 0.65f, 1f, 1f);
        else font.setColor(Color.WHITE);

        font.draw(batch, name, drawX + 15f * scale, drawY + HEIGHT * scale - 18f * scale);

        // USES
        font.getData().setScale(0.68f * scale);

        font.setColor(isUsed() ? Color.DARK_GRAY : new Color(1f, 0.80f, 0.30f, 1f));

        String usesText = isUsed() ? "USED" : "USES: " + usesRemaining;

        font.draw(batch, usesText, drawX + 15f * scale, drawY + HEIGHT * scale - 38f * scale);

        // MOVEMENT TYPE
        font.getData().setScale(0.78f * scale);

        if (isUsed()) font.setColor(Color.DARK_GRAY);
        else if (isClaude()) font.setColor(1f, 0.55f, 0.15f, 1f);
        else if (isShifter()) font.setColor(0.75f, 0.50f, 1f, 1f);
        else font.setColor(0.30f, 0.65f, 0.95f, 1f);

        font.draw(batch, movementType.toString(), drawX + 20f * scale, drawY + 75f * scale);

        // DESCRIPTION
        font.getData().setScale(0.72f * scale);

        font.setColor(isUsed() ? Color.DARK_GRAY : new Color(0.70f, 0.75f, 0.82f, 1f));

        font.draw(batch, getDescription(), drawX + 20f * scale, drawY + 30f * scale);

        // Reset font.
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // =========================================================
    // DESCRIPTION
    // =========================================================

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

            case CLAUDE:
                return "Knight + 1 step";

            case LEAPER:
                return "Jump 2 tiles";

            case SHIFTER:
                return "Now: " + formLabel(currentForm);

            default:
                return "";
        }
    }

    // =========================================================
    // CLICK / TOUCH DETECTION
    // =========================================================

    public boolean isClicked(float touchX, float touchY) {

        if (isUsed()) {
            return false;
        }

        float drawX = animatedX + (WIDTH - WIDTH * scale) / 2f;
        float drawY = animatedY + (HEIGHT - HEIGHT * scale) / 2f;

        float drawWidth = WIDTH * scale;
        float drawHeight = HEIGHT * scale;

        return touchX >= drawX
            && touchX <= drawX + drawWidth
            && touchY >= drawY
            && touchY <= drawY + drawHeight;
    }
}
