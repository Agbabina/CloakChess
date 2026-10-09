package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import java.util.Random;

public class Board {
    public static final int SIZE = 6;
    public static final float TILE_SIZE = 100f;

    private final Texture darkTile;
    private final Texture lightTile;
    private final Texture poisonTile;
    private final SpriteBatch batch;
    private final Random random;

    private float boardX;
    private float boardY;
    private final boolean[][] poisonedTiles;

    public Board(SpriteBatch batch) {
        this.batch = batch;
        this.random = new Random();
        this.poisonedTiles = new boolean[SIZE][SIZE];

        darkTile = new Texture("square gray dark _png_256px.png");
        lightTile = new Texture("square gray light _png_256px.png");
        poisonTile = new Texture("poisonedtile.png");

        setPoisonedTiles(-1, -1);
    }

    public int getSize() {
        return SIZE;
    }

    /** Marks one tile as toxic without rerolling the rest of the board. */
    public void poisonTile(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return;
        poisonedTiles[y][x] = true;
    }

    /** True if the tile at board coordinates (x, y) is poisoned. Out-of-range is false. */
    public boolean isPoisoned(int x, int y) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) return false;
        return poisonedTiles[y][x];
    }

    public void render() {
        batch.begin();
        boardX = (Gdx.graphics.getWidth() - SIZE * TILE_SIZE) / 2f;
        boardY = (Gdx.graphics.getHeight() - SIZE * TILE_SIZE) / 2f;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                Texture baseTile = ((x + y) % 2 == 0) ? lightTile : darkTile;
                float tileX = boardX + x * TILE_SIZE;
                float tileY = boardY + y * TILE_SIZE;

                batch.draw(baseTile, tileX, tileY, TILE_SIZE, TILE_SIZE);

                if (poisonedTiles[y][x]) {
                    batch.draw(poisonTile, tileX, tileY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
        batch.end();
    }

    /** Re-roll poison tiles with no safe tile (kept for compatibility). */
    public void setPoisonedTiles() {
        setPoisonedTiles(-1, -1);
    }

    /** Re-roll 1-3 poisoned tiles, never on (safeX, safeY) - pass the player's tile. */
    public void setPoisonedTiles(int safeX, int safeY) {
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                poisonedTiles[y][x] = false;
            }
        }

        int noofPoisonTiles = 1 + random.nextInt(3);
        int assignedCount = 0;

        while (assignedCount < noofPoisonTiles) {
            int randomX = random.nextInt(SIZE);
            int randomY = random.nextInt(SIZE);
            if (randomX == safeX && randomY == safeY) continue;

            if (!poisonedTiles[randomY][randomX]) {
                poisonedTiles[randomY][randomX] = true;
                assignedCount++;
            }
        }
    }

    public void dispose() {
        if (darkTile != null) darkTile.dispose();
        if (lightTile != null) lightTile.dispose();
        if (poisonTile != null) poisonTile.dispose();
    }
}
