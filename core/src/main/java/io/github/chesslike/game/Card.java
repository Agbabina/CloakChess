package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
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

    public static final float WIDTH = 115f;
    public static final float HEIGHT = 155f;

    // =========================================================
    // SOUNDS (drawing and textures live in CardView)
    // =========================================================

    private static Sound selectSound;
    private static Sound hoverSound;

    public static void loadTextures() {

        CardView.loadTextures();

        if (selectSound == null) {
            selectSound = Gdx.audio.newSound(Gdx.files.internal("freesound_community-flipcard-91468.mp3"));
        }

        if (hoverSound == null) {
            hoverSound = Gdx.audio.newSound(Gdx.files.internal("666herohero-click-21156.mp3"));
        }
    }

    public static void disposeTextures() {

        CardView.disposeTextures();

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
    // BALATRO-STYLE MOTION
    // =========================================================

    private static final float ROT_STIFFNESS = 260f;
    private static final float ROT_DAMPING = 8f;
    private static final float HOVER_TILT_MAX = 8f;     // degrees, follows the pointer
    private static final float SELECTED_SLANT = 10f;    // degrees, counter-clockwise
    private static final float IDLE_SWAY = 1.2f;        // degrees
    private static final float FLY_SLANT = 35f;         // matches the slanted card sketch
    private static final float FLY_TIME = 0.5f;
    private static final float SHAKE_DURATION = 0.35f;
    private static final float POP_STIFFNESS = 300f;
    private static final float POP_DAMPING = 10f;
    private static final float POP_KICK = 2.2f;

    private float rotation = 0f;
    private float rotationVel = 0f;
    private float mouseOffsetX = 0f;                    // -1..1 while hovered

    private float flipT = 0f;                           // 0 = front, 1 = back
    private float flipTarget = 0f;

    private float shakeTime = 0f;
    private float shakeOffset = 0f;

    private float popScale = 0f;
    private float popVel = 0f;

    private boolean flying = false;
    private float flyX;
    private float flyY;
    private float flyTimer = 0f;

    private final float idlePhase = MathUtils.random(MathUtils.PI2);

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
    private final java.util.EnumSet<AugmentType> augments = java.util.EnumSet.noneOf(AugmentType.class);

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

    /** Base capture damage for this card's current movement form. */
    public int getAttackDamage() {
        switch (currentForm) {
            case KNIGHT:
            case ROOK:
            case QUEEN:
            case MADROOK:
            case ARCHBISHOP:
                return 2;
            case LEAPER:
                return 3;
            case BISHOP:
            case PAWN:
            case DASH:
            case JESTER:
            case BLINKER:
            case CHAMELEON:
            case COUNCILLOR:
            case CLAUDE:
            case SHIFTER:
            default:
                return 1;
        }
    }

    public boolean isUsed() {
        return usesRemaining <= 0;
    }

    boolean isClaude() {
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

        popVel += POP_KICK;

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
        augments.add(augment.getType());

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

    public boolean hasAugment(AugmentType type) {
        return type != null && augments.contains(type);
    }

    java.util.EnumSet<AugmentType> getAugments() {
        return augments;
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
            || hasEcho
            || !augments.isEmpty();
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
        for (AugmentType type : augments) {
            if (type != AugmentType.FURY && type != AugmentType.MANA_SURGE
                && type != AugmentType.VAMPIRIC && type != AugmentType.GILDED
                && type != AugmentType.AEGIS && type != AugmentType.VENOM
                && type != AugmentType.ECHO) sb.append(type.name()).append(",");
        }
        return sb.toString();
    }

    public void restoreExtraAugments(String code) {
        if (code == null || code.isEmpty()) return;
        for (String part : code.split(",")) {
            try {
                AugmentType type = AugmentType.valueOf(part.trim());
                augments.add(type);
                switch (type) {
                    case FURY: hasFury = true; break;
                    case MANA_SURGE: hasManaSurge = true; break;
                    case VAMPIRIC: hasVampiric = true; break;
                    case GILDED: hasGilded = true; break;
                    case AEGIS: hasAegis = true; break;
                    case VENOM: hasVenom = true; break;
                    case ECHO: hasEcho = true; break;
                    default: break;
                }
            } catch (IllegalArgumentException ignored) {
            }
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

        if (isUsed() || dragging || flying) {
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

    // ---- animation triggers ----

    /** Call every frame while hovered: tilts the card toward the pointer. */
    public void trackPointer(float pointerX) {
        float half = WIDTH * getDrawScale() / 2f;
        mouseOffsetX = half <= 0f ? 0f : MathUtils.clamp((pointerX - getCenterX()) / half, -1f, 1f);
    }

    /** Flip to the other face. */
    public void flip() {
        flipTarget = flipTarget == 0f ? 1f : 0f;
    }

    /** New card: starts face down and flips to the front. */
    public void dealFlipIn() {
        flipT = 1f;
        flipTarget = 0f;
    }

    /** New card: starts at (startX, startY), slanted and small, and springs to its slot. */
    public void dealFrom(float startX, float startY) {
        animatedX = startX;
        animatedY = startY;
        initialized = true;
        rotation = 25f;
        rotationVel = 0f;
        scale = layoutScale * 0.6f;
    }

    /** Invalid action: quick sideways wobble. */
    public void shake() {
        shakeTime = SHAKE_DURATION;
        rotationVel += 140f;
    }

    /** Pop, then fly to (targetX, targetY) at the 35 degree slant while shrinking. */
    public void popAndFly(float targetX, float targetY) {
        popVel += POP_KICK;
        flying = true;
        flyTimer = 0f;
        flyX = targetX;
        flyY = targetY;
        selected = false;
        hovered = false;
        dragging = false;
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isFlightDone() {
        return flying && flyTimer >= FLY_TIME;
    }

    public void update(float delta) {

        pulseTime += delta;
        float dt = Math.min(delta, 1f / 30f);

        float targetX = screenX;
        float targetY = screenY;

        if (flying) {
            targetX = flyX;
            targetY = flyY;
            flyTimer += delta;
        }

        // While dragging, do not add the normal selected lift
        // because the card is already following the pointer.
        if (!dragging && !isUsed() && !flying) {

            if (selected) {
                targetY += SELECTED_LIFT;

            } else if (hovered) {
                targetY += HOVER_LIFT;
            }
        }

        // SCALE
        if (flying) {
            targetScale = layoutScale * 0.25f;

        } else if (isUsed()) {
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
        float amount = Math.min(delta * (flying ? 9f : ANIMATION_SPEED), 1f);

        // Don't interpolate the dragged card.
        // It should stay directly underneath the finger/mouse.
        if (!dragging) {
            animatedX = MathUtils.lerp(animatedX, targetX, amount);
            animatedY = MathUtils.lerp(animatedY, targetY, amount);
        }

        scale = MathUtils.lerp(scale, targetScale, amount);

        // ROTATION (spring toward a target angle)
        float rotTarget;
        if (flying) {
            rotTarget = FLY_SLANT;
        } else if (hovered) {
            rotTarget = -mouseOffsetX * HOVER_TILT_MAX;
        } else if (selected || dragging) {
            rotTarget = SELECTED_SLANT;
        } else {
            rotTarget = MathUtils.sin(pulseTime * 1.6f + idlePhase) * IDLE_SWAY;
        }
        rotationVel += (rotTarget - rotation) * ROT_STIFFNESS * dt;
        rotationVel *= Math.max(0f, 1f - ROT_DAMPING * dt);
        rotation += rotationVel * dt;

        // POP (scale spring around 0)
        popVel += (0f - popScale) * POP_STIFFNESS * dt;
        popVel *= Math.max(0f, 1f - POP_DAMPING * dt);
        popScale += popVel * dt;

        // SHAKE
        if (shakeTime > 0f) {
            shakeTime = Math.max(0f, shakeTime - delta);
            shakeOffset = MathUtils.sin(shakeTime * 60f) * 10f * (shakeTime / SHAKE_DURATION);
        } else {
            shakeOffset = 0f;
        }

        // FLIP
        flipT = MathUtils.lerp(flipT, flipTarget, 1f - (float) Math.exp(-12f * delta));
        if (Math.abs(flipT - flipTarget) < 0.002f) flipT = flipTarget;
    }

    // Values CardView needs to draw this card
    float getDrawScale() {
        return scale * (1f + popScale);
    }

    float getCenterX() {
        return animatedX + WIDTH / 2f + shakeOffset;
    }

    float getCenterY() {
        return animatedY + HEIGHT / 2f;
    }

    float getDrawX() {
        return getCenterX() - WIDTH * getDrawScale() / 2f;
    }

    float getDrawY() {
        return getCenterY() - HEIGHT * getDrawScale() / 2f;
    }

    float getRotation() {
        return rotation;
    }

    float getFlipScaleX() {
        return Math.abs(MathUtils.cos(MathUtils.PI * flipT));
    }

    boolean isShowingBack() {
        return flipT > 0.5f;
    }

    float getPulseTime() {
        return pulseTime;
    }

    // =========================================================
    // DRAW METHODS (delegate to CardView)
    // =========================================================

    public void drawShape(SpriteBatch batch) {
        CardView.drawFrame(this, batch);
    }

    public void drawText(SpriteBatch batch, BitmapFont font) {
        CardView.drawText(this, batch, font);
    }

    // =========================================================
    // DESCRIPTION
    // =========================================================

    String getDescription() {

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
    // CLICK / TOUCH DETECTION (accounts for the card's tilt)
    // =========================================================

    public boolean isClicked(float touchX, float touchY) {

        if (isUsed() || flying) {
            return false;
        }

        float s = getDrawScale();
        float dx = touchX - getCenterX();
        float dy = touchY - getCenterY();

        float rad = -rotation * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);
        float localX = dx * cos - dy * sin;
        float localY = dx * sin + dy * cos;

        return Math.abs(localX) <= WIDTH * s / 2f
            && Math.abs(localY) <= HEIGHT * s / 2f;
    }
}
