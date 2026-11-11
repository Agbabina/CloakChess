package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;

/**
 * A deterministic, run-start King Enchantment draft. Every option is visible;
 * the player chooses one instead of rolling random rewards.
 */
public final class KingEnchantmentScreen {
    private final Rectangle[] options = new Rectangle[KingEnchantmentSystem.Enchantment.values().length];
    private final Rectangle closeButton = new Rectangle();
    private boolean visible;

    public KingEnchantmentScreen() {
        for (int i = 0; i < options.length; i++) options[i] = new Rectangle();
    }

    public void show() { visible = true; }
    public void hide() { visible = false; }
    public boolean isVisible() { return visible; }

    private Rectangle layoutPanel() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float pw = Math.min(780f, w - 28f), ph = Math.min(560f, h - 28f);
        float px = (w - pw) / 2f, py = (h - ph) / 2f;
        float gap = 12f, cardW = (pw - 44f - gap) / 2f, cardH = Math.min(112f, (ph - 180f) / 3f);
        float startY = py + ph - 112f - cardH;
        for (int i = 0; i < options.length; i++) {
            int col = i % 2, row = i / 2;
            options[i].set(px + 14f + col * (cardW + gap), startY - row * (cardH + 10f), cardW, cardH);
        }
        closeButton.set(px + pw - 150f, py + 7f, 136f, 32f);
        return new Rectangle(px, py, pw, ph);
    }

    /** Returns the chosen enchantment; null means no new selection was made. */
    public KingEnchantmentSystem.Enchantment handleClick(float x, float y, KingEnchantmentSystem system) {
        if (!visible) return null;
        Rectangle panel = layoutPanel();
        if (!panel.contains(x, y)) return null;
        if (!system.getActiveEnchantments().isEmpty()) {
            if (closeButton.contains(x, y)) visible = false;
            return null;
        }
        KingEnchantmentSystem.Enchantment[] all = KingEnchantmentSystem.Enchantment.values();
        for (int i = 0; i < options.length; i++) {
            if (options[i].contains(x, y)) {
                visible = false;
                return all[i];
            }
        }
        return null;
    }

    public void render(SpriteBatch batch, ShapeRenderer shapes, BitmapFont font, KingEnchantmentSystem system) {
        if (!visible) return;
        Rectangle panel = layoutPanel();
        boolean chosen = !system.getActiveEnchantments().isEmpty();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, .82f);
        shapes.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapes.setColor(.045f, .055f, .09f, .99f);
        shapes.rect(panel.x, panel.y, panel.width, panel.height);
        shapes.setColor(.95f, .76f, .27f, 1f);
        shapes.rect(panel.x, panel.y + panel.height - 4f, panel.width, 4f);
        for (int i = 0; i < options.length; i++) {
            boolean active = system.has(KingEnchantmentSystem.Enchantment.values()[i]);
            shapes.setColor(active ? .20f : .105f, active ? .30f : .13f, active ? .22f : .20f, 1f);
            shapes.rect(options[i].x, options[i].y, options[i].width, options[i].height);
            shapes.setColor(active ? .45f : .30f, active ? .88f : .35f, active ? .52f : .48f, 1f);
            shapes.rect(options[i].x, options[i].y + options[i].height - 3f, options[i].width, 3f);
        }
        shapes.setColor(chosen ? .20f : .48f, chosen ? .22f : .35f, chosen ? .28f : .12f, 1f);
        shapes.rect(closeButton.x, closeButton.y, closeButton.width, closeButton.height);
        shapes.end();

        batch.begin();
        font.setColor(.98f, .78f, .30f, 1f);
        font.getData().setScale(1.05f);
        font.draw(batch, "THE KING'S ENCHANTMENTS", panel.x + 18f, panel.y + panel.height - 22f);
        font.setColor(.78f, .82f, .91f, 1f);
        font.getData().setScale(.48f);
        font.draw(batch, chosen
            ? "YOUR RUN'S CHOSEN OATH. Review its effect below."
            : "CHOOSE ONE OATH. ALL SIX ARE SHOWN — NO RANDOM ROLLS.",
            panel.x + 20f, panel.y + panel.height - 54f);

        KingEnchantmentSystem.Enchantment[] all = KingEnchantmentSystem.Enchantment.values();
        for (int i = 0; i < all.length; i++) {
            KingEnchantmentSystem.Enchantment e = all[i];
            Rectangle r = options[i];
            boolean active = system.has(e);
            font.getData().setScale(.68f);
            font.setColor(active ? new Color(.55f, 1f, .65f, 1f) : Color.WHITE);
            font.draw(batch, e.getDisplayName() + (active ? "  [ACTIVE]" : ""),
                r.x + 12f, r.y + r.height - 16f, r.width - 24f, Align.left, false);
            font.getData().setScale(.43f);
            font.setColor(.78f, .82f, .90f, 1f);
            font.draw(batch, e.getDescription(), r.x + 12f, r.y + r.height - 43f,
                r.width - 24f, Align.left, true);
            font.getData().setScale(.34f);
            font.setColor(.95f, .72f, .28f, 1f);
            font.draw(batch, e.getRarity().name(), r.x + 12f, r.y + 13f);
        }
        font.getData().setScale(.46f);
        font.setColor(Color.WHITE);
        font.draw(batch, chosen ? "CLOSE" : "PICK AN OATH TO BEGIN",
            closeButton.x + 8f, closeButton.y + 21f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}
