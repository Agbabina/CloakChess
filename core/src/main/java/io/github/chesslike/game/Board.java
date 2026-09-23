package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Board {
    private float boardX;
    private float boardY;
    private static final int SIZE = 5;
    private static final int TILE_SIZE = 100;

    private final Texture darkTile;
    private final Texture lightTile;
    private final SpriteBatch batch;

    public Board(SpriteBatch batch) {
        this.batch = batch;

        darkTile = new Texture("square gray dark _png_256px.png");
        lightTile = new Texture("square gray light _png_256px.png");
    }

    public void render() {
        batch.begin();
        boardX=(Gdx.graphics.getWidth()- SIZE*TILE_SIZE)/ 2f;
        boardY= (Gdx.graphics.getHeight()-SIZE*TILE_SIZE)/2F;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {

                Texture tile;

                if ((x + y) % 2 == 0) {
                    tile = lightTile;
                } else {
                    tile = darkTile;
                }

                batch.draw(
                    tile,
                    boardX+x* TILE_SIZE,
                    boardY+y * TILE_SIZE,
                    TILE_SIZE,
                    TILE_SIZE
                );
            }
        }

        batch.end();
    }

    public void dispose() {
        darkTile.dispose();
        lightTile.dispose();
    }
}
