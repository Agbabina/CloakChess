package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ObjectMap;

/** All card drawing lives here: frame textures, tints, flip/tilt, text and augment icons. */
public final class CardView {

    private static Texture frameStandard;
    private static Texture frameAugmented;
    private static Texture backTexture;                 // optional: card_back.png
    private static TextureRegion standardRegion;
    private static TextureRegion augmentedRegion;
    private static TextureRegion backRegion;
    // Optional per-augment artwork. Missing files are safely skipped so builds still run.
    private static final ObjectMap<AugmentType, Texture> augmentIcons = new ObjectMap<>();
    // Full illustrated 256x328 card faces. Missing variants fall back to the frame.
    private static final ObjectMap<Card.MovementType, TextureRegion> cardFaces = new ObjectMap<>();

    private static final Color USES_COLOR = new Color(1f, 0.80f, 0.30f, 1f);
    private static final Color DMG_COLOR = new Color(1f, 0.42f, 0.30f, 1f);
    private static final Color DESC_COLOR = new Color(0.70f, 0.75f, 0.82f, 1f);
    private static final Color AUG_TEXT_COLOR = new Color(1f, 0.82f, 0.42f, 1f);

    private static final Matrix4 cardTransform = new Matrix4();
    private static final Matrix4 savedTransform = new Matrix4();

    private CardView() {
    }

    // =========================================================
    // TEXTURES
    // =========================================================

