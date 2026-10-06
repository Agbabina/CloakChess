package io.github.chesslike.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import io.github.chesslike.Main;

public class MenuScreen extends ScreenAdapter {

    private final Game game;

    private final SpriteBatch batch;
    private final ShapeRenderer shapes;
    private final BitmapFont font;

    private final Rectangle startButton;
    private final Rectangle tutorialButton;
    private final Rectangle bestiaryButton;

    public MenuScreen(Game game) {

        this.game = game;

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        font = new BitmapFont();

        startButton = new Rectangle();
        tutorialButton = new Rectangle();
        bestiaryButton = new Rectangle();
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(
            0.025f,
            0.025f,
            0.035f,
            1f
        );

        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
        );

        float centerX =
            Gdx.graphics.getWidth() / 2f;

        float centerY =
            Gdx.graphics.getHeight() / 2f;

        float buttonWidth = 300f;
        float buttonHeight = 65f;

        startButton.set(
            centerX - buttonWidth / 2f,
            centerY + 70f,
            buttonWidth,
            buttonHeight
        );

        tutorialButton.set(
            centerX - buttonWidth / 2f,
            centerY - 10f,
            buttonWidth,
            buttonHeight
        );

        bestiaryButton.set(
            centerX - buttonWidth / 2f,
            centerY - 90f,
            buttonWidth,
            buttonHeight
        );

        shapes.begin(
            ShapeRenderer.ShapeType.Filled
        );

        drawButton(
            startButton,
            false
        );

        drawButton(
            tutorialButton,
            false
        );

        drawButton(
            bestiaryButton,
            true
        );

        shapes.end();

        batch.begin();

        font.getData().setScale(2.2f);

        font.setColor(Color.WHITE);

        font.draw(
            batch,
            "CloakChess",
            centerX - 105f,
            centerY + 190f
        );

        font.getData().setScale(1.35f);

        drawCentered(
            "START",
            startButton
        );

        drawCentered(
            "TUTORIAL",
            tutorialButton
        );

        font.setColor(
            0.5f,
            0.5f,
            0.5f,
            1f
        );

        drawCentered(
            "BESTIARY - COMING SOON",
            bestiaryButton
        );

        batch.end();

        handleInput();
    }

    private void drawButton(
        Rectangle button,
        boolean locked
    ) {

        if (locked) {

            shapes.setColor(
                0.09f,
                0.09f,
                0.11f,
                1f
            );

        } else {

            shapes.setColor(
                0.12f,
                0.14f,
                0.18f,
                1f
            );
        }

        shapes.rect(
            button.x,
            button.y,
            button.width,
            button.height
        );
    }

    private void drawCentered(
        String text,
        Rectangle rectangle
    ) {

        float width =
            font.getRegion().getRegionWidth();

        font.draw(
            batch,
            text,
            rectangle.x
                + rectangle.width / 2f
                - text.length() * 5f,
            rectangle.y
                + rectangle.height / 2f
                + 8f
        );
    }

    private void handleInput() {

        if (!Gdx.input.justTouched()) {
            return;
        }

        float x = Gdx.input.getX();

        float y =
            Gdx.graphics.getHeight()
                - Gdx.input.getY();

        if (startButton.contains(x, y)) {

            // Keep your existing game-start code here.
            return;
        }

        if (tutorialButton.contains(x, y)) {

            if (game instanceof Main) {
                ((Main) game).openTutorial();
            }

            return;
        }

        if (bestiaryButton.contains(x, y)) {
            return;
        }
    }

    @Override
    public void dispose() {

        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}
