package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

public class ShopScreen {

    public static final int NONE = -2;
    public static final int LEAVE = -1;
    public static final int REROLL = -3;

    public enum Kind { POTION, SPELL, RELIC, AMMO, SELL, STABILIZE, PURIFY }

    public static class Item {

        public final Kind kind;
        public final String name;
        public final String description;
        public final int cost;      // for SELL this is the payout
        public final Spell spell;   // only for SPELL
        public final Relic relic;   // only for RELIC
        public boolean sold;

        public Item(
            Kind kind,
            String name,
            String description,
            int cost,
            Spell spell,
            Relic relic
        ) {
            this.kind = kind;
            this.name = name;
            this.description = description;
            this.cost = cost;
            this.spell = spell;
            this.relic = relic;
        }
    }

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private boolean visible = false;
    private int rerollCost = 15;

    private final Array<Item> items = new Array<>();
    private final Array<Rectangle> itemRects = new Array<>();

    private final Rectangle panel = new Rectangle();
    private final Rectangle leaveRect = new Rectangle();
    private final Rectangle rerollRect = new Rectangle();

    private String message = "";

    public ShopScreen(
        SpriteBatch batch,
        ShapeRenderer shapeRenderer,
        BitmapFont font
    ) {
        this.batch = batch;
        this.shapeRenderer = shapeRenderer;
        this.font = font;
    }

    // -----------------------------------------
    // SHOW / HIDE
    // -----------------------------------------

    public void show(Array<Item> newItems, int rerollCost) {

        items.clear();
        items.addAll(newItems);

        this.rerollCost = rerollCost;
        message = "";
        visible = true;
    }

    public void hide() {

        visible = false;

        items.clear();
        itemRects.clear();

        message = "";
    }

    public boolean isVisible() {
        return visible;
    }

    public Item getItem(int index) {

        if (index < 0 || index >= items.size) {
            return null;
        }

        return items.get(index);
    }

