package io.github.chesslike;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/**
 * A short, choice-driven prologue for Chapter I. The story UI is deliberately
 * independent of the combat loop so it works with both desktop and touch input.
 */
public final class StoryChapterOne {
    public enum Action { NONE, BACK, START_RUN }

    private static final int LAST_SCENE = 4;
    private final Rectangle[] choices = { new Rectangle(), new Rectangle() };
    private final Rectangle backButton = new Rectangle();
    private final Rectangle continueButton = new Rectangle();
    private final GlyphLayout glyph = new GlyphLayout();

    private int scene;
    private boolean openedDoor;
    private boolean acceptedCoin;
    private boolean readDiary;
    private boolean stoodGround;

    private static final String[] TITLES = {
        "CHAPTER I  /  THE KNOCK",
        "A COIN THAT REMEMBERS",
        "THE MOUNTAIN ROAD",
        "THE FIRST HUNGER",
        "THE GOLDEN HUNTER"
    };

    public void reset() {
        scene = 0;
        openedDoor = false;
        acceptedCoin = false;
        readDiary = false;
        stoodGround = false;
    }

    public int getStartingGoldBonus() {
        return (acceptedCoin ? 20 : 0) + (readDiary ? 10 : 0);
    }

    private String body() {
        switch (scene) {
            case 0:
                return "Elias had counted the last pieces of bread twice. There was still not enough. "
                    + "Rain pressed against the roof of his little house when three quiet knocks came from the door. "
                    + "A stranger stood outside, dry despite the storm.\n\n"
                    + "\"There is a mountain that grants what a man cannot earn,\" the stranger said. "
                    + "\"But it always collects what it is owed.\"";
            case 1:
                return openedDoor
                    ? "The stranger placed a single gold coin in Elias's palm. It was warm, almost alive. "
                        + "For one heartbeat, Elias heard something breathing beneath the metal. "
                        + "The stranger was already walking into the rain."
                    : "By morning the stranger was gone. On the doorstep lay a single gold coin, warm despite the cold. "
                        + "A thin black mark curled across its face like a sleeping eye. "
                        + "Elias heard the stranger's voice in the wind: \"The mountain is waiting.\"";
            case 2:
                return "The trail climbed into a mountain split by old stone stairs. "
                    + (readDiary
                        ? "At an abandoned camp, Elias found a water-stained diary. Its final line read: "
                            + "\"Gold does not reward the greedy. It teaches them to hunger.\""
                        : "Fresh footprints crossed the ash around an abandoned camp. None led back down the mountain.")
                    + " Beyond the camp, a doorway had been carved into the rock. No builder's name remained.";
            case 3:
                return (stoodGround
                    ? "A goblin-like thing crawled from beneath the camp stones. Elias held his ground as it lunged. "
                    : "A goblin-like thing crawled from beneath the camp stones. Elias slipped between broken pillars as it lunged. ")
                    + "Its eyes shone the same dull gold as the coin. When it fell, the black mark on Elias's hand tightened. "
                    + "Somewhere deeper in the mountain, a chain dragged across stone.";
            default:
                return "At the dungeon mouth, a figure waited in a crown of tarnished gold. "
                    + "It carried the shape of a hunter, but its shadow moved like a king crossing a chessboard. "
                    + "\"The first coin was a promise,\" it said. \"The next will be a choice.\"\n\n"
                    + "The Golden Hunter has noticed you. The mountain has opened.";
        }
    }

    private String[] options() {
        switch (scene) {
            case 0:
                return new String[] { "Open the door", "Stay silent" };
            case 1:
                return new String[] { "Keep the coin", "Leave it behind" };
            case 2:
                return new String[] { "Read the camp diary", "Follow the footprints" };
            case 3:
                return new String[] { "Stand your ground", "Slip into the ruins" };
            default:
                return new String[] { "ENTER THE DUNGEON", "RETURN TO MENU" };
        }
    }