    public static void loadTextures() {

        if (frameStandard == null) {
            frameStandard = new Texture(Gdx.files.internal("card_frame_standard.png"));
            frameStandard.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            standardRegion = new TextureRegion(frameStandard);
        }

        if (frameAugmented == null) {
            frameAugmented = new Texture(Gdx.files.internal("card_frame_augmented.png"));
            frameAugmented.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            augmentedRegion = new TextureRegion(frameAugmented);
        }

        // Optional card back used by the flip animation.
        if (backTexture == null && Gdx.files.internal("card_back.png").exists()) {
            backTexture = new Texture(Gdx.files.internal("card_back.png"));
            backTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            backRegion = new TextureRegion(backTexture);
        }

        // Full card-face artwork lives at assets/card_face_<movement>.png.
        Card.MovementType[] faceTypes = {Card.MovementType.KNIGHT, Card.MovementType.BISHOP,
            Card.MovementType.ROOK, Card.MovementType.QUEEN, Card.MovementType.PAWN,
            Card.MovementType.DASH, Card.MovementType.CLAUDE};
        for (Card.MovementType type : faceTypes) {
            if (cardFaces.containsKey(type)) continue;
            String file = "card_face_" + type.name().toLowerCase(java.util.Locale.ROOT) + ".png";
            if (Gdx.files.internal(file).exists()) {
                Texture face = new Texture(Gdx.files.internal(file));
                face.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                cardFaces.put(type, new TextureRegion(face));
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
    }

    public static void disposeTextures() {

        if (frameStandard != null) {
            frameStandard.dispose();
            frameStandard = null;
            standardRegion = null;
        }

        if (frameAugmented != null) {
            frameAugmented.dispose();
            frameAugmented = null;
            augmentedRegion = null;
        }

        if (backTexture != null) {
            backTexture.dispose();
            backTexture = null;
            backRegion = null;
        }

        for (Texture icon : augmentIcons.values()) icon.dispose();
        augmentIcons.clear();
        for (TextureRegion face : cardFaces.values()) face.getTexture().dispose();
        cardFaces.clear();
    }

    // =========================================================
    // FRAME
    // =========================================================

    public static void drawFrame(Card card, SpriteBatch batch) {

        if (batch == null || standardRegion == null) {
            return;
        }

        float s = card.getDrawScale();
        float w = Card.WIDTH * s;
        float h = Card.HEIGHT * s;
        float cx = card.getCenterX();
        float cy = card.getCenterY();
        float sx = card.getFlipScaleX();
        float shade = 1f - 0.45f * (1f - sx);

        TextureRegion face = cardFaces.get(card.getMovementType());
        TextureRegion region;
        if (card.isShowingBack()) {
            region = backRegion != null ? backRegion : standardRegion;
            float dark = backRegion != null ? 1f : 0.45f;
            batch.setColor(shade * dark, shade * dark, shade * dark, 1f);
        } else {
            // Use the new illustrated face whenever one exists; retain frame fallback
            // for special movement types that do not yet have dedicated artwork.
            region = face != null ? face : standardRegion;
            applyTint(card, batch);
            Color c = batch.getColor();
            batch.setColor(c.r * shade, c.g * shade, c.b * shade, c.a);
        }

        // Enchanted cards radiate a living aura behind the frame.
        // The pulse is subtle at rest and brightens while the card is selected.
        if (!card.isShowingBack() && card.hasAnyAugment() && !card.isUsed() && augmentedRegion != null) {
            float pulse = 0.5f + 0.5f * MathUtils.sin(card.getPulseTime() * 3.2f);
            float glowScale = 1.035f + pulse * 0.025f;
            float glowW = w * glowScale;
            float glowH = h * glowScale;
            float glowAlpha = 0.16f + pulse * 0.12f;

            if (card.hasFury()) {
                batch.setColor(1f, 0.20f, 0.12f, glowAlpha);
            } else if (card.hasManaSurge()) {
                batch.setColor(0.18f, 0.55f, 1f, glowAlpha);
            } else {
                // General enchantment aura: arcane violet with a warm gold core.
                batch.setColor(0.62f, 0.28f, 1f, glowAlpha);
            }

            batch.draw(augmentedRegion,
                cx - glowW / 2f, cy - glowH / 2f,
                glowW / 2f, glowH / 2f,
                glowW, glowH, sx, 1f, card.getRotation());
        }

        batch.draw(region, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, sx, 1f, card.getRotation());
        batch.setColor(Color.WHITE);
    }

    private static void applyTint(Card card, SpriteBatch batch) {

        boolean claude = card.isClaude();

        if (card.isUsed()) {

            batch.setColor(0.55f, 0.55f, 0.55f, 1f);

        } else if (card.isDragging()) {

            // Dragged card gets a bright highlight.
            if (claude) batch.setColor(1f, 0.72f, 0.30f, 1f);
            else batch.setColor(1f, 0.90f, 0.55f, 1f);

        } else if (card.isSelected()) {

            if (claude) batch.setColor(1f, 0.70f, 0.28f, 1f);
            else batch.setColor(1f, 0.85f, 0.55f, 1f);

        } else if (card.isHovered()) {

            if (claude) batch.setColor(1f, 0.80f, 0.45f, 1f);
            else batch.setColor(0.75f, 0.92f, 1f, 1f);

        } else if (claude) {

            // Special orange tint with a slow pulse
            float pulse = 0.5f + 0.5f * MathUtils.sin(card.getPulseTime() * 3f);
            batch.setColor(1f, 0.58f + 0.12f * pulse, 0.22f + 0.08f * pulse, 1f);

        } else if (card.isShifter()) {

            // Purple tint that shimmers
            float pulse = 0.5f + 0.5f * MathUtils.sin(card.getPulseTime() * 2.5f);
            batch.setColor(0.70f + 0.25f * pulse, 0.55f, 1f, 1f);

        } else if (card.hasFury() && card.hasManaSurge()) {

            batch.setColor(0.85f, 0.65f, 1f, 1f);

        } else if (card.hasFury()) {

            // Red tint: extra damage
            batch.setColor(1f, 0.55f, 0.55f, 1f);

        } else if (card.hasManaSurge()) {

            // Blue tint: mana
            batch.setColor(0.55f, 0.72f, 1f, 1f);

        } else {

            batch.setColor(Color.WHITE);
        }
    }

    // =========================================================
    // TEXT
    // =========================================================

    public static void drawText(Card card, SpriteBatch batch, BitmapFont font) {

        if (batch == null || font == null) {
            return;
        }

        float sx = card.getFlipScaleX();
        if (card.isShowingBack() || sx < 0.12f) {
            return;                                     // no text on the back or edge-on
        }

        float scale = card.getDrawScale();
        float drawX = card.getDrawX();
        float drawY = card.getDrawY();
        float cx = card.getCenterX();
        float cy = card.getCenterY();
        boolean used = card.isUsed();
        boolean claude = card.isClaude();
        boolean shifter = card.isShifter();

        // Rotate / squash the text with the card.
        savedTransform.set(batch.getTransformMatrix());
        cardTransform.set(savedTransform)
            .translate(cx, cy, 0f)
            .rotate(0f, 0f, 1f, card.getRotation())
            .scale(sx, 1f, 1f)
            .translate(-cx, -cy, 0f);
        batch.setTransformMatrix(cardTransform);

        // NAME
        font.getData().setScale(1.00f * scale);

        if (used) font.setColor(Color.GRAY);
        else if (claude) font.setColor(1f, 0.62f, 0.22f, 1f);
        else if (shifter) font.setColor(0.82f, 0.65f, 1f, 1f);
        else font.setColor(Color.WHITE);

        font.draw(batch, card.getName(), drawX + 15f * scale, drawY + Card.HEIGHT * scale - 18f * scale);

        // USES
        font.getData().setScale(0.68f * scale);
        font.setColor(used ? Color.DARK_GRAY : USES_COLOR);

        String usesText = used ? "USED" : "USES: " + card.getUsesRemaining();
        font.draw(batch, usesText, drawX + 15f * scale, drawY + Card.HEIGHT * scale - 38f * scale);

        // MOVEMENT TYPE
        font.getData().setScale(0.78f * scale);

        if (used) font.setColor(Color.DARK_GRAY);
        else if (claude) font.setColor(1f, 0.55f, 0.15f, 1f);
        else if (shifter) font.setColor(0.75f, 0.50f, 1f, 1f);
        else font.setColor(0.30f, 0.65f, 0.95f, 1f);

        font.draw(batch, card.getMovementType().toString(), drawX + 20f * scale, drawY + 75f * scale);

        drawAugmentStrip(card, batch, font, drawX, drawY, scale);
        font.getData().setScale(0.78f * scale);

        // ATTACK DAMAGE
        font.getData().setScale(0.70f * scale);
        font.setColor(used ? Color.DARK_GRAY : DMG_COLOR);
        font.draw(batch, "DMG " + card.getAttackDamage(), drawX + 20f * scale, drawY + 55f * scale);

        // DESCRIPTION
        font.getData().setScale(0.72f * scale);
        font.setColor(used ? Color.DARK_GRAY : DESC_COLOR);
        font.draw(batch, card.getDescription(), drawX + 20f * scale, drawY + 30f * scale);

        // Reset font and transform.
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.setTransformMatrix(savedTransform);
    }

    // Augment icons form a compact, readable strip at the card's upper-right.
    // Each icon is optional; text fallback keeps augments discoverable if artwork is absent.
    private static void drawAugmentStrip(Card card, SpriteBatch batch, BitmapFont font,
                                         float drawX, float drawY, float scale) {
        float iconSize = 18f * scale;
        float iconX = drawX + Card.WIDTH * scale - iconSize - 9f * scale;
        float iconY = drawY + Card.HEIGHT * scale - 62f * scale;
        int shown = 0;
        for (AugmentType type : card.getAugments()) {
            if (shown >= 4) break;
            Texture icon = augmentIcons.get(type);
            if (icon != null) {
                batch.setColor(Color.WHITE);
                batch.draw(icon, iconX - shown * (iconSize + 2f * scale), iconY, iconSize, iconSize);
            } else if (shown == 0) {
                font.getData().setScale(0.38f * scale);
                font.setColor(AUG_TEXT_COLOR);
                font.draw(batch, "AUG", drawX + Card.WIDTH * scale - 32f * scale, drawY + Card.HEIGHT * scale - 62f * scale);
            }
            shown++;
        }
    }
}
