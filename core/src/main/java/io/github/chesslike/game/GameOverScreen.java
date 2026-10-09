package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

public class GameOverScreen {

    private boolean visible = false;
    private boolean victory = false;

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private final Rectangle restartButton =
        new Rectangle();

    public GameOverScreen(
        SpriteBatch batch,
        ShapeRenderer shapeRenderer,
        BitmapFont font
    ) {
        this.batch = batch;
        this.shapeRenderer = shapeRenderer;
        this.font = font;
    }

    // ------------------------------------------------
    // SHOW / HIDE
    // ------------------------------------------------

    public void show() {
        victory = false;
        visible = true;
    }

    public void showVictory() {
        victory = true;
        visible = true;
    }

    public void hide() {
        visible = false;
        victory = false;
    }

    public boolean isVisible() {
        return visible;
    }

    // ------------------------------------------------
    // RENDER
    // ------------------------------------------------

    public void render() {

        if (!visible) {
            return;
        }

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // --------------------------------------------
        // DARK OVERLAY
        // --------------------------------------------

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        shapeRenderer.setColor(
            new Color(0f, 0f, 0f, 0.85f)
        );

        shapeRenderer.rect(0, 0, width, height);

        shapeRenderer.end();

        // --------------------------------------------
        // PANEL
        // --------------------------------------------

        float panelWidth =
            Math.min(width - 40f, 420f);

        float panelHeight = victory ? 250f : 220f;

        float panelX =
            (width - panelWidth) / 2f;

        float panelY =
            (height - panelHeight) / 2f;

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        shapeRenderer.setColor(
            new Color(0.12f, 0.12f, 0.16f, 1f)
        );

        shapeRenderer.rect(
            panelX,
            panelY,
            panelWidth,
            panelHeight
        );

        shapeRenderer.end();

        // --------------------------------------------
        // RESTART BUTTON
        // --------------------------------------------

        float buttonWidth = 200f;
        float buttonHeight = 60f;

        float buttonX =
            panelX + (panelWidth - buttonWidth) / 2f;

        float buttonY = panelY + 30f;

        restartButton.set(
            buttonX,
            buttonY,
            buttonWidth,
            buttonHeight
        );

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        shapeRenderer.setColor(
            new Color(0.30f, 0.80f, 1f, 1f)
        );

        shapeRenderer.rect(
            restartButton.x,
            restartButton.y,
            restartButton.width,
            restartButton.height
        );

        shapeRenderer.end();

        // --------------------------------------------
        // TEXT
        // --------------------------------------------

        batch.begin();

        font.getData().setScale(1.6f);
        font.setColor(Color.WHITE);

        String title = victory ? "YOU WIN!" : "GAME OVER";
        font.draw(
            batch,
            title,
            victory ? panelX + 115f : panelX + 55f,
            panelY + panelHeight - 30f
        );

        if (victory) {
            font.getData().setScale(0.85f);
            font.setColor(new Color(1f, .82f, .30f, 1f));
            font.draw(batch, "All three bosses defeated.",
                panelX + 72f, panelY + panelHeight - 75f);
        }

        font.getData().setScale(0.9f);
        font.setColor(
            new Color(0.05f, 0.05f, 0.08f, 1f)
        );

        font.draw(
            batch,
            "PLAY AGAIN",
            buttonX + 43f,
            buttonY + buttonHeight / 2f + 8f
        );

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        batch.end();
    }

    // ------------------------------------------------
    // CLICK HANDLING
    // ------------------------------------------------

    public boolean handleClick(
        float x,
        float y
    ) {

        if (!visible) {
            return false;
        }

        return restartButton.contains(x, y);
    }
}
