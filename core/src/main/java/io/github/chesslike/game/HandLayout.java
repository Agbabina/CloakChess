package io.github.chesslike.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

/**
 * Compact bottom-right card dock. Only the current page is positioned;
 * Main renders at most three cards and the cycle control reveals the next page.
 */
public final class HandLayout {

    private static final float EDGE_MARGIN = 18f;
    private static final float CARD_GAP = 12f;
    private static final float MAX_SCALE = 0.82f;
    private static final float MIN_SCALE = 0.48f;

    private HandLayout() {
    }

    public static void apply(Array<Card> hand, float screenW, float screenH,
                             float boardRight, int visibleStart, int visibleCount) {
        if (hand == null || hand.size == 0 || visibleCount <= 0) return;

        int start = MathUtils.clamp(visibleStart, 0, hand.size);
        int end = Math.min(hand.size, start + visibleCount);
        int count = end - start;
        if (count <= 0) return;

        // A three-card row stays readable on desktop and scales down on narrow phones.
        float gap = CARD_GAP;
        float availableW = Math.max(1f, screenW - EDGE_MARGIN * 2f);
        float scale = Math.min(MAX_SCALE,
            (availableW - gap * Math.max(0, count - 1)) / (Card.WIDTH * count));
        scale = MathUtils.clamp(scale, MIN_SCALE, MAX_SCALE);

        // Portrait screens get a slightly smaller, centered dock so the cards
        // don't cover the board's important lower controls.
        if (screenH > screenW) {
            scale = Math.min(scale, 0.58f);
            gap = 7f;
        }

        float cardW = Card.WIDTH * scale;
        float cardH = Card.HEIGHT * scale;
        float stepX = cardW + gap;
        float totalW = cardW + Math.max(0, count - 1) * stepX;
        float startX;

        if (screenH > screenW) {
            startX = Math.max(EDGE_MARGIN, (screenW - totalW) * 0.5f);
        } else {
            // Anchor the hand at the bottom-right, but keep it inside the viewport.
            startX = Math.max(EDGE_MARGIN, screenW - totalW - EDGE_MARGIN);
        }

        float y = Math.max(EDGE_MARGIN, EDGE_MARGIN + cardH * 0.08f);
        for (int i = start; i < end; i++) {
            Card card = hand.get(i);
            card.setLayoutScale(scale);
            card.setPosition(startX + (i - start) * stepX, y);
        }
    }
}