    private void layout() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float panelW = Math.min(760f, w - 32f);
        float panelH = Math.min(590f, h - 32f);
        float x = (w - panelW) / 2f;
        float y = (h - panelH) / 2f;
        float buttonGap = 12f;
        float buttonH = MathUtils.clamp(panelH * .105f, 48f, 66f);
        float buttonW = (panelW - 56f - buttonGap) / 2f;
        float buttonY = y + 28f;
        choices[0].set(x + 28f, buttonY, buttonW, buttonH);
        choices[1].set(x + 28f + buttonW + buttonGap, buttonY, buttonW, buttonH);
        backButton.set(x + 20f, y + panelH - 58f, 112f, 38f);
        continueButton.set(x + panelW - 20f - 112f, y + 18f, 112f, 38f);
    }

    public Action handleInput() {
        if (!Gdx.input.justTouched()) return Action.NONE;
        layout();
        float x = Gdx.input.getX();
        float y = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (backButton.contains(x, y)) return Action.BACK;
        for (int i = 0; i < choices.length; i++) {
            if (!choices[i].contains(x, y)) continue;
            if (scene == LAST_SCENE) {
                return i == 0 ? Action.START_RUN : Action.BACK;
            }
            choose(i);
            return Action.NONE;
        }
        return Action.NONE;
    }

    private void choose(int choice) {
        switch (scene) {
            case 0: openedDoor = choice == 0; break;
            case 1: acceptedCoin = choice == 0; break;
            case 2: readDiary = choice == 0; break;
            case 3: stoodGround = choice == 0; break;
            default: break;
        }
        scene = Math.min(LAST_SCENE, scene + 1);
    }

    public void render(SpriteBatch batch, BitmapFont font, ShapeRenderer shapes) {
        layout();
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float panelW = Math.min(760f, w - 32f);
        float panelH = Math.min(590f, h - 32f);
        float x = (w - panelW) / 2f;
        float y = (h - panelH) / 2f;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(.012f, .018f, .025f, 1f);
        shapes.rect(0f, 0f, w, h);
        shapes.setColor(.035f, .055f, .052f, .98f);
        shapes.rect(x, y, panelW, panelH);
        shapes.setColor(.48f, .34f, .15f, 1f);
        shapes.rect(x, y + panelH - 4f, panelW, 4f);
        shapes.setColor(.16f, .28f, .22f, 1f);
        shapes.rect(x + 18f, y + 78f, panelW - 36f, 1f);
        for (int i = 0; i < choices.length; i++) {
            shapes.setColor(i == 0 ? .22f : .12f, i == 0 ? .18f : .14f, .10f, 1f);
            shapes.rect(choices[i].x, choices[i].y, choices[i].width, choices[i].height);
            shapes.setColor(.48f, .34f, .15f, .9f);
            shapes.rect(choices[i].x, choices[i].y + choices[i].height - 2f, choices[i].width, 2f);
        }
        shapes.end();

        batch.begin();
        font.setColor(new Color(.94f, .72f, .34f, 1f));
        font.getData().setScale(.82f);
        font.draw(batch, TITLES[scene], x + 30f, y + panelH - 34f);

        font.setColor(new Color(.54f, .67f, .59f, 1f));
        font.getData().setScale(.48f);
        font.draw(batch, "A STORY OF GOLD, HUNGER, AND THE THINGS THAT FOLLOW US",
            x + 30f, y + panelH - 62f);

        font.setColor(new Color(.86f, .85f, .78f, 1f));
        font.getData().setScale(.68f);
        drawWrapped(batch, font, body(), x + 30f, y + panelH - 112f, panelW - 60f, y + 112f);

        String[] labels = options();
        for (int i = 0; i < choices.length; i++) {
            font.setColor(i == 0 ? new Color(1f, .88f, .62f, 1f) : new Color(.83f, .84f, .80f, 1f));
            font.getData().setScale(.62f);
            glyph.setText(font, labels[i]);
            float tx = choices[i].x + (choices[i].width - glyph.width) / 2f;
            float ty = choices[i].y + (choices[i].height + glyph.height) / 2f;
            font.draw(batch, labels[i], tx, ty);
        }

        font.setColor(new Color(.52f, .55f, .52f, 1f));
        font.getData().setScale(.48f);
        font.draw(batch, "CHAPTER I  •  SCENE " + (scene + 1) + " / 5", x + 30f, y + 35f);
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private void drawWrapped(SpriteBatch batch, BitmapFont font, String text,
                             float x, float y, float maxWidth, float minY) {
        float lineHeight = font.getLineHeight() * .95f;
        String[] paragraphs = text.split("\\n");
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                y -= lineHeight * .55f;
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                glyph.setText(font, candidate);
                if (glyph.width > maxWidth && line.length() > 0) {
                    font.draw(batch, line.toString(), x, y);
                    y -= lineHeight;
                    line.setLength(0);
                    line.append(word);
                } else {
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
                if (y < minY) return;
            }
            if (line.length() > 0) {
                font.draw(batch, line.toString(), x, y);
                y -= lineHeight;
            }
            if (y < minY) return;
        }
    }
}
