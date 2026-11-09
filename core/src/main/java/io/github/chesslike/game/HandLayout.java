package io.github.chesslike.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/** Right-side hand stack layout: card scale and positions. */
public final class HandLayout {

    private static final float STACK_OVERLAP = 0.66f;
    private static final float SIDE_PADDING = 16f;
    private static final float VERTICAL_PADDING = 32f;
    private static final float EDGE_MARGIN = 12f;
    private static final float MAX_SCALE = 1.05f;
    private static final float MIN_SCALE = 0.55f;

    private HandLayout() {
    }

    /** Scale that fits `handSize` cards between the board and the screen edge. */
    public static float scaleFor(int handSize, float screenW, float screenH, float boardRight) {
        int n = Math.max(1, handSize);
        float sideSpace = screenW - boardRight - SIDE_PADDING;
        float widthScale = sideSpace / Card.WIDTH;
        float requiredHeight = Card.HEIGHT * (1f + (n - 1) * STACK_OVERLAP);
        float heightScale = (screenH - VERTICAL_PADDING) / requiredHeight;
        return MathUtils.clamp(Math.min(MAX_SCALE, Math.min(widthScale, heightScale)), MIN_SCALE, MAX_SCALE);
    }

    /** Clean right-side stack: aligned cards with a controlled overlap, never a diagonal fan. */
    public static void apply(Array<Card> hand, float screenW, float screenH, float boardRight) {
        if (hand == null || hand.size == 0) return;

        // Portrait phones need a horizontal hand: a vertical right rail would
        // cover the board and leave no room for touch controls.
        if (screenH > screenW) {
            float gap = 8f;
            float availableW = screenW - EDGE_MARGIN * 2f;
            float scale = Math.min(0.62f, availableW /
                (Card.WIDTH * hand.size + gap * Math.max(0, hand.size - 1)));
            scale = MathUtils.clamp(scale, 0.38f, 0.62f);
            float cw = Card.WIDTH * scale;
            float ch = Card.HEIGHT * scale;
            float stepX = cw + gap;
            float totalW = cw + Math.max(0, hand.size - 1) * stepX;
            float startX = Math.max(EDGE_MARGIN, (screenW - totalW) / 2f);
            float y = 12f;
            for (int i = 0; i < hand.size; i++) {
                Card c = hand.get(i);
                c.setLayoutScale(scale);
                c.setPosition(startX + i * stepX, y);
            }
            return;
        }

        float scale = scaleFor(hand.size, screenW, screenH, boardRight);
        float cw = Card.WIDTH * scale;
        float ch = Card.HEIGHT * scale;
        float stepY = ch * STACK_OVERLAP;
        float totalH = ch + Math.max(0, hand.size - 1) * stepY;
        float startY = Math.max(8f, (screenH - totalH) / 2f);
        float startX = screenW - cw - EDGE_MARGIN;

        if (totalH > screenH - 16f && hand.size > 1) {
            stepY = Math.max(36f, (screenH - 16f - ch) / (hand.size - 1));
            totalH = ch + (hand.size - 1) * stepY;
            startY = Math.max(8f, (screenH - totalH) / 2f);
        }

        for (int i = 0; i < hand.size; i++) {
            Card c = hand.get(i);
            c.setLayoutScale(scale);
            c.setPosition(startX, startY + i * stepY);
        }
    }
}
