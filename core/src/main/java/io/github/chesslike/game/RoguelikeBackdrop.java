package io.github.chesslike.game;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import java.util.Random;

/** Procedural dark-fantasy dungeon backdrop; no external art asset required. */
public final class RoguelikeBackdrop {
    private final Texture stoneTexture;
    private final Random random = new Random(7319L);

    public RoguelikeBackdrop() {
        Pixmap pixmap = new Pixmap(512, 512, Pixmap.Format.RGBA8888);
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                float edge = Math.min(Math.min(x, 511 - x), Math.min(y, 511 - y)) / 256f;
                float noise = random.nextFloat() * .035f;
                float shade = .055f + Math.min(.045f, edge * .045f) + noise;
                pixmap.setColor(shade * .82f, shade * .91f, shade * 1.12f, 1f);
                pixmap.drawPixel(x, y);
            }
        }

        // Large, irregular stone blocks and mortar seams.
        pixmap.setColor(.025f, .032f, .047f, 1f);
        for (int y = 0; y < 512; y += 86) pixmap.drawLine(0, y, 512, y);
        for (int row = 0; row < 6; row++) {
            int offset = (row % 2) * 52;
            for (int x = offset; x < 512; x += 104) {
                pixmap.drawLine(x, row * 86, x, Math.min(512, row * 86 + 86));
            }
        }

        // Sparse cracks catch a faint cold light.
        pixmap.setColor(.12f, .14f, .18f, 1f);
        for (int i = 0; i < 34; i++) {
            int x = 12 + random.nextInt(488), y = 12 + random.nextInt(488);
            int length = 8 + random.nextInt(26);
            pixmap.drawLine(x, y, Math.min(511, x + length), Math.max(0, y - length / 2));
            pixmap.drawLine(Math.min(511, x + length), Math.max(0, y - length / 2),
                Math.min(511, x + length + 7), Math.max(0, y - length / 2 - 8));
        }

        // Faded ritual circles and runic marks around the edges, away from the board.
        pixmap.setColor(.20f, .105f, .055f, 1f);
        pixmap.drawCircle(64, 255, 34);
        pixmap.drawCircle(64, 255, 29);
        pixmap.drawLine(64, 211, 64, 299);
        pixmap.drawLine(20, 255, 108, 255);
        pixmap.drawCircle(448, 255, 34);
        pixmap.drawCircle(448, 255, 29);
        pixmap.drawLine(448, 211, 448, 299);
        pixmap.drawLine(404, 255, 492, 255);

        stoneTexture = new Texture(pixmap);
        stoneTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, float width, float height, float time) {
        batch.begin();
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(stoneTexture, 0f, 0f, width, height);
        batch.end();

        // Subtle ember motes at the perimeter; the playfield stays readable.
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 12; i++) {
            float phase = time * (.22f + (i % 4) * .035f) + i * 1.71f;
            float x = (i % 2 == 0 ? 24f : width - 24f) + (float) Math.sin(phase * .7f) * 13f;
            float y = 24f + (i * 67f + (float) Math.sin(phase) * 18f) % Math.max(1f, height - 48f);
            float alpha = .18f + .18f * (float) Math.sin(phase * 1.8f);
            shapes.setColor(.92f, .39f, .13f, Math.max(.04f, alpha));
            shapes.circle(x, y, 1.5f + (i % 3) * .7f, 10);
        }
        shapes.end();
    }

    public void dispose() {
        stoneTexture.dispose();
    }
}
