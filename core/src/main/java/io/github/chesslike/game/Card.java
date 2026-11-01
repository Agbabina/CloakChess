package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
        HUNTER,
    }

    public enum Rarity {
        COMMON("COMMON", 0.81f, 0.85f, 0.89f),
        UNCOMMON("UNCOMMON", 0.49f, 1f, 0.84f),
        RARE("RARE", 0.61f, 0.78f, 1f),
        LEGENDARY("LEGENDARY", 1f, 0.69f, 0.18f);

        public final String label;
        public final Color color;

        Rarity(String label, float r, float g, float b) {
            this.label = label;
            this.color = new Color(r, g, b, 1f);
        }

        public String frameFile() {
            return "cards/card_" + name().toLowerCase(java.util.Locale.ROOT) + ".png";
        }
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

    // The card art is 256x328 (a 64x82 pixel grid at 4x). It is drawn at half
    // size, so every art pixel is exactly 2 base units wide at layoutScale 1.
    public static final float BASE_WIDTH = 128f;
    public static final float BASE_HEIGHT = 164f;
    private static final float GRID_W = 64f;
    private static final float GRID_H = 82f;

    // =========================================================
    // CARD TEXTURES
    // =========================================================

    private static Texture frameStandard;   // legacy fallback if cards/ frames are missing
    private static Texture frameAugmented;
    private static final ObjectMap<Rarity, Texture> rarityFrames = new ObjectMap<>();
    // Artwork per card type: cards/art_<type>.png wins, otherwise a piece sprite is reused.
    private static final ObjectMap<MovementType, Texture> cardArt = new ObjectMap<>();
    private static final GlyphLayout layout = new GlyphLayout();
    // Optional per-augment artwork. Missing files are safely skipped so builds still run.
    private static final ObjectMap<AugmentType, Texture> augmentIcons = new ObjectMap<>();

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


        for (Rarity r : Rarity.values()) {
            if (rarityFrames.containsKey(r)) continue;
            try {
                if (Gdx.files.internal(r.frameFile()).exists()) {
                    Texture t = new Texture(Gdx.files.internal(r.frameFile()));
                    t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    rarityFrames.put(r, t);
                }
            } catch (Exception ignored) {
                // Falls back to the legacy frame so the game still runs.
            }
        }

        for (MovementType type : MovementType.values()) {
            if (cardArt.containsKey(type)) continue;
            String custom = "cards/art_" + type.name().toLowerCase(java.util.Locale.ROOT) + ".png";
            String[] candidates = { custom, defaultArtFile(type) };
            for (String file : candidates) {
                try {
                    if (file != null && Gdx.files.internal(file).exists()) {
                        Texture t = new Texture(Gdx.files.internal(file));
                        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                        cardArt.put(type, t);
                        break;
                    }
                } catch (Exception ignored) {
                    // Try the next candidate.
                }
            }
        }

        // Icons live beside the card frames: icon_augment_soul_eater.png, etc.
        for (AugmentType type : AugmentType.values()) {
            if (augmentIcons.containsKey(type)) continue;
            String file = "icon_augment_" + type.name().toLowerCase(java.util.Locale.ROOT) + ".png";
            try {
                if (Gdx.files.internal(file).exists()) {
                    Texture icon = new Texture(Gdx.files.internal(file));
                    icon.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                    augmentIcons.put(type, icon);
                }
            } catch (Exception ignored) {
                // Artwork is optional; the augment name remains visible in the card text.
            }
        }

        if (selectSound == null) {
            selectSound = Gdx.audio.newSound(Gdx.files.internal("freesound_community-flipcard-91468.mp3"));
        }

        if (hoverSound == null) {
            hoverSound = Gdx.audio.newSound(Gdx.files.internal("666herohero-click-21156.mp3"));
        }
    }

    /** Existing sprites reused as card artwork until dedicated art_<type>.png files exist. */
    private static String defaultArtFile(MovementType type) {
        switch (type) {
            case KNIGHT:     return "w_knight_png_256px.png";
            case BISHOP:     return "w_bishop_png_256px.png";
            case ROOK:       return "w_rook_png_256px.png";
            case QUEEN:      return "w_queen_png_256px.png";
            case PAWN:       return "w_pawn_png_256px.png";
            case DASH:       return "pixelarrow.png";
            case MADROOK:    return "mad_rook.png";
            case JESTER:     return "jester.png";
            case BLINKER:    return "blinker.png";
            case CHAMELEON:  return "chameleon.png";
            case SHIFTER:    return "chameleon.png";
            case HUNTER:     return "hunter.png";
            case ARCHBISHOP: return "w_bishop_png_256px.png";
            case COUNCILLOR: return "w_queen_png_256px.png";
            case LEAPER:     return "w_knight_png_256px.png";
            case CLAUDE:     return "w_king_png_256px.png";
            default:         return null;
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

        for (Texture icon : augmentIcons.values()) icon.dispose();
        augmentIcons.clear();

        for (Texture t : rarityFrames.values()) t.dispose();
        rarityFrames.clear();

        for (Texture t : cardArt.values()) t.dispose();
        cardArt.clear();

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

            case HUNTER:
                return new HunterRules();

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

    /** Drives which frame is drawn. Edit this table to rebalance card rarity. */
    public Rarity getRarity() {
        switch (movementType) {
            case KNIGHT:
            case BISHOP:
            case ROOK:
            case PAWN:
            case DASH:
                return Rarity.COMMON;
            case QUEEN:
            case LEAPER:
            case JESTER:
            case BLINKER:
                return Rarity.UNCOMMON;
            case MADROOK:
            case CHAMELEON:
            case ARCHBISHOP:
            case COUNCILLOR:
            case HUNTER:
                return Rarity.RARE;
            case CLAUDE:
            case SHIFTER:
            default:
                return Rarity.LEGENDARY;
        }
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
        if (!initialized) this.scale = layoutScale;
    }

    /** Width/height of the slot this card occupies in the hand (before hover/select scaling). */
    public float getSlotWidth() { return BASE_WIDTH * layoutScale; }
    public float getSlotHeight() { return BASE_HEIGHT * layoutScale; }

    private float drawLeft() { return animatedX + (getSlotWidth() - BASE_WIDTH * scale) / 2f; }
    private float drawBottom() { return animatedY + (getSlotHeight() - BASE_HEIGHT * scale) / 2f; }

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

        if (batch == null) {
            return;
        }

        Texture frame = rarityFrames.get(getRarity());
        if (frame == null) frame = frameStandard;
        if (frame == null) return;

        float scaledWidth = BASE_WIDTH * scale;
        float scaledHeight = BASE_HEIGHT * scale;

        float drawX = drawLeft();
        float drawY = drawBottom();

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

    // Draws one line of text inside a box, shrinking it until it fits, centred on (cx, cy).
    private static void drawFitted(SpriteBatch batch, BitmapFont font, String text, Color color,
                                   float cx, float cy, float maxW, float maxH,
                                   float startScale, float minScale) {
        float s = startScale;
        font.getData().setScale(s);
        layout.setText(font, text);
        while ((layout.width > maxW || layout.height > maxH) && s > minScale) {
            s *= 0.92f;
            font.getData().setScale(s);
            layout.setText(font, text);
        }
        font.setColor(color);
        font.draw(batch, text, cx - layout.width / 2f, cy + layout.height / 2f);
    }

    public void drawText(SpriteBatch batch, BitmapFont font) {

        if (batch == null || font == null) {
            return;
        }

        final float u = (BASE_WIDTH / GRID_W) * scale;   // size of one art pixel on screen
        final float left = drawLeft();
        final float bottom = drawBottom();
        final boolean used = isUsed();
        final Rarity rarity = getRarity();

        // ---- ARTWORK (window is grid x 9..54, y 22..45 measured from the top)
        Texture art = cardArt.get(movementType == MovementType.SHIFTER ? currentForm : movementType);
        if (art == null) art = cardArt.get(movementType);
        if (art != null) {
            float side = 22f * u;
            batch.setColor(used ? new Color(0.45f, 0.45f, 0.45f, 1f) : Color.WHITE);
            batch.draw(art, left + 32f * u - side / 2f, bottom + (GRID_H - 34f) * u - side / 2f, side, side);
            batch.setColor(Color.WHITE);
        }

        // ---- AUGMENT ICONS (top-right corner of the art window)
        float iconSize = 8f * u;
        float iconRight = left + 54f * u - iconSize;
        float iconY = bottom + (GRID_H - 22f) * u - iconSize - u;
        int shown = 0;
        for (AugmentType type : augments) {
            if (shown >= 4) break;
            Texture icon = augmentIcons.get(type);
            if (icon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(icon, iconRight - shown * (iconSize + u), iconY, iconSize, iconSize);
            }
            shown++;
        }
        batch.setColor(Color.WHITE);

        // ---- NAME (panel inner: grid x 8..46, y 7..14)
        Color nameColor;
        if (used) nameColor = Color.GRAY;
        else if (isClaude()) nameColor = new Color(1f, 0.62f, 0.22f, 1f);
        else if (isShifter()) nameColor = new Color(0.82f, 0.65f, 1f, 1f);
        else nameColor = Color.WHITE;
        drawFitted(batch, font, name, nameColor,
            left + 27f * u, bottom + (GRID_H - 10.5f) * u, 37f * u, 7f * u, 0.9f * scale, 0.25f * scale);

        // ---- USES inside the cost gem (centre grid 54.5, 9.5)
        Color usesColor = used ? Color.DARK_GRAY : new Color(0.92f, 1f, 0.96f, 1f);
        drawFitted(batch, font, String.valueOf(Math.max(0, usesRemaining)), usesColor,
            left + 54.5f * u, bottom + (GRID_H - 9.5f) * u, 5f * u, 5f * u, 0.8f * scale, 0.25f * scale);

        // ---- RARITY label (banner inner: grid x 20..43, y 53..57)
        drawFitted(batch, font, rarity.label, used ? Color.DARK_GRAY : rarity.color,
            left + 31.5f * u, bottom + (GRID_H - 55f) * u, 22f * u, 4f * u, 0.5f * scale, 0.15f * scale);

        // ---- DAMAGE + DESCRIPTION (box inner: grid x 8..55, y 62..76)
        float boxX = left + 8f * u;
        float boxTop = bottom + (GRID_H - 62.5f) * u;
        float boxW = 47f * u;
        float boxH = 13f * u;
        String desc = getDescription();
        String dmg = "DMG " + getAttackDamage();

        float s = 0.55f * scale;
        float dmgH, descH = 0f;
        while (true) {
            font.getData().setScale(s);
            layout.setText(font, dmg);
            dmgH = layout.height;
            if (desc.isEmpty()) { descH = 0f; }
            else {
                layout.setText(font, desc, Color.WHITE, boxW, com.badlogic.gdx.utils.Align.left, true);
                descH = layout.height;
            }
            float total = dmgH + (desc.isEmpty() ? 0f : descH + 2.5f * scale);
            if (total <= boxH || s <= 0.15f * scale) break;
            s *= 0.93f;
        }
        font.getData().setScale(s);
        font.setColor(used ? Color.DARK_GRAY : new Color(1f, 0.42f, 0.30f, 1f));
        font.draw(batch, dmg, boxX, boxTop);
        if (!desc.isEmpty()) {
            font.setColor(used ? Color.DARK_GRAY : new Color(0.78f, 0.82f, 0.90f, 1f));
            font.draw(batch, desc, boxX, boxTop - dmgH - 2.5f * scale, boxW,
                com.badlogic.gdx.utils.Align.left, true);
        }

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

            case HUNTER:
                return "King step + knight leap";

            case MADROOK:
                return "Erratic lines";

            case JESTER:
                return "Cycles move shapes";

            case BLINKER:
                return "Blinks to random tiles";

            case CHAMELEON:
                return "Copies a piece";

            case ARCHBISHOP:
                return "Bishop + knight";

            case COUNCILLOR:
                return "Zebra: 3 + 2 leap";

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

        float drawX = drawLeft();
        float drawY = drawBottom();

        float drawWidth = BASE_WIDTH * scale;
        float drawHeight = BASE_HEIGHT * scale;

        return touchX >= drawX
            && touchX <= drawX + drawWidth
            && touchY >= drawY
            && touchY <= drawY + drawHeight;
    }
}
