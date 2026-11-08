package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

/** Background art: themed backdrop, overlays, board frame, hand panel and HUD bar. */
public final class GameBackdrop {

    public enum Theme {
        DUNGEON("bg_dungeon.png"),
        MOSSY_CRYPT("bg_mossy_crypt.png"),
        GOLD_SHRINE("bg_gold_shrine.png");

        final String file;
        Theme(String file) { this.file = file; }

        /** Cycle themes by floor number: floor 1 = dungeon, 2 = crypt, 3 = shrine, ... */
        public static Theme forFloor(int floor) {
            Theme[] all = values();
            return all[Math.floorMod(floor - 1, all.length)];
        }
    }

    public enum Accent {
        NONE(null),
        FIRE("overlay_accent_fire.png"),
        FROST("overlay_accent_frost.png"),
        NECRO("overlay_accent_necro.png"),
        BLOOD("overlay_accent_blood.png");

        final String file;
        Accent(String file) { this.file = file; }
    }

    private static final float FRAME_BORDER = 24f;     // board_frame_648.png ring thickness
    private static final float HUD_BAR_HEIGHT = 32f;

    private final ObjectMap<Theme, Texture> backgrounds = new ObjectMap<>();
    private final ObjectMap<Accent, Texture> accents = new ObjectMap<>();
    private Texture vignette, fog, boardFrame, panelTex, hudBar;
    private NinePatch panel;

    private Theme theme = Theme.DUNGEON;
    private Accent accent = Accent.NONE;
    private Accent fadingAccent = Accent.NONE;
    private float accentAlpha = 0f;
    private float time = 0f;

    // =========================================================
    // LOAD / DISPOSE
    // =========================================================

    public void load() {
        for (Theme t : Theme.values()) {
            Texture tex = loadTexture(t.file);
            if (tex != null) backgrounds.put(t, tex);
        }
        for (Accent a : Accent.values()) {
            if (a.file == null) continue;
            Texture tex = loadTexture(a.file);
            if (tex != null) accents.put(a, tex);
        }
        vignette = loadTexture("overlay_vignette.png");
        fog = loadTexture("overlay_fog_emerald.png");
        boardFrame = loadTexture("board_frame_648.png");
        hudBar = loadTexture("hud_bar_1280x32.png");
        panelTex = loadTexture("panel_9slice_96.png");
        if (panelTex != null) panel = new NinePatch(panelTex, 16, 16, 16, 16);
    }

    private static Texture loadTexture(String file) {
        if (!Gdx.files.internal(file).exists()) return null;     // art is optional; game still runs
        Texture t = new Texture(Gdx.files.internal(file));
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return t;
    }

    public void dispose() {
        for (Texture t : backgrounds.values()) t.dispose();
        for (Texture t : accents.values()) t.dispose();
        backgrounds.clear();
        accents.clear();
        if (vignette != null) vignette.dispose();
        if (fog != null) fog.dispose();
        if (boardFrame != null) boardFrame.dispose();
        if (hudBar != null) hudBar.dispose();
        if (panelTex != null) panelTex.dispose();
        vignette = fog = boardFrame = hudBar = panelTex = null;
        panel = null;
    }

    // =========================================================
    // STATE
    // =========================================================

    public void setTheme(Theme theme) {
        if (theme != null) this.theme = theme;
    }

    /** Fades the coloured rim glow in (FIRE, FROST, NECRO, BLOOD) or out (NONE). */
    public void setAccent(Accent next) {
        if (next == null) next = Accent.NONE;
        if (next != Accent.NONE) fadingAccent = next;
        accent = next;
    }

    public void update(float delta) {
        time += delta;
        float target = accent == Accent.NONE ? 0f : 1f;
        accentAlpha = MathUtils.lerp(accentAlpha, target, Math.min(delta * 3f, 1f));
        if (accentAlpha < 0.01f && accent == Accent.NONE) accentAlpha = 0f;
    }

    // =========================================================
    // DRAWING (each call does its own batch.begin()/end())
    // =========================================================

    /** Backdrop + drifting fog + vignette + accent glow. Draw before the board. */
    public void drawBackground(SpriteBatch batch, float w, float h) {
        batch.begin();
        batch.setColor(Color.WHITE);

        Texture bg = backgrounds.get(theme);
        if (bg != null) batch.draw(bg, 0f, 0f, w, h);

        if (fog != null) {
            float drift = (time * 6f) % w;
            batch.setColor(1f, 1f, 1f, 0.85f);
            batch.draw(fog, -drift, 0f, w, h);
            batch.draw(fog, w - drift, 0f, w, h);
        }

        batch.setColor(Color.WHITE);
        if (vignette != null) batch.draw(vignette, 0f, 0f, w, h);

        Texture acc = accents.get(fadingAccent);
        if (acc != null && accentAlpha > 0f) {
            float pulse = 0.85f + 0.15f * MathUtils.sin(time * 2.2f);
            batch.setColor(1f, 1f, 1f, accentAlpha * pulse);
            batch.draw(acc, 0f, 0f, w, h);
        }

        batch.setColor(Color.WHITE);
        batch.end();
    }

    /** Stone/gold frame around the board. Pass the board's bottom-left and its pixel size. */
    public void drawBoardFrame(SpriteBatch batch, float boardX, float boardY, float boardPx) {
        if (boardFrame == null) return;
        float size = boardPx + FRAME_BORDER * 2f;
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(boardFrame, boardX - FRAME_BORDER, boardY - FRAME_BORDER, size, size);
        batch.end();
    }

    /** Stretchable panel behind the right-hand card stack. */
    public void drawHandPanel(SpriteBatch batch, float x, float y, float width, float height) {
        if (panel == null || width <= 0f || height <= 0f) return;
        batch.begin();
        batch.setColor(Color.WHITE);
        panel.draw(batch, x, y, width, height);
        batch.end();
    }

    /** Thin strip along the top of the screen. */
    public void drawHudBar(SpriteBatch batch, float w, float h) {
        if (hudBar == null) return;
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(hudBar, 0f, h - HUD_BAR_HEIGHT, w, HUD_BAR_HEIGHT);
        batch.end();
    }
}
