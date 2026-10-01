package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Board {
    private float boardX;
    private float boardY;
    public static final int SIZE = 5;
    public static final float TILE_SIZE = 100f;

    private final Texture darkTile;
    private final Texture lightTile;
    private final SpriteBatch batch;

    public Board(SpriteBatch batch) {
        this.batch = batch;

        darkTile = new Texture("square gray dark _png_256px.png");
        lightTile = new Texture("square gray light _png_256px.png");
    }

    public int getSize() {
        return SIZE;
    }

    public void render() {
        batch.begin();
        boardX = (Gdx.graphics.getWidth() - SIZE * TILE_SIZE) / 2f;
        boardY = (Gdx.graphics.getHeight() - SIZE * TILE_SIZE) / 2f;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {

                Texture tile = ((x + y) % 2 == 0) ? lightTile : darkTile;

                batch.draw(
                    tile,
                    boardX + x * TILE_SIZE,
                    boardY + y * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE
                );
            }
        }

        batch.end();
    }

    public void dispose() {
        if (darkTile != null) darkTile.dispose();
        if (lightTile != null) lightTile.dispose();
    }
}