    public void markSold(int index) {

        if (index >= 0 && index < items.size) {
            items.get(index).sold = true;
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // -----------------------------------------
    // LAYOUT
    // -----------------------------------------

    private void layout() {

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        float panelWidth = Math.min(width - 40f, 760f);
        float panelHeight = Math.min(height - 40f, 520f);

        panel.set(
            (width - panelWidth) / 2f,
            (height - panelHeight) / 2f,
            panelWidth,
            panelHeight
        );

        float gap = 16f;

        int rows = Math.max(1, (items.size + 1) / 2);

        float boxWidth = (panelWidth - 60f - gap) / 2f;
        float boxHeight = Math.min(
            130f,
            (panelHeight - 170f - (rows - 1) * gap) / rows
        );

        float topY = panel.y + panelHeight - 90f;

        itemRects.clear();

        for (int i = 0; i < items.size; i++) {

            int column = i % 2;
            int row = i / 2;

            itemRects.add(
                new Rectangle(
                    panel.x + 30f + column * (boxWidth + gap),
                    topY - (row + 1) * boxHeight - row * gap,
                    boxWidth,
                    boxHeight
                )
            );
        }

        leaveRect.set(
            panel.x + panelWidth - 30f - 150f,
            panel.y + 20f,
            150f,
            46f
        );

        rerollRect.set(
            leaveRect.x - 12f - 190f,
            panel.y + 20f,
            190f,
            46f
        );
    }

    // -----------------------------------------
    // CLICK
    // -----------------------------------------

    // Returns an item index, LEAVE, REROLL, or NONE
    public int handleClick(float x, float y) {

        if (!visible) {
            return NONE;
        }

        layout();

        if (leaveRect.contains(x, y)) {
            return LEAVE;
        }

        if (rerollRect.contains(x, y)) {
            return REROLL;
        }

        for (int i = 0; i < itemRects.size; i++) {

            if (itemRects.get(i).contains(x, y)) {
                return i;
            }
        }

        return NONE;
    }

    // -----------------------------------------
    // RENDER
    // -----------------------------------------

    public void render(int gold) {

        if (!visible) {
            return;
        }

        layout();

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Dim everything behind
        shapeRenderer.setColor(0f, 0f, 0f, 0.78f);
        shapeRenderer.rect(0, 0, width, height);

        // Panel with a light border
        shapeRenderer.setColor(0.35f, 0.40f, 0.55f, 1f);
        shapeRenderer.rect(
            panel.x - 3f,
            panel.y - 3f,
            panel.width + 6f,
            panel.height + 6f
        );

        shapeRenderer.setColor(0.10f, 0.11f, 0.16f, 1f);
        shapeRenderer.rect(panel.x, panel.y, panel.width, panel.height);

        // Item boxes
        for (int i = 0; i < items.size; i++) {

            Item item = items.get(i);
            Rectangle rect = itemRects.get(i);

            if (item.sold) {
                shapeRenderer.setColor(0.14f, 0.14f, 0.16f, 1f);
            } else if (item.kind == Kind.SELL) {
                shapeRenderer.setColor(0.13f, 0.26f, 0.19f, 1f);
            } else if (gold >= item.cost) {
                shapeRenderer.setColor(0.16f, 0.20f, 0.30f, 1f);
            } else {
                shapeRenderer.setColor(0.22f, 0.14f, 0.16f, 1f);
            }

            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        // Reroll button
        if (gold >= rerollCost) {
            shapeRenderer.setColor(0.36f, 0.30f, 0.12f, 1f);
        } else {
            shapeRenderer.setColor(0.22f, 0.14f, 0.16f, 1f);
        }
        shapeRenderer.rect(
            rerollRect.x,
            rerollRect.y,
            rerollRect.width,
            rerollRect.height
        );

        // Leave button
        shapeRenderer.setColor(0.22f, 0.42f, 0.28f, 1f);
        shapeRenderer.rect(
            leaveRect.x,
            leaveRect.y,
            leaveRect.width,
            leaveRect.height
        );

        shapeRenderer.end();

        batch.begin();

        font.getData().setScale(1.4f);
        font.setColor(Color.WHITE);
        font.draw(batch, "SHOP", panel.x + 30f, panel.y + panel.height - 30f);

        font.getData().setScale(0.9f);
        font.setColor(new Color(1f, 0.78f, 0.25f, 1f));
        font.draw(
            batch,
            "GOLD: " + gold,
            panel.x + panel.width - 200f,
            panel.y + panel.height - 34f
        );

        for (int i = 0; i < items.size; i++) {

            Item item = items.get(i);
            Rectangle rect = itemRects.get(i);

            boolean cursedRelic =
                item.kind == Kind.RELIC
                    && item.relic != null
                    && item.relic.isCursed();

            font.getData().setScale(0.75f);

            if (item.sold) {
                font.setColor(Color.GRAY);
            } else if (cursedRelic) {
                font.setColor(new Color(0.85f, 0.35f, 0.5f, 1f));
            } else {
                font.setColor(Color.WHITE);
            }

            font.draw(
                batch,
                item.name,
                rect.x + 12f,
                rect.y + rect.height - 12f
            );

            font.getData().setScale(0.6f);

            if (item.sold) {
                font.setColor(Color.GRAY);
                font.draw(
                    batch,
                    "SOLD",
                    rect.x + 12f,
                    rect.y + rect.height - 40f
                );
            } else if (item.kind == Kind.SELL) {
                font.setColor(new Color(0.45f, 1f, 0.55f, 1f));
                font.draw(
                    batch,
                    "PAYS: " + item.cost,
                    rect.x + 12f,
                    rect.y + rect.height - 40f
                );
            } else {
                font.setColor(
                    gold >= item.cost
                        ? new Color(1f, 0.78f, 0.25f, 1f)
                        : new Color(1f, 0.4f, 0.35f, 1f)
                );
                font.draw(
                    batch,
                    "COST: " + item.cost,
                    rect.x + 12f,
                    rect.y + rect.height - 40f
                );
            }

            font.getData().setScale(0.5f);
            font.setColor(
                item.sold
                    ? Color.DARK_GRAY
                    : new Color(0.75f, 0.80f, 0.88f, 1f)
            );

            font.draw(
                batch,
                item.description,
                rect.x + 12f,
                rect.y + rect.height - 62f,
                rect.width - 24f,
                Align.left,
                true
            );
        }

        if (!message.isEmpty()) {

            font.getData().setScale(0.65f);
            font.setColor(new Color(0.30f, 0.80f, 1f, 1f));
            font.draw(batch, message, panel.x + 30f, panel.y + 55f);
        }

        font.getData().setScale(0.65f);
        font.setColor(Color.WHITE);
        font.draw(
            batch,
            "REROLL " + rerollCost + "g",
            rerollRect.x + 24f,
            rerollRect.y + 31f
        );

        font.getData().setScale(0.8f);
        font.setColor(Color.WHITE);
        font.draw(
            batch,
            "LEAVE",
            leaveRect.x + 40f,
            leaveRect.y + 32f
        );

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        batch.end();
    }
}
