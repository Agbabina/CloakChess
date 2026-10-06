package io.github.chesslike;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.chesslike.game.*;

import java.util.Random;

public class Main extends Game {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Board board;
    private Player player;

    // ---------- Capture slash FX ----------
    private static class Slash {
        float x, y, timer, angle;
    }

    private static final float SLASH_DURATION = 0.28f;
    private final Array<Slash> slashes = new Array<>();

    private void spawnSlash(int tx, int ty) {
        Slash s = new Slash();
        s.x = tileCenterX(tx);
        s.y = tileCenterY(ty);
        s.angle = MathUtils.random(25f, 65f);
        slashes.add(s);
    }

    private void updateSlashes(float delta) {
        for (int i = slashes.size - 1; i >= 0; i--) {
            Slash s = slashes.get(i);
            s.timer += delta;
            if (s.timer >= SLASH_DURATION) slashes.removeIndex(i);
        }
    }

    private void renderSlashes() {
        if (slashes.size == 0) return;
        float ts = Player.TILE_SIZE;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < slashes.size; i++) {
            Slash s = slashes.get(i);
            float p = s.timer / SLASH_DURATION;
            float len = ts * (0.35f + 0.55f * p);
            float a = 1f - p;
            for (int k = 0; k < 2; k++) {
                float ang = (s.angle + k * 90f) * MathUtils.degreesToRadians;
                float dx = MathUtils.cos(ang) * len, dy = MathUtils.sin(ang) * len;
                shapeRenderer.setColor(1f, .9f, .5f, a * .6f);
                shapeRenderer.rectLine(s.x - dx, s.y - dy, s.x + dx, s.y + dy, 10f * a + 3f);
                shapeRenderer.setColor(1f, 1f, 1f, a);
                shapeRenderer.rectLine(s.x - dx, s.y - dy, s.x + dx, s.y + dy, 3f * a + 1.5f);
            }
        }
        shapeRenderer.end();
    }

    // ---------- CloakChess intro (plays before Room 1) ----------
    private static final float INTRO_FLY_START = 0.55f;
    private static final float INTRO_IMPACT = 0.85f;
    private static final float INTRO_OUT_START = 2.30f;
    private static final float INTRO_TOTAL = 2.80f;
    // ---------- Meta progression (Souls) ----------
    private static final int SOULS_PER_ROOM = 3;
    private static final String[] UP_NAMES = {"Vitality", "Wealth", "Arcana", "Dark Pact"};
    private static final String[] UP_DESC = {"+1 starting max HP", "+25 starting gold", "+1 max mana", "Sacrifice heals 1 HP per level"};
    private static final int[] UP_MAX = {3, 3, 2, 2};
    private static final int[] UP_BASE_COST = {15, 10, 20, 25};
    private static final float CARD_REWARD_CHANCE = 0.40f;
    // Poison: stacks up to this many turns; 6+ turns = 2 damage per tick
    private static final int POISON_MAX_TURNS = 8;
    // ---------- Defense ----------
    private static final int MAX_DEFENSE = 2;
    private int defense = 0;
    private int frenzyTurns = 0;
    private int flawlessStreak = 0;
    private int shopRerolls = 0;
    // ---------- Objectives / Combo / Sacrifice ----------
    private static final int OBJECTIVE_CAPTURES = 0;
    private static final int OBJECTIVE_COMBO = 1;
    private static final int OBJECTIVE_SACRIFICE = 2;
    private static final int OBJECTIVE_NO_DAMAGE = 3;
    // ---------- Omens (random room modifiers) ----------

    private static final int OMEN_NONE = 0, OMEN_BLOOD_MOON = 1, OMEN_FORTUNE = 2, OMEN_VENOM = 3, OMEN_MANA_TIDE = 4, OMEN_IRON = 5,
        OMEN_FAMINE = 6, OMEN_WRATH = 7, OMEN_ECLIPSE = 8, OMEN_DREAD = 9, OMEN_WITHERING = 10;
    private static final int OMEN_COUNT = 10;
    private static final int OMEN_FIRST_BRUTAL = 6;
    private static final String[] OMEN_NAMES = {"", "Blood Moon", "Fortune's Eye", "Venom Mist", "Mana Tide", "Iron Tide",
        "Famine", "Red Dawn", "Eclipse", "Dread", "Withering"};
    private static final String[] OMEN_DESC = {"", "Every enemy is cursed. +50% room gold", "Critical captures: 40% chance",
        "Enemies start poisoned", "Spells cost 1 less mana", "Enemies start shielded. +50% room gold",
        "Card drops halved. -30% room gold", "Enemies hit for +1 damage", "Spells cost 1 more mana",
        "DEF cannot block damage", "No healing while enemies remain"};
    private static final float EVENT_CHANCE = 0.40f;
    private static final float BOLT_DURATION = 0.35f;
    private static final float UI_BTN_W = 120f, UI_BTN_H = 48f, UI_BTN_GAP = 7f;
    // ---------- Animations ----------
    private static final float SLIDE_DURATION = 0.20f;
    private static final float JUMP_DURATION = 0.50f;
    private static final float JUMP_HEIGHT = 80f;
    private static final float FLASH_DURATION = 0.7f;
    // ---------- Card dragging / combining ----------
    private static final float DRAG_THRESHOLD = 14f;
    // ---------- Shop (direct access, no reflection) ----------
    private static final int POTION_HEAL = 2;
    private static final float SHOP_DISCOUNT = 1.15f;
    private final GlyphLayout tipLayout = new GlyphLayout();
    private final Rectangle menuLogo = new Rectangle();
    private final int[] upLevels = new int[4];
    private final Rectangle[] upgradeRows = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};
    // ---------- Pacts (buff + debuff bargains) ----------
    private static final int PACT_COUNT = 6;
    private static final String[] PACT_NAMES = {"Glass Cannon", "Blood Money", "Arcane Pact", "Lucky Devil", "Iron Will", "Soul Broker"};
    private static final String[] PACT_BUFF = {"+1 mana per card played", "+50% room gold", "Spells cost 1 less mana",
        "+20% critical capture chance", "Heal 1 HP after every room", "+50% souls earned"};
    private static final String[] PACT_BANE = {"-1 max HP", "Enemies hit for 2 from room 4", "-2 max mana",
        "+20% enemy curse chance", "-20% card drop chance", "All enemies start shielded"};
    private final boolean[] pactOn = new boolean[PACT_COUNT];    // what you have equipped in the menu
    private final boolean[] runPact = new boolean[PACT_COUNT];   // snapshot used by the current run
    private int pactSlots = 1;
    private boolean pactsVisible = false;
    private String pactMsg = "";
    private final Rectangle[] pactRows = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};
    private final Rectangle pactSlotButton = new Rectangle();
    private final Rectangle pactsButton = new Rectangle();
    private final Rectangle upgradesButton = new Rectangle();
    private final Rectangle menuPanel = new Rectangle();
    private final String[] evtOptions = new String[3];
    private final boolean[] evtEnabled = new boolean[3];
    private final Rectangle[] evtRects = {new Rectangle(), new Rectangle(), new Rectangle()};
    private final Array<Enemy> pendingArrivals = new Array<>();
    private static final Color COL_BURN = new Color(1f, .35f, .12f, 1f);
    // ---------- Tutorial ----------
    // ---------- Tutorial ----------
    private static final String[] TUT_TITLES = {"THE BASICS", "MOVES - TAP A CARD", "CARDS", "AUGMENTS", "MANA & SPELLS", "DEFENSE & HP", "CURSES & STATUS 1/3", "CURSES & STATUS 2/3", "CURSES & STATUS 3/3", "OMENS", "RUN SYSTEMS", "BLITZ MODE", "BLITZ SCORING", "BLITZ RELICS", "BLITZ TIME GAMBLE"};
    private static final int TUT_MOVES_PAGE = 1;
    private static final int TUT_AUGMENT_PAGE = 3;
    private static final int TUT_CURSE_PAGE = 6;
    private static final int TUT_CURSE_PAGES = 3;
    private static final int TUT_OMEN_PAGE = 9;
    private static final String[][] TUT_LINES = {
        {
            "Capture every enemy to clear the room.",
            "Pick a card, tap your piece, tap a tile.",
            "Enemies move after each card.",
            "Tap an enemy to see its info."
        },
        {},
        {
            "Hold up to 5 cards. Each has limited uses.",
            "Drag one card onto another to combine them.",
            "Dash shoves enemies in its path.",
            "SHIFTER changes its movement type after every turn. Check its 'Now:' line.",
            "SACRIFICE (S): lose 1 HP, gain gold, mana and DEF, hit every enemy.",
            "REROLL (R): spend 1 mana to swap a card. Once per room."
        },
        {},
        {
            "Each card you play restores 1 mana.",
            "Tap a spell, then tap an enemy.",
            "Cloak: enemies freeze for 2 turns. Capturing while cloaked is an assassination.",
            "ARROW (A): pick a direction, the first enemy in line dies.",
            "Warded enemies ignore spells."
        },
        {
            "If HP hits zero, your run ends.",
            "DEF absorbs 1 damage per hit. Max " + MAX_DEFENSE + ".",
            "Sacrificing a card gives +1 DEF.",
            "DEF carries over between rooms."
        },
        {},
        {},
        {},
        {},
        {
            "Each room has a bonus objective (+15 gold).",
            "Chain captures for combos. A x4 combo starts Blood Frenzy: free cards.",
            "Clear rooms without damage for bonus gold.",
            "Shop every 3rd room: buy, sell cards, or reroll the stock.",
            "Souls buy upgrades. Pacts trade a buff for a drawback."
        },
        {
            "BLITZ is a score-attack mode. Your run starts with a timer.",
            "Clear rooms, chain captures and buy relics before the clock hits zero.",
            "Blitz uses denser enemy rooms and special omens.",
            "Your score and multiplier stay at the bottom so the board stays readable."
        },
        {
            "Every capture builds your CHAIN. Breaking it resets the multiplier.",
            "Relics add score rules, multipliers and time interactions.",
            "Special captures, low-health enemies and streak milestones can explode your score.",
            "Play fast, but protect the chain: a missed capture can cost more than the move itself."
        },
        {
            "You can carry up to 6 Blitz relics.",
            "Relics can add score, multiplier, gold scaling, spell value or extra time.",
            "The Relic Forge appears every 2 rooms.",
            "Some relics trigger from captures, room clears, spells, cards or relic purchases."
        },
        {
            "TIME GAMBLE costs 10 seconds and lasts 8 seconds.",
            "While active, capture score is doubled.",
            "It has an 18-second cooldown after use.",
            "Keyboard: G. Phone: tap TIME GAMBLE. Use it when a big chain is ready."
        }
    };

    // ---------- Card augments (tutorial reference + shared card pool) ----------
    private static final Card.MovementType[] CARD_POOL = {
        Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK, Card.MovementType.QUEEN,
        Card.MovementType.PAWN, Card.MovementType.DASH, Card.MovementType.CLAUDE, Card.MovementType.SHIFTER
    };
    private static final String[] AUG_NAMES = {
        "Extra Uses", "Rebound", "Pierce", "Double Move", "Teleportation", "Burn",
        "Fury", "Mana Surge", "Vampiric", "Gilded", "Aegis", "Venom", "Echo"
    };
    private static final String[] AUG_DESC = {
        "The card gets extra uses.",
        "Capturing gives an extra action.",
        "Damages the first enemy in line along your move.",
        "Act again after playing this card.",
        "Move to any tile on the board.",
        "Next play burns a random enemy (used up once).",
        "Red frame. Captures deal +1 extra damage.",
        "Blue frame. Restores 2 extra mana when played.",
        "Capturing with this card heals 1 HP.",
        "Capturing with this card gives +8 gold.",
        "Playing this card gives +1 DEF.",
        "Playing this card poisons the nearest enemy for 3 turns.",
        "35% chance the card does not lose a use."
    };
    private static final Color[] AUG_COLORS = {
        new Color(.95f, .80f, .30f, 1f), new Color(.55f, .85f, .95f, 1f), new Color(.80f, .80f, .85f, 1f),
        new Color(.60f, .95f, .60f, 1f), new Color(.75f, .50f, 1f, 1f), new Color(1f, .55f, .15f, 1f),
        new Color(1f, .30f, .30f, 1f), new Color(.35f, .60f, 1f, 1f), new Color(.85f, .20f, .45f, 1f),
        new Color(1f, .85f, .25f, 1f), new Color(.45f, .70f, 1f, 1f), new Color(.45f, .95f, .35f, 1f),
        new Color(.90f, .90f, .90f, 1f)
    };

    // ---------- Tutorial: move preview ----------
    private static final Card.MovementType[] TUT_MOVE_TYPES = {
        Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK,
        Card.MovementType.QUEEN, Card.MovementType.PAWN, Card.MovementType.DASH,
        Card.MovementType.CLAUDE, Card.MovementType.SHIFTER, Card.MovementType.ARCHBISHOP, Card.MovementType.COUNCILLOR
    };
    private static final String[] TUT_MOVE_NAMES = {"Knight", "Bishop", "Rook", "Queen", "Pawn", "Dash", "Claude", "Shifter", "Archbishop", "Councillor"};
    private static final String[] TUT_MOVE_LABELS = {"Knight", "Bishop", "Rook", "Queen", "Pawn", "Dash", "Claude", "Shifter", "Archbp", "Council"};
    private static final String[] TUT_MOVE_HINTS = {
        "KNIGHT - L-shaped jump", "BISHOP - diagonal lines", "ROOK - straight lines",
        "QUEEN - any direction", "PAWN - short forward step", "DASH - forward, shoves enemies",
        "CLAUDE - knight jump + 1 free step", "SHIFTER - changes form every turn",
        "ARCHBISHOP - merged card", "COUNCILLOR - merged card"
    };
    private float tutLastShift = 0f;

    private void layoutTutorialMoves(float w, float h) {
        int n = TUT_MOVE_TYPES.length;
        float gap = 8f, contentW = Math.min(w - 80f, 900f);
        float bw = Math.min(100f, (contentW - gap * (n - 1)) / n), bh = 40f;
        float y = h - 215f;
        for (int i = 0; i < n; i++) {
            if (tutMoveBtns[i] == null) tutMoveBtns[i] = new Rectangle();
            tutMoveBtns[i].set(40f + i * (bw + gap), y, bw, bh);
        }
        float top = y - 24f, bottom = 100f;
        tutCell = MathUtils.clamp((top - bottom) / Player.BOARD_SIZE, 20f, 60f);
        float bs = tutCell * Player.BOARD_SIZE;
        tutBoardX = (w - bs) / 2f;
        tutBoardY = top - bs;
    }

    private Card tutMoveCard(int i) {
        if (tutMoveCards[i] == null) tutMoveCards[i] = new Card(TUT_MOVE_NAMES[i], TUT_MOVE_TYPES[i], 2);
        return tutMoveCards[i];
    }

    // Shapes: button plates and the mini board. Call outside batch.begin()/end().
    private void renderTutorialMovesShapes(float w, float h) {
        layoutTutorialMoves(w, h);

        // 1. Begin shape batch before drawing
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int n = Player.BOARD_SIZE, c = n / 2;
        for (int i = 0; i < TUT_MOVE_TYPES.length; i++) {
            Rectangle r = tutMoveBtns[i];
            if (i == tutMoveSel) shapeRenderer.setColor(.38f, .30f, .12f, 1f);
            else shapeRenderer.setColor(.15f, .17f, .25f, 1f);
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        Card card = tutMoveCard(tutMoveSel);
        if (card.isShifter() && uiTime - tutLastShift > 1.2f) {
            card.shift();
            tutLastShift = uiTime;
        }
        boolean claude = TUT_MOVE_TYPES[tutMoveSel] == Card.MovementType.CLAUDE;
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                boolean valid = false;
                try {
                    valid = (x != c || y != c) && card.getMovementRules().isValidMove(c, c, x, y, n);
                } catch (Exception ignored) {
                }
                if (x == c && y == c) shapeRenderer.setColor(.30f, .55f, .95f, 1f);
                else if (valid) {
                    if (claude) shapeRenderer.setColor(1f, .58f, .20f, 1f);
                    else if (card.isShifter()) shapeRenderer.setColor(.65f, .45f, 1f, 1f);
                    else shapeRenderer.setColor(.30f, .78f, .45f, 1f);
                } else if ((x + y) % 2 == 0) shapeRenderer.setColor(.20f, .22f, .30f, 1f);
                else shapeRenderer.setColor(.14f, .16f, .23f, 1f);
                shapeRenderer.rect(tutBoardX + x * tutCell + 1f, tutBoardY + y * tutCell + 1f, tutCell - 2f, tutCell - 2f);
            }
        }

        // 2. End shape batch when finished
        shapeRenderer.end();
    }

    // Text for the moves page. Call inside an active batch.
    private void renderTutorialMovesText(float w, float h) {
        layoutTutorialMoves(w, h);
        font.getData().setScale(.55f);
        font.setColor(new Color(.95f, .80f, .30f, 1f));
        String hint = TUT_MOVE_HINTS[tutMoveSel];
        if (TUT_MOVE_TYPES[tutMoveSel] == Card.MovementType.SHIFTER) {
            hint += "  (now: " + tutMoveCard(tutMoveSel).getCurrentForm().name() + ")";
        }
        font.draw(batch, hint, 40f, h - 150f);
        font.getData().setScale(.4f);
        for (int i = 0; i < TUT_MOVE_TYPES.length; i++) {
            Rectangle r = tutMoveBtns[i];
            font.setColor(i == tutMoveSel ? new Color(1f, .80f, .35f, 1f) : Color.WHITE);
            tipLayout.setText(font, TUT_MOVE_LABELS[i]);
            font.draw(batch, TUT_MOVE_LABELS[i], r.x + (r.width - tipLayout.width) / 2f, r.y + r.height / 2f + tipLayout.height / 2f);
        }
        int c = Player.BOARD_SIZE / 2;
        font.setColor(Color.WHITE);
        tipLayout.setText(font, "YOU");
        font.draw(batch, "YOU", tutBoardX + c * tutCell + (tutCell - tipLayout.width) / 2f, tutBoardY + c * tutCell + tutCell / 2f + tipLayout.height / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private final Rectangle[] tutMoveBtns = new Rectangle[TUT_MOVE_TYPES.length];
    private final Card[] tutMoveCards = new Card[TUT_MOVE_TYPES.length];
    private int tutMoveSel = 0;
    private float tutBoardX, tutBoardY, tutCell;
    // ---------- HUD / bouncing arrows / interest ----------
    private float hudHp = -1f, hudMana = -1f, hudGold = 0f, goldPunch = 0f;
    private int hudLastGold = -1, goldDir = 1, hudArrowMax = 0;
    private int bounceArrows = 0, arrowBouncesLeft = 0;
    private boolean arrowBouncing = false;
    private final Array<Enemy> arrowHits = new Array<>();
    private static final int BOUNCE_PACK = 1, BOUNCE_BOUNCES = 1, INTEREST_CAP = 10;
    private boolean tutorialVisible = false;
    private int tutPage = 0;
    private final Rectangle tutorialButton = new Rectangle();
    private final Rectangle tutPrev = new Rectangle();
    private final Rectangle tutNext = new Rectangle();
    private Texture circleTex;
    private final ObjectMap<String, Texture> iconTex = new ObjectMap<>();

    private String curseSymbol(EnemyCurse c) {
        return c.name().toLowerCase();   // "hasty", "armored", ... = the icon key
    }

    private void loadIcons() {
        // Status + player icons
        String[][] map = {
            {"SH", "shield"}, {"PS", "poison"}, {"BR", "burn"}, {"??", "confused"}, {"DF", "defense"}
        };
        for (String[] m : map) loadIcon(m[0], m[1]);

        // Curse icons: key and file name are both the lowercase curse name
        for (EnemyCurse c : EnemyCurse.values()) {
            String name = c.name().toLowerCase();
            loadIcon(name, name);
        }
    }

    private void loadIcon(String key, String file) {
        // Looks in assets/icons/ first, then in the assets root
        String[] paths = {"icons/icon_" + file + ".png", "icon_" + file + ".png"};
        for (String path : paths) {
            try {
                if (Gdx.files.internal(path).exists()) {
                    Texture t = new Texture(Gdx.files.internal(path));
                    t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    iconTex.put(key, t);
                    return;
                }
            } catch (Exception ex) {
                Gdx.app.error("CloakChess", "Icon failed to load: " + path + " (" + ex.getMessage() + ")");
            }
        }
        Gdx.app.error("CloakChess", "Icon missing: icons/icon_" + file + ".png");
    }

    // Draws one round icon. Must be called between batch.begin()/end(). count <= 0 hides the number bubble.

    private Music backgroundMusic;
    private Sound moveSound, victorySound, gameOverSound, clickSound, augmentSound, arrowSound;

    private Array<Card> hand;
    private Card activeCard;
    private static final int MAX_HAND_SIZE = 5;
    private final Random random = new Random();
    private final Array<Debris> debris = new Array<>();

    private final ObjectMap<Card, CardModifier> cardModifiers = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyCurse> enemyCurses = new ObjectMap<>();
    private final ObjectMap<Enemy, CaptureCurse> captureCurses = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> poisonTurns = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> burnTurns = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> armorShields = new ObjectMap<>();
    private final Array<FloatText> floats = new Array<>();

    private Array<Spell> ownedSpells = new Array<>();
    private Array<Relic> ownedRelics = new Array<>();
    private Spell selectedSpell;
    private ShopScreen shopScreen;
    private BlitzRelicShopScreen blitzRelicShop;
    private BlitzManager blitzManager;
    private boolean blitzMode = false;
    private boolean blitzLaunch = false;
    private float blitzHudScore = 0f;
    private float blitzScorePulse = 0f;
    private String blitzLastLabel = "";
    private float blitzLastLabelTimer = 0f;
    private BlitzScore.Result blitzLastResult = null;
    private float blitzBreakdownTimer = 0f;
    private final Rectangle blitzGambleButton = new Rectangle();

    // Main menu
    private boolean menuVisible = true;
    private boolean bestiaryVisible = false;
    private final Rectangle startButton = new Rectangle();
    private final Rectangle bestiaryButton = new Rectangle();
    private final Rectangle blitzButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();
    private final Rectangle continueButton = new Rectangle();
    private final Rectangle returnMenuButton = new Rectangle();
    private final Array<Spark> sparks = new Array<>();
    private Preferences savePrefs;

    private int playerMaxHp = 3;
    private int playerHp = 3;
    private boolean doubleMoveActive = false;
    private boolean shopWasOpenedThisRoom = false;
    private final Array<Bolt> bolts = new Array<>();
    private final ObjectMap<Enemy, Float> confusedUntil = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> stunnedTurns = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> glassedTurns = new ObjectMap<>();
    private int freezeTurns = 0;
    // On-screen touch buttons: 0 = arrow, 1-3 = spell slots, 4 = cancel, 5 = sacrifice, 6 = reroll
    private final Rectangle[] uiButtons = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};
    // ---------- Placeholder SFX (optional: missing files are skipped silently) ----------
    private final Array<Sound> extraSfx = new Array<>();
    private final Matrix4 tmpMat = new Matrix4();
    private final Matrix4 viewProj = new Matrix4();
    private Card draggedCard = null;
    // Menu textures (generated assets)
    private Texture menuButtonTexture;
    private Texture menuButtonLockedTexture;
    private Texture menuPanelTexture;
    private Texture logoChessTexture;
    private Texture logoCloakTexture;
    private Texture daggerTexture;
    private Texture tooltipTexture;
    private NinePatch tooltipPatch;
    private Texture tintGreenTexture, tintRedTexture;
    private NinePatch tintGreenPatch, tintRedPatch;
    private Enemy tooltipEnemy;
    private boolean introActive = false;
    private boolean introImpacted = false;
    private float introT = 0f;
    private Runnable introAction;
    private boolean logoRevealed = false;   // flips to CLOAKCHESS once any menu button is pressed
    private float logoFade = 0f;
    private float uiTime = 0f;

    // MANA
    private static final int BASE_MAX_MANA = 5;
    private static final int MANA_REGEN_PER_CARD = 1;
    private int mana = BASE_MAX_MANA;

    private Array<Enemy> enemies;
    private EnemyGenerator enemyGenerator;
    private int gold = 0;
    private RewardScreen rewardScreen;
    private GameOverScreen gameOverScreen;
    private int difficulty = 1;

    private static final float DROP_STAGGER = 0.18f;

    private static class Shockwave {
        float x, y, timer, maxRadius;
    }

    private final Array<Shockwave> shockwaves = new Array<>();
    private int runBonusSouls = 0;
    private static final float SHOCKWAVE_DURATION = 0.35f;
    private int souls = 0;
    private int lastRunSouls = 0;
    private boolean upgradesVisible = false;
    private Preferences metaPrefs;

    private String eventMessage = "";
    private float messageTimer = 0f;

    private boolean shootMode = false;
    private Texture arrowTexture;
    private boolean arrowInFlight = false;
    private Enemy pendingHitEnemy;
    private float arrowStartX, arrowStartY, arrowTargetX, arrowTargetY, arrowProgress = 0f;
    private static final float ARROW_FLIGHT_DURATION = 0.30f;
    private static final float ARROW_DRAW_WIDTH = 50f;
    private static final float ARROW_DRAW_HEIGHT = 16f;
    private int objectiveType = OBJECTIVE_CAPTURES;
    private int objectiveTarget = 2;
    private int objectiveProgress = 0;
    private boolean objectiveCompleted = false;
    private boolean roomDamageTaken = false;
    private boolean roomSacrificed = false;
    private boolean roomRerolled = false;
    private int captureCombo = 0;
    private Card captureCard = null;

    private void shiftCards() {
        for (int i = 0; i < hand.size; i++) {
            Card c = hand.get(i);
            if (c.isShifter() && !c.isUsed()) c.shift();
        }
    }

    private int bestCombo = 0;
    private int comboRewardedAt = 0;
    private int omen = OMEN_NONE;
    private boolean evtVisible = false;
    private int evtId = 0;
    private String evtTitle = "", evtText = "";
    private int invisibleTurns = 0;
    private Sound lightningSound, poisonSound, burnSound, confuseSound;
    private Sound curseSound, blessingSound;
    private Sound hitSound, enemyDeathSound, healSound, buySound, errorSound;
    private Sound daggerSound;
    private Sound drawSound, jumpSound, smashSound, armorSound, slamSound, roomStartSound, shopOpenSound;
    private Sound combineSound;
    private boolean moveAnimActive = false;
    private boolean moveAnimJump = false;
    private boolean playerTransformApplied = false;
    private float moveAnimT = 0f, moveAnimDur = SLIDE_DURATION;
    private int moveFromX, moveFromY;
    private Card moveAnimCard;
    private float shakeTimer = 0f, shakeDuration = 0.01f, shakeMag = 0f;
    private float flashTimer = 0f;
    private Card dragCandidate = null;
    private float dragDownX, dragDownY;

    private Sound loadSfx(String file) {
        try {
            if (Gdx.files.internal(file).exists()) {
                Sound sound = Gdx.audio.newSound(Gdx.files.internal(file));
                extraSfx.add(sound);
                return sound;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void loadPlaceholderSfx() {
        lightningSound = loadSfx("sfx_lightning.mp3");
        poisonSound = loadSfx("sfx_poison.mp3");
        burnSound = loadSfx("sfx_burn.mp3");
        confuseSound = loadSfx("sfx_confuse.mp3");
        curseSound = loadSfx("sfx_curse.mp3");
        blessingSound = loadSfx("sfx_blessing.mp3");
        hitSound = loadSfx("sfx_player_hit.mp3");
        enemyDeathSound = loadSfx("sfx_enemy_death.mp3");
        healSound = loadSfx("sfx_heal.mp3");
        buySound = loadSfx("sfx_buy.mp3");
        errorSound = loadSfx("sfx_error.mp3");
        drawSound = loadSfx("sfx_card_draw.mp3");
        jumpSound = loadSfx("sfx_jump.mp3");
        smashSound = loadSfx("sfx_smash.mp3");
        armorSound = loadSfx("sfx_armor_block.mp3");
        slamSound = loadSfx("sfx_slam.mp3");
        roomStartSound = loadSfx("sfx_room_start.mp3");
        shopOpenSound = loadSfx("sfx_shop_open.mp3");
        daggerSound = loadSfx("sfx_dagger.mp3");
        combineSound = loadSfx("sfx_combine.mp3");
    }

    private void playSfx(Sound sound) {
        if (sound != null) sound.play();
    }

    private void playSpellSound(Spell spell) {
        if (spell == Spell.LIGHTNING) playSfx(lightningSound);
        else if (spell == Spell.POISON) playSfx(poisonSound);
        else if (spell == Spell.BURN) playSfx(burnSound);
        else if (spell == Spell.CONFUSE) playSfx(confuseSound);
        else if (augmentSound != null) augmentSound.play();
    }

    public void openTutorial() {
        tutorialVisible = true;
        tutPage = 0;
        tutMoveSel = 0;
        tutLastShift = uiTime;
    }

    public void closeTutorial() {
        tutorialVisible = false;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        savePrefs = Gdx.app.getPreferences("CloakChessSave");
        metaPrefs = Gdx.app.getPreferences("CloakChessMeta");
        loadMeta();
        moveSound = Gdx.audio.newSound(Gdx.files.internal("freesound_community-ficha-de-ajedrez-34722.mp3"));
        victorySound = Gdx.audio.newSound(Gdx.files.internal("victory.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameOver.mp3"));
        clickSound = Gdx.audio.newSound(Gdx.files.internal("dragon-studio-button-press-386165.mp3"));
        augmentSound = Gdx.audio.newSound(Gdx.files.internal("spell.mp3"));
        arrowSound = Gdx.audio.newSound(Gdx.files.internal("arrow.mp3"));
        loadPlaceholderSfx();

        menuButtonTexture = new Texture(Gdx.files.internal("menu_button.png"));
        menuButtonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        menuButtonLockedTexture = new Texture(Gdx.files.internal("menu_button_locked.png"));
        menuButtonLockedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        menuPanelTexture = new Texture(Gdx.files.internal("menu_panel.png"));
        menuPanelTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        logoChessTexture = new Texture(Gdx.files.internal("logo_chess.png"));
        logoChessTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        logoCloakTexture = new Texture(Gdx.files.internal("logo_cloakchess.png"));
        logoCloakTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        daggerTexture = new Texture(Gdx.files.internal("dagger.png"));
        daggerTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tooltipTexture = new Texture(Gdx.files.internal("tooltip_panel.png"));
        tooltipTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        tooltipPatch = new NinePatch(tooltipTexture, 8, 8, 8, 8);
        tintGreenTexture = new Texture(Gdx.files.internal("edge_tint_green.png"));
        tintGreenTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tintGreenPatch = new NinePatch(tintGreenTexture, 96, 96, 96, 96);
        tintRedTexture = new Texture(Gdx.files.internal("edge_tint_red.png"));
        tintRedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        tintRedPatch = new NinePatch(tintRedTexture, 96, 96, 96, 96);

        setScreen(
            new MenuScreen(this)
        );
        // Generated round icon base (white filled circle, tinted at draw time)
        Pixmap pm = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillCircle(32, 32, 30);
        circleTex = new Texture(pm);
        loadIcons();
        circleTex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();

        if (Gdx.files.internal("background.mp3").exists()) {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.75f);
            backgroundMusic.play();
        }

        arrowTexture = new Texture(Gdx.files.internal("pixelarrow.png"));
        arrowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Card.loadTextures();
        RewardScreen.loadTextures();
        shapeRenderer = new ShapeRenderer();

        // Readable Pixel Operator UI font.
        // Prefer the Pixel Operator font when it is present, but keep Font.ttf
        // as a fallback so older/local copies of the project still launch.
        String fontFile = "PixelOperator-Bold.ttf";
        if (!Gdx.files.internal(fontFile).exists()) {
            fontFile = "PixelOperator.ttf";
        }
        if (!Gdx.files.internal(fontFile).exists()) {
            fontFile = "Font.ttf";
        }

        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(Gdx.files.internal(fontFile));

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Generate the glyphs large instead of enlarging a tiny bitmap font.
        parameter.size = 32;
        parameter.minFilter = Texture.TextureFilter.Nearest;
        parameter.magFilter = Texture.TextureFilter.Nearest;

        font = generator.generateFont(parameter);
        generator.dispose();

        board = new Board(batch);
        player = new Player(2, 2);
        enemyGenerator = new EnemyGenerator();
        hand = new Array<>();
        rewardScreen = new RewardScreen(batch, shapeRenderer, font);
        gameOverScreen = new GameOverScreen(batch, shapeRenderer, font);
        shopScreen = new ShopScreen(batch, shapeRenderer, font);
        blitzRelicShop = new BlitzRelicShopScreen(batch, shapeRenderer, font);
        blitzManager = new BlitzManager();

        resetPlayerStats();
        buildStartingHand();
        positionCards();

        // Start on the main menu instead of immediately entering Room 1.
        menuVisible = true;
        bestiaryVisible = false;
    }

    // ---------- HUD / bouncing arrows / interest ----------
    private void resetPlayerStats() {
        int usedPacts = 0;
        for (int i = 0; i < PACT_COUNT; i++) {
            runPact[i] = pactOn[i] && usedPacts < pactSlots;
            if (runPact[i]) usedPacts++;
        }
        playerMaxHp = Math.max(1, 3 + upLevels[0] - (runPact[0] ? 1 : 0));
        playerHp = playerMaxHp;
        defense = 0;
        gold = 25 * upLevels[1];
        flawlessStreak = 0;
        runBonusSouls = 0;
        bounceArrows = 0;
        hudHp = -1f;
        hudLastGold = -1;
        ownedSpells.clear();
        ownedRelics.clear();
        mana = getMaxMana();
    }

    private void hudText(String s, float x, float midY, float scale, float r, float g, float b) {
        font.getData().setScale(scale);
        font.setColor(r, g, b, 1f);
        tipLayout.setText(font, s);
        font.draw(batch, s, x, midY + tipLayout.height / 2f);
    }

    // shown = animated value, actual = real value. The pale part is the trail between the two.
    private void drawPillBar(
        float x, float y, float w, float h,
        float shown, float actual, int max,
        float r, float g, float b
    ) {
        max = Math.max(1, max);

        float radius = h * 0.5f;
        float lo = MathUtils.clamp(Math.min(shown, actual) / max, 0f, 1f);
        float hi = MathUtils.clamp(Math.max(shown, actual) / max, 0f, 1f);

        // Outer shadow
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);
        shapeRenderer.rect(x + radius, y - 2f, w - radius * 2f, h + 4f);
        shapeRenderer.circle(x + radius, y + h * 0.5f, radius + 2f, 20);
        shapeRenderer.circle(x + w - radius, y + h * 0.5f, radius + 2f, 20);

        // Background
        shapeRenderer.setColor(0.06f, 0.08f, 0.13f, 1f);
        shapeRenderer.rect(x + radius, y, w - radius * 2f, h);
        shapeRenderer.circle(x + radius, y + h * 0.5f, radius, 20);
        shapeRenderer.circle(x + w - radius, y + h * 0.5f, radius, 20);

        // Animated trail
        if (hi > lo) {
            shapeRenderer.setColor(1f, 1f, 1f, 0.18f);

            float trailX = x + radius + (w - radius * 2f) * lo;
            float trailW = (w - radius * 2f) * (hi - lo);

            shapeRenderer.rect(trailX, y, trailW, h);
            shapeRenderer.circle(trailX, y + h * 0.5f, h * 0.5f, 20);
            shapeRenderer.circle(trailX + trailW, y + h * 0.5f, h * 0.5f, 20);
        }

        // Actual fill
        if (lo > 0f) {
            float fillW = Math.max(h, w * lo);

            shapeRenderer.setColor(r, g, b, 1f);
            shapeRenderer.rect(
                x + radius,
                y,
                Math.max(0f, fillW - radius * 2f),
                h
            );
            shapeRenderer.circle(x + radius, y + h * 0.5f, radius, 20);
            shapeRenderer.circle(
                Math.min(x + fillW - radius, x + w - radius),
                y + h * 0.5f,
                radius,
                20
            );

            // Highlight strip
            shapeRenderer.setColor(1f, 1f, 1f, 0.16f);
            shapeRenderer.rect(
                x + radius,
                y + h * 0.62f,
                Math.max(0f, fillW - radius * 2f),
                h * 0.22f
            );
        }
    }

    private void renderHud() {
        float delta = Gdx.graphics.getDeltaTime();
        float screenH = Gdx.graphics.getHeight();

        float x0 = 18f;
        float y0 = screenH - 122f;

        float W = 350f;
        float H = 88f;

        int maxHp = playerMaxHp;
        int maxMana = getMaxMana();

        // ---------------------------------------------------------
        // Animated values
        // ---------------------------------------------------------

        if (hudHp < 0f) {
            hudHp = playerHp;
            hudMana = mana;
        }

        hudHp += (playerHp - hudHp) * Math.min(1f, delta * 3.5f);
        hudMana += (mana - hudMana) * Math.min(1f, delta * 3.5f);

        if (hudLastGold < 0) {
            hudLastGold = gold;
            hudGold = gold;
        }

        if (gold != hudLastGold) {
            int d = gold - hudLastGold;

            hudLastGold = gold;
            goldPunch = 1f;
            goldDir = d > 0 ? 1 : -1;

            if (d > 0) {
                spawnFloat(
                    "+" + d,
                    x0 + W - 55f,
                    y0 + H - 10f,
                    1f,
                    .85f,
                    .25f
                );
            } else {
                spawnFloat(
                    "" + d,
                    x0 + W - 55f,
                    y0 + H - 10f,
                    1f,
                    .4f,
                    .35f
                );
            }
        }

        hudGold += (gold - hudGold) * Math.min(1f, delta * 7f);

        if (Math.abs(gold - hudGold) < 0.5f) {
            hudGold = gold;
        }

        goldPunch = Math.max(0f, goldPunch - delta * 2.5f);

        int arrows = player.getArrowsRemaining();
        hudArrowMax = Math.min(6, Math.max(hudArrowMax, arrows));

        // ---------------------------------------------------------
        // Panel
        // ---------------------------------------------------------

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Shadow
        shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        shapeRenderer.rect(x0 + 3f, y0 - 3f, W, H);

        // Main panel
        shapeRenderer.setColor(0.055f, 0.065f, 0.10f, 0.94f);
        shapeRenderer.rect(x0, y0, W, H);

        // Inner panel
        shapeRenderer.setColor(0.09f, 0.105f, 0.16f, 0.92f);
        shapeRenderer.rect(x0 + 3f, y0 + 3f, W - 6f, H - 6f);

        // Gold top edge
        shapeRenderer.setColor(.95f, .80f, .30f, .9f);
        shapeRenderer.rect(x0 + 3f, y0 + H - 3f, W - 6f, 3f);

        // Divider
        shapeRenderer.setColor(1f, 1f, 1f, .07f);
        shapeRenderer.rect(x0 + 225f, y0 + 12f, 1.5f, H - 24f);

        // ---------------------------------------------------------
        // HP / MANA
        // ---------------------------------------------------------

        float lowPulse =
            playerHp <= 1
                ? .78f + .22f * MathUtils.sin(uiTime * 7f)
                : 1f;

        drawPillBar(
            x0 + 44f,
            y0 + 53f,
            158f,
            17f,
            hudHp,
            playerHp,
            maxHp,
            .90f * lowPulse,
            .20f * lowPulse,
            .23f * lowPulse
        );

        drawPillBar(
            x0 + 44f,
            y0 + 29f,
            158f,
            15f,
            hudMana,
            mana,
            maxMana,
            .22f,
            .50f,
            .95f
        );

        // ---------------------------------------------------------
        // DEF pips
        // ---------------------------------------------------------

        for (int i = 0; i < MAX_DEFENSE; i++) {
            float px = x0 + 55f + i * 22f;
            float py = y0 + 10f;

            shapeRenderer.setColor(0f, 0f, 0f, .8f);
            shapeRenderer.circle(px, py, 8f, 20);

            if (i < defense) {
                shapeRenderer.setColor(.35f, .65f, 1f, 1f);
                shapeRenderer.circle(px, py, 6.2f, 20);

                shapeRenderer.setColor(1f, 1f, 1f, .20f);
                shapeRenderer.circle(px - 1.5f, py + 1.5f, 2.2f, 12);
            } else {
                shapeRenderer.setColor(.12f, .15f, .22f, 1f);
                shapeRenderer.circle(px, py, 6.2f, 20);
            }
        }

        // ---------------------------------------------------------
        // Arrow pips
        // ---------------------------------------------------------

        for (int i = 0; i < hudArrowMax; i++) {
            float px = x0 + 128f + i * 12f;
            float py = y0 + 10f;

            if (i < arrows) {
                shapeRenderer.setColor(.95f, .78f, .24f, 1f);

                shapeRenderer.rect(
                    px - 1.5f,
                    py - 5f,
                    3f,
                    10f
                );

                shapeRenderer.triangle(
                    px - 4f,
                    py + 1f,
                    px + 4f,
                    py + 1f,
                    px,
                    py + 7f
                );
            } else {
                shapeRenderer.setColor(.20f, .22f, .27f, 1f);

                shapeRenderer.rect(
                    px - 1.5f,
                    py - 5f,
                    3f,
                    10f
                );
            }
        }

        // ---------------------------------------------------------
        // Bouncing arrow indicator
        // ---------------------------------------------------------

        if (bounceArrows > 0) {
            float bx = x0 + 205f;
            float by = y0 + 10f;

            shapeRenderer.setColor(1f, .45f, .10f, 1f);

            shapeRenderer.triangle(
                bx, by + 7f,
                bx - 7f, by,
                bx, by - 7f
            );

            shapeRenderer.triangle(
                bx, by + 7f,
                bx + 7f, by,
                bx, by - 7f
            );
        }

        // ---------------------------------------------------------
        // Gold coin
        // ---------------------------------------------------------

        float coinW =
            7f * (.3f + .7f * Math.abs(MathUtils.cos(uiTime * 2.5f)));

        shapeRenderer.setColor(.35f, .24f, .04f, 1f);
        shapeRenderer.ellipse(
            x0 + 275f - coinW - 1f,
            y0 + 52f - 7f,
            (coinW + 1f) * 2f,
            16f
        );

        shapeRenderer.setColor(1f, .82f, .25f, 1f);
        shapeRenderer.ellipse(
            x0 + 275f - coinW,
            y0 + 52f - 6f,
            coinW * 2f,
            13f
        );

        shapeRenderer.end();

        // ---------------------------------------------------------
        // Text
        // ---------------------------------------------------------

        batch.begin();

        // HP
        hudText(
            "HP",
            x0 + 10f,
            y0 + 59f,
            .42f,
            .95f,
            .52f,
            .52f
        );

        hudText(
            playerHp + "/" + maxHp,
            x0 + 148f,
            y0 + 59f,
            .46f,
            1f,
            1f,
            1f
        );

        // Mana
        hudText(
            "MP",
            x0 + 10f,
            y0 + 35f,
            .42f,
            .45f,
            .70f,
            1f
        );

        hudText(
            mana + "/" + maxMana,
            x0 + 148f,
            y0 + 35f,
            .46f,
            1f,
            1f,
            1f
        );

        // Defense
        hudText(
            "DEF",
            x0 + 10f,
            y0 + 10f,
            .38f,
            .55f,
            .72f,
            1f
        );

        // Arrows
        hudText(
            "ARR",
            x0 + 91f,
            y0 + 10f,
            .38f,
            .95f,
            .80f,
            .30f
        );

        // Bounce count
        if (bounceArrows > 0) {
            hudText(
                "x" + bounceArrows,
                x0 + 208f,
                y0 + 10f,
                .42f,
                1f,
                .55f,
                .15f
            );
        }

        // ---------------------------------------------------------
        // Right-side info
        // ---------------------------------------------------------

        hudText(
            "GOLD",
            x0 + 238f,
            y0 + 69f,
            .36f,
            .65f,
            .68f,
            .75f
        );

        float gr = 1f;
        float gg = 1f;
        float gb = 1f;

        if (goldPunch > 0f) {
            float cr = goldDir > 0 ? .45f : 1f;
            float cg = goldDir > 0 ? 1f : .4f;
            float cb = goldDir > 0 ? .5f : .35f;

            gr = MathUtils.lerp(1f, cr, goldPunch);
            gg = MathUtils.lerp(1f, cg, goldPunch);
            gb = MathUtils.lerp(1f, cb, goldPunch);
        }

        hudText(
            String.valueOf(Math.round(hudGold)),
            x0 + 292f,
            y0 + 47f,
            .78f * (1f + goldPunch * .30f),
            gr,
            gg,
            gb
        );

        hudText(
            "ROOM",
            x0 + 238f,
            y0 + 18f,
            .36f,
            .68f,
            .70f,
            .78f
        );

        hudText(
            String.valueOf(difficulty),
            x0 + 292f,
            y0 + 18f,
            .52f,
            .95f,
            .80f,
            .30f
        );

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        batch.end();
    }

    // MANA (relic-aware)
    private int getMaxMana() {
        int m = BASE_MAX_MANA + upLevels[2];
        if (hasRelic(Relic.Blessing.ARCANE_WELL)) m += 2;
        if (hasCurse(Relic.Curse.DRAINED)) m -= 2;
        if (runPact[2]) m -= 2;
        return Math.max(1, m);
    }

    private float cardRewardChance() {
        float chance = CARD_REWARD_CHANCE;
        if (hasRelic(Relic.Blessing.LUCKY_DRAW)) chance += 0.25f;
        if (hasCurse(Relic.Curse.STINGY_DECK)) chance -= 0.25f;
        if (runPact[4]) chance -= 0.20f;
        if (omen == OMEN_FAMINE) chance *= 0.5f;
        return MathUtils.clamp(chance, 0f, 1f);
    }

    // MANA COSTS (edit the base numbers to tune spell balance)
    private int manaCost(Spell spell) {
        int cost;
        if (spell == Spell.LIGHTNING) cost = 3;
        else if (spell == Spell.POISON) cost = 2;
        else if (spell == Spell.BURN) cost = 2;
        else if (spell == Spell.CONFUSE) cost = 4;
        else if (spell == Spell.CLOAK) cost = 3;
        else if (spell == Spell.STUN) cost = 2;
        else if (spell == Spell.ICE) cost = 2;
        else if (spell == Spell.FREEZE) cost = 3;
        else if (spell == Spell.GLASSING) cost = 4;
        else if (spell == Spell.SHIELD) cost = 3;
        else if (spell == Spell.CLEANSE) cost = 2;
        else cost = 2;
        if (hasRelic(Relic.Blessing.THRIFTY_MAGE)) cost -= 1;
        if (hasCurse(Relic.Curse.COSTLY_MAGIC)) cost += 1;
        if (omen == OMEN_MANA_TIDE) cost -= 1;
        if (omen == OMEN_ECLIPSE) cost+=1;
        if (runPact[2]) cost -= 1;
        return Math.max(1, cost);
    }

    private void buildStartingHand() {
        hand.clear();
        hand.add(createCard(Card.MovementType.KNIGHT));
        hand.add(createCard(Card.MovementType.BISHOP));
        hand.add(createCard(Card.MovementType.ROOK));
        hand.add(createCard(Card.MovementType.QUEEN));
        hand.add(createCard(Card.MovementType.CLAUDE));
    }

    private Card createCard(Card.MovementType type) {
        Card card;
        switch (type) {
            case KNIGHT:
                card = new Card("Knight", Card.MovementType.KNIGHT, 2);
                break;
            case BISHOP:
                card = new Card("Bishop", Card.MovementType.BISHOP, 2);
                break;
            case ROOK:
                card = new Card("Rook", Card.MovementType.ROOK, 2);
                break;
            case QUEEN:
                card = new Card("Queen", Card.MovementType.QUEEN, 2);
                break;
            case PAWN:
                card = new Card("Pawn", Card.MovementType.PAWN, 2);
                break;
            case DASH:
                card = new Card("Dash", Card.MovementType.DASH, 2);
                break;
            case CLAUDE:
                card = new Card("Claude", Card.MovementType.CLAUDE, 1);
                break;
            case ARCHBISHOP:
                card = new Card("ArchBishop", Card.MovementType.ARCHBISHOP, 2);
                break;
            case COUNCILLOR:
                card = new Card("Councillor", Card.MovementType.COUNCILLOR, 2);
                break;
            case SHIFTER:
                card = new Card("Shifter", Card.MovementType.SHIFTER, 3);
                break;
            default:
                return createCard(Card.MovementType.KNIGHT);
        }
        if (random.nextFloat() < 0.25f) {
            CardModifier modifier = CardModifier.random(random);
            cardModifiers.put(card, modifier);
        }
        return card;
    }

    // Chance that a capture is a "critical capture" (bonus gold + mana)
    private float critChance() {
        return (omen == OMEN_FORTUNE ? 0.40f : 0.12f) + (runPact[3] ? 0.20f : 0f);
    }

    // ---------- Omens ----------
    private void rollOmen() {
        omen = OMEN_NONE;
        if (difficulty < 2) return;
        float chance = Math.min(0.80f, 0.55f + 0.03f * (difficulty - 2));
        if (random.nextFloat() >= chance) return;
        // Brutal omens (6-10) only appear from room 3
        int pool = difficulty >= 3 ? OMEN_COUNT : OMEN_FIRST_BRUTAL - 1;
        omen = 1 + random.nextInt(pool);
    }

    private Color omenColor() {
        return omenColorFor(omen);
    }

    private Color omenColorFor(int id) {
        switch (id) {
            case OMEN_BLOOD_MOON:
                return new Color(.95f, .30f, .30f, 1f);
            case OMEN_FORTUNE:
                return new Color(1f, .85f, .30f, 1f);
            case OMEN_VENOM:
                return new Color(.45f, .95f, .35f, 1f);
            case OMEN_MANA_TIDE:
                return new Color(.40f, .70f, 1f, 1f);
            case OMEN_IRON:
                return new Color(.72f, .76f, .85f, 1f);
            case OMEN_FAMINE:
                return new Color(.75f, .60f, .35f, 1f);
            case OMEN_WRATH:
                return new Color(1f, .40f, .25f, 1f);
            case OMEN_ECLIPSE:
                return new Color(.60f, .45f, .90f, 1f);
            case OMEN_DREAD:
                return new Color(.80f, .25f, .55f, 1f);
            case OMEN_WITHERING:
                return new Color(.60f, .65f, .55f, 1f);
            default:
                return new Color(.8f, .8f, .8f, 1f);
        }
    }

    private void generateEnemiesForCurrentRoom() {
        BlitzOmen blitzOmen = blitzMode && blitzManager != null ? blitzManager.getCurrentOmen() : null;
        enemies = blitzOmen != null
            ? enemyGenerator.generateEnemies(difficulty, player.getX(), player.getY(), blitzOmen)
            : enemyGenerator.generateEnemies(difficulty, player.getX(), player.getY());
        startRoomObjective();
        tooltipEnemy = null;
        pendingArrivals.clear();
        enemyCurses.clear();
        captureCurses.clear();
        stunnedTurns.clear();
        glassedTurns.clear();
        freezeTurns = 0;
        poisonTurns.clear();
        burnTurns.clear();
        armorShields.clear();
        confusedUntil.clear();
        bolts.clear();
        sparks.clear();
        floats.clear();
        invisibleTurns = 0;
        frenzyTurns = 0;
        if (blitzMode) {
            omen = OMEN_NONE;
            BlitzOmen bo = blitzManager.getCurrentOmen();
            if (bo != null) {
                showPopup("BLITZ OMEN: " + bo.getName().toUpperCase(),
                    1f, .65f, .25f, 1.8f);
            }
        } else {
            rollOmen();
        }
        if (omen != OMEN_NONE) {
            Color oc = omenColor();
            showPopup("OMEN: " + OMEN_NAMES[omen].toUpperCase(), oc.r, oc.g, oc.b, 1.8f);
        }
        for (int i = 0; i < enemies.size; i++) {
            assignEnemyCurse(enemies.get(i));
            assignCaptureCurse(enemies.get(i));
        }
        if (omen == OMEN_BLOOD_MOON) for (int i = 0; i < enemies.size; i++) forceEnemyCurse(enemies.get(i));
        if (enemyCurses.size > 0) playSfx(curseSound);
        if (hasCurse(Relic.Curse.HARDENED)) {
            for (int i = 0; i < enemies.size; i++) {
                Enemy en = enemies.get(i);
                Integer s = armorShields.get(en);
                armorShields.put(en, (s == null ? 0 : s) + 1);
            }
        }
        if (omen == OMEN_IRON) {
            for (int i = 0; i < enemies.size; i++) {
                Enemy en = enemies.get(i);
                Integer s = armorShields.get(en);
                armorShields.put(en, (s == null ? 0 : s) + 1);
            }
        }
        if (runPact[5]) {
            for (int i = 0; i < enemies.size; i++) {
                Enemy en = enemies.get(i);
                Integer s = armorShields.get(en);
                armorShields.put(en, (s == null ? 0 : s) + 1);
            }
        }
        if (omen == OMEN_VENOM) {
            for (int i = 0; i < enemies.size; i++) poisonTurns.put(enemies.get(i), 3);
        }
        startEnemyDrop();
    }

    private void assignCaptureCurse(Enemy enemy) {
        if (enemy == null) return;
        CaptureCurse[] pool = CaptureCurse.values();
        captureCurses.put(enemy, pool[random.nextInt(pool.length)]);
    }

    // Curses get more common the deeper you go (25% in room 1, +5% per room, max 75%)
    private float curseChance() {
        return Math.min(
            0.65f,
            0.18f
                + 0.035f * (difficulty - 1)
                + (runPact[3] ? 0.15f : 0f)
        );
    }

    private void assignEnemyCurse(Enemy enemy) {
        if (random.nextFloat() >= curseChance()) return;
        forceEnemyCurse(enemy);
    }

    private void forceEnemyCurse(Enemy enemy) {
        if (enemy == null || enemyCurses.get(enemy) != null) return;

        EnemyCurse curse = rollCurse();
        enemyCurses.put(enemy, curse);

        if (curse == EnemyCurse.ARMORED) armorShields.put(enemy, 2);
        if (curse == EnemyCurse.JUGGERNAUT) armorShields.put(enemy, 3);
    }

    // Weighted pool: new curses unlock deeper into the run
    private EnemyCurse rollCurse() {
        Array<EnemyCurse> pool = new Array<>();
        EnemyCurse[] base = {EnemyCurse.ARMORED, EnemyCurse.WARDED, EnemyCurse.VENGEFUL, EnemyCurse.VOLATILE,
            EnemyCurse.ANCHORED, EnemyCurse.GREEDY, EnemyCurse.SAPPING, EnemyCurse.HASTY, EnemyCurse.THORNY, EnemyCurse.FRAIL};
        for (EnemyCurse c : base) {
            pool.add(c);
            pool.add(c);
        }
        if (difficulty >= 3) {
            EnemyCurse[] tier1 = {EnemyCurse.BRUTAL, EnemyCurse.CRIPPLING, EnemyCurse.CORROSIVE, EnemyCurse.HUNTER};
            for (EnemyCurse c : tier1) {
                pool.add(c);
                pool.add(c);
            }
        }
        if (difficulty >= 4) {
            pool.add(EnemyCurse.MARTYR);
            pool.add(EnemyCurse.MARTYR);
        }
        if (difficulty >= 5) {
            EnemyCurse[] tier3 = {EnemyCurse.RELENTLESS, EnemyCurse.JUGGERNAUT};
            for (EnemyCurse c : tier3) {
                pool.add(c);
                pool.add(c);
            }
        }
        if (difficulty >= 6) pool.add(EnemyCurse.MAIMING);
        return pool.get(random.nextInt(pool.size));
    }

    private void positionCards() {
        if (hand == null || hand.size == 0) return;
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        if (w >= h) {
            float spacing = 10f;
            float required = hand.size * 155f + (hand.size - 1) * spacing;
            float scale = MathUtils.clamp(h - 40f < required ? (h - 40f) / required : 1f, 0.50f, 0.82f);
            float cw = 115f * scale, ch = 155f * scale, gap = spacing * scale;
            float startY = (h - (hand.size * ch + (hand.size - 1) * gap)) / 2f;
            for (int i = 0; i < hand.size; i++) {
                Card c = hand.get(i);
                c.setLayoutScale(scale);
                c.setPosition(w - cw - 20f, startY + i * (ch + gap));
            }
        } else {
            float spacing = 6f;
            float required = hand.size * 115f + (hand.size - 1) * spacing;
            float scale = MathUtils.clamp(w - 20f < required ? (w - 20f) / required : 1f, 0.42f, 0.75f);
            float cw = 115f * scale, ch = 155f * scale, gap = spacing * scale;
            float startX = (w - (hand.size * cw + (hand.size - 1) * gap)) / 2f;
            for (int i = 0; i < hand.size; i++) {
                Card c = hand.get(i);
                c.setLayoutScale(scale);
                c.setPosition(startX + i * (cw + gap), 15f);
            }
        }
    }

    private void curseAllEnemies() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive()) forceEnemyCurse(e);
        }
        playSfx(curseSound);
    }

    private void updateCardHover() {
        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        for (Card c : hand) c.setHovered(c.isClicked(mx, my));
    }

    private void startEnemyDrop() {
        for (int i = 0; i < enemies.size; i++) enemies.get(i).startFall(0.15f + i * DROP_STAGGER);
    }

    private boolean anyEnemyFalling() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && e.isFalling()) return true;
        }
        return false;
    }

    private void checkEnemyLandings() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.consumeLanded()) {
                spawnShockwave(e.getX(), e.getY(), 0.9f);
                spawnBurst(tileCenterX(e.getX()), tileCenterY(e.getY()), 10, .7f, .65f, .55f, 25f);
                startShake(0.12f, 4f);
                moveSound.play();
            }
        }
    }

    private void spawnShockwave(int x, int y, float radius) {
        Shockwave w = new Shockwave();
        w.x = tileCenterX(x);
        w.y = tileCenterY(y);
        w.maxRadius = radius * Player.TILE_SIZE;
        shockwaves.add(w);
    }

    private void updateShockwaves(float delta) {
        for (int i = shockwaves.size - 1; i >= 0; i--) {
            Shockwave w = shockwaves.get(i);
            w.timer += delta;
            if (w.timer >= SHOCKWAVE_DURATION) shockwaves.removeIndex(i);
        }
    }

    private void renderShockwaves() {
        if (shockwaves.size == 0) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < shockwaves.size; i++) {
            Shockwave w = shockwaves.get(i);
            float p = MathUtils.clamp(w.timer / SHOCKWAVE_DURATION, 0f, 1f);
            float r = w.maxRadius * (1f - (1f - p) * (1f - p));
            shapeRenderer.setColor(1f, .85f, .4f, 1f - p);
            shapeRenderer.circle(w.x, w.y, r, 48);
        }
        shapeRenderer.end();
    }

    private Enemy getEnemyAt(int x, int y, Enemy ignore) {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e != ignore && e.isAlive() && e.getX() == x && e.getY() == y) return e;
        }
        return null;
    }

    private void giveRandomCard() {
        if (hand.size >= MAX_HAND_SIZE) {
            message("Hand full!", 2f);
            return;
        }
        if (random.nextFloat() > cardRewardChance()) {
            message("No card this turn...", 2f);
            return;
        }
        Card.MovementType[] types = {Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK, Card.MovementType.QUEEN, Card.MovementType.PAWN, Card.MovementType.DASH, Card.MovementType.CLAUDE};
        Card c = createCard(types[random.nextInt(types.length)]);
        hand.add(c);
        positionCards();
        playSfx(drawSound);
        message("You received a " + c.getName() + "!", 2f);
    }

    // Enemy piece breaks into falling fragments
    private void spawnCrumble(int tx, int ty) {
        float cx = tileCenterX(tx), cy = tileCenterY(ty);
        for (int i = 0; i < 16; i++) {
            Debris d = new Debris();
            float angle = random.nextFloat() * MathUtils.PI2;
            float speed = 120f + random.nextFloat() * 200f;
            d.x = cx + MathUtils.random(-14f, 14f);
            d.y = cy + MathUtils.random(-14f, 14f);
            d.vx = MathUtils.cos(angle) * speed;
            d.vy = MathUtils.sin(angle) * speed + 160f;
            d.rot = random.nextFloat() * 360f;
            d.vrot = MathUtils.random(-540f, 540f);
            d.size = MathUtils.random(6f, 16f);
            d.maxLife = MathUtils.random(0.6f, 1.0f);
            d.life = d.maxLife;
            debris.add(d);
        }
        spawnShockwave(tx, ty, 0.5f);
    }

    private void updateDebris(float delta) {
        for (int i = debris.size - 1; i >= 0; i--) {
            Debris d = debris.get(i);
            d.life -= delta;
            if (d.life <= 0f) {
                debris.removeIndex(i);
                continue;
            }
            d.vy -= 900f * delta;
            d.x += d.vx * delta;
            d.y += d.vy * delta;
            d.rot += d.vrot * delta;
        }
    }

    private void renderDebris() {
        if (debris.size == 0) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < debris.size; i++) {
            Debris d = debris.get(i);
            shapeRenderer.setColor(.78f, .74f, .66f, MathUtils.clamp(d.life / d.maxLife, 0f, 1f));
            shapeRenderer.rect(d.x - d.size / 2f, d.y - d.size / 2f, d.size / 2f, d.size / 2f, d.size, d.size, 1f, 1f, d.rot);
        }
        shapeRenderer.end();
    }

    // ---------- Floating combat text ----------
    private void spawnFloat(String text, float x, float y, float r, float g, float b) {
        if (floats.size > 40) floats.removeIndex(0);
        FloatText f = new FloatText();
        f.text = text;
        f.x = x + MathUtils.random(-10f, 10f);
        f.y = y;
        f.maxLife = 0.9f;
        f.life = f.maxLife;
        f.r = r;
        f.g = g;
        f.b = b;
        floats.add(f);
    }

    private void updateFloats(float delta) {
        for (int i = floats.size - 1; i >= 0; i--) {
            FloatText f = floats.get(i);
            f.life -= delta;
            if (f.life <= 0f) {
                floats.removeIndex(i);
                continue;
            }
            f.y += 45f * delta;
        }
    }

    private void renderFloatTexts() {
        if (floats.size == 0) return;
        batch.begin();
        font.getData().setScale(.65f);
        for (int i = 0; i < floats.size; i++) {
            FloatText f = floats.get(i);
            float a = MathUtils.clamp(f.life / f.maxLife * 1.6f, 0f, 1f);
            font.setColor(f.r, f.g, f.b, a);
            tipLayout.setText(font, f.text);
            font.draw(batch, f.text, f.x - tipLayout.width / 2f, f.y);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // Kills an enemy outright (ignores armor shields) - used for captures so nothing is left on a tile
    private void killEnemy(Enemy enemy) {
        if (enemy == null) return;
        armorShields.remove(enemy);
        damageEnemy(enemy, 999);
    }

    // Confuse: move to a random free tile (never onto the player or another enemy)
    private void relocateEnemyRandomly(Enemy enemy) {
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = random.nextInt(Player.BOARD_SIZE), y = random.nextInt(Player.BOARD_SIZE);
            if (x == player.getX() && y == player.getY()) continue;
            if (getEnemyAt(x, y, enemy) != null) continue;
            enemy.setX(x);
            enemy.setY(y);
            return;
        }
    }

    // ---------- Spell visual effects ----------
    private void spawnBurst(float cx, float cy, int count, float r, float g, float b, float lift) {
        for (int i = 0; i < count; i++) {
            Spark s = new Spark();
            float angle = random.nextFloat() * MathUtils.PI2;
            float speed = 30f + random.nextFloat() * 110f;
            s.x = cx + MathUtils.random(-8f, 8f);
            s.y = cy + MathUtils.random(-8f, 8f);
            s.vx = MathUtils.cos(angle) * speed;
            s.vy = MathUtils.sin(angle) * speed + lift;
            s.maxLife = MathUtils.random(0.4f, 0.85f);
            s.life = s.maxLife;
            s.size = MathUtils.random(3f, 7f);
            s.r = r;
            s.g = g;
            s.b = b;
            sparks.add(s);
        }
    }

    private void spawnBolt(float x1, float y1, float x2, float y2, int segs, float jitter) {
        Bolt b = new Bolt();
        b.xs = new float[segs + 1];
        b.ys = new float[segs + 1];
        for (int k = 0; k <= segs; k++) {
            float t = (float) k / segs;
            boolean end = (k == 0 || k == segs);
            b.xs[k] = MathUtils.lerp(x1, x2, t) + (end ? 0f : MathUtils.random(-jitter, jitter));
            b.ys[k] = MathUtils.lerp(y1, y2, t) + (end ? 0f : MathUtils.random(-jitter, jitter));
        }
        bolts.add(b);
    }

    private void updateSpellFx(float delta) {
        for (int i = sparks.size - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            s.life -= delta;
            if (s.life <= 0f) {
                sparks.removeIndex(i);
                continue;
            }
            s.x += s.vx * delta;
            s.y += s.vy * delta;
            s.vx *= 0.96f;
            s.vy *= 0.96f;
        }
        for (int i = bolts.size - 1; i >= 0; i--) {
            Bolt b = bolts.get(i);
            b.timer += delta;
            if (b.timer >= BOLT_DURATION) bolts.removeIndex(i);
        }
    }

    // Poisoned tiles: bubbling toxic pools drawn right on top of the board
    private void renderPoisonTiles() {
        if (menuVisible || introActive || board == null) return;
        float ts = Player.TILE_SIZE;
        int n = Player.BOARD_SIZE;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                if (!board.isPoisoned(x, y)) continue;
                float cx = tileCenterX(x), cy = tileCenterY(y);
                float pulse = .5f + .5f * MathUtils.sin(uiTime * 3f + x * 1.7f + y * 2.3f);
                shapeRenderer.setColor(.22f, .65f, .18f, .22f + .12f * pulse);
                shapeRenderer.circle(cx, cy, ts * .40f + pulse * 3f, 28);
                shapeRenderer.setColor(.12f, .42f, .10f, .30f);
                shapeRenderer.circle(cx, cy, ts * .24f + pulse * 2f, 22);
                for (int k = 0; k < 4; k++) {
                    float ph = (uiTime * .7f + k * .27f + ((x * 7 + y * 13) % 10) * .1f) % 1f;
                    float bx = cx + MathUtils.sin(k * 2.1f + x * 1.3f) * ts * .22f;
                    float by = cy - ts * .15f + ph * ts * .45f;
                    float r = 2f + (1f - ph) * 3.5f;
                    shapeRenderer.setColor(.6f, 1f, .4f, (1f - ph) * .8f);
                    shapeRenderer.circle(bx, by, r, 10);
                }
            }
        }
        shapeRenderer.end();
    }

    // Looping burn / poison / confused visuals are driven by the existing status maps,
    // so they start and stop on their own.
    private void renderSpellFx() {
        boolean any = sparks.size > 0 || bolts.size > 0 || burnTurns.size > 0 || poisonTurns.size > 0 || confusedUntil.size > 0;
        if (!any) return;
        float ts = Player.TILE_SIZE;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() || e.isFalling()) continue;
            float cx = tileCenterX(e.getX()), cy = tileCenterY(e.getY());

            Integer bn = burnTurns.get(e);
            if (bn != null && bn > 0) {
                for (int k = 0; k < 7; k++) {
                    float ph = (uiTime * 1.6f + k / 7f) % 1f;
                    float px = cx + MathUtils.sin(k * 12.9898f) * ts * 0.28f + MathUtils.sin(uiTime * 9f + k) * 3f;
                    float py = cy - ts * 0.30f + ph * ts * 0.75f;
                    float rad = (1f - ph) * ts * 0.14f + 2f;
                    shapeRenderer.setColor(1f, .45f + .4f * ph, .10f, (1f - ph) * .85f);
                    shapeRenderer.circle(px, py, rad, 10);
                }
            }

            // Poison: toxic aura grows with stacks, rising bubbles, and drips running down the piece
            Integer pz = poisonTurns.get(e);
            if (pz != null && pz > 0) {
                float inten = MathUtils.clamp(pz / 6f, 0.35f, 1f);
                float pulse = .5f + .5f * MathUtils.sin(uiTime * 4f + i);

                int bubbles = 5 + (int) (inten * 5f);
                for (int k = 0; k < bubbles; k++) {
                    float ph = (uiTime * (.8f + .05f * k) + k / (float) bubbles) % 1f;
                    float px = cx + MathUtils.sin(k * 7.77f) * ts * .30f + MathUtils.sin(uiTime * 3f + k * 2f) * 4f;
                    float py = cy - ts * .25f + ph * ts * .75f;
                    float rad = ts * .05f + (k % 3) * 1.5f;
                    shapeRenderer.setColor(.45f, .95f, .30f, (1f - ph) * .8f);
                    shapeRenderer.circle(px, py, rad, 10);
                }

                for (int k = 0; k < 3; k++) {
                    float ph = (uiTime * 1.1f + k * .33f) % 1f;
                    float dx = cx + (k - 1) * ts * .18f;
                    float dy = cy + ts * .05f - ph * ts * .40f;
                    shapeRenderer.setColor(.35f, .9f, .25f, (1f - ph) * .9f);
                    shapeRenderer.circle(dx, dy, 2.5f + (1f - ph) * 1.5f, 8);
                }
            }

            Float cf = confusedUntil.get(e);
            if (cf != null && uiTime < cf) {
                for (int k = 0; k < 3; k++) {
                    float ang = uiTime * 5f + k * MathUtils.PI2 / 3f;
                    float sx = cx + MathUtils.cos(ang) * ts * .28f;
                    float sy = cy + ts * .38f + MathUtils.sin(ang) * ts * .08f;
                    shapeRenderer.setColor(1f, .9f, .3f, 1f);
                    shapeRenderer.rect(sx - 4f, sy - 4f, 4f, 4f, 8f, 8f, 1f, 1f, uiTime * 300f + k * 40f);
                }
            }
        }

        for (int i = 0; i < sparks.size; i++) {
            Spark s = sparks.get(i);
            float a = s.life / s.maxLife;
            shapeRenderer.setColor(s.r, s.g, s.b, a);
            shapeRenderer.circle(s.x, s.y, s.size * a + 1f, 8);
        }

        float flash = 0f;
        for (int i = 0; i < bolts.size; i++) {
            Bolt b = bolts.get(i);
            float a = 1f - b.timer / BOLT_DURATION;
            flash = Math.max(flash, 1f - b.timer / 0.10f);
            for (int k = 0; k < b.xs.length - 1; k++) {
                shapeRenderer.setColor(.4f, .7f, 1f, a * .6f);
                shapeRenderer.rectLine(b.xs[k], b.ys[k], b.xs[k + 1], b.ys[k + 1], 10f * a + 4f);
                shapeRenderer.setColor(1f, 1f, 1f, a);
                shapeRenderer.rectLine(b.xs[k], b.ys[k], b.xs[k + 1], b.ys[k + 1], 3f * a + 1.5f);
            }
        }
        if (flash > 0f) {
            shapeRenderer.setColor(1f, 1f, 1f, .3f * flash);
            shapeRenderer.rect(-60f, -60f, Gdx.graphics.getWidth() + 120f, Gdx.graphics.getHeight() + 120f);
        }

        shapeRenderer.end();
    }

    // ---------- Curse / status icons (round, stacked above enemies) ----------
    private Color curseColor(EnemyCurse c) {
        switch (c.name()) {
            case "ARMORED":
                return new Color(.60f, .65f, .75f, 1f);
            case "WARDED":
                return new Color(.35f, .55f, 1f, 1f);
            case "VOLATILE":
                return new Color(1f, .55f, .15f, 1f);
            case "VENGEFUL":
                return new Color(.90f, .20f, .20f, 1f);
            case "ANCHORED":
                return new Color(.65f, .48f, .25f, 1f);
            case "GREEDY":
                return new Color(.95f, .80f, .20f, 1f);
            case "SAPPING":
                return new Color(.70f, .35f, .90f, 1f);
            case "BRUTAL":
                return new Color(.85f, .15f, .15f, 1f);
            case "CORROSIVE":
                return new Color(.60f, .85f, .20f, 1f);
            case "CRIPPLING":
                return new Color(.80f, .55f, .35f, 1f);
            case "RELENTLESS":
                return new Color(1f, .30f, .55f, 1f);
            case "HUNTER":
                return new Color(.35f, .80f, .70f, 1f);
            case "JUGGERNAUT":
                return new Color(.50f, .50f, .58f, 1f);
            case "MARTYR":
                return new Color(.95f, .95f, .65f, 1f);
            case "MAIMING":
                return new Color(.70f, .10f, .30f, 1f);
            default:
                return new Color(.90f, .90f, .90f, 1f);
        }
    }


    private static final Color COL_SHIELD = new Color(.62f, .74f, .95f, 1f);
    private static final Color COL_POISON = new Color(.45f, .95f, .35f, 1f);
    private static final Color COL_DEFENSE = new Color(.45f, .70f, 1f, 1f);

    // Draws one round icon. Must be called between batch.begin()/end(). count <= 0 hides the number bubble.
    // Draws one round icon. Must be called between batch.begin()/end(). count <= 0 hides the number bubble.
    private void drawRoundIcon(float cx, float cy, float r, Color col, String sym, int count) {
        Texture icon = iconTex.get(sym);
        if (icon != null) {
            batch.setColor(Color.WHITE);
            float d = r * 2f + 4f;
            batch.draw(icon, cx - d / 2f, cy - d / 2f, d, d);
        } else {
            // Fallback: old text badge
            batch.setColor(0f, 0f, 0f, .92f);
            batch.draw(circleTex, cx - r - 2f, cy - r - 2f, (r + 2f) * 2f, (r + 2f) * 2f);
            batch.setColor(col.r, col.g, col.b, 1f);
            batch.draw(circleTex, cx - r, cy - r, r * 2f, r * 2f);
            float ir = r * .78f;
            batch.setColor(col.r * .28f, col.g * .28f, col.b * .28f, 1f);
            batch.draw(circleTex, cx - ir, cy - ir, ir * 2f, ir * 2f);
            batch.setColor(Color.WHITE);

            String badge = sym.length() > 2 ? sym.substring(0, 2).toUpperCase() : sym;
            font.getData().setScale(r / (badge.length() > 1 ? 30f : 24f));
            font.setColor(col.r * .5f + .5f, col.g * .5f + .5f, col.b * .5f + .5f, 1f);
            tipLayout.setText(font, badge);
            font.draw(batch, badge, cx - tipLayout.width / 2f, cy + tipLayout.height / 2f);
        }

        if (count > 0) {
            float br = r * .55f, bx = cx + r * .72f, by = cy - r * .72f;
            batch.setColor(0f, 0f, 0f, .95f);
            batch.draw(circleTex, bx - br - 1.5f, by - br - 1.5f, (br + 1.5f) * 2f, (br + 1.5f) * 2f);
            batch.setColor(col.r * .6f, col.g * .6f, col.b * .6f, 1f);
            batch.draw(circleTex, bx - br, by - br, br * 2f, br * 2f);
            batch.setColor(Color.WHITE);
            String n = String.valueOf(count);
            font.getData().setScale(r / 34f);
            font.setColor(Color.WHITE);
            tipLayout.setText(font, n);
            font.draw(batch, n, bx - tipLayout.width / 2f, by + tipLayout.height / 2f);
        }
        batch.setColor(Color.WHITE);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void renderEnemyCurseBadges() {
        if (enemies == null || menuVisible || introActive) return;

        float ts = Player.TILE_SIZE;
        float r = MathUtils.clamp(ts * .13f, 9f, 14f);

        batch.begin();

        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);

            if (!e.isAlive() || e.isFalling()) continue;

            EnemyCurse curse = enemyCurses.get(e);
            CaptureCurse captureCurse = captureCurses.get(e);

            Integer shield = armorShields.get(e);
            Integer poison = poisonTurns.get(e);
            Integer burn = burnTurns.get(e);
            Float confused = confusedUntil.get(e);

            boolean hasShield = shield != null && shield > 0;
            boolean hasPoison = poison != null && poison > 0;
            boolean hasBurn = burn != null && burn > 0;
            boolean hasConfused = confused != null && uiTime < confused;

            int count = 0;

            if (curse != null) count++;
            if (captureCurse != null) count++;
            if (hasShield) count++;
            if (hasPoison) count++;
            if (hasBurn) count++;
            if (hasConfused) count++;

            if (count == 0) continue;

            float enemyX = tileCenterX(e.getX());
            float enemyY = tileCenterY(e.getY());

            // Put badges on the RIGHT side of the enemy.
            float startX = enemyX + ts * .48f;
            float startY = enemyY + ts * .28f;

            float spacing = r * 1.65f;

            int index = 0;

            // CURSE
            if (curse != null) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    curseColor(curse),
                    curseSymbol(curse),
                    0
                );

                index++;
            }

            // CAPTURE CURSE
            if (captureCurse != null) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    new Color(.95f, .45f, .25f, 1f),
                    captureCurse.getShortLabel(),
                    0
                );
                index++;
            }

            // ARMOR / SHIELD
            if (hasShield) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    COL_SHIELD,
                    "SH",
                    shield
                );

                index++;
            }

            // POISON
            if (hasPoison) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    COL_POISON,
                    "PS",
                    poison
                );

                index++;
            }

            // BURN
            if (hasBurn) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    COL_BURN,
                    "BR",
                    burn
                );

                index++;
            }

            // CONFUSED
            if (hasConfused) {
                drawRoundIcon(
                    startX,
                    startY - spacing * index,
                    r,
                    new Color(1f, .85f, .25f, 1f),
                    "??",
                    0
                );
            }
        }

        batch.end();
    }

    private boolean isWarded(Enemy e) {
        return enemyCurses.get(e) == EnemyCurse.WARDED;
    }

    private void explodeVolatile(Enemy source) {
        int sx = source.getX(), sy = source.getY();
        spawnShockwave(sx, sy, 1.6f);
        spawnBurst(tileCenterX(sx), tileCenterY(sy), 24, 1f, .55f, .15f, 60f);
        startShake(0.25f, 8f);
        message("Volatile enemy exploded!", 1.5f);
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e == source || !e.isAlive()) continue;
            if (Math.abs(e.getX() - sx) <= 1 && Math.abs(e.getY() - sy) <= 1) damageEnemy(e, 1);
        }
        int pd = Math.max(Math.abs(player.getX() - sx), Math.abs(player.getY() - sy));
        if (pd == 1) damagePlayer(1);
    }

    // Plague: a poisoned enemy that dies spreads poison to every neighbour (warded enemies are immune)
    private void spreadPlague(Enemy source, int turns) {
        int sx = source.getX(), sy = source.getY();
        boolean any = false;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e == source || !e.isAlive() || e.isFalling() || isWarded(e)) continue;
            if (Math.abs(e.getX() - sx) <= 1 && Math.abs(e.getY() - sy) <= 1) {
                Integer cur = poisonTurns.get(e);
                poisonTurns.put(e, Math.min(POISON_MAX_TURNS, (cur == null ? 0 : cur) + turns));
                spawnBurst(tileCenterX(e.getX()), tileCenterY(e.getY()), 10, .45f, .95f, .3f, 40f);
                spawnFloat("PLAGUE", tileCenterX(e.getX()), tileCenterY(e.getY()) + 24f, .5f, 1f, .35f);
                any = true;
            }
        }
        spawnBurst(tileCenterX(sx), tileCenterY(sy), 16, .4f, .9f, .25f, 50f);
        if (any) {
            playSfx(poisonSound);
            message("Plague spreads!", 1.3f);
        }
    }

    private boolean damageEnemy(Enemy enemy, int amount) {
        return damageEnemy(enemy, amount, 1f, .35f, .3f);
    }

    private boolean damageEnemy(Enemy enemy, int amount, float fr, float fg, float fb) {
        if (enemy == null || !enemy.isAlive()) return false;
        Integer shield = armorShields.get(enemy);
        if (shield != null && shield > 0) {
            armorShields.put(enemy, shield - 1);
            playSfx(armorSound);
            spawnFloat("BLOCK", tileCenterX(enemy.getX()), tileCenterY(enemy.getY()) + 20f, .7f, .75f, .9f);
            message("Armored blocked 1 damage!", 1.2f);
            return false;
        }
        if (enemyCurses.get(enemy) == EnemyCurse.FRAIL) {
            amount += 1;
            enemyCurses.remove(enemy);

            spawnFloat(
                "+1 FRAIL",
                tileCenterX(enemy.getX()),
                tileCenterY(enemy.getY()) + 35f,
                .8f, .9f, 1f
            );

            message("Frail! +1 damage!", 1.2f);
        }
        Integer glass = glassedTurns.get(enemy);
        if (glass != null && glass > 0) {
            amount += 1;
            if (glass - 1 <= 0) glassedTurns.remove(enemy);
            else glassedTurns.put(enemy, glass - 1);
            spawnFloat("+1 GLASS", tileCenterX(enemy.getX()), tileCenterY(enemy.getY()) + 42f, .55f, .85f, 1f);
        }
        int dealt = Math.min(amount, Math.max(0, enemy.getHealth()));
        boolean died = enemy.takeDamage(amount);
        if (dealt > 0) spawnFloat("-" + dealt, tileCenterX(enemy.getX()), tileCenterY(enemy.getY()) + 20f, fr, fg, fb);
        if (died) {
            EnemyCurse curse = enemyCurses.get(enemy);
            Integer pz = poisonTurns.get(enemy);
            enemyCurses.remove(enemy);
            captureCurses.remove(enemy);
            stunnedTurns.remove(enemy);
            glassedTurns.remove(enemy);
            poisonTurns.remove(enemy);
            burnTurns.remove(enemy);
            playSfx(enemyDeathSound);
            spawnCrumble(enemy.getX(), enemy.getY());
            if (pz != null && pz > 0) spreadPlague(enemy, Math.max(2, pz / 2 + 1));
            if (curse == EnemyCurse.VOLATILE) explodeVolatile(enemy);
            if (curse==EnemyCurse.MARTYR) empowerAllies(enemy);
        }
        return died;
    }
    // Martyr: when it dies, every other living enemy gains 1 shield
    private void empowerAllies(Enemy source) {
        boolean any = false;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e == source || !e.isAlive() || e.isFalling()) continue;
            Integer s = armorShields.get(e);
            armorShields.put(e, (s == null ? 0 : s) + 1);
            spawnFloat("+1 SHIELD", tileCenterX(e.getX()), tileCenterY(e.getY()) + 24f, .62f, .74f, .95f);
            any = true;
        }
        if (any) message("Martyr! Its allies are shielded.", 1.5f);
    }

    private void groundPound(int cx, int cy) {
        spawnShockwave(cx, cy, 1.8f);

        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && !e.isFalling()) {
                int dx = e.getX() - cx;
                int dy = e.getY() - cy;

                // Check if enemy is in 3x3 neighborhood around impact (excluding center)
                if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && (dx != 0 || dy != 0)) {
                    if (hasRelic(Relic.Blessing.SHOCKWAVE)) {
                        damageEnemy(e, 1);
                    }
                    // Push strictly AWAY from center (cx, cy)
                    pushEnemy(e, Integer.signum(dx), Integer.signum(dy));
                }
            }
        }
    }

    private void pushEnemy(Enemy enemy, int dx, int dy) {
        if (enemy == null || !enemy.isAlive()) return;

        if (enemyCurses.get(enemy) == EnemyCurse.ANCHORED) {
            message("Anchored! It won't budge.", 1.2f);
            return;
        }

        int nx = enemy.getX() + dx;
        int ny = enemy.getY() + dy;

        boolean outOfBounds = nx < 0 || nx >= Player.BOARD_SIZE || ny < 0 || ny >= Player.BOARD_SIZE;
        boolean occupiedByEnemy = getEnemyAt(nx, ny, enemy) != null;
        boolean occupiedByPlayer = (nx == player.getX() && ny == player.getY());

        if (outOfBounds || occupiedByEnemy || occupiedByPlayer) {
            spawnShockwave(enemy.getX(), enemy.getY(), 0.6f);
            playSfx(slamSound);
            boolean died = damageEnemy(enemy, 1);
            message(died ? "Enemy crushed!" : "Enemy slammed!", 1.5f);
        } else {
            enemy.pushTo(nx, ny);
        }
    }

    private void pushEnemiesAlongDash(int tx, int ty) {
        int dx = tx - player.getX(), dy = ty - player.getY();
        if (dx != 0 && dy != 0 && Math.abs(dx) != Math.abs(dy)) return;
        int sx = Integer.signum(dx), sy = Integer.signum(dy), steps = Math.max(Math.abs(dx), Math.abs(dy));
        boolean any = false;
        for (int i = steps - 1; i >= 1; i--) {
            Enemy e = getEnemyAt(player.getX() + sx * i, player.getY() + sy * i, null);
            if (e != null && !e.isFalling()) {
                pushEnemy(e, sx, sy);
                any = true;
            }
        }
        if (any) message("Dash shove!", 1.5f);
    }

    private Enemy findFirstEnemyInLine(int tx, int ty) {
        int dx = Integer.signum(tx - player.getX()), dy = Integer.signum(ty - player.getY());
        int x = player.getX() + dx, y = player.getY() + dy;
        while (x >= 0 && x < Player.BOARD_SIZE && y >= 0 && y < Player.BOARD_SIZE) {
            Enemy e = getEnemyAt(x, y, null);
            if (e != null && captureCurses.get(e) != CaptureCurse.ARROWPROOF) return e;
            if (x == tx && y == ty) break;
            x += dx;
            y += dy;
        }
        return null;
    }

    private float tileCenterX(int x) {
        return Player.getBoardX() + x * Player.TILE_SIZE + Player.TILE_SIZE / 2f;
    }

    private float tileCenterY(int y) {
        return Player.getBoardY() + y * Player.TILE_SIZE + Player.TILE_SIZE / 2f;
    }

    private void updateArrowAnimation(float delta) {
        if (!arrowInFlight) return;
        arrowProgress += delta / ARROW_FLIGHT_DURATION;
        if (arrowProgress >= 1f) {
            arrowProgress = 1f;
            resolveArrowHit();
        }
    }

    private boolean isDashCard(Card c) {
        return c.getName().equalsIgnoreCase("Dash") || c.getName().equalsIgnoreCase("Knight Dash")
            || c.getCurrentForm() == Card.MovementType.DASH;
    }

    private void message(String s, float t) {
        eventMessage = s;
        messageTimer = t;
    }

    // Blessings apply whether or not the relic is cursed; curses apply only on cursed relics
    private boolean hasRelic(Relic.Blessing b) {
        for (int i = 0; i < ownedRelics.size; i++) if (ownedRelics.get(i).getBlessing() == b) return true;
        return false;
    }

    private boolean hasCurse(Relic.Curse c) {
        for (int i = 0; i < ownedRelics.size; i++) {
            Relic r = ownedRelics.get(i);
            if (r.isCursed() && r.getCurse() == c) return true;
        }
        return false;
    }

    private void rollPoison() {
        board.setPoisonedTiles(player.getX(), player.getY());
    }

    // ---------- Defense ----------
    private void gainDefense(int amount) {
        if (amount <= 0) return;
        int before = defense;
        defense = Math.min(MAX_DEFENSE, defense + amount);
        if (defense > before) {
            spawnFloat("+" + (defense - before) + " DEF", tileCenterX(player.getX()), tileCenterY(player.getY()) + 52f, .45f, .70f, 1f);
        }
    }

    // Blue DEF icon on the player's tile (top-left corner) so defense is visible on the board
    private void renderDefenseIcon() {
        if (defense <= 0 || menuVisible || introActive || gameOverScreen.isVisible() || player == null) return;
        float ts = Player.TILE_SIZE;
        float r = MathUtils.clamp(ts * .15f, 10f, 16f);
        batch.begin();
        drawRoundIcon(tileCenterX(player.getX()) - ts / 2f + r + 2f, tileCenterY(player.getY()) + ts / 2f - r - 2f, r, COL_DEFENSE, "DF", defense);
        batch.end();
    }

    // ---------- Objectives / Combo / Sacrifice ----------
    private void startRoomObjective() {
        objectiveType = random.nextInt(4);
        if (objectiveType == OBJECTIVE_CAPTURES) {
            objectiveTarget = Math.min(4, 2 + difficulty / 4);
        } else if (objectiveType == OBJECTIVE_COMBO) {
            objectiveTarget = difficulty >= 5 ? 4 : 3;
        } else if (objectiveType == OBJECTIVE_SACRIFICE) {
            objectiveTarget = 1;
        } else {
            objectiveTarget = 1;
        }

        objectiveProgress = 0;
        objectiveCompleted = false;
        roomDamageTaken = false;
        roomSacrificed = false;
        roomRerolled = false;
        captureCombo = 0;
        bestCombo = 0;
        comboRewardedAt = 0;
    }

    private String objectiveName() {
        switch (objectiveType) {
            case OBJECTIVE_CAPTURES:
                return "Capture " + objectiveTarget + " enemies";
            case OBJECTIVE_COMBO:
                return "Reach a x" + objectiveTarget + " combo";
            case OBJECTIVE_SACRIFICE:
                return "Sacrifice a card";
            case OBJECTIVE_NO_DAMAGE:
                return "Take no damage";
            default:
                return "Complete the objective";
        }
    }

    private String objectiveProgressText() {
        if (objectiveType == OBJECTIVE_CAPTURES) return objectiveProgress + "/" + objectiveTarget;
        if (objectiveType == OBJECTIVE_COMBO) return captureCombo + "/" + objectiveTarget;
        if (objectiveType == OBJECTIVE_SACRIFICE) return roomSacrificed ? "DONE" : "0/1";
        return roomDamageTaken ? "FAILED" : "SAFE";
    }

    private void updateObjectiveProgress() {
        if (objectiveType == OBJECTIVE_CAPTURES) {
            objectiveProgress = Math.min(objectiveTarget, objectiveProgress);
            if (objectiveProgress >= objectiveTarget) objectiveCompleted = true;
        } else if (objectiveType == OBJECTIVE_COMBO) {
            objectiveProgress = Math.max(objectiveProgress, captureCombo);
            if (captureCombo >= objectiveTarget) objectiveCompleted = true;
        } else if (objectiveType == OBJECTIVE_SACRIFICE) {
            objectiveCompleted = roomSacrificed;
        } else if (objectiveType == OBJECTIVE_NO_DAMAGE) {
            objectiveCompleted = !roomDamageTaken;
        }
    }

    private void registerCapture(Enemy capturedEnemy, Card captureCard, EnemyCurse captureCurse, boolean assassination, int healthPercent) {
        captureCombo++;

        if (blitzMode && blitzManager != null) {
            String movement = captureCard == null ? "" : captureCard.getMovementType().name();

            BlitzScore.Result result = blitzManager.capture(
                captureCurse != null,
                movement,
                assassination,
                healthPercent,
                gold,
                difficulty
            );

            blitzScorePulse = 1f;
            blitzLastLabel = result.label;
            blitzLastLabelTimer = 1.15f;
            blitzLastResult = result;
            blitzBreakdownTimer = 2.2f;

            float cx = tileCenterX(player.getX());
            float cy = tileCenterY(player.getY());
            spawnFloat(
                "+" + result.points + "  " + result.label,
                cx,
                cy + 92f,
                result.points >= 1000 ? 1f : .9f,
                result.points >= 1000 ? .78f : .62f,
                .25f
            );

            if (result.multiplierUp) {
                showPopup("MULTIPLIER UP!  x" + result.multiplier,
                    1f, .78f, .22f, 1.0f);
                startShake(.12f, 3f);
            } else if (result.points >= 1000) {
                showPopup("+" + result.points + "  " + result.label,
                    1f, .78f, .22f, .85f);
                startShake(.08f, 2.5f);
            }
        }

        runBonusSouls++;
        runBonusSouls++;
        int roll = random.nextInt(0, 100);
        if (roll>80){
            gainDefense(1);
        }
        bestCombo = Math.max(bestCombo, captureCombo);
        // ... your existing capture rewards ...
        if (captureCombo >= 2) {
            if (blitzMode) {
                blitzLastLabel = "CHAIN x" + captureCombo;
                blitzLastLabelTimer = 1.15f;
                blitzScorePulse = Math.max(blitzScorePulse, .65f);
            } else if (captureCombo >= 5) {
                showPopup("x" + captureCombo + " COMBO!", 1f, .35f, .2f, 1.0f);
            } else if (captureCombo >= 3) {
                showPopup("x" + captureCombo + " COMBO!", 1f, .85f, .25f, 1.0f);
            } else {
                showPopup("x" + captureCombo + " COMBO", .6f, .85f, 1f, 1.0f);
            }
        }
        if (frenzyTurns > 0) {
            gold += 5;
            spawnFloat("+5g", tileCenterX(player.getX()), tileCenterY(player.getY()) + 70f, 1f, .8f, .3f);
        } else if (captureCombo == 4) {
            startFrenzy();
        }
        // Add a card if the hand is getting low
        if (hand.size <= 3) {
            addRandomCardToHand();
        }
    }

    // Luck: some captures are critical (bonus gold + mana)
    private boolean rollCriticalCapture(Enemy e) {
        if (random.nextFloat() >= critChance()) return false;

        // Critical capture = instant kill
        killEnemy(e);

        gold += 10;
        mana = Math.min(getMaxMana(), mana + 1);

        float cx = tileCenterX(player.getX());
        float cy = tileCenterY(player.getY());

        spawnBurst(
            cx, cy,
            20,
            1f, .85f, .25f,
            60f
        );

        spawnShockwave(
            player.getX(),
            player.getY(),
            1.3f
        );

        spawnFloat(
            "CRIT! +10g",
            cx,
            cy + 44f,
            1f, .85f, .25f
        );

        playSfx(blessingSound);
        showPopup(
            "CRITICAL!",
            1f, .85f, .25f,
            1.1f
        );

        return true;
    }

    private int rollCaptureDamage() {
        // 75% chance for 1 damage
        // 25% chance for 2 damage
        return random.nextFloat() < 0.75f ? 1 : 2;
    }

    private void breakCombo() {
        if (blitzMode && blitzManager != null) blitzManager.breakStreak();
        if (captureCombo > 0) {
            captureCombo = 0;
            if (objectiveType == OBJECTIVE_COMBO) objectiveProgress = Math.max(objectiveProgress, bestCombo);
            message("Combo broken!", 1.0f);
        }
    }

    private void sacrificeActiveCard() {
        if (activeCard == null) {
            message("Select a card to sacrifice.", 1.5f);
            return;
        }
        if (roomSacrificed) {
            message("Only one sacrifice per room.", 1.5f);
            return;
        }
        if (hand.size <= 1) {
            message("Keep at least one card.", 1.5f);
            return;
        }
        if (playerHp <= 1) {
            playSfx(errorSound);
            message("Too weak to sacrifice (needs 2+ HP).", 1.5f);
            return;
        }

        Card sacrificed = activeCard;
        sacrificed.setSelected(false);
        activeCard = null;
        player.setSelected(false);

        hand.removeValue(sacrificed, true);
        cardModifiers.remove(sacrificed);
        roomSacrificed = true;
        objectiveProgress = 1;
        updateObjectiveProgress();

        // Price: 1 HP (Dark Pact heals it back)
        playerHp = Math.max(1, playerHp - 1);
        healPlayer(upLevels[3]);

        // Rewards
        gold += 20;
        mana = Math.min(getMaxMana(), mana + 2);
        runBonusSouls += 3;
        gainDefense(1);
        breakCombo();

        playSfx(smashSound);
        spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 24, .75f, .25f, .85f, 35f);
        startShake(0.25f, 8f);

        // Blood blast: 1 damage to every enemy (armor still blocks)
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && !e.isFalling()) {
                spawnBurst(tileCenterX(e.getX()), tileCenterY(e.getY()), 8, .75f, .25f, .85f, 20f);
                damageEnemy(e, 1);
            }
        }

        positionCards();
        message("Sacrificed " + sacrificed.getName() + "! -1 HP, +20 gold, +2 mana, +1 DEF, +3 souls", 2.5f);
        autosave();
        if (allEnemiesDefeated()) winRoom();
    }

    // Reroll: spend 1 mana to turn the selected card into a random different card (once per room)
    private void rerollActiveCard() {
        if (activeCard == null) {
            message("Select a card to reroll.", 1.5f);
            return;
        }
        if (roomRerolled) {
            playSfx(errorSound);
            message("Only one reroll per room.", 1.5f);
            return;
        }
        if (mana < 1) {
            playSfx(errorSound);
            message("Need 1 mana to reroll.", 1.5f);
            return;
        }
        int idx = hand.indexOf(activeCard, true);
        if (idx < 0) return;

        Card old = activeCard;
        Card.MovementType[] types = {Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK, Card.MovementType.QUEEN, Card.MovementType.PAWN, Card.MovementType.DASH, Card.MovementType.CLAUDE};
        Card fresh = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            Card candidate = createCard(types[random.nextInt(types.length)]);
            if (candidate.getMovementType() != old.getMovementType()) {
                fresh = candidate;
                break;
            }
            cardModifiers.remove(candidate);
        }
        if (fresh == null) return;

        hand.set(idx, fresh);
        cardModifiers.remove(old);
        old.setSelected(false);
        activeCard = null;
        player.setSelected(false);
        mana -= 1;
        roomRerolled = true;
        positionCards();
        playSfx(drawSound);
        spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 12, .6f, .8f, 1f, 30f);
        message("Rerolled into " + fresh.getName() + "!", 1.8f);
    }

    private void renderObjectivePanel() {
        if (gameOverScreen.isVisible() || shopScreen.isVisible() || rewardScreen.isVisible() || evtVisible) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        float panelW = Math.min(310f, w - 260f);
        if (panelW < 210f) panelW = Math.max(210f, w - 40f);
        float panelH = 82f;
        float x = Math.max(10f, (w - panelW) / 2f);
        float y = h - 120f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.08f, .10f, .16f, .92f);
        shapeRenderer.rect(x, y, panelW, panelH);
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(.62f);
        font.setColor(new Color(.95f, .80f, .30f, 1f));
        font.draw(batch, "OBJECTIVE", x + 12f, y + panelH - 16f);

        font.getData().setScale(.42f);
        font.setColor(Color.WHITE);
        font.draw(batch, objectiveName(), x + 12f, y + panelH - 39f);

        String progress = objectiveProgressText();
        font.setColor(objectiveCompleted ? new Color(.35f, 1f, .55f, 1f)
            : new Color(.75f, .80f, .88f, 1f));
        font.draw(batch, progress, x + 12f, y + 20f);

        font.setColor(new Color(.55f, .65f, .80f, 1f));
        font.draw(batch, "Combo x" + captureCombo + "   Best x" + bestCombo, x + panelW - 135f, y + 20f);

        if (omen != OMEN_NONE) {
            font.getData().setScale(.45f);
            font.setColor(omenColor());
            font.draw(batch, "OMEN: " + OMEN_NAMES[omen], x, y - 8f);
            font.getData().setScale(.38f);
            font.setColor(new Color(.75f, .78f, .85f, 1f));
            font.draw(batch, OMEN_DESC[omen], x, y - 28f);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderBlitzComboText() {
        if (!blitzMode || blitzLastLabelTimer <= 0f) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        blitzLastLabelTimer -= Gdx.graphics.getDeltaTime();
        float alpha = MathUtils.clamp(blitzLastLabelTimer / 0.7f, 0f, 1f);

        batch.begin();
        font.getData().setScale(.68f);
        font.setColor(.55f, .57f, .61f, alpha);
        font.draw(batch, blitzLastLabel, 18f, 42f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderBlitzHud() {
        if (!blitzMode || blitzManager == null) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float delta = Gdx.graphics.getDeltaTime();

        float target = blitzManager.getScore();
        float catchup = 1f - (float)Math.pow(0.001, delta);
        blitzHudScore = MathUtils.lerp(blitzHudScore, target, catchup);
        if (Math.abs(blitzHudScore - target) < 0.5f) blitzHudScore = target;

        blitzScorePulse = Math.max(0f, blitzScorePulse - delta * 3.5f);
        blitzLastLabelTimer = Math.max(0f, blitzLastLabelTimer - delta);
        blitzBreakdownTimer = Math.max(0f, blitzBreakdownTimer - delta);

        // ----- RELIC BAR -----
        int relicCount = blitzManager.getActiveRelics().size();
        float relicAreaW = Math.min(760f, w - 28f);
        float relicGap = 8f;
        float relicCardW = relicCount > 0
            ? Math.min(118f, (relicAreaW - relicGap * (relicCount - 1)) / relicCount)
            : 0f;
        float relicCardH = 62f;
        float relicStart = (w - (relicCardW * relicCount + relicGap * Math.max(0, relicCount - 1))) * .5f;
        float relicY = h - relicCardH - 14f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < relicCount; i++) {
            float rx = relicStart + i * (relicCardW + relicGap);
            shapeRenderer.setColor(.055f, .065f, .105f, .97f);
            shapeRenderer.rect(rx, relicY, relicCardW, relicCardH);
            shapeRenderer.setColor(.45f, .32f, .12f, 1f);
            shapeRenderer.rect(rx, relicY, relicCardW, 2f);
            shapeRenderer.setColor(.95f, .68f, .20f, .9f);
            shapeRenderer.rect(rx, relicY + relicCardH - 3f, relicCardW, 3f);

            // Little relic emblem.
            float cx = rx + 25f;
            float cy = relicY + relicCardH * .5f;
            shapeRenderer.setColor(.17f, .12f, .25f, 1f);
            shapeRenderer.circle(cx, cy, 17f);
            shapeRenderer.setColor(1f, .72f, .22f, 1f);
            shapeRenderer.circle(cx, cy, 13f);
        }
        shapeRenderer.end();

        batch.begin();
        for (int i = 0; i < relicCount; i++) {
            BlitzRelic relic = blitzManager.getActiveRelics().get(i);
            float rx = relicStart + i * (relicCardW + relicGap);

            String[] words = relic.getName().toUpperCase().split(" ");
            String line1 = words[0];
            String line2 = words.length > 1 ? words[1] : "";
            if (line1.length() > 8) line1 = line1.substring(0, 8);
            if (line2.length() > 8) line2 = line2.substring(0, 8);

            font.getData().setScale(.50f);
            font.setColor(new Color(.18f, .12f, .08f, 1f));
            tipLayout.setText(font, relic.getName().substring(0, 1));
            font.draw(batch, relic.getName().substring(0, 1), rx + 25f - tipLayout.width / 2f,
                relicY + relicCardH * .5f + tipLayout.height * .5f);

            font.getData().setScale(.46f);
            font.setColor(new Color(1f, .82f, .35f, 1f));
            font.draw(batch, line1, rx + 48f, relicY + 42f);
            if (!line2.isEmpty()) font.draw(batch, line2, rx + 48f, relicY + 27f);

            font.getData().setScale(.40f);
            font.setColor(new Color(.60f, .65f, .76f, 1f));
            font.draw(batch, "RELIC", rx + 48f, relicY + 12f);
        }
        batch.end();

        // ----- SCORE BREAKDOWN -----
        if (blitzLastResult != null && blitzBreakdownTimer > 0f) {
            float bw = Math.min(300f, Math.max(235f, w * .30f));
            float bh = Math.min(235f, Math.max(170f, h * .28f));
            float bx = w - bw - 12f;
            float by = h - relicCardH - bh - 28f;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(.035f, .045f, .075f, .96f);
            shapeRenderer.rect(bx, by, bw, bh);
            shapeRenderer.setColor(1f, .68f, .20f, .9f);
            shapeRenderer.rect(bx, by + bh - 3f, bw, 3f);
            shapeRenderer.end();

            batch.begin();
            font.getData().setScale(.72f);
            font.setColor(new Color(1f, .80f, .28f, 1f));
            font.draw(batch, blitzLastResult.label, bx + 12f, by + bh - 18f);

            font.getData().setScale(.48f);
            font.setColor(new Color(.60f, .66f, .78f, 1f));
            font.draw(batch, "SCORE BREAKDOWN", bx + 12f, by + bh - 38f);

            float ty = by + bh - 60f;
            float row = 23f;
            int maxRows = Math.max(4, Math.min(7, (int)((bh - 105f) / row)));
            int shown = Math.min(maxRows, blitzLastResult.components.size());

            for (int i = 0; i < shown; i++) {
                BlitzScore.Component c = blitzLastResult.components.get(i);
                font.getData().setScale(.46f);
                font.setColor(new Color(.82f, .85f, .92f, 1f));
                String label = c.label;
                if (label.length() > 23) label = label.substring(0, 23);
                font.draw(batch, label, bx + 12f, ty);

                String pts = "+" + String.format("%,d", c.points);
                tipLayout.setText(font, pts);
                font.setColor(new Color(1f, .72f, .28f, 1f));
                font.draw(batch, pts, bx + bw - 12f - tipLayout.width, ty);
                ty -= row;
            }

            if (blitzLastResult.components.size() > shown) {
                font.getData().setScale(.34f);
                font.setColor(new Color(.55f, .60f, .70f, 1f));
                font.draw(batch, "+" + (blitzLastResult.components.size() - shown) + " more...", bx + 12f, ty);
                ty -= row;
            }

            font.getData().setScale(.40f);
            font.setColor(new Color(.58f, .63f, .72f, 1f));
            font.draw(batch, "RAW", bx + 12f, by + 52f);
            font.getData().setScale(.52f);
            font.setColor(Color.WHITE);
            font.draw(batch, String.format("%,d", blitzLastResult.basePoints), bx + 52f, by + 52f);

            font.getData().setScale(.40f);
            font.setColor(new Color(1f, .55f, .28f, 1f));
            font.draw(batch, "x" + blitzLastResult.multiplier, bx + bw - 78f, by + 52f);

            font.getData().setScale(.72f);
            font.setColor(new Color(1f, .84f, .30f, 1f));
            String finalText = "+" + String.format("%,d", blitzLastResult.points);
            tipLayout.setText(font, finalText);
            font.draw(batch, finalText, bx + bw - 12f - tipLayout.width, by + 23f);

            font.getData().setScale(1f);
            font.setColor(Color.WHITE);
            batch.end();
        }

        // ----- BOTTOM SCORE PLATE -----
        float panelW = Math.min(720f, w - 28f);
        float panelH = 102f;
        float panelX = (w - panelW) * .5f;
        float panelY = 10f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.025f, .035f, .06f, .98f);
        shapeRenderer.rect(panelX, panelY, panelW, panelH);
        shapeRenderer.setColor(.18f, .20f, .28f, 1f);
        shapeRenderer.rect(panelX, panelY + panelH - 3f, panelW, 3f);

        // Time Gamble button
        float gbw = Math.min(170f, panelW * .25f);
        float gbh = 38f;
        blitzGambleButton.set(panelX + panelW - gbw - 12f, panelY + 10f, gbw, gbh);
        boolean gamble = blitzManager.isTimeGambleActive();
        boolean ready = blitzManager.getGambleCooldown() <= 0f && !gamble;
        shapeRenderer.setColor(gamble ? .55f : (ready ? .18f : .10f),
            gamble ? .18f : .12f,
            gamble ? .08f : .18f, .98f);
        shapeRenderer.rect(blitzGambleButton.x, blitzGambleButton.y, blitzGambleButton.width, blitzGambleButton.height);
        shapeRenderer.setColor(gamble ? 1f : .75f, gamble ? .55f : .68f, .20f, 1f);
        shapeRenderer.rect(blitzGambleButton.x, blitzGambleButton.y + blitzGambleButton.height - 3f,
            blitzGambleButton.width, 3f);
        shapeRenderer.end();

        batch.begin();

        float baseY = panelY + 67f;

        font.getData().setScale(.92f + blitzScorePulse * .12f);
        font.setColor(new Color(1f, .84f, .30f, 1f));
        String scoreText = String.format("%,d", Math.round(blitzHudScore));
        font.draw(batch, scoreText, panelX + 16f, baseY);

        font.getData().setScale(.28f);
        font.setColor(new Color(.65f, .68f, .78f, 1f));
        font.draw(batch, "SCORE", panelX + 18f, panelY + 19f);

        font.getData().setScale(.78f);
        font.setColor(Color.WHITE);
        font.draw(batch, "x" + blitzManager.getMultiplier(), panelX + panelW * .40f, baseY);
        if (blitzLastLabelTimer > 0f) {
            font.getData().setScale(.62f);
            font.setColor(new Color(1f, .72f, .22f, 1f));
            font.draw(batch, blitzLastLabel, panelX + panelW * .40f, panelY + 22f);
        }
        font.getData().setScale(.28f);
        font.setColor(new Color(.65f, .68f, .78f, 1f));
        font.draw(batch, "MULT", panelX + panelW * .40f + 2f, panelY + 19f);

        font.getData().setScale(.70f);
        font.setColor(new Color(1f, .55f, .28f, 1f));
        font.draw(batch, "CHAIN " + blitzManager.getStreak(), panelX + panelW * .52f, baseY);
        font.getData().setScale(.28f);
        font.setColor(new Color(.65f, .68f, .78f, 1f));
        font.draw(batch, "STREAK", panelX + panelW * .52f + 2f, panelY + 19f);

        int seconds = (int)Math.ceil(blitzManager.getTimeRemaining());
        int mins = seconds / 60;
        int secs = seconds % 60;
        font.getData().setScale(.70f);
        font.setColor(seconds <= 20 ? new Color(1f, .28f, .28f, 1f) : Color.WHITE);
        font.draw(batch, String.format("%02d:%02d", mins, secs), panelX + panelW * .70f, baseY);
        font.getData().setScale(.28f);
        font.setColor(new Color(.65f, .68f, .78f, 1f));
        font.draw(batch, "TIME", panelX + panelW * .70f + 2f, panelY + 19f);

        font.getData().setScale(.40f);
        font.setColor(gamble ? new Color(1f, .62f, .20f, 1f)
            : (ready ? new Color(1f, .82f, .35f, 1f) : new Color(.45f, .48f, .56f, 1f)));

        String gambleText;
        if (gamble) gambleText = "GAMBLE  " + String.format("%.1f", blitzManager.getGambleRemaining()) + "s";
        else if (ready) gambleText = "TIME GAMBLE  -10s  x2";
        else gambleText = "GAMBLE  " + String.format("%.0f", blitzManager.getGambleCooldown()) + "s";
        font.draw(batch, gambleText, blitzGambleButton.x + 8f, blitzGambleButton.y + 23f);

        font.getData().setScale(.32f);
        font.setColor(new Color(.65f, .68f, .78f, 1f));
        font.draw(batch, "G = BET", blitzGambleButton.x + 8f, blitzGambleButton.y + 10f);

        if (blitzLastLabelTimer > 0f) {
            float a = MathUtils.clamp(blitzLastLabelTimer * 1.8f, 0f, 1f);
            font.getData().setScale(.38f);
            font.setColor(1f, .82f, .35f, a);
            tipLayout.setText(font, blitzLastLabel);
            font.draw(batch, blitzLastLabel, (w - tipLayout.width) / 2f, panelY + panelH + 18f);
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ---------- Touch UI buttons ----------
    private void layoutUiButtons() {
        float h = Gdx.graphics.getHeight();
        float x = 20f, top = h - 120f - UI_BTN_H, y = top;
        for (int i = 0; i < uiButtons.length; i++) {
            if (y < 10f) {
                y = top;
                x += UI_BTN_W + UI_BTN_GAP;
            } // overflow into a second column on short screens
            uiButtons[i].set(x, y, UI_BTN_W, UI_BTN_H);
            y -= UI_BTN_H + UI_BTN_GAP;
        }
    }


    private void cancelAction() {
        selectedSpell = null;
        if (shootMode) {
            shootMode = false;
            player.setSelected(false);
        }
        message("Cancelled", 1f);
    }

    private boolean handleUiTap(float x, float y) {
        layoutUiButtons();
        for (int i = 0; i < uiButtons.length; i++) {
            if (!uiButtons[i].contains(x, y)) continue;
            clickSound.play();
            if (i == 0) toggleArrowMode();
            else if (i >= 1 && i <= 3) selectSpell(i - 1);
            else if (i == 4) cancelAction();
            else if (i == 5) sacrificeActiveCard();
            else if (i == 6) rerollActiveCard();
            return true;
        }
        return false;
    }
    // --------------------------------------

    private void renderUiButtons() {
        if (gameOverScreen.isVisible() || shopScreen.isVisible() || rewardScreen.isVisible() || evtVisible) return;
        layoutUiButtons();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < uiButtons.length; i++) {
            boolean active = (i == 0 && shootMode) || (i >= 1 && i <= 3 && selectedSpell != null && i - 1 < ownedSpells.size && ownedSpells.get(i - 1) == selectedSpell) || (i == 5 && activeCard != null) || (i == 6 && activeCard != null && !roomRerolled);
            if (active) shapeRenderer.setColor(.3f, .8f, 1f, .9f);
            else shapeRenderer.setColor(.15f, .17f, .25f, .85f);
            Rectangle r = uiButtons[i];
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        shapeRenderer.end();
        batch.begin();
        font.getData().setScale(.5f);
        for (int i = 0; i < uiButtons.length; i++) {
            String label;
            if (i == 0)
                label = "ARROW (" + player.getArrowsRemaining() + (bounceArrows > 0 ? " +" + bounceArrows + "B" : "") + ")";
            else if (i <= 3)
                label = (i - 1 < ownedSpells.size) ? ownedSpells.get(i - 1).getLabel() + " (" + manaCost(ownedSpells.get(i - 1)) + ")" : "-";
            else if (i == 4) label = "CANCEL";
            else if (i == 5) label = "SACRIFICE";
            else label = roomRerolled ? "REROLL (used)" : "REROLL (1)";
            Rectangle r = uiButtons[i];
            font.setColor(Color.WHITE);
            font.draw(batch, label, r.x + 8f, r.y + r.height / 2f + font.getCapHeight() / 2f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // Same scale maths as positionCards(), so the dragged card is centred on the pointer
    private float currentCardScale() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        int n = Math.max(1, hand.size);
        if (w >= h) {
            float spacing = 10f;
            float required = n * 155f + (n - 1) * spacing;
            return MathUtils.clamp(h - 40f < required ? (h - 40f) / required : 1f, 0.50f, 0.82f);
        }
        float spacing = 6f;
        float required = n * 115f + (n - 1) * spacing;
        return MathUtils.clamp(w - 20f < required ? (w - 20f) / required : 1f, 0.42f, 0.75f);
    }

    private void selectCardFromTap(Card c) {
        clickSound.play();
        activeCard = c;
        c.setSelected(true);
        message(c.getName() + " selected", 1.5f);
        tooltipEnemy = null;
    }

    // Returns true when it consumed the touch
    private boolean handleCardDrag() {
        // Dragging only works when no card/spell/arrow mode is active
        if (activeCard != null || shootMode || selectedSpell != null) {
            if (draggedCard != null) {
                draggedCard = null;
                positionCards();
            }
            dragCandidate = null;
            return false;
        }

        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Press: remember which card was grabbed
        if (Gdx.input.justTouched()) {
            dragCandidate = null;
            draggedCard = null;
            for (int i = hand.size - 1; i >= 0; i--) {
                if (hand.get(i).isClicked(x, y)) {
                    dragCandidate = hand.get(i);
                    break;
                }
            }
            dragDownX = x;
            dragDownY = y;
            return dragCandidate != null;
        }

        if (dragCandidate == null) return false;

        // Held: start dragging once the pointer moves far enough, then follow it
        if (Gdx.input.isTouched()) {
            if (draggedCard == null && Math.hypot(x - dragDownX, y - dragDownY) > DRAG_THRESHOLD) {
                draggedCard = dragCandidate;
            }
            if (draggedCard != null) {
                float s = currentCardScale();
                draggedCard.setPosition(x - 115f * s / 2f, y - 155f * s / 2f);
            }
            return true;
        }

        // Released
        Card grabbed = dragCandidate;
        dragCandidate = null;

        if (draggedCard == null) {          // never moved far enough = it was a tap
            selectCardFromTap(grabbed);
            return true;
        }

        Card dragged = draggedCard;
        draggedCard = null;

        Card target = null;
        for (int i = hand.size - 1; i >= 0; i--) {
            Card c = hand.get(i);
            if (c != dragged && c.isClicked(x, y)) {
                target = c;
                break;
            }
        }

        Card result = (target == null) ? null : CardCombination.combine(dragged, target);
        if (result != null) {
            hand.removeValue(dragged, true);
            hand.removeValue(target, true);
            cardModifiers.remove(dragged);
            cardModifiers.remove(target);
            hand.add(result);
            positionCards();
            if (combineSound != null) combineSound.play();
            else augmentSound.play();
            spawnBurst(x, y, 18, .75f, .55f, 1f, 30f);
            message("Combined into " + result.getName() + "!", 2f);
            autosave();
        } else {
            positionCards();                // snap back
            if (target != null) {
                playSfx(errorSound);
                message("Those cards can't combine", 1.5f);
            }
        }
        return true;
    }

    // ---------- Meta progression (Souls) ----------
    private void loadMeta() {
        souls = metaPrefs.getInteger("souls", 0);
        for (int i = 0; i < upLevels.length; i++) upLevels[i] = metaPrefs.getInteger("up" + i, 0);
        pactSlots = MathUtils.clamp(metaPrefs.getInteger("pactSlots", 1), 1, 3);
        for (int i = 0; i < PACT_COUNT; i++) pactOn[i] = metaPrefs.getBoolean("pact" + i, false);
    }

    private void saveMeta() {
        metaPrefs.putInteger("souls", souls);
        for (int i = 0; i < upLevels.length; i++) metaPrefs.putInteger("up" + i, upLevels[i]);
        metaPrefs.putInteger("pactSlots", pactSlots);
        for (int i = 0; i < PACT_COUNT; i++) metaPrefs.putBoolean("pact" + i, pactOn[i]);
        metaPrefs.flush();
    }

    private int upgradeCost(int i) {
        return UP_BASE_COST[i] * (upLevels[i] + 1);
    }

    private void layoutUpgradeRows() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float rw = Math.min(560f, w - 60f), rh = 78f, gap = 12f;
        float x = (w - rw) / 2f, top = h - 190f;
        for (int i = 0; i < upgradeRows.length; i++) {
            upgradeRows[i].set(x, top - (i + 1) * rh - i * gap, rw, rh);
        }
    }

    private void handleUpgradesInput() {
        if (!Gdx.input.justTouched()) return;
        layoutMenuButtons();
        layoutUpgradeRows();
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (containsWithPadding(backButton, x, y, 20f)) {
            clickSound.play();
            upgradesVisible = false;
            return;
        }
        if (containsWithPadding(pactsButton, x, y, 20f)) {
            clickSound.play();
            pactsVisible = true;
            pactMsg = "";
            return;
        }
        for (int i = 0; i < upgradeRows.length; i++) {
            if (!upgradeRows[i].contains(x, y)) continue;
            int cost = upgradeCost(i);
            if (upLevels[i] >= UP_MAX[i] || souls < cost) {
                playSfx(errorSound);
                return;
            }
            souls -= cost;
            upLevels[i]++;
            saveMeta();
            playSfx(buySound);
            return;
        }
    }

    private void renderUpgrades() {
        if (pactsVisible) {
            renderPacts();
            return;
        }
        layoutMenuButtons();
        layoutUpgradeRows();
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.10f, .11f, .16f, 1f);
        shapeRenderer.rect(0, 0, w, h);
        for (int i = 0; i < upgradeRows.length; i++) {
            boolean maxed = upLevels[i] >= UP_MAX[i];
            boolean afford = souls >= upgradeCost(i);
            if (maxed) shapeRenderer.setColor(.15f, .17f, .25f, .95f);
            else if (afford) shapeRenderer.setColor(.18f, .42f, .28f, .95f);
            else shapeRenderer.setColor(.28f, .16f, .16f, .95f);
            Rectangle r = upgradeRows[i];
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        shapeRenderer.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, backButton.x, backButton.y, backButton.width, backButton.height);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.8f);
        font.draw(batch, "UPGRADES", 40f, h - 55f);
        font.getData().setScale(.8f);
        font.draw(batch, "Souls: " + souls, 40f, h - 110f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        font.draw(batch, "Spend souls to improve your next run.", 40f, h - 145f);
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, pactsButton.x, pactsButton.y, pactsButton.width, pactsButton.height);
        font.setColor(Color.WHITE);
        font.getData().setScale(.8f);
        font.draw(batch, "PACTS", pactsButton.x + 52f, pactsButton.y + 35f);
        for (int i = 0; i < upgradeRows.length; i++) {
            Rectangle r = upgradeRows[i];
            boolean maxed = upLevels[i] >= UP_MAX[i];
            font.setColor(Color.WHITE);
            font.getData().setScale(.8f);
            font.draw(batch, UP_NAMES[i] + "  Lv " + upLevels[i] + "/" + UP_MAX[i], r.x + 16f, r.y + r.height - 16f);
            font.getData().setScale(.5f);
            font.setColor(new Color(.82f, .84f, .9f, 1f));
            font.draw(batch, UP_DESC[i], r.x + 16f, r.y + r.height - 52f);
            font.getData().setScale(.7f);
            font.setColor(maxed ? new Color(.65f, .68f, .75f, 1f) : Color.WHITE);
            font.draw(batch, maxed ? "MAXED" : upgradeCost(i) + " souls", r.x + r.width - 150f, r.y + r.height / 2f + 10f);
        }

        font.setColor(Color.WHITE);
        font.getData().setScale(.8f);
        font.draw(batch, "BACK", backButton.x + 38f, backButton.y + 35f);
        font.getData().setScale(1f);
        batch.end();
    }

    private void renderUpgradesButton() {
        layoutMenuButtons();
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, upgradesButton.x, upgradesButton.y, upgradesButton.width, upgradesButton.height);
        font.setColor(Color.WHITE);
        font.getData().setScale(.62f);
        font.draw(batch, "UPGRADES", upgradesButton.x + 40f, upgradesButton.y + 36f);
        font.getData().setScale(.5f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        String info = "Souls: " + souls + ((gameOverScreen.isVisible() && lastRunSouls > 0) ? "  (+" + lastRunSouls + ")" : "");
        font.draw(batch, info, upgradesButton.x, upgradesButton.y + upgradesButton.height + 22f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void renderTutorialButton() {
        layoutMenuButtons();
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, tutorialButton.x, tutorialButton.y, tutorialButton.width, tutorialButton.height);
        font.setColor(Color.WHITE);
        font.getData().setScale(.62f);
        font.draw(batch, "TUTORIAL", tutorialButton.x + 40f, tutorialButton.y + 36f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ---------- Tutorial screens ----------
    private void handleTutorialInput() {
        if (!Gdx.input.justTouched()) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        layoutMenuButtons();

        float x = Gdx.input.getX();
        float y = h - Gdx.input.getY();

        // 1. Handle piece button clicks if on the Moves page
        if (tutPage == TUT_MOVES_PAGE) {
            layoutTutorialMoves(w, h); // Calculate button bounds for touch detection
            for (int i = 0; i < TUT_MOVE_TYPES.length; i++) {
                if (tutMoveBtns[i] != null && tutMoveBtns[i].contains(x, y)) {
                    clickSound.play();
                    tutMoveSel = i;
                    return;
                }
            }
        }

        // 2. Handle Back button click
        if (containsWithPadding(backButton, x, y, 20f)) {
            clickSound.play();
            tutorialVisible = false;
            return;
        }

        // 3. Handle Previous Page button click
        if (tutPage > 0 && containsWithPadding(tutPrev, x, y, 10f)) {
            clickSound.play();
            tutPage--;
            return;
        }

        // 4. Handle Next Page button click
        if (tutPage < TUT_TITLES.length - 1 && containsWithPadding(tutNext, x, y, 10f)) {
            clickSound.play();
            tutPage++;
        }
    }

    private void renderTutorial() {
        layoutMenuButtons();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.10f, .11f, .16f, 1f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();

        // Page-specific shape rendering
        if (tutPage == TUT_MOVES_PAGE) {
            renderTutorialMovesShapes(w, h);
        }

        batch.begin();
        batch.setColor(Color.WHITE);

        // Back / previous / next buttons
        batch.draw(menuButtonTexture,
            backButton.x, backButton.y,
            backButton.width, backButton.height);

        batch.setColor(1f, 1f, 1f,
            tutPage > 0 ? 1f : .35f);

        batch.draw(menuButtonTexture,
            tutPrev.x, tutPrev.y,
            tutPrev.width, tutPrev.height);

        batch.setColor(1f, 1f, 1f,
            tutPage < TUT_TITLES.length - 1 ? 1f : .35f);

        batch.draw(menuButtonTexture,
            tutNext.x, tutNext.y,
            tutNext.width, tutNext.height);

        batch.setColor(Color.WHITE);

        // Title
        font.setColor(Color.WHITE);
        font.getData().setScale(1.4f);
        font.draw(batch, "TUTORIAL", 40f, h - 40f);

        // Page title
        font.getData().setScale(.85f);
        font.setColor(new Color(.95f, .80f, .30f, 1f));
        font.draw(batch, TUT_TITLES[tutPage], 40f, h - 92f);

        // Page number
        font.getData().setScale(.5f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        font.draw(
            batch,
            "Page " + (tutPage + 1) + " / " + TUT_TITLES.length,
            40f,
            h - 122f
        );

        float top = h - 150f;
        float contentW = Math.min(w - 80f, 900f);

        // MOVES PAGE
        if (tutPage == TUT_MOVES_PAGE) {
            renderTutorialMovesText(w, h);
        }
        // CURSE PAGES
        else if (tutPage >= TUT_CURSE_PAGE && tutPage < TUT_CURSE_PAGE + TUT_CURSE_PAGES) {
            renderTutorialCurses(w, top, tutPage - TUT_CURSE_PAGE);
        }
        // OMEN PAGE
        else if (tutPage == TUT_OMEN_PAGE) {
            renderTutorialOmens(w, top);
        }
        // AUGMENTS PAGE
        else if (tutPage == TUT_AUGMENT_PAGE) {
            renderTutorialAugments(w, top);
        }
        // NORMAL TEXT PAGES
        else {
            float scale = h < 600f ? 1.00f : 1.08f;
            float y = top;

            font.getData().setScale(scale);
            font.setColor(new Color(.88f, .90f, .95f, 1f));

            for (int i = 0; i < TUT_LINES[tutPage].length; i++) {
                GlyphLayout l = font.draw(
                    batch,
                    "    " + TUT_LINES[tutPage][i],
                    40f,
                    y,
                    contentW,
                    Align.left,
                    true
                );

                y -= l.height + 16f;
            }
        }

        // Navigation labels
        font.getData().setScale(.8f);

        font.setColor(Color.WHITE);
        font.draw(
            batch,
            "BACK",
            backButton.x + 38f,
            backButton.y + 35f
        );

        font.setColor(
            tutPage > 0
                ? Color.WHITE
                : new Color(.5f, .5f, .55f, 1f)
        );

        font.draw(
            batch,
            "PREV",
            tutPrev.x + 52f,
            tutPrev.y + 35f
        );

        font.setColor(
            tutPage < TUT_TITLES.length - 1
                ? Color.WHITE
                : new Color(.5f, .5f, .55f, 1f)
        );

        font.draw(
            batch,
            "NEXT",
            tutNext.x + 52f,
            tutNext.y + 35f
        );

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        batch.end();
    }

    private void refillHandIfNeeded() {
        int usableCards = 0;

        for (Card card : hand) {
            if (!card.isUsed()) {
                usableCards++;
            }
        }

        if (usableCards > 3) return;
        if (hand.size >= MAX_HAND_SIZE) return;

        addRandomCardToHand();
    }

    // Augment reference page. Called inside an active batch.
    private void renderTutorialAugments(float w, float top) {
        font.getData().setScale(.5f);
        font.setColor(new Color(.88f, .90f, .95f, 1f));
        font.draw(batch, "Augments come from rewards. Red frame = extra damage, blue frame = mana.", 40f, top);
        top -= 34f;

        int total = AUG_NAMES.length;
        int cols = w >= 800f ? 2 : 1;
        int perCol = (total + cols - 1) / cols;
        float availH = top - 100f;
        float rowH = MathUtils.clamp(availH / perCol, 34f, 60f);
        float colW = (w - 80f) / cols;
        float r = MathUtils.clamp(rowH * .28f, 8f, 14f);
        float textX = r * 2f + 20f;

        for (int i = 0; i < total; i++) {
            int col = i / perCol, row = i % perCol;
            float ix = 40f + col * colW;
            float iy = top - row * rowH - rowH / 2f;
            float cx = ix + r + 4f;
            Color c = AUG_COLORS[i];

            batch.setColor(0f, 0f, 0f, .92f);
            batch.draw(circleTex, cx - r - 2f, iy - r - 2f, (r + 2f) * 2f, (r + 2f) * 2f);
            batch.setColor(c.r, c.g, c.b, 1f);
            batch.draw(circleTex, cx - r, iy - r, r * 2f, r * 2f);
            batch.setColor(Color.WHITE);

            font.getData().setScale(.5f);
            font.setColor(c.r * .4f + .6f, c.g * .4f + .6f, c.b * .4f + .6f, 1f);
            font.draw(batch, AUG_NAMES[i].toUpperCase(), ix + textX, iy + rowH / 2f - 4f);
            font.getData().setScale(.4f);
            font.setColor(new Color(.78f, .80f, .88f, 1f));
            font.draw(batch, AUG_DESC[i], ix + textX, iy + rowH / 2f - 22f, colW - textX - 10f, Align.left, true);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void addRandomCardToHand() {
        if (hand.size >= MAX_HAND_SIZE) return;

        Card.MovementType[] types = {
            Card.MovementType.KNIGHT,
            Card.MovementType.BISHOP,
            Card.MovementType.ROOK,
            Card.MovementType.QUEEN,
            Card.MovementType.PAWN,
            Card.MovementType.DASH,
            Card.MovementType.CLAUDE
        };

        Card.MovementType type = types[random.nextInt(types.length)];

        Card newCard = createCard(type);
        hand.add(newCard);
        positionCards();

        message("NEW CARD: " + newCard.getName(), 1.2f);
    }

    // Icon reference page: every curse and status icon with its meaning. Called inside an active batch.
    private static final int TUT_ITEMS_PER_PAGE = 8;

    // Icon reference page: every curse and status icon with its meaning. Called inside an active batch.
    private void renderTutorialCurses(float w, float top, int page) {
        EnemyCurse[] cs = EnemyCurse.values();
        int total = cs.length + 4;
        int start = page * TUT_ITEMS_PER_PAGE;
        int end = Math.min(total, start + TUT_ITEMS_PER_PAGE);

        font.getData().setScale(.5f);
        font.setColor(new Color(.88f, .90f, .95f, 1f));
        font.draw(batch, "Icons stack above an enemy. Numbers show stacks or turns left. Tap an enemy for details.", 40f, top);
        top -= 34f;

        int count = Math.max(0, end - start);
        int cols = w >= 900f ? 2 : 1;
        int perCol = Math.max(1, (count + cols - 1) / cols);
        float availH = top - 100f;
        float rowH = MathUtils.clamp(availH / perCol, 44f, 64f);
        float colW = (w - 80f) / cols;
        float r = MathUtils.clamp(rowH * .36f, 12f, 18f);
        float textX = r * 2f + 22f;

        for (int i = start; i < end; i++) {
            int slot = i - start;
            int col = slot / perCol, row = slot % perCol;
            float ix = 40f + col * colW;
            float iy = top - row * rowH - rowH / 2f;

            Color c;
            String sym, name, desc;
            int shown = 0;
            if (i < cs.length) {
                c = curseColor(cs[i]);
                sym = curseSymbol(cs[i]);
                name = cs[i].getLabel();
                desc = cs[i].getDescription();
            } else if (i == cs.length) {
                c = COL_SHIELD;
                sym = "SH";
                shown = 2;
                name = "Shield";
                desc = "Blocks hits before the enemy takes damage. The number is how many hits it absorbs.";
            } else if (i == cs.length + 1) {
                c = COL_POISON;
                sym = "PS";
                shown = 4;
                name = "Poison";
                desc = "Damage every turn (2 per turn at 6+). The number is turns left. Spreads to neighbours on death.";
            } else if (i == cs.length + 2) {
                c = COL_BURN;
                sym = "BR";
                shown = 2;
                name = "Burn";
                desc = "1 damage every turn. The number is turns left.";
            } else {
                c = COL_DEFENSE;
                sym = "DF";
                shown = 3;
                name = "Your Defense";
                desc = "Appears on your piece. Absorbs damage before HP.";
            }

            drawRoundIcon(ix + r + 4f, iy, r, c, sym, shown);

            font.getData().setScale(.5f);
            font.setColor(c.r * .4f + .6f, c.g * .4f + .6f, c.b * .4f + .6f, 1f);
            font.draw(batch, name == null ? "" : name.toUpperCase(), ix + textX, iy + rowH / 2f - 4f);
            font.getData().setScale(.4f);
            font.setColor(new Color(.78f, .80f, .88f, 1f));
            font.draw(batch, desc == null ? "" : desc, ix + textX, iy + rowH / 2f - 22f, colW - textX - 10f, Align.left, true);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // Omen reference page. Called inside an active batch.
    private void renderTutorialOmens(float w, float top) {
        font.getData().setScale(.5f);
        font.setColor(new Color(.88f, .90f, .95f, 1f));
        font.draw(batch, "From room 2 a random Omen can change the whole room. Brutal omens start in room 3.", 40f, top);
        top -= 34f;

        int total = OMEN_COUNT;
        int cols = w >= 800f ? 2 : 1;
        int perCol = (total + cols - 1) / cols;
        float availH = top - 100f;
        float rowH = MathUtils.clamp(availH / perCol, 34f, 60f);
        float colW = (w - 80f) / cols;
        float r = MathUtils.clamp(rowH * .28f, 8f, 14f);
        float textX = r * 2f + 20f;

        for (int i = 0; i < total; i++) {
            int id = i + 1;
            int col = i / perCol, row = i % perCol;
            float ix = 40f + col * colW;
            float iy = top - row * rowH - rowH / 2f;
            float cx = ix + r + 4f;
            Color c = omenColorFor(id);

            batch.setColor(0f, 0f, 0f, .92f);
            batch.draw(circleTex, cx - r - 2f, iy - r - 2f, (r + 2f) * 2f, (r + 2f) * 2f);
            batch.setColor(c.r, c.g, c.b, 1f);
            batch.draw(circleTex, cx - r, iy - r, r * 2f, r * 2f);
            batch.setColor(Color.WHITE);

            font.getData().setScale(.5f);
            font.setColor(c.r * .4f + .6f, c.g * .4f + .6f, c.b * .4f + .6f, 1f);
            font.draw(batch, OMEN_NAMES[id].toUpperCase(), ix + textX, iy + rowH / 2f - 4f);
            font.getData().setScale(.4f);
            font.setColor(new Color(.78f, .80f, .88f, 1f));
            font.draw(batch, OMEN_DESC[id], ix + textX, iy + rowH / 2f - 22f, colW - textX - 10f, Align.left, true);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    private void autosave() {
        if (savePrefs == null || menuVisible || gameOverScreen.isVisible()) return;

        savePrefs.putBoolean("hasSave", true);
        savePrefs.putInteger("difficulty", difficulty);
        savePrefs.putInteger("playerHp", playerHp);
        savePrefs.putInteger("playerMaxHp", playerMaxHp);
        savePrefs.putInteger("defense", defense);
        savePrefs.putInteger("flawlessStreak", flawlessStreak);
        savePrefs.putInteger("arrowsLeft", player.getArrowsRemaining());
        savePrefs.putBoolean("roomRerolled", roomRerolled);
        savePrefs.putInteger("gold", gold);
        savePrefs.putInteger("mana", mana);
        savePrefs.putInteger("bounceArrows", bounceArrows);
        savePrefs.putInteger("runBonusSouls", runBonusSouls);
        for (int i = 0; i < PACT_COUNT; i++) savePrefs.putBoolean("runPact" + i, runPact[i]);
        savePrefs.putInteger("objectiveType", objectiveType);
        savePrefs.putInteger("objectiveTarget", objectiveTarget);
        savePrefs.putInteger("objectiveProgress", objectiveProgress);
        savePrefs.putBoolean("objectiveCompleted", objectiveCompleted);
        savePrefs.putBoolean("roomDamageTaken", roomDamageTaken);
        savePrefs.putBoolean("roomSacrificed", roomSacrificed);
        savePrefs.putInteger("captureCombo", captureCombo);
        savePrefs.putInteger("bestCombo", bestCombo);

        savePrefs.putInteger("handSize", hand.size);
        for (int i = 0; i < hand.size; i++) {
            Card c = hand.get(i);
            savePrefs.putString("cardType" + i, c.getMovementType().name());
            savePrefs.putString("cardName" + i, c.getName());
            savePrefs.putInteger("cardUses" + i, c.getUsesRemaining());
            CardModifier modifier = cardModifiers.get(c);
            savePrefs.putString("cardModifier" + i, modifier == null ? "" : modifier.name());
            savePrefs.putBoolean("cardRebound" + i, c.hasRebound());
            savePrefs.putBoolean("cardPierce" + i, c.hasPierce());
            savePrefs.putBoolean("cardDoubleMove" + i, c.hasDoubleMove());
            savePrefs.putBoolean("cardTeleport" + i, c.hasTeleportationInfusion());
            savePrefs.putBoolean("cardBurn" + i, c.hasBurn());
            savePrefs.putString("cardExtra" + i, c.getExtraAugmentCode());
        }

        savePrefs.putInteger("spellCount", ownedSpells.size);
        for (int i = 0; i < ownedSpells.size; i++) {
            savePrefs.putString("spell" + i, ownedSpells.get(i).name());
        }

        savePrefs.putInteger("relicCount", ownedRelics.size);
        for (int i = 0; i < ownedRelics.size; i++) {
            savePrefs.putString("relic" + i, ownedRelics.get(i).getName());
        }

        savePrefs.flush();
    }

    private void continueGameFromMenu() {
        if (!hasAutosave()) {
            startGameFromMenu();
            return;
        }

        clearMoveAnim();
        menuVisible = false;
        bestiaryVisible = false;
        tutorialVisible = false;
        evtVisible = false;
        omen = OMEN_NONE;
        difficulty = Math.max(1, savePrefs.getInteger("difficulty", 1));
        playerHp = Math.max(1, savePrefs.getInteger("playerHp", 3));
        playerMaxHp = Math.max(playerHp, savePrefs.getInteger("playerMaxHp", 3));
        defense = MathUtils.clamp(savePrefs.getInteger("defense", 0), 0, MAX_DEFENSE);
        gold = Math.max(0, savePrefs.getInteger("gold", 0));
        mana = Math.max(1, savePrefs.getInteger("mana", BASE_MAX_MANA));
        runBonusSouls = Math.max(0, savePrefs.getInteger("runBonusSouls", 0));
        for (int i = 0; i < PACT_COUNT; i++) runPact[i] = savePrefs.getBoolean("runPact" + i, false);
        objectiveType = savePrefs.getInteger("objectiveType", OBJECTIVE_CAPTURES);
        objectiveTarget = Math.max(1, savePrefs.getInteger("objectiveTarget", 2));
        objectiveProgress = Math.max(0, savePrefs.getInteger("objectiveProgress", 0));
        objectiveCompleted = savePrefs.getBoolean("objectiveCompleted", false);
        roomDamageTaken = savePrefs.getBoolean("roomDamageTaken", false);
        roomSacrificed = savePrefs.getBoolean("roomSacrificed", false);
        captureCombo = Math.max(0, savePrefs.getInteger("captureCombo", 0));
        bestCombo = Math.max(captureCombo, savePrefs.getInteger("bestCombo", 0));

        if (player != null) player.dispose();
        player = new Player(2, 2);

        hand.clear();
        cardModifiers.clear();
        int savedHandSize = Math.min(MAX_HAND_SIZE, Math.max(0, savePrefs.getInteger("handSize", 0)));
        for (int i = 0; i < savedHandSize; i++) {
            try {
                Card.MovementType type = Card.MovementType.valueOf(savePrefs.getString("cardType" + i, "KNIGHT"));
                int uses = Math.max(0, savePrefs.getInteger("cardUses" + i, 2));
                String savedName = savePrefs.getString("cardName" + i, cardName(type));
                if (savedName == null || savedName.isEmpty()) savedName = cardName(type);
                Card c = new Card(savedName, type, uses);
                hand.add(c);
                String modifierName = savePrefs.getString("cardModifier" + i, "");
                if (!modifierName.isEmpty()) {
                    try {
                        cardModifiers.put(c, CardModifier.valueOf(modifierName));
                    } catch (Exception ignored) {
                    }
                }
                restoreCardAugments(c, i);
                c.restoreExtraAugments(savePrefs.getString("cardExtra" + i, ""));
            } catch (Exception ignored) {
            }
        }

        ownedSpells.clear();
        int spellCount = Math.max(0, savePrefs.getInteger("spellCount", 0));
        for (int i = 0; i < spellCount; i++) {
            try {
                ownedSpells.add(Spell.valueOf(savePrefs.getString("spell" + i, "LIGHTNING")));
            } catch (Exception ignored) {
            }
        }

        ownedRelics.clear();
        int relicCount = Math.max(0, savePrefs.getInteger("relicCount", 0));
        for (int i = 0; i < relicCount; i++) {
            Relic relic = findRelicByName(savePrefs.getString("relic" + i, ""));
            if (relic != null) ownedRelics.add(relic);
        }
        bounceArrows = Math.max(0, savePrefs.getInteger("bounceArrows", 0));
        flawlessStreak = Math.max(0, savePrefs.getInteger("flawlessStreak", 0));
        int savedArrows = savePrefs.getInteger("arrowsLeft", player.getArrowsRemaining());
        while (player.getArrowsRemaining() > savedArrows && player.hasArrows()) player.useArrow();
        hudHp = -1f;
        hudLastGold = -1;

        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        doubleMoveActive = false;
        rewardScreen.hide();
        gameOverScreen.hide();
        shopScreen.hide();

        rollPoison();
        generateEnemiesForCurrentRoom();
        positionCards();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.play();
        roomRerolled = savePrefs.getBoolean("roomRerolled", false);
        message("Autosave loaded - Room " + difficulty, 2f);
    }

    // ---------- Main Menu ----------
    // ---------- Autosave ----------
    private boolean hasAutosave() {
        return savePrefs != null && savePrefs.getBoolean("hasSave", false);
    }

    private void returnToMainMenu() {
        autosave();
        clearMoveAnim();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.stop();
        rewardScreen.hide();
        shopScreen.hide();
        gameOverScreen.hide();
        evtVisible = false;
        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        activeCard = null;
        draggedCard = null;
        dragCandidate = null;
        if (player != null) player.setSelected(false);
        upgradesVisible = false;
        tutorialVisible = false;
        menuVisible = true;
        bestiaryVisible = false;
    }

    private void clearAutosave() {
        if (savePrefs == null) return;
        savePrefs.clear();
        savePrefs.flush();
    }

    private void layoutMenuButtons() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        float buttonW = Math.min(420f, w * 0.65f);
        float buttonH = 70f;
        float gap = 18f;
        float x = (w - buttonW) / 2f;

        float startY = hasAutosave() ? h * 0.55f : h * 0.48f;
        startButton.set(x, startY, buttonW, buttonH);

        float lowestY;
        if (hasAutosave()) {
            continueButton.set(x, startY - (buttonH + gap), buttonW, buttonH);
            bestiaryButton.set(x, startY - 2f * (buttonH + gap), buttonW, buttonH);
            blitzButton.set(x, startY - 3f * (buttonH + gap), buttonW, buttonH);
            lowestY = blitzButton.y;
        } else {
            bestiaryButton.set(x, startY - (buttonH + gap), buttonW, buttonH);
            blitzButton.set(x, startY - 2f * (buttonH + gap), buttonW, buttonH);
            lowestY = blitzButton.y;
        }

        // Panel wraps the title, subtitle and all buttons
        float panelPad = 45f;
        float panelW = buttonW + panelPad * 2f;
        float logoW = panelW - 40f;
        float logoH = logoW * 220f / 960f;
        float panelTop = startButton.y + buttonH + panelPad + logoH + 55f;
        float panelBottom = lowestY - panelPad;
        menuPanel.set((w - panelW) / 2f, panelBottom, panelW, panelTop - panelBottom);
        menuLogo.set(menuPanel.x + 20f, panelTop - 25f - logoH, logoW, logoH);

        backButton.set(25f, 25f, 150f, 55f);
        returnMenuButton.set(w - 210f, 25f, 185f, 58f);
        pactsButton.set(w - 210f, 25f, 185f, 58f);
        upgradesButton.set(w - 210f, menuVisible ? 25f : 95f, 185f, 58f);
        tutorialButton.set(25f, 25f, 185f, 58f);
        tutNext.set(w - 195f, 25f, 170f, 55f);
        tutPrev.set(w - 385f, 25f, 170f, 55f);
    }

    // ---------- Pacts screen ----------
    private int activePactCount(boolean[] src) {
        int n = 0;
        for (int i = 0; i < PACT_COUNT; i++) if (src[i]) n++;
        return n;
    }

    private float soulMultiplier(boolean[] src) {
        return 1f + 0.10f * activePactCount(src) + (src[5] ? 0.5f : 0f);
    }

    private int pactSlotCost() {
        return 30 * pactSlots;
    }

    private void layoutPactRows() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float rw = Math.min(560f, w - 60f), x = (w - rw) / 2f;
        pactSlotButton.set(x, h - 215f, rw, 40f);
        float top = h - 225f, gap = 8f;
        float rh = MathUtils.clamp((top - 105f - (PACT_COUNT - 1) * gap) / PACT_COUNT, 56f, 84f);
        for (int i = 0; i < PACT_COUNT; i++) pactRows[i].set(x, top - (i + 1) * rh - i * gap, rw, rh);
    }

    private void handlePactsInput() {
        if (!Gdx.input.justTouched()) return;
        layoutMenuButtons();
        layoutPactRows();
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (containsWithPadding(backButton, x, y, 20f)) {
            clickSound.play();
            pactsVisible = false;
            pactMsg = "";
            return;
        }
        if (pactSlots < 3 && pactSlotButton.contains(x, y)) {
            int cost = pactSlotCost();
            if (souls < cost) {
                playSfx(errorSound);
                pactMsg = "Not enough souls";
                return;
            }
            souls -= cost;
            pactSlots++;
            saveMeta();
            playSfx(buySound);
            pactMsg = "Pact slot unlocked!";
            return;
        }
        for (int i = 0; i < PACT_COUNT; i++) {
            if (!pactRows[i].contains(x, y)) continue;
            if (pactOn[i]) {
                pactOn[i] = false;
                clickSound.play();
                pactMsg = "";
            } else if (activePactCount(pactOn) >= pactSlots) {
                playSfx(errorSound);
                pactMsg = "No free pact slot";
                return;
            } else {
                pactOn[i] = true;
                if (curseSound != null) curseSound.play();
                else clickSound.play();
                pactMsg = PACT_NAMES[i] + " sealed";
            }
            saveMeta();
            return;
        }
    }

    private void renderPacts() {
        layoutMenuButtons();
        layoutPactRows();
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        int used = activePactCount(pactOn);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.10f, .11f, .16f, 1f);
        shapeRenderer.rect(0, 0, w, h);
        if (pactSlots < 3) {
            boolean afford = souls >= pactSlotCost();
            if (afford) shapeRenderer.setColor(.18f, .42f, .28f, .95f);
            else shapeRenderer.setColor(.28f, .16f, .16f, .95f);
            shapeRenderer.rect(pactSlotButton.x, pactSlotButton.y, pactSlotButton.width, pactSlotButton.height);
        }
        for (int i = 0; i < PACT_COUNT; i++) {
            Rectangle r = pactRows[i];
            if (pactOn[i]) shapeRenderer.setColor(.38f, .18f, .30f, .95f);
            else if (used >= pactSlots) shapeRenderer.setColor(.12f, .12f, .15f, .9f);
            else shapeRenderer.setColor(.15f, .17f, .25f, .9f);
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        shapeRenderer.end();

        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, backButton.x, backButton.y, backButton.width, backButton.height);

        font.setColor(Color.WHITE);
        font.getData().setScale(1.8f);
        font.draw(batch, "PACTS", 40f, h - 55f);
        font.getData().setScale(.8f);
        font.draw(batch, "Souls: " + souls + "   Slots: " + used + "/" + pactSlots, 40f, h - 110f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        font.draw(batch, "Soul bonus: +" + Math.round((soulMultiplier(pactOn) - 1f) * 100f) + "%  (applies to your next run)", 40f, h - 145f);

        font.getData().setScale(.55f);
        font.setColor(pactSlots < 3 ? Color.WHITE : new Color(.65f, .68f, .75f, 1f));
        font.draw(batch, pactSlots < 3 ? "UNLOCK PACT SLOT - " + pactSlotCost() + " souls" : "ALL PACT SLOTS UNLOCKED",
            pactSlotButton.x + 14f, pactSlotButton.y + pactSlotButton.height / 2f + 8f);

        for (int i = 0; i < PACT_COUNT; i++) {
            Rectangle r = pactRows[i];
            font.setColor(Color.WHITE);
            font.getData().setScale(.65f);
            font.draw(batch, PACT_NAMES[i], r.x + 14f, r.y + r.height - 10f);
            font.getData().setScale(.42f);
            font.setColor(new Color(.45f, .95f, .55f, 1f));
            font.draw(batch, "+ " + PACT_BUFF[i], r.x + 14f, r.y + r.height - 32f);
            font.setColor(new Color(1f, .45f, .40f, 1f));
            font.draw(batch, "- " + PACT_BANE[i], r.x + 14f, r.y + r.height - 50f);
            font.getData().setScale(.7f);
            font.setColor(pactOn[i] ? new Color(1f, .8f, .4f, 1f) : new Color(.55f, .57f, .62f, 1f));
            font.draw(batch, pactOn[i] ? "ON" : "OFF", r.x + r.width - 60f, r.y + r.height / 2f + 8f);
        }

        font.getData().setScale(.8f);
        font.setColor(Color.WHITE);
        font.draw(batch, "BACK", backButton.x + 38f, backButton.y + 35f);
        if (!pactMsg.isEmpty()) {
            font.getData().setScale(.55f);
            font.setColor(new Color(1f, .85f, .4f, 1f));
            font.draw(batch, pactMsg, 200f, 62f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void restoreCardAugments(Card card, int index) {
        try {
            java.lang.reflect.Field f = Card.class.getDeclaredField("hasRebound");
            f.setAccessible(true);
            f.setBoolean(card, savePrefs.getBoolean("cardRebound" + index, false));
            f = Card.class.getDeclaredField("hasPierce");
            f.setAccessible(true);
            f.setBoolean(card, savePrefs.getBoolean("cardPierce" + index, false));
            f = Card.class.getDeclaredField("hasDoubleMove");
            f.setAccessible(true);
            f.setBoolean(card, savePrefs.getBoolean("cardDoubleMove" + index, false));
            f = Card.class.getDeclaredField("hasTeleportationInfusion");
            f.setAccessible(true);
            f.setBoolean(card, savePrefs.getBoolean("cardTeleport" + index, false));
            f = Card.class.getDeclaredField("hasBurn");
            f.setAccessible(true);
            f.setBoolean(card, savePrefs.getBoolean("cardBurn" + index, false));
        } catch (Exception ignored) {
        }
    }

    private Relic findRelicByName(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            for (java.lang.reflect.Field field : Relic.class.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
                if (!Relic.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(null);
                if (value instanceof Relic) {
                    Relic relic = (Relic) value;
                    if (name.equals(relic.getName())) return relic;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String cardName(Card.MovementType type) {
        switch (type) {
            case KNIGHT:
                return "Knight";
            case BISHOP:
                return "Bishop";
            case ROOK:
                return "Rook";
            case QUEEN:
                return "Queen";
            case PAWN:
                return "Pawn";
            case DASH:
                return "Dash";
            case CLAUDE:
                return "Claude";
            case SHIFTER:
                return "Shifter";
            default:
                return "Knight";
        }
    }

    private void startGameFromMenu() {
        blitzMode = false;
        beginIntro(this::beginNewGame);
    }

    private void startBlitzFromMenu() {
        blitzMode = true;
        beginIntro(this::beginNewGame);
    }

    private void beginNewGame() {
        clearAutosave();
        clearMoveAnim();
        menuVisible = false;
        bestiaryVisible = false;
        tutorialVisible = false;
        evtVisible = false;
        omen = OMEN_NONE;

        if (blitzMode) {
            blitzManager.start();
            difficulty = 1;
            blitzHudScore = 0f;
            blitzScorePulse = 0f;
            blitzLastLabel = "";
            blitzLastLabelTimer = 0f;
            blitzLastResult = null;
            blitzBreakdownTimer = 0f;
        } else {
            difficulty = 1;
        }
        cardModifiers.clear();
        enemyCurses.clear();
        poisonTurns.clear();
        burnTurns.clear();
        armorShields.clear();
        doubleMoveActive = false;
        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;

        if (player != null) player.dispose();
        player = new Player(2, 2);

        resetPlayerStats();
        buildStartingHand();

        if (enemies != null) {
            for (int i = 0; i < enemies.size; i++) enemies.get(i).dispose();
        }

        rollPoison();
        generateEnemiesForCurrentRoom();
        positionCards();

        if (backgroundMusic != null && !backgroundMusic.isPlaying()) {
            backgroundMusic.play();
        }

        message("Room 1", 2f);
        autosave();
    }

    private void handleMenuInput() {
        layoutMenuButtons();

        if (upgradesVisible) {
            handleUpgradesInput();
            return;
        }
        if (tutorialVisible) {
            handleTutorialInput();
            return;
        }
        if (!Gdx.input.justTouched()) return;

        // Get the actual touch position.
        float touchX = Gdx.input.getX();
        float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Small touch padding makes buttons easier to press on phones.
        float padding = 20f;

        if (!bestiaryVisible && containsWithPadding(upgradesButton, touchX, touchY, padding)) {
            clickSound.play();
            upgradesVisible = true;
            return;
        }

        if (!bestiaryVisible && containsWithPadding(tutorialButton, touchX, touchY, padding)) {
            clickSound.play();
            tutorialVisible = true;
            tutPage = 0;
            logoRevealed = true;
            return;
        }

        // Any menu button press reveals the CLOAKCHESS logo
        if (bestiaryVisible ? containsWithPadding(backButton, touchX, touchY, padding)
            : (containsWithPadding(startButton, touchX, touchY, padding)
            || (hasAutosave() && containsWithPadding(continueButton, touchX, touchY, padding))
            || containsWithPadding(bestiaryButton, touchX, touchY, padding)
            || containsWithPadding(blitzButton, touchX, touchY, padding))) {
            logoRevealed = true;
        }

        if (bestiaryVisible) {
            if (containsWithPadding(backButton, touchX, touchY, padding)) {
                clickSound.play();
                bestiaryVisible = false;
            }
            return;
        }

        if (containsWithPadding(startButton, touchX, touchY, padding)) {
            clickSound.play();
            startGameFromMenu();

        } else if (hasAutosave()
            && containsWithPadding(continueButton, touchX, touchY, padding)) {

            clickSound.play();
            continueGameFromMenu();

        } else if (containsWithPadding(bestiaryButton, touchX, touchY, padding)) {

            clickSound.play();
            bestiaryVisible = true;

        } else if (containsWithPadding(blitzButton, touchX, touchY, padding)) {

            clickSound.play();
            startBlitzFromMenu();
        }
    }

    private void renderMenu() {
        if (upgradesVisible) {
            renderUpgrades();
            return;
        }
        if (tutorialVisible) {
            renderTutorial();
            return;
        }

        layoutMenuButtons();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        if (bestiaryVisible) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(.10f, .11f, .16f, 1f);
            shapeRenderer.rect(0, 0, w, h);
            shapeRenderer.end();

            batch.begin();

            // Back button uses the same button plate as the main menu
            batch.setColor(Color.WHITE);
            batch.draw(menuButtonTexture, backButton.x, backButton.y, backButton.width, backButton.height);

            font.setColor(Color.WHITE);
            font.getData().setScale(1.8f);
            font.draw(batch, "BESTIARY", 40f, h - 55f);

            font.getData().setScale(.8f);
            font.setColor(new Color(.65f, .68f, .75f, 1f));
            font.draw(batch, "Enemy information will appear here.", 40f, h - 110f);
            font.draw(batch, "Discover enemies as you progress through CloakChess.", 40f, h - 145f);

            font.setColor(Color.WHITE);
            font.getData().setScale(.8f);
            font.draw(batch, "BACK", backButton.x + 38f, backButton.y + 35f);

            font.getData().setScale(1f);
            batch.end();
            return;
        }

        // Plain dark background behind everything
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.08f, .09f, .12f, 1f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.end();

        batch.begin();
        batch.setColor(Color.WHITE);

        // Panel, then button plates
        batch.draw(menuPanelTexture, menuPanel.x, menuPanel.y, menuPanel.width, menuPanel.height);
        batch.draw(menuButtonTexture, startButton.x, startButton.y, startButton.width, startButton.height);
        if (hasAutosave())
            batch.draw(menuButtonTexture, continueButton.x, continueButton.y, continueButton.width, continueButton.height);
        batch.draw(menuButtonTexture, bestiaryButton.x, bestiaryButton.y, bestiaryButton.width, bestiaryButton.height);
        batch.draw(menuButtonTexture, blitzButton.x, blitzButton.y, blitzButton.width, blitzButton.height);

        // Logo: CHESS on launch, crossfades to CLOAKCHESS after any menu button press
        if (logoRevealed) logoFade = Math.min(1f, logoFade + Gdx.graphics.getDeltaTime() / 0.45f);
        batch.setColor(1f, 1f, 1f, 1f - logoFade);
        if (logoFade < 1f) batch.draw(logoChessTexture, menuLogo.x, menuLogo.y, menuLogo.width, menuLogo.height);
        batch.setColor(1f, 1f, 1f, logoFade);
        if (logoFade > 0f) batch.draw(logoCloakTexture, menuLogo.x, menuLogo.y, menuLogo.width, menuLogo.height);
        batch.setColor(Color.WHITE);

        // Subtitle
        font.getData().setScale(.8f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        String subtitle = "Chess. Cards. Roguelike.";
        float subtitleWidth = font.getSpaceXadvance() * subtitle.length();
        font.draw(batch, subtitle, (w - subtitleWidth) / 2f, menuLogo.y - 8f);

        // Button labels
        drawMenuLabel(batch, "START", startButton, Color.WHITE);
        if (hasAutosave()) drawMenuLabel(batch, "CONTINUE", continueButton, Color.WHITE);
        drawMenuLabel(batch, "BESTIARY", bestiaryButton, Color.WHITE);

        drawMenuLabel(batch, "BLITZ", blitzButton, Color.WHITE);


        // Version tag moved to the top-left so it doesn't sit under the TUTORIAL button
        font.getData().setScale(.55f);
        font.setColor(new Color(.38f, .40f, .46f, 1f));
        String version = "CloakChess";
        font.draw(batch, version, 20f, h - 20f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ---------- Random events ----------
    private void maybeOpenEvent() {
        if (difficulty < 2) return;
        if (random.nextFloat() < EVENT_CHANCE) openEvent(random.nextInt(4));
    }

    private boolean containsWithPadding(
        Rectangle button,
        float x,
        float y,
        float padding
    ) {
        return x >= button.x - padding
            && x <= button.x + button.width + padding
            && y >= button.y - padding
            && y <= button.y + button.height + padding;
    }

    private void openEvent(int id) {
        evtId = id;
        evtVisible = true;
        for (int i = 0; i < 3; i++) evtEnabled[i] = true;
        evtOptions[2] = "Walk away";
        switch (id) {
            case 0:
                evtTitle = "THE GAMBLER";
                evtText = "A hooded figure rattles a pair of bone dice. 'Fortune favors the bold, traveller.'";
                evtOptions[0] = "Bet 30 gold - 50% chance to win 100";
                evtOptions[1] = "Bet 70 gold - 45% chance to win 270";
                evtEnabled[0] = gold >= 30;
                evtEnabled[1] = gold >= 70;
                break;
            case 1:
                evtTitle = "BLOOD ALTAR";
                evtText = "A crimson altar hums quietly. It wants a taste of you.";
                evtOptions[0] = "Offer 1 HP - gain a random relic";
                evtOptions[1] = "Offer 1 max HP - heal to full";
                evtEnabled[0] = playerHp >= 2 && pickShopRelic(null) != null;
                evtEnabled[1] = playerMaxHp >= 2;
                break;
            case 2:
                evtTitle = "WHISPERING SHRINE";
                evtText = "Faint voices promise gifts from the dark. Some gifts have teeth.";
                evtOptions[0] = "Pray - 40% heal 2 HP, 35% nothing, 25% lose 1 HP";
                evtOptions[1] = "Take the offering - +30 gold, all enemies gain a curse";
                break;
            default:
                evtTitle = "FORGOTTEN FORGE";
                evtText = "A cold anvil glows faintly. A smith's ghost offers its craft.";
                evtOptions[0] = "Pay 25 gold - forge a random card";
                evtOptions[1] = "Melt a random card - +40 gold, +2 souls";
                evtEnabled[0] = gold >= 25 && hand.size < MAX_HAND_SIZE;
                evtEnabled[1] = hand.size >= 2;
                break;
        }
        playSfx(shopOpenSound);
        message("EVENT", 1.5f);
    }

    private void drawMenuLabel(SpriteBatch targetBatch, String text, Rectangle r, Color color) {
        font.setColor(color);
        font.getData().setScale(1.1f);
        float approximateWidth = font.getSpaceXadvance() * text.length();
        font.draw(targetBatch, text,
            r.x + r.width / 2f - approximateWidth / 2f,
            r.y + r.height / 2f + 13f);
    }

    private void layoutEvent() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float pw = Math.min(560f, w - 40f), ph = Math.min(520f, h - 60f);
        float px = (w - pw) / 2f, py = (h - ph) / 2f;
        for (int i = 0; i < 3; i++) {
            evtRects[i].set(px + 20f, py + 20f + (2 - i) * (64f + 12f), pw - 40f, 64f);
        }
    }

    private void handleEventInput() {
        if (!Gdx.input.justTouched()) return;
        layoutEvent();
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        for (int i = 0; i < 3; i++) {
            if (!evtRects[i].contains(x, y)) continue;
            if (!evtEnabled[i]) {
                playSfx(errorSound);
                return;
            }
            clickSound.play();
            resolveEvent(evtId, i);
            evtVisible = false;
            positionCards();
            autosave();
            return;
        }
    }

    private void resolveEvent(int id, int choice) {
        float px = tileCenterX(player.getX()), py = tileCenterY(player.getY()) + 30f;
        if (choice == 2) {
            message("You walk away.", 1.5f);
            return;
        }
        switch (id) {
            case 0: {
                int bet = choice == 0 ? 30 : 70;
                int payout = choice == 0 ? 100 : 270;
                float odds = choice == 0 ? 0.50f : 0.45f;
                gold -= bet;
                if (random.nextFloat() < odds) {
                    gold += payout;
                    playSfx(buySound);
                    spawnFloat("+" + (payout - bet) + " gold", px, py, 1f, .85f, .25f);
                    message("The dice love you! +" + (payout - bet) + " gold", 2.5f);
                } else {
                    playSfx(errorSound);
                    spawnFloat("-" + bet + " gold", px, py, 1f, .35f, .3f);
                    message("Snake eyes. You lose " + bet + " gold.", 2.5f);
                }
                break;
            }
            case 1: {
                if (choice == 0) {
                    playerHp = Math.max(1, playerHp - 1);
                    Relic r = pickShopRelic(null);
                    if (r != null) {
                        ownedRelics.add(r);
                        applyRelic(r);
                        playSfx(blessingSound);
                        if (r.isCursed()) playSfx(curseSound);
                        message("The altar grants " + r.getName() + "! (-1 HP)", 2.5f);
                    }
                    spawnFloat("-1 HP", px, py, 1f, .35f, .3f);
                } else {
                    playerMaxHp = Math.max(1, playerMaxHp - 1);
                    playerHp = playerMaxHp;
                    playSfx(healSound);
                    spawnFloat("FULL HEAL", px, py, .4f, 1f, .5f);
                    message("Max HP -1, but you are fully healed.", 2.5f);
                }
                break;
            }
            case 2: {
                if (choice == 0) {
                    float roll = random.nextFloat();
                    if (roll < 0.40f) {
                        healPlayer(2);
                        message("The voices mend your wounds. +2 HP", 2.5f);
                    } else if (roll < 0.75f) {
                        message("The voices go silent.", 2f);
                    } else {
                        playerHp = Math.max(1, playerHp - 1);
                        playSfx(hitSound);
                        spawnFloat("-1 HP", px, py, 1f, .35f, .3f);
                        message("The voices bite back. -1 HP", 2.5f);
                    }
                } else {
                    gold += 30;
                    curseAllEnemies();
                    spawnFloat("+30 gold", px, py, 1f, .85f, .25f);
                    message("+30 gold... and the enemies feel stronger.", 2.5f);
                }
                break;
            }
            default: {
                if (choice == 0) {
                    gold -= 25;
                    Card.MovementType[] types = {Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK, Card.MovementType.QUEEN, Card.MovementType.PAWN, Card.MovementType.DASH, Card.MovementType.CLAUDE};
                    Card c = createCard(types[random.nextInt(types.length)]);
                    hand.add(c);
                    playSfx(drawSound);
                    message("The smith forges a " + c.getName() + "!", 2.5f);
                } else if (hand.size >= 2) {
                    Card melted = hand.get(random.nextInt(hand.size));
                    hand.removeValue(melted, true);
                    cardModifiers.remove(melted);
                    gold += 40;
                    runBonusSouls += 2;
                    playSfx(smashSound);
                    spawnFloat("+40 gold", px, py, 1f, .85f, .25f);
                    message("Melted " + melted.getName() + ": +40 gold, +2 souls", 2.5f);
                }
                break;
            }
        }
    }

    private void renderEvent() {
        layoutEvent();
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float pw = Math.min(560f, w - 40f), ph = Math.min(520f, h - 60f);
        float px = (w - pw) / 2f, py = (h - ph) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, .72f);
        shapeRenderer.rect(0, 0, w, h);
        shapeRenderer.setColor(.10f, .11f, .17f, .97f);
        shapeRenderer.rect(px, py, pw, ph);
        for (int i = 0; i < 3; i++) {
            Rectangle r = evtRects[i];
            if (evtEnabled[i]) shapeRenderer.setColor(.18f, .24f, .38f, .95f);
            else shapeRenderer.setColor(.16f, .16f, .18f, .9f);
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
        shapeRenderer.end();

        batch.begin();
        font.getData().setScale(.5f);
        font.setColor(new Color(.55f, .65f, .80f, 1f));
        font.draw(batch, "EVENT", px + 20f, py + ph - 18f);
        font.getData().setScale(.95f);
        font.setColor(new Color(.95f, .80f, .30f, 1f));
        font.draw(batch, evtTitle, px + 20f, py + ph - 42f);
        font.getData().setScale(.5f);
        font.setColor(new Color(.85f, .87f, .93f, 1f));
        font.draw(batch, evtText, px + 20f, py + ph - 86f, pw - 40f, Align.left, true);

        for (int i = 0; i < 3; i++) {
            Rectangle r = evtRects[i];
            font.setColor(evtEnabled[i] ? Color.WHITE : new Color(.5f, .5f, .54f, 1f));
            font.draw(batch, evtOptions[i], r.x + 14f, r.y + r.height - 18f, r.width - 28f, Align.left, true);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void handleInput() {
        if (menuVisible) {
            handleMenuInput();
            renderMenu();
            return;
        }

        if (gameOverScreen.isVisible()) {
            if (upgradesVisible) {
                handleUpgradesInput();
                return;
            }
            if (pactsVisible) {
                handlePactsInput();
                return;
            }
            if (!Gdx.input.justTouched()) return;
            float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
            layoutMenuButtons();
            if (upgradesButton.contains(x, y)) {
                clickSound.play();
                upgradesVisible = true;
                return;
            }
            if (returnMenuButton.contains(x, y)) {
                clickSound.play();
                returnToMainMenu();
                return;
            }
            if (gameOverScreen.handleClick(x, y)) {
                clickSound.play();
                restartGame();
            }
            return;
        }
        if (blitzRelicShop.isVisible()) {
            handleBlitzRelicShopInput();
            return;
        }
        if (shopScreen.isVisible()) {
            handleShopInput();
            return;
        }
        if (evtVisible) {
            handleEventInput();
            return;
        }
        if (rewardScreen.isVisible()) {
            if (!Gdx.input.justTouched()) return;
            float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
            clickSound.play();
            rewardScreen.handleClick(x, y, hand);
            return;
        }
        if (arrowInFlight || anyEnemyFalling() || moveAnimActive || pendingArrivals.size > 0 || anyEnemyMoving())
            return;

        if (rewardScreen.hasGrantedNewCard()) {
            Card.MovementType type = rewardScreen.getGrantedCardType();
            if (hand.size < MAX_HAND_SIZE) {
                Card c = createCard(type);
                hand.add(c);
                positionCards();
                message("You received a " + c.getName() + "!", 2f);
            } else message("Hand full!", 2f);
            rewardScreen.hide();
            generateNextRoom();
            return;
        }
        if (rewardScreen.hasSelectedAugment()) {
            augmentSound.play();
            Card c = rewardScreen.getSelectedCard();
            Augment a = rewardScreen.getSelectedAugment();
            c.applyAugment(a);
            message(c.getName() + " gained " + a.getName() + "!", 2f);
            rewardScreen.hide();
            generateNextRoom();
            return;
        }

        if (Gdx.input.justTouched()) {
            float ux = Gdx.input.getX(), uy = Gdx.graphics.getHeight() - Gdx.input.getY();
            if (blitzMode && blitzManager != null && blitzGambleButton.contains(ux, uy)) {
                if (blitzManager.activateTimeGamble()) {
                    clickSound.play();
                    showPopup("TIME GAMBLE  ×2 SCORE!", 1f, .55f, .2f, .9f);
                    startShake(.08f, 2f);
                } else {
                    playSfx(errorSound);
                    message("Time Gamble is unavailable.", 1f);
                }
                return;
            }
            if (handleUiTap(ux, uy)) return;
        }
        if (handleCardDrag()) return;

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectSpell(0);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectSpell(1);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectSpell(2);
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            cancelAction();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            toggleArrowMode();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            sacrificeActiveCard();
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            rerollActiveCard();
            return;
        }
        if (blitzMode && blitzManager != null && Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            if (blitzManager.activateTimeGamble()) {
                showPopup("TIME GAMBLE  ×2 SCORE!", 1f, .55f, .2f, .9f);
                startShake(.08f, 2f);
            } else {
                message("Time Gamble is unavailable.", 1f);
            }
            return;
        }
        if (shootMode) {
            handleArrowInput();
            return;
        }
        if (selectedSpell != null) {
            handleSpellInput();
            return;
        }
        if (!Gdx.input.justTouched()) return;

        float touchX = Gdx.input.getX(), touchY = Gdx.graphics.getHeight() - Gdx.input.getY();
        if (activeCard == null) {
            boolean hitCard = false;
            for (int i = hand.size - 1; i >= 0; i--) {
                Card c = hand.get(i);
                if (c.isClicked(touchX, touchY)) {
                    clickSound.play();
                    activeCard = c;
                    c.setSelected(true);
                    message(c.getName() + " selected", 1.5f);
                    hitCard = true;
                    break;
                }
            }
            if (hitCard) {
                tooltipEnemy = null;
                return;
            }
            Enemy tapped = null;
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (e.isAlive() && !e.isFalling() && e.isClicked(touchX, touchY)) {
                    tapped = e;
                    break;
                }
            }
            if (tapped != null) clickSound.play();
            tooltipEnemy = (tapped != null && tapped != tooltipEnemy) ? tapped : null; // tap again or tap elsewhere to close
            return;
        }
        if (activeCard.isClicked(touchX, touchY)) {
            clickSound.play();
            activeCard.setSelected(false);
            activeCard = null;
            player.setSelected(false);
            return;
        }
        if (!player.isSelected()) {
            if (player.isClicked(touchX, touchY)) {
                clickSound.play();
                player.setSelected(true);
                message("Choose a destination", 1.5f);
            }
            return;
        }

        int tx = (int) ((touchX - Player.getBoardX()) / Player.TILE_SIZE), ty = (int) ((touchY - Player.getBoardY()) / Player.TILE_SIZE);
        boolean valid = activeCard.hasTeleportationInfusion() ? tx >= 0 && tx < Player.BOARD_SIZE && ty >= 0 && ty < Player.BOARD_SIZE : player.isValidMove(tx, ty, activeCard.getMovementRules(), board);
        if (!valid) {
            playSfx(errorSound);
            message("Invalid move!", 1.5f);
            return;
        }

        if (isDashCard(activeCard)) pushEnemiesAlongDash(tx, ty);
        if (activeCard.hasPierce()) {
            Enemy e = findFirstEnemyInLine(tx, ty);
            if (e != null) damageEnemy(e, 1);
        }
        boolean jump = getEnemyAt(tx, ty, null) != null;
        int fromX = player.getX(), fromY = player.getY();
        player.setPosition(tx, ty);
        Card played = activeCard;
        player.setSelected(false);
        activeCard = null;
        startMoveAnim(fromX, fromY, played, jump);
    }

    // ---------- Move animation: slide, or jump + smash on capture ----------
    private void startMoveAnim(int fromX, int fromY, Card card, boolean jump) {
        moveFromX = fromX;
        moveFromY = fromY;
        moveAnimCard = card;
        moveAnimJump = jump;
        moveAnimDur = jump ? JUMP_DURATION : SLIDE_DURATION;
        moveAnimT = 0f;
        moveAnimActive = true;
        if (jump) playSfx(jumpSound);
    }

    private void clearMoveAnim() {
        pendingArrivals.clear();
        moveAnimActive = false;
        moveAnimCard = null;
        moveAnimT = 0f;
    }

    private void updateMoveAnim(float delta) {
        if (!moveAnimActive) return;
        moveAnimT += delta;
        if (moveAnimT < moveAnimDur) return;
        moveAnimActive = false;
        Card played = moveAnimCard;
        moveAnimCard = null;
        moveSound.play();
        captureCard = played;
        boolean captured = checkPlayerCapture();
        captureCard = null;
        if (captured) {
            startShake(0.30f, 10f);
            playSfx(smashSound);
        }
        if (board.isPoisoned(player.getX(), player.getY())) {
            playSfx(poisonSound);
            damagePlayer(1);
            message("Poisoned tile!", 1.5f);
        }
        if (gameOverScreen.isVisible()) return;
        if (played != null) playCardWithModifier(played, captured);
        if (!captured && played != null) breakCombo();
        updateObjectiveProgress();
    }

    private void beginPlayerAnim() {
        if (!moveAnimActive) return;
        float t = MathUtils.clamp(moveAnimT / moveAnimDur, 0f, 1f);
        float p, arc = 0f, sc = 1f;
        if (moveAnimJump) {
            p = (float) Math.pow(t, 1.5f); // slow rise, fast fall = smash
            float s = MathUtils.sin(MathUtils.PI * p);
            arc = JUMP_HEIGHT * s;
            sc = 1f + 0.3f * s;
        } else {
            float inv = 1f - t;
            p = 1f - inv * inv * inv; // ease-out
        }
        float ox = (moveFromX - player.getX()) * Player.TILE_SIZE * (1f - p);
        float oy = (moveFromY - player.getY()) * Player.TILE_SIZE * (1f - p);
        float cx = tileCenterX(player.getX()), cy = tileCenterY(player.getY());
        tmpMat.idt().translate(cx + ox, cy + oy + arc, 0f).scale(sc, sc, 1f).translate(-cx, -cy, 0f);
        batch.setTransformMatrix(tmpMat);
        playerTransformApplied = true;
    }

    private void endPlayerAnim() {
        if (!playerTransformApplied) return;
        batch.setTransformMatrix(tmpMat.idt());
        playerTransformApplied = false;
    }

    private void startShake(float duration, float magnitude) {
        shakeDuration = duration;
        shakeTimer = duration;
        shakeMag = magnitude;
    }

    private void triggerHitFx() {
        startShake(0.30f, 12f);
        flashTimer = FLASH_DURATION;
        playSfx(hitSound);
    }

    private void triggerBurnAugment() {
        Array<Enemy> alive = new Array<>();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive()) alive.add(e);
        }
        if (alive.size > 0) {
            Enemy target = alive.get(random.nextInt(alive.size));
            damageEnemy(target, 1, 1f, .6f, .15f);
            message("Burn hit an enemy!", 1.5f);
            playSfx(burnSound);
        }
    }

    private void selectSpell(int index) {
        if (index < 0 || index >= ownedSpells.size) {
            message("No spell in that slot", 1.2f);
            return;
        }
        Spell spell = ownedSpells.get(index);
        if (mana < manaCost(spell)) {
            playSfx(errorSound);
            message("Not enough mana! (" + mana + "/" + manaCost(spell) + ")", 1.5f);
            return;
        }
        if (spell == Spell.CLOAK || spell == Spell.SHIELD || spell == Spell.CLEANSE) {
            if (spell == Spell.CLOAK && invisibleTurns > 0) {
                message("Already invisible!", 1.2f);
                return;
            }
            selectedSpell = null;
            shootMode = false;
            if (activeCard != null) {
                activeCard.setSelected(false);
                activeCard = null;
            }
            player.setSelected(false);
            castSpell(spell, null); // self-cast, no target needed
            return;
        }
        selectedSpell = spell;
        shootMode = false;
        if (activeCard != null) {
            activeCard.setSelected(false);
            activeCard = null;
        }
        player.setSelected(false);
        message(selectedSpell.getLabel() + ": tap an enemy", 1.5f);
    }

    private void startFrenzy() {
        frenzyTurns = 4; // the capture's own card is free too, so 3 more free cards after it
        playSfx(blessingSound);
        startShake(0.30f, 10f);
        spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 30, 1f, .3f, .15f, 80f);
        showPopup("BLOOD FRENZY!", 1f, .25f, .15f, 1.4f);
    }

    private void renderFrenzy() {
        if (frenzyTurns <= 0 || menuVisible || gameOverScreen.isVisible()) return;
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float pulse = .45f + .25f * MathUtils.sin(uiTime * 8f);
        batch.begin();
        batch.setColor(1f, .55f, .15f, pulse);
        tintRedPatch.draw(batch, 0, 0, w, h);
        batch.setColor(Color.WHITE);
        font.getData().setScale(.5f);
        font.setColor(1f, .55f, .15f, 1f);
        font.draw(batch, "BLOOD FRENZY - free cards: " + frenzyTurns, 18f, h - 128f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void playCardWithModifier(Card card, boolean captured) {
        if (blitzMode && blitzManager != null) blitzManager.cardPlayed();
        CardModifier modifier = cardModifiers.get(card);
        boolean echo = card.hasEcho() && random.nextFloat() < 0.35f;
        boolean free = (modifier == CardModifier.FRUGAL && random.nextBoolean()) || echo;
        if (echo) spawnFloat("ECHO", tileCenterX(player.getX()), tileCenterY(player.getY()) + 40f, .9f, .9f, .9f);
        if (modifier == CardModifier.BLOODPRICE) {
            if (!damagePlayer(1)) {
                message("Bloodprice took 1 HP", 1.5f);
            }
        }
        boolean frenzy = frenzyTurns > 0;
        if (frenzy) {
            frenzyTurns--;
            if (frenzyTurns == 0) message("Frenzy over...", 1.2f);
        }
        if (!free && !frenzy) {
            card.use();
            if (modifier == CardModifier.BRITTLE && !card.isUsed()) card.use();
        }
        if (card.hasBurn()) {
            triggerBurnAugment();
            card.consumeBurn();
        }
        if (modifier == CardModifier.LEECH && captured) healPlayer(1);
        if (card.hasAegis()) gainDefense(1);
        if (card.hasVenom()) applyVenom();
        int surge = card.hasManaSurge() ? 2 : 0;
        if (surge > 0) spawnFloat("+2 MP", tileCenterX(player.getX()), tileCenterY(player.getY()) + 48f, .4f, .65f, 1f);
        if (card.isUsed()) hand.removeValue(card, true);
        mana = Math.min(getMaxMana(), mana + MANA_REGEN_PER_CARD + surge + (hasRelic(Relic.Blessing.MANA_SPRING) ? 1 : 0) + (runPact[0] ? 1 : 0));
        positionCards();
        giveRandomCard();
        if (captured && card.hasRebound()) {
            message("Rebound! Extra action!", 1.5f);
            if (allEnemiesDefeated()) winRoom();
            return;
        }
        if (card.hasDoubleMove() && !doubleMoveActive) {
            doubleMoveActive = true;
            message("Double Move! Act again.", 1.5f);
            return;
        }
        doubleMoveActive = false;
        moveEnemies();
        if (allEnemiesDefeated()) winRoom();
    }

    // Venom augment: poison the nearest living enemy (warded enemies are immune)
    private void applyVenom() {
        Enemy best = null;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() || e.isFalling() || isWarded(e)) continue;
            int dx = e.getX() - player.getX(), dy = e.getY() - player.getY();
            int d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        if (best == null) return;
        Integer cur = poisonTurns.get(best);
        poisonTurns.put(best, Math.min(POISON_MAX_TURNS, (cur == null ? 0 : cur) + 3));
        float cx = tileCenterX(best.getX()), cy = tileCenterY(best.getY());
        spawnBurst(cx, cy, 10, .45f, .95f, .3f, 40f);
        spawnFloat("VENOM", cx, cy + 26f, .5f, 1f, .35f);
        playSfx(poisonSound);
    }

    private void castSpell(Spell spell, Enemy target) {
        if (spell == null) return;
        int cost = manaCost(spell);
        if (mana < cost) {
            message("Not enough mana!", 1.5f);
            return;
        }
        mana -= cost;
        if (blitzMode && blitzManager != null) blitzManager.spellCast();

        if (spell == Spell.CLOAK) {
            invisibleTurns = 2;
            spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 22, .6f, .4f, .9f, 30f);
            spawnShockwave(player.getX(), player.getY(), 1.2f);
        } else if (spell == Spell.SHIELD) {
            defense = Math.min(MAX_DEFENSE, defense + 2);
            spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 18, .35f, .65f, 1f, 40f);
            spawnFloat("+2 DEF", tileCenterX(player.getX()), tileCenterY(player.getY()) + 34f, .45f, .70f, 1f);
        } else if (spell == Spell.CLEANSE) {
            poisonTurns.clear();
            burnTurns.clear();
            confusedUntil.clear();
            invisibleTurns = 0;
            message("CLEANSED!", 1.5f);
            spawnBurst(tileCenterX(player.getX()), tileCenterY(player.getY()), 20, .55f, .95f, 1f, 50f);
        } else if (isWarded(target)) {
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 10, .7f, .7f, .8f, 0f);
            playSfx(errorSound);
            message("Warded! The spell fizzled.", 1.5f);
            moveEnemies();
            if (allEnemiesDefeated()) winRoom();
            return;
        } else if (spell == Spell.LIGHTNING) {
            float tx = tileCenterX(target.getX()), ty = tileCenterY(target.getY());
            spawnBolt(tx + MathUtils.random(-30f, 30f), Gdx.graphics.getHeight(), tx, ty, 9, 22f);
            startShake(0.25f, 7f);
            int targetX = target.getX(), targetY = target.getY();
            damageEnemy(target, 2);
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (e != target && e.isAlive() && !isWarded(e) && Math.abs(e.getX() - targetX) <= 1 && Math.abs(e.getY() - targetY) <= 1) {
                    spawnBolt(tx, ty, tileCenterX(e.getX()), tileCenterY(e.getY()), 4, 10f);
                    damageEnemy(e, 1);
                }
            }
        } else if (spell == Spell.POISON) {
            // Stacking poison: first cast = 4 turns, each recast adds 3 (max 8). 6+ turns = 2 damage per tick.
            Integer cur = poisonTurns.get(target);
            int nt = (cur == null || cur <= 0) ? 4 : Math.min(POISON_MAX_TURNS, cur + 3);
            poisonTurns.put(target, nt);
            float tcx = tileCenterX(target.getX()), tcy = tileCenterY(target.getY());
            spawnBurst(tcx, tcy, 14, .4f, .95f, .3f, 40f);
            spawnBurst(tcx, tcy, 8, .7f, 1f, .5f, 90f);
            spawnShockwave(target.getX(), target.getY(), 0.8f);
            spawnFloat("POISON " + nt, tcx, tcy + 26f, .5f, 1f, .35f);
        } else if (spell == Spell.BURN) {
            burnTurns.put(target, 2);
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 16, 1f, .5f, .1f, 90f);
        } else if (spell == Spell.CONFUSE) {
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 16, 1f, .9f, .3f, 20f);
            relocateEnemyRandomly(target);
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 16, 1f, .9f, .3f, 20f);
            confusedUntil.put(target, uiTime + 2.5f);
        } else if (spell == Spell.STUN) {
            stunnedTurns.put(target, 2);
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 14, .45f, .75f, 1f, 25f);
        } else if (spell == Spell.ICE) {
            stunnedTurns.put(target, 1);
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 14, .45f, .85f, 1f, 25f);
        } else if (spell == Spell.FREEZE) {
            freezeTurns = 1;
            spawnShockwave(player.getX(), player.getY(), 2.5f);
        } else if (spell == Spell.GLASSING) {
            glassedTurns.put(target, 2);
            stunnedTurns.put(target, Math.max(2, stunnedTurns.get(target) == null ? 0 : stunnedTurns.get(target)));
            spawnBurst(tileCenterX(target.getX()), tileCenterY(target.getY()), 18, .65f, .85f, 1f, 15f);
            message("GLASSED!", 1.3f);
        }

        playSpellSound(spell);
        message(spell.getLabel() + " cast! (-" + cost + " mana)", 1.5f);
        moveEnemies();
        if (allEnemiesDefeated()) winRoom();
    }

    // Capturing while cloaked = guaranteed kill, and the cloak is extended by 1 turn (max 3)
    private boolean assassinate(Enemy e) {
        killEnemy(e);
        gold += 20;
        mana = Math.min(getMaxMana(), mana + 2);
        runBonusSouls += 2;
        invisibleTurns = Math.min(3, invisibleTurns + 1);

        float cx = tileCenterX(player.getX());
        float cy = tileCenterY(player.getY());
        spawnBurst(cx, cy, 28, .65f, .35f, 1f, 70f);
        spawnShockwave(player.getX(), player.getY(), 1.6f);
        spawnFloat("ASSASSINATE +20g", cx, cy + 44f, .75f, .45f, 1f);
        playSfx(daggerSound);
        startShake(0.35f, 12f);
        showPopup("ASSASSINATION!", .75f, .45f, 1f, 1.2f);
        return true;
    }

    private boolean checkPlayerCapture() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);

            if (!e.isAlive()
                || e.getX() != player.getX()
                || e.getY() != player.getY()) {
                continue;
            }
            EnemyCurse curse = enemyCurses.get(e);
            CaptureCurse captureCurse = captureCurses.get(e);
            Card cc = captureCard;
            int captureHealthPercent = Math.round(
                100f * e.getHealth() / Math.max(1, e.getMaxHealth())
            );

            if (captureCurse != null && captureCurse.blocks(captureCard, invisibleTurns > 0, rollCaptureDamage())) {
                message(captureCurse.getLabel() + "! This enemy resists that capture.", 1.5f);
                spawnFloat(captureCurse.getShortLabel(), tileCenterX(e.getX()), tileCenterY(e.getY()) + 42f, .85f, .55f, .30f);
                breakCombo();
                moveEnemies();
                return false;
            }

            // Critical capture happens first.
            boolean critical = invisibleTurns > 0 ? assassinate(e) : rollCriticalCapture(e);

            if (!critical) {
                // Normal capture damages the enemy.
                int captureDamage = rollCaptureDamage();
                if (cc != null && cc.hasFury()) {
                    captureDamage += 1;
                    spawnFloat("FURY +1", tileCenterX(player.getX()), tileCenterY(player.getY()) + 62f, 1f, .3f, .3f);
                }

                damageEnemy(e, captureDamage);

                if (captureDamage >= 2) {
                    showPopup(
                        "HEAVY CAPTURE!",
                        1f, .45f, .20f,
                        0.9f
                    );
                }
            }

            // Curse retaliation
            if (curse == EnemyCurse.VENGEFUL) {
                damagePlayer(1);
                message("Vengeful! +1 damage", 1.5f);
            }

            if (curse == EnemyCurse.THORNY) {
                damagePlayer(1);
                message("Thorns! The capture hurt you.", 1.5f);
            }

            // Augment capture effects
            if (cc != null && !gameOverScreen.isVisible()) {
                if (cc.hasVampiric()) healPlayer(1);
                if (cc.hasGilded()) {
                    gold += 8;
                    spawnFloat("+8g", tileCenterX(player.getX()), tileCenterY(player.getY()) + 80f, 1f, .85f, .25f);
                }
            }

            spawnSlash(
                player.getX(),
                player.getY()
            );

            registerCapture(e, cc, curse, invisibleTurns > 0, captureHealthPercent);
            refillHandIfNeeded();

            groundPound(
                player.getX(),
                player.getY()
            );

            return true;
        }

        return false;
    }

    private void handleSpellInput() {
        if (!Gdx.input.justTouched()) return;
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        Enemy target = null;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && e.isClicked(x, y)) {
                target = e;
                break;
            }
        }
        if (selectedSpell == Spell.SHIELD || selectedSpell == Spell.CLEANSE) {
            castSpell(selectedSpell, null);
            selectedSpell = null;
            return;
        }
        if (target == null) {
            message("Choose an enemy", 1.2f);
            return;
        }
        castSpell(selectedSpell, target);
        selectedSpell = null;
    }

    private void moveEnemies() {
        shiftCards();
        applyEnemyStatuses();
        if (allEnemiesDefeated()) return;

        if (freezeTurns > 0) {
            freezeTurns--;
            playSfx(augmentSound);
            message("FREEZE! Enemies lose their turn.", 1.2f);
            return;
        }

        // Cloak: enemies can't see you, so they hold still (Hunters still can)
        boolean cloaked = invisibleTurns > 0;
        if (cloaked) {
            invisibleTurns--;
            if (invisibleTurns == 0) message("Cloak faded...", 1.2f);
        }

        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive()) continue;
            if (cloaked && enemyCurses.get(e) != EnemyCurse.HUNTER) continue;

            Integer stun = stunnedTurns.get(e);
            if (stun != null && stun > 0) {
                if (stun - 1 <= 0) stunnedTurns.remove(e);
                else stunnedTurns.put(e, stun - 1);
                continue;
            }
            int startX = e.getX(), startY = e.getY();
            int moves = enemyMoveCount(e);

            for (int step = 0; step < moves; step++) {

                e.moveTowards(player.getX(), player.getY());

                boolean moved = e.getX() != startX || e.getY() != startY;

                if (!moved) break;

                if (enemyCapturedPlayer(e) || board.isPoisoned(e.getX(), e.getY())) {
                    pendingArrivals.add(e);
                    break;
                }
            }
        }
    }

    private int enemyMoveCount(Enemy e) {
        EnemyCurse c = enemyCurses.get(e);
        if (c == EnemyCurse.HASTY) return 2;
        if (c == EnemyCurse.RELENTLESS && playerHp * 2 <= playerMaxHp) return 2;
        return 1;
    }
    // Crippling: one card loses a use. It never exhausts a card, so your hand can't be emptied by it.
    private void crippleRandomCard() {
        Array<Card> options = new Array<>();
        for (int i = 0; i < hand.size; i++) {
            if (hand.get(i).getUsesRemaining() >= 2) options.add(hand.get(i));
        }
        if (options.size == 0) return;
        Card c = options.get(random.nextInt(options.size));
        c.use();
        spawnFloat("-1 USE", tileCenterX(player.getX()), tileCenterY(player.getY()) + 64f, .80f, .55f, .35f);
        message("Crippling! " + c.getName() + " lost a use.", 1.5f);
    }

    // Maiming: permanent -1 max HP for the run (never below 1)
    private void maimPlayer() {
        if (playerMaxHp <= 1) return;
        playerMaxHp--;
        playerHp = Math.min(playerHp, playerMaxHp);
        spawnFloat("-1 MAX HP", tileCenterX(player.getX()), tileCenterY(player.getY()) + 64f, .85f, .20f, .40f);
        message("Maiming! -1 max HP", 1.5f);
    }

    private boolean anyEnemyMoving() {
        for (int i = 0; i < enemies.size; i++) if (enemies.get(i).isAlive() && enemies.get(i).isMoving()) return true;
        return false;
    }

    private void updatePendingArrivals() {
        if (pendingArrivals.size == 0 || anyEnemyMoving()) return;
        Array<Enemy> arrived = new Array<>(pendingArrivals);
        pendingArrivals.clear();

        for (int i = 0; i < arrived.size; i++) {
            Enemy e = arrived.get(i);
            if (!e.isAlive()) continue;

            if (board.isPoisoned(e.getX(), e.getY())) {
                // Stepping into a toxic pool: 1 damage now AND the enemy becomes poisoned (+2 turns)
                playSfx(poisonSound);
                damageEnemy(e, 1, .45f, .95f, .3f);
                message("Enemy poisoned!", 1.2f);
                if (!e.isAlive()) continue;
                Integer cur = poisonTurns.get(e);
                poisonTurns.put(e, Math.min(POISON_MAX_TURNS, (cur == null ? 0 : cur) + 2));
                spawnBurst(tileCenterX(e.getX()), tileCenterY(e.getY()), 8, .45f, .95f, .3f, 40f);
            }

            if (enemyCapturedPlayer(e)) {
                EnemyCurse curse = enemyCurses.get(e);
                if (curse == EnemyCurse.VENGEFUL) {
                    damagePlayer(1);
                    message("Vengeful! Extra damage!", 1.5f);
                    if (gameOverScreen.isVisible()) return;
                }
                int hit = difficulty >= (runPact[1] ? 4 : 8) ? 2 : 1;
                damagePlayer(hit);
                message("Enemy hit you! (-" + hit + ")", 1.5f);
                if (gameOverScreen.isVisible()) return;

                if (curse == EnemyCurse.GREEDY) {
                    int stolen = Math.min(gold, 10);
                    gold -= stolen;
                    if (stolen > 0) message("Greedy! Stole " + stolen + " gold", 1.5f);
                } else if (curse == EnemyCurse.SAPPING) {
                    mana = Math.max(0, mana - 2);
                    message("Sapping! -2 mana", 1.5f);
                }

                killEnemy(e);
            }
        }

        if (!gameOverScreen.isVisible() && !rewardScreen.isVisible() && allEnemiesDefeated()) winRoom();
    }

    // Poison ticks 1 damage (2 at 6+ turns), burn ticks 1. Dying from a tick still triggers plague / volatile.
    private void applyEnemyStatuses() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive()) continue;
            Integer p = poisonTurns.get(e);
            if (p != null && p > 0) {
                spawnBurst(tileCenterX(e.getX()), tileCenterY(e.getY()), 6, .4f, .95f, .3f, 30f);
                damageEnemy(e, p >= 6 ? 2 : 1, .45f, .95f, .3f);
                if (!e.isAlive()) continue;
                if (p - 1 <= 0) poisonTurns.remove(e);
                else poisonTurns.put(e, p - 1);
            }
            Integer b = burnTurns.get(e);
            if (b != null && b > 0) {
                damageEnemy(e, 1, 1f, .6f, .15f);
                if (!e.isAlive()) continue;
                if (b - 1 <= 0) burnTurns.remove(e);
                else burnTurns.put(e, b - 1);
            }
        }
    }

    // Defense absorbs damage first. Only damage that gets through to HP counts as "taking damage".
    private boolean damagePlayer(int amount) {
        amount = Math.max(0, amount);

        // DEF can block only ONE damage per hit.
        int absorbed = omen == OMEN_DREAD ? 0 : Math.min(defense, Math.min(amount, 1));

        if (absorbed > 0) {
            defense -= absorbed;

            playSfx(armorSound);

            spawnFloat(
                "-1 DEF",
                tileCenterX(player.getX()),
                tileCenterY(player.getY()) + 52f,
                .45f, .70f, 1f
            );

            spawnBurst(
                tileCenterX(player.getX()),
                tileCenterY(player.getY()),
                6,
                .45f, .70f, 1f,
                20f
            );
        }

        int dmg = amount - absorbed;

        if (dmg > 0) {
            if (blitzMode && blitzManager != null) blitzManager.breakStreak();
            roomDamageTaken = true;
            updateObjectiveProgress();

            triggerHitFx();

            spawnFloat(
                "-" + dmg,
                tileCenterX(player.getX()),
                tileCenterY(player.getY()) + 30f,
                1f, .3f, .3f
            );
        }

        playerHp = Math.max(0, playerHp - dmg);

        if (playerHp <= 0) {
            gameOver();
            return false;
        }

        return true;
    }

    private void healPlayer(int amount) {
        if (omen == OMEN_WITHERING && amount > 0 && enemies != null && !allEnemiesDefeated()
            && !shopScreen.isVisible() && !evtVisible) {
            spawnFloat("WITHERED", tileCenterX(player.getX()), tileCenterY(player.getY()) + 30f, .60f, .65f, .55f);
            message("Withering! You can't heal right now.", 1.5f);
            return;
        }
    }

    private boolean enemyCapturedPlayer(Enemy e) {
        return e.getX() == player.getX() && e.getY() == player.getY();
    }

    private void winRoom() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.stop();
        victorySound.play();
        for (int i = 0; i < 10; i++) {
            float bx = Player.getBoardX() + random.nextFloat() * Player.TILE_SIZE * Player.BOARD_SIZE;
            float by = Player.getBoardY() + random.nextFloat() * Player.TILE_SIZE * Player.BOARD_SIZE;
            spawnBurst(bx, by, 10, MathUtils.random(.5f, 1f), MathUtils.random(.5f, 1f), MathUtils.random(.3f, 1f), 120f);
        }
        showPopup("ROOM CLEAR!", .4f, 1f, .55f, 1.4f);
        updateObjectiveProgress();
        if (blitzMode && blitzManager != null) {
            int roomPoints = blitzManager.roomClear();
            spawnFloat("+" + roomPoints + " ROOM CLEAR",
                tileCenterX(player.getX()),
                tileCenterY(player.getY()) + 96f,
                1f, .82f, .30f);
        }
        int reward = 25 + (hasRelic(Relic.Blessing.GOLD_MAGNET) ? 10 : 0) - (hasCurse(Relic.Curse.TOLL) ? 10 : 0);
        reward = Math.max(0, reward);
        if (omen == OMEN_BLOOD_MOON || omen == OMEN_IRON) reward = Math.round(reward * 1.5f);
        if (runPact[1]) reward = Math.round(reward * 1.5f);
        if (omen == OMEN_FAMINE) reward = Math.round(reward * 0.7f);
        if (objectiveCompleted) {
            reward += 15;
            message("Objective complete! +15 gold", 2f);
        }
        if (!roomDamageTaken) {
            flawlessStreak++;
            if (flawlessStreak >= 2) {
                int streakBonus = Math.min(25, 5 * (flawlessStreak - 1));
                reward += streakBonus;
                spawnFloat("FLAWLESS x" + flawlessStreak + " +" + streakBonus + "g",
                    tileCenterX(player.getX()), tileCenterY(player.getY()) + 90f, .5f, 1f, .6f);
            }
        } else {
            flawlessStreak = 0;
        }
        // Interest: 1 gold per 10 gold held (before this room's reward), capped
        int interest = Math.min(INTEREST_CAP, gold / 10);
        if (interest > 0) {
            spawnFloat("+" + interest + " INTEREST", tileCenterX(player.getX()), tileCenterY(player.getY()) + 60f, 1f, .85f, .25f);
        }

        gold += reward + interest;
        healPlayer((hasRelic(Relic.Blessing.SECOND_WIND) ? 1 : 0) + (runPact[4] ? 1 : 0));
        rewardScreen.show(reward);
    }

    private void generateNextRoom() {
        difficulty++;
        if (blitzMode && blitzManager != null) blitzManager.resetRoom();
        rollPoison();
        for (int i = 0; i < enemies.size; i++) enemies.get(i).dispose();
        shockwaves.clear();
        debris.clear();
        generateEnemiesForCurrentRoom();
        player.resetArrows();
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        selectedSpell = null;
        doubleMoveActive = false;
        mana = hasCurse(Relic.Curse.MANA_LEAK) ? Math.max(1, getMaxMana() / 2) : getMaxMana();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.play();
        eventMessage = "ROOM " + difficulty;
        messageTimer = 2f;
        playSfx(roomStartSound);
        if (blitzMode && difficulty % 2 == 0) {
            openBlitzRelicShop();
        } else if (difficulty % 3 == 0) {
            openShop();
        } else {
            maybeOpenEvent();
        }
        autosave();
    }

    private boolean allEnemiesDefeated() {
        for (int i = 0; i < enemies.size; i++) if (enemies.get(i).isAlive()) return false;
        return true;
    }

    private int shopPrice(int base) {
        return Math.max(1, Math.round(base * SHOP_DISCOUNT));
    }

    private boolean ownsSpell(Spell s) {
        return ownedSpells.contains(s, true);
    }

    private boolean ownsRelic(Relic r) {
        for (int i = 0; i < ownedRelics.size; i++) if (ownedRelics.get(i).getName().equals(r.getName())) return true;
        return false;
    }

    private Spell pickShopSpell(Spell avoid) {
        Spell[] all = Spell.values();
        for (int attempt = 0; attempt < 30; attempt++) {
            Spell s = all[random.nextInt(all.length)];
            if (!ownsSpell(s) && s != avoid) return s;
        }
        return null;
    }

    private Relic pickShopRelic(Relic avoid) {
        for (int attempt = 0; attempt < 30; attempt++) {
            Relic r = Relic.random(random);
            if (!ownsRelic(r) && r != avoid) return r;
        }
        return null;
    }

    private int rerollCost() {
        return 25 + 15 * shopRerolls;
    }

    private Card weakestCard() {
        Card best = null;
        for (int i = 0; i < hand.size; i++) {
            Card c = hand.get(i);
            if (best == null || c.getUsesRemaining() < best.getUsesRemaining()) best = c;
        }
        return best;
    }

    private int sellValue(Card c) {
        int v = 10 + 5 * c.getUsesRemaining();
        if (cardModifiers.get(c) != null) v += 5;
        if (c.hasBurn() || c.hasPierce() || c.hasRebound() || c.hasDoubleMove() || c.hasTeleportationInfusion())
            v += 10;
        return v;
    }

    private Array<ShopScreen.Item> buildShopItems() {
        Array<ShopScreen.Item> items = new Array<>();
        items.add(new ShopScreen.Item(ShopScreen.Kind.POTION, "Healing Potion", "Heal " + POTION_HEAL + " HP", shopPrice(25), null, null));

        boolean spellSlotsFull = ownedSpells.size >= 3;
        Spell s = spellSlotsFull ? null : pickShopSpell(null);
        Relic r = pickShopRelic(null);

        if (r != null)
            items.add(new ShopScreen.Item(ShopScreen.Kind.RELIC, r.getName(), r.getDescription(), shopPrice(r.getPrice()), null, r));
        if (s != null) {
            items.add(new ShopScreen.Item(ShopScreen.Kind.SPELL, s.getLabel(), s.getDescription(), shopPrice(s.getCost()), s, null));
        } else {
            Relic r2 = pickShopRelic(r);
            if (r2 != null)
                items.add(new ShopScreen.Item(ShopScreen.Kind.RELIC, r2.getName(), r2.getDescription(), shopPrice(r2.getPrice()), null, r2));
        }

        items.add(new ShopScreen.Item(ShopScreen.Kind.AMMO, "Bouncing Arrows x" + BOUNCE_PACK,
            "Ricochet to " + BOUNCE_BOUNCES + " more enemies", shopPrice(50), null, null));

        if (hand.size >= 2) {
            Card w = weakestCard();
            items.add(new ShopScreen.Item(ShopScreen.Kind.SELL, "Sell " + w.getName(),
                "Your weakest card (" + w.getUsesRemaining() + " uses left). Frees a slot.", sellValue(w), null, null));
        }
        return items;
    }

    private void openShop() {
        shopRerolls = 0;
        shopScreen.show(buildShopItems(), rerollCost());
        playSfx(shopOpenSound);
        shopWasOpenedThisRoom = true;
        message("SHOP", 2f);
    }

    private void openBlitzRelicShop() {
        if (!blitzMode || blitzManager == null || !blitzManager.isActive()) return;
        blitzManager.refreshRelicShop();
        blitzRelicShop.show(blitzManager.getRelicShopOffers());
        message("BLITZ RELIC FORGE", 2f);
        playSfx(shopOpenSound);
    }

    private void handleBlitzRelicShopInput() {
        if (!Gdx.input.justTouched()) return;
        float x = Gdx.input.getX();
        float y = Gdx.graphics.getHeight() - Gdx.input.getY();
        int result = blitzRelicShop.handleClick(x, y);

        if (result == BlitzRelicShopScreen.LEAVE) {
            clickSound.play();
            blitzRelicShop.hide();
            message("Forge closed", 1.2f);
            return;
        }
        if (result == BlitzRelicShopScreen.NONE) return;

        BlitzRelic relic = blitzRelicShop.getOffer(result);
        if (relic == null) return;

        if (blitzManager.hasRelic(relic)) {
            blitzRelicShop.setMessage("Already active");
            return;
        }

        if (blitzManager.getActiveRelics().size() >= 6) {
            blitzRelicShop.setMessage("Relic slots full (6/6)");
            playSfx(errorSound);
            return;
        }

        int cost = blitzManager.getRelicCost(relic);
        if (gold < cost) {
            blitzRelicShop.setMessage("Not enough gold");
            playSfx(errorSound);
            return;
        }

        gold -= cost;
        if (blitzManager.buyRelic(relic)) {
            blitzRelicShop.setMessage(relic.getName() + " activated! +4s");
            playSfx(blessingSound);
            autosave();
        } else {
            gold += cost;
            blitzRelicShop.setMessage("Could not activate relic");
            playSfx(errorSound);
        }
    }

    private void handleShopInput() {
        if (!Gdx.input.justTouched()) return;
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        int result = shopScreen.handleClick(x, y);
        if (result == ShopScreen.LEAVE) {
            clickSound.play();
            shopScreen.hide();
            message("Shop closed", 1.2f);
            return;
        }
        if (result == ShopScreen.REROLL) {
            int cost = rerollCost();
            if (shopRerolls >= 2) {
                playSfx(errorSound);
                shopScreen.setMessage("No more restocks");
                return;
            }
            if (gold < cost) {
                playSfx(errorSound);
                shopScreen.setMessage("Not enough gold to reroll");
                return;
            }
            gold -= cost;
            shopRerolls++;
            shopScreen.show(buildShopItems(), rerollCost());
            shopScreen.setMessage("Stock refreshed");
            playSfx(drawSound);
            return;
        }
        if (result == ShopScreen.NONE) return;
        ShopScreen.Item item = shopScreen.getItem(result);
        if (item == null) return;
        if (item.sold) {
            shopScreen.setMessage("Already sold");
            return;
        }

        if (item.kind == ShopScreen.Kind.SELL) {
            Card c = weakestCard();
            if (c == null || hand.size < 2) {
                playSfx(errorSound);
                shopScreen.setMessage("Keep at least one card");
                return;
            }
            int value = sellValue(c);
            hand.removeValue(c, true);
            cardModifiers.remove(c);
            if (activeCard == c) activeCard = null;
            gold += value;
            playSfx(buySound);
            shopScreen.markSold(result);
            shopScreen.setMessage("Sold " + c.getName() + " for " + value);
            positionCards();
            autosave();
            return;
        }

        if (gold < item.cost) {
            playSfx(errorSound);
            shopScreen.setMessage("Not enough gold");
            return;
        }
        gold -= item.cost;
        playSfx(buySound);
        if (item.kind == ShopScreen.Kind.POTION) {
            healPlayer(POTION_HEAL + (hasRelic(Relic.Blessing.HEARTY_BREW) ? 1 : 0));
            message("Potion bought!", 1.5f);
        } else if (item.kind == ShopScreen.Kind.SPELL) {
            ownedSpells.add(item.spell);
            message(item.spell.getLabel() + " learned!", 1.5f);
        } else if (item.kind == ShopScreen.Kind.RELIC) {
            ownedRelics.add(item.relic);
            applyRelic(item.relic);
            playSfx(blessingSound);
            if (item.relic != null && item.relic.isCursed()) playSfx(curseSound);
            message(item.relic.getName() + " acquired!", 1.5f);
        } else if (item.kind == ShopScreen.Kind.AMMO) {
            bounceArrows += BOUNCE_PACK;
            message("+" + BOUNCE_PACK + " bouncing arrows!", 1.5f);
        }
        shopScreen.markSold(result);
        autosave();
    }

    private void gameOver() {
        if (gameOverScreen.isVisible()) return;

        // Blitz is run-based: dying wipes the active score, multiplier and relics.
        if (blitzMode && blitzManager != null) {
            blitzManager.endRun();
            blitzManager.resetRun();
            blitzMode = false;
            blitzHudScore = 0f;
            blitzLastLabel = "";
            blitzLastLabelTimer = 0f;
            blitzLastResult = null;
            blitzBreakdownTimer = 0f;
        }

        lastRunSouls = Math.round((difficulty * SOULS_PER_ROOM + runBonusSouls) * soulMultiplier(runPact));
        souls += lastRunSouls;
        saveMeta();
        clearAutosave();
        evtVisible = false;
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.stop();
        gameOverSound.play();
        message("You were captured! +" + lastRunSouls + " souls", 3f);
        gameOverScreen.show();
    }

    private void restartGame() {
        gameOverScreen.hide();
        beginIntro(this::doRestart);
    }

    private void doRestart() {
        clearMoveAnim();
        difficulty = 1;
        omen = OMEN_NONE;
        evtVisible = false;
        cardModifiers.clear();
        enemyCurses.clear();
        poisonTurns.clear();
        burnTurns.clear();
        armorShields.clear();
        doubleMoveActive = false;
        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        if (player != null) player.dispose();
        player = new Player(2, 2);
        resetPlayerStats();
        buildStartingHand();
        if (enemies != null) for (int i = 0; i < enemies.size; i++) enemies.get(i).dispose();
        rollPoison();
        generateEnemiesForCurrentRoom();
        rewardScreen.hide();
        gameOverScreen.hide();
        shopScreen.hide();
        positionCards();
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.play();
        }
        message("Room 1", 2f);
    }

    // ---------- Intro: CHESS gets stabbed by a dagger and becomes CLOAKCHESS ----------
    private void beginIntro(Runnable action) {
        introAction = action;
        introT = 0f;
        introImpacted = false;
        introActive = true;
        menuVisible = false;
        bestiaryVisible = false;
        tutorialVisible = false;
        logoRevealed = true;
        logoFade = 1f;
    }
    // ---------------------------------------------------------

    private void applyRelic(Relic relic) {
        if (relic == null) return;
        if (relic.getBlessing() == Relic.Blessing.IRON_HEART) playerMaxHp++;
        if (relic.isCursed() && relic.getCurse() == Relic.Curse.FRAGILE) {
            playerMaxHp = Math.max(1, playerMaxHp - 1);
            playerHp = Math.min(playerHp, playerMaxHp);
        }
        if (relic.getBlessing() == Relic.Blessing.ARCANE_WELL) mana = Math.min(getMaxMana(), mana + 2);
        mana = Math.min(mana, getMaxMana());
    }

    private void toggleArrowMode() {
        if (player.hasArrows() || bounceArrows > 0) {
            shootMode = !shootMode;
            if (activeCard != null) activeCard.setSelected(false);
            activeCard = null;
            selectedSpell = null;
            player.setSelected(shootMode);
            message(shootMode ? "Arrow mode: choose a target" : "Arrow mode cancelled", 1.5f);
        } else message("No arrows left!", 1.5f);
    }

    private Enemy nearestEnemyTo(int fx, int fy) {
        Enemy best = null;
        int bestD = Integer.MAX_VALUE;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive() || e.isFalling() || arrowHits.contains(e, true)) continue;
            int dx = e.getX() - fx, dy = e.getY() - fy;
            int d = dx * dx + dy * dy;
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    private void resolveArrowHit() {
        Enemy hit = pendingHitEnemy;
        int fx = 0, fy = 0;
        if (hit != null) {
            fx = hit.getX();
            fy = hit.getY();
        }
        if (hit != null && hit.isAlive()) {
            if (captureCurses.get(hit) == CaptureCurse.ARROWPROOF) {
                spawnFloat("ARROWPROOF", tileCenterX(hit.getX()), tileCenterY(hit.getY()) + 34f, .85f, .55f, .25f);
                message("Arrowproof! The arrow fizzled.", 1.2f);
            } else {
                damageEnemy(hit, 999);
            }
        }
        pendingHitEnemy = null;

        if (arrowBouncesLeft > 0 && hit != null) {
            Enemy next = nearestEnemyTo(fx, fy);
            if (next != null) {
                arrowBouncesLeft--;
                arrowHits.add(next);
                arrowStartX = tileCenterX(fx);
                arrowStartY = tileCenterY(fy);
                arrowTargetX = tileCenterX(next.getX());
                arrowTargetY = tileCenterY(next.getY());
                pendingHitEnemy = next;
                arrowProgress = 0f;
                spawnBurst(arrowStartX, arrowStartY, 14, 1f, .6f, .2f, 40f);
                spawnShockwave(fx, fy, 0.7f);
                arrowSound.play();
                return; // arrow stays in flight for the next leg
            }
        }

        arrowBouncesLeft = 0;
        arrowBouncing = false;
        arrowInFlight = false;
        if (allEnemiesDefeated()) winRoom();
        else moveEnemies();
    }

    private void handleArrowInput() {
        if (!Gdx.input.justTouched()) return;
        float x = Gdx.input.getX(), y = Gdx.graphics.getHeight() - Gdx.input.getY();
        int tx = (int) ((x - Player.getBoardX()) / Player.TILE_SIZE), ty = (int) ((y - Player.getBoardY()) / Player.TILE_SIZE);
        if (!player.isValidArrowTarget(tx, ty)) {
            message("Invalid arrow direction!", 1.5f);
            return;
        }
        Enemy hit = findFirstEnemyInLine(tx, ty);
        if (hit == null) {
            message("No target in line!", 1.5f);
            return;
        }
        pendingHitEnemy = hit;
        arrowHits.clear();
        arrowHits.add(hit);
        arrowStartX = tileCenterX(player.getX());
        arrowStartY = tileCenterY(player.getY());
        arrowTargetX = tileCenterX(hit.getX());
        arrowTargetY = tileCenterY(hit.getY());
        arrowProgress = 0;
        arrowInFlight = true;
        if (bounceArrows > 0) {
            bounceArrows--;
            arrowBouncing = true;
            arrowBouncesLeft = BOUNCE_BOUNCES;
        } else {
            player.useArrow();
            arrowBouncing = false;
            arrowBouncesLeft = 0;
        }
        arrowSound.play();
        shootMode = false;
        player.setSelected(false);
    }

    private void updateIntro(float delta) {
        introT += delta;
        // Tap to skip to the fade-out
        if (Gdx.input.justTouched() && introT > 0.6f && introT < INTRO_OUT_START) introT = INTRO_OUT_START;
        if (!introImpacted && introT >= INTRO_IMPACT) {
            introImpacted = true;
            playSfx(daggerSound);
            startShake(0.35f, 14f);
        }
        if (introT >= INTRO_TOTAL) {
            introActive = false;
            Runnable action = introAction;
            introAction = null;
            if (action != null) action.run();
        }
    }

    private void renderIntro() {
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float t = introT;
        float fadeIn = MathUtils.clamp(t / 0.4f, 0f, 1f);
        float fadeOut = 1f - MathUtils.clamp((t - INTRO_OUT_START) / (INTRO_TOTAL - INTRO_OUT_START), 0f, 1f);
        float cloakA = introImpacted ? MathUtils.clamp((t - INTRO_IMPACT) / 0.2f, 0f, 1f) : 0f;

        float lw = Math.min(w * 0.9f, 900f), lh = lw * 220f / 960f;
        float lx = (w - lw) / 2f, ly = (h - lh) / 2f;
        float cx = w / 2f, cy = h / 2f;

        // Dagger travels down-right toward the logo centre
        float dirX = 0.6f, dirY = -0.8f;
        float reach = Math.max(w, h) * 0.7f;
        float sx = cx - dirX * reach, sy = cy - dirY * reach;
        float flyP = MathUtils.clamp((t - INTRO_FLY_START) / (INTRO_IMPACT - INTRO_FLY_START), 0f, 1f);
        float ease = flyP * flyP;
        float tipX = sx + (cx - sx) * ease, tipY = sy + (cy - sy) * ease;

        // Background + incoming trail
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.03f, .03f, .05f, 1f);
        shapeRenderer.rect(-60f, -60f, w + 120f, h + 120f);
        if (t >= INTRO_FLY_START && flyP < 1f) {
            shapeRenderer.setColor(1f, 1f, 1f, .35f * fadeOut);
            shapeRenderer.rectLine(sx, sy, tipX, tipY, 3f);
        }
        shapeRenderer.end();

        // Logos: CHESS -> CLOAKCHESS on impact
        batch.begin();
        batch.setColor(1f, 1f, 1f, fadeIn * (1f - cloakA) * fadeOut);
        if (cloakA < 1f) batch.draw(logoChessTexture, lx, ly, lw, lh);
        batch.setColor(1f, 1f, 1f, cloakA * fadeOut);
        if (cloakA > 0f) batch.draw(logoCloakTexture, lx, ly, lw, lh);
        batch.setColor(Color.WHITE);
        batch.end();

        // Slash streak + white flash on impact
        if (introImpacted) {
            float s = (t - INTRO_IMPACT) / 0.35f;
            float flash = 1f - MathUtils.clamp((t - INTRO_IMPACT) / 0.15f, 0f, 1f);
            if (s < 1f || flash > 0f) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                if (s < 1f) {
                    float L = lw * 0.65f;
                    shapeRenderer.setColor(1f, 1f, 1f, (1f - s) * fadeOut);
                    shapeRenderer.rectLine(cx - dirX * L, cy - dirY * L, cx + dirX * L, cy + dirY * L, 2f + 10f * (1f - s));
                }
                if (flash > 0f) {
                    shapeRenderer.setColor(1f, 1f, 1f, .45f * flash);
                    shapeRenderer.rect(-60f, -60f, w + 120f, h + 120f);
                }
                shapeRenderer.end();
            }
        }

        // Dagger (stays stuck in the logo, then fades)
        if (t >= INTRO_FLY_START) {
            float dh = Math.min(h * 0.4f, 320f), dw = dh / 4f;
            float rot = MathUtils.atan2(dirY, dirX) * MathUtils.radiansToDegrees - 90f;
            float a = fadeOut * (1f - MathUtils.clamp((t - INTRO_IMPACT - 0.8f) / 0.4f, 0f, 1f));
            batch.begin();
            batch.setColor(1f, 1f, 1f, a);
            batch.draw(daggerTexture, tipX - dw / 2f, tipY - dh, dw / 2f, dh, dw, dh, 1f, 1f, rot,
                0, 0, daggerTexture.getWidth(), daggerTexture.getHeight(), false, false);
            batch.setColor(Color.WHITE);
            batch.end();
        }
    }

    private void renderReturnMenuButton() {
        layoutMenuButtons();
        batch.begin();
        batch.setColor(Color.WHITE);
        batch.draw(menuButtonTexture, returnMenuButton.x, returnMenuButton.y, returnMenuButton.width, returnMenuButton.height);
        font.setColor(Color.WHITE);
        font.getData().setScale(.62f);
        font.draw(batch, "MAIN MENU", returnMenuButton.x + 34f, returnMenuButton.y + 36f);
        font.getData().setScale(1f);
        batch.end();
    }

    private void describeMovement(Enemy e, Array<String> lines) {
        switch (e.getMovementType().name()) {
            case "KNIGHT":
                lines.add("Moves like a knight");
                break;
            case "BISHOP":
                lines.add("Moves like a bishop");
                break;
            case "ROOK":
                lines.add("Moves like a rook");
                break;
            case "QUEEN":
                lines.add("Moves like a queen");
                break;
            case "PAWN":
                lines.add("Moves like a pawn");
                break;
            case "CHAMELEON":
                lines.add("Becomes a knight, bishop");
                lines.add("or rook each turn");
                break;
            case "JESTER":
                lines.add("Cycles each turn:");
                lines.add("diagonal, knight, straight, 1 step");
                if (e.getMovementRules() instanceof JesterRules) {
                    String[] names = {"diagonal", "knight", "straight", "1 step"};
                    int turn = ((JesterRules) e.getMovementRules()).getTurnCounter();
                    lines.add("Next move: " + names[MathUtils.clamp(turn, 1, 4) - 1]);
                }
                break;
            case "MADROOK":
                lines.add("Erratic: random sideways,");
                lines.add("vertical or diagonal lines");
                break;
            case "BLINKER":
                lines.add("Blinks to random tiles");
                lines.add("(40% chance per tile)");
                break;
            default:
                break;
        }
    }

    private void renderTooltip() {
        if (tooltipEnemy == null) return;
        boolean allowed = activeCard == null && !player.isSelected() && selectedSpell == null && !shootMode
            && !moveAnimActive && !arrowInFlight && !gameOverScreen.isVisible() && !shopScreen.isVisible()
            && !rewardScreen.isVisible() && !evtVisible && tooltipEnemy.isAlive() && enemies.contains(tooltipEnemy, true);
        if (!allowed) {
            tooltipEnemy = null;
            return;
        }
        Enemy e = tooltipEnemy;

        // Build the lines
        String title = e.getMovementType().name();
        Array<String> lines = new Array<>();
        lines.add("HP: " + e.getHealth() + "/" + e.getMaxHealth());
        EnemyCurse curse = enemyCurses.get(e);
        if (curse != null) lines.add(curse.getLabel() + ": " + curse.getDescription());
        CaptureCurse captureCurse = captureCurses.get(e);
        if (captureCurse != null) lines.add(captureCurse.getLabel() + ": " + captureCurse.getDescription());
        Integer shield = armorShields.get(e);
        if (shield != null && shield > 0) lines.add("Shield: " + shield);
        Integer pz = poisonTurns.get(e);
        if (pz != null && pz > 0) lines.add("Poisoned: " + pz + " turns" + (pz >= 6 ? " (2 dmg/turn)" : ""));
        Integer bn = burnTurns.get(e);
        if (bn != null && bn > 0) lines.add("Burning: " + bn + " turns");
        Float cf = confusedUntil.get(e);
        if (cf != null && uiTime < cf) lines.add("Confused");
        describeMovement(e, lines);

        // Measure
        float titleScale = .7f, bodyScale = .5f, pad = 16f, gap = 6f;
        font.getData().setScale(titleScale);
        tipLayout.setText(font, title);
        float maxW = tipLayout.width, totalH = tipLayout.height + gap + 4f;
        font.getData().setScale(bodyScale);
        for (int i = 0; i < lines.size; i++) {
            tipLayout.setText(font, lines.get(i));
            maxW = Math.max(maxW, tipLayout.width);
            totalH += tipLayout.height + gap;
        }
        float boxW = maxW + pad * 2f, boxH = totalH + pad * 2f - gap;

        // Place next to the enemy (flip to the left if it would leave the screen)
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        float ex = tileCenterX(e.getX()), ey = tileCenterY(e.getY());
        float bx = ex + Player.TILE_SIZE / 2f + 8f;
        if (bx + boxW > w - 6f) bx = ex - Player.TILE_SIZE / 2f - 8f - boxW;
        bx = MathUtils.clamp(bx, 6f, Math.max(6f, w - boxW - 6f));
        float by = MathUtils.clamp(ey - boxH / 2f, 6f, Math.max(6f, h - boxH - 6f));

        batch.begin();
        batch.setColor(Color.WHITE);
        tooltipPatch.draw(batch, bx, by, boxW, boxH);
        float ty = by + boxH - pad;
        font.getData().setScale(titleScale);
        font.setColor(new Color(.66f, .63f, .52f, 1f));
        tipLayout.setText(font, title);
        font.draw(batch, title, bx + pad, ty);
        ty -= tipLayout.height + gap + 4f;
        font.getData().setScale(bodyScale);
        for (int i = 0; i < lines.size; i++) {
            String line = lines.get(i);
            if ((curse != null && line.startsWith(curse.getLabel())) || line.startsWith("Shield"))
                font.setColor(new Color(.9f, .55f, .4f, 1f));
            else if (line.startsWith("Poisoned")) font.setColor(new Color(.6f, .9f, .5f, 1f));
            else if (line.startsWith("Burning")) font.setColor(new Color(1f, .7f, .3f, 1f));
            else font.setColor(new Color(.82f, .84f, .9f, 1f));
            tipLayout.setText(font, line);
            font.draw(batch, line, bx + pad, ty);
            ty -= tipLayout.height + gap;
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // Screen-edge tint images (drawn art, stretched to the screen as nine-patches so it fits desktop and mobile):
    // green while standing on poison, red when on last HP or just hurt.
    private void renderStatusBorder() {
        if (gameOverScreen.isVisible()) return;
        boolean poisoned = board.isPoisoned(player.getX(), player.getY());
        float beat = playerHp <= 1 ? 0.55f + 0.35f * (0.5f + 0.5f * MathUtils.sin(uiTime * 7f)) : 0f;
        float redI = Math.min(1f, Math.max(beat, flashTimer / FLASH_DURATION));
        if (!poisoned && redI <= 0f) return;
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        batch.begin();
        if (poisoned) {
            batch.setColor(1f, 1f, 1f, 1f);
            tintGreenPatch.draw(batch, 0, 0, w, h);
        }
        if (redI > 0f) {
            batch.setColor(1f, 1f, 1f, redI);
            tintRedPatch.draw(batch, 0, 0, w, h);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    // ---------- Center-screen popups ----------
    private String popupText = "";
    private float popupTimer = 0f, popupDur = 1f;
    private final Color popupColor = new Color(1f, 1f, 1f, 1f);

    private void showPopup(String text, float r, float g, float b, float dur) {
        popupText = text;
        popupTimer = dur;
        popupDur = dur;
        popupColor.set(r, g, b, 1f);
    }

    private void renderPopup() {
        if (popupTimer <= 0f) return;
        popupTimer -= Gdx.graphics.getDeltaTime();
        float p = 1f - MathUtils.clamp(popupTimer / popupDur, 0f, 1f);
        float scale = 1.0f + 0.6f * (1f - MathUtils.clamp(p * 5f, 0f, 1f)); // pops in big, then settles
        float a = MathUtils.clamp(popupTimer * 2.5f, 0f, 1f);
        float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
        batch.begin();
        font.getData().setScale(scale);
        tipLayout.setText(font, popupText);
        float tx = (w - tipLayout.width) / 2f, ty = h * 0.62f;
        font.setColor(0f, 0f, 0f, a * .7f);
        font.draw(batch, popupText, tx + 3f, ty - 3f);
        font.setColor(popupColor.r, popupColor.g, popupColor.b, a);
        font.draw(batch, popupText, tx, ty);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(.08f, .09f, .12f, 1f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Screen shake: offset the projection for both batch and shapeRenderer
        float shakeX = 0f, shakeY = 0f;
        if (!menuVisible && shakeTimer > 0f) {
            shakeTimer = Math.max(0f, shakeTimer - delta);
            float k = shakeTimer / shakeDuration;
            shakeX = MathUtils.random(-1f, 1f) * shakeMag * k;
            shakeY = MathUtils.random(-1f, 1f) * shakeMag * k;
        }
        if (flashTimer > 0f) flashTimer = Math.max(0f, flashTimer - delta);
        uiTime += delta;
        viewProj.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()).translate(shakeX, shakeY, 0f);
        batch.setProjectionMatrix(viewProj);
        shapeRenderer.setProjectionMatrix(viewProj);

        if (menuVisible) {
            handleMenuInput();
            renderMenu();
            if (!bestiaryVisible && !upgradesVisible && !tutorialVisible) {
                renderUpgradesButton();
                renderTutorialButton();
            }
            return;
        }

        if (introActive) {
            updateIntro(delta);
            if (introActive) renderIntro();
            return;
        }

        if (blitzMode && blitzManager != null && blitzManager.isActive()) {
            blitzManager.update(delta);
            if (!blitzManager.isActive()) {
                BlitzRelic unlocked = blitzManager.getLastUnlockedRelic();
                String relicText = unlocked == null ? "" : "  •  RELIC: " + unlocked.getName();
                showPopup("BLITZ OVER  " + String.format("%,d", blitzManager.getScore()) + relicText,
                    1f, .75f, .25f, 4f);
                blitzMode = false;
            }
        }

        for (Card c : hand) c.update(delta);
        if (!gameOverScreen.isVisible() && !shopScreen.isVisible() && !blitzRelicShop.isVisible()) updateCardHover();
        checkEnemyLandings();
        handleInput();
        if (blitzMode && blitzManager != null) {
            float timeDelta = blitzManager.consumeLastTimeDelta();
            if (Math.abs(timeDelta) > 0.01f) {
                String sign = timeDelta > 0f ? "+" : "";
                showPopup(sign + String.format("%.1f", timeDelta) + "s " + blitzManager.getLastTimeReason(),
                    timeDelta > 0f ? .35f : 1f,
                    timeDelta > 0f ? 1f : .35f,
                    timeDelta > 0f ? .55f : .35f,
                    .75f);
            }
        }
        updateMessage(delta);
        updateArrowAnimation(delta);
        updateMoveAnim(delta);
        updatePendingArrivals();
        updateShockwaves(delta);
        updateDebris(delta);
        updateSpellFx(delta);
        updateFloats(delta);
        updateSlashes(delta);
        board.render();
        renderPoisonTiles();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < enemies.size; i++) enemies.get(i).renderShadow(shapeRenderer);
        if (activeCard != null && player.isSelected()) player.renderLegalMoves(shapeRenderer, activeCard, board);
        if (shootMode) player.renderArrowRange(shapeRenderer);
        shapeRenderer.end();
        batch.begin();
        for (Card c : hand) c.drawShape(batch);
        for (int i = 0; i < enemies.size; i++) enemies.get(i).render(batch);
        beginPlayerAnim();
        if (invisibleTurns > 0) batch.setColor(1f, 1f, 1f, 0.35f + 0.1f * MathUtils.sin(uiTime * 4f));
        player.render(batch);
        batch.setColor(Color.WHITE);
        endPlayerAnim();
        if (arrowInFlight) {
            float x = MathUtils.lerp(arrowStartX, arrowTargetX, arrowProgress), y = MathUtils.lerp(arrowStartY, arrowTargetY, arrowProgress);
            float angle = MathUtils.radiansToDegrees * MathUtils.atan2(arrowTargetY - arrowStartY, arrowTargetX - arrowStartX);
            if (arrowBouncing) batch.setColor(1f, .6f, .2f, 1f);
            batch.draw(arrowTexture, x - ARROW_DRAW_WIDTH, y - ARROW_DRAW_HEIGHT, ARROW_DRAW_WIDTH, ARROW_DRAW_HEIGHT, ARROW_DRAW_WIDTH, ARROW_DRAW_HEIGHT, 2f, 2f, angle, 0, 0, arrowTexture.getWidth(), arrowTexture.getHeight(), false, false);
            batch.setColor(Color.WHITE);
        }
        for (Card c : hand) c.drawText(batch, font);
        font.getData().setScale(1f);
        font.setColor(new Color(.3f, .8f, 1f, 1f));
        font.draw(batch, eventMessage, 20, Gdx.graphics.getHeight() - 20);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
        renderHud();
        renderShockwaves();
        renderDebris();
        renderSpellFx();
        renderSlashes();
        renderEnemyCurseBadges();
        renderDefenseIcon();
        renderFloatTexts();
        renderObjectivePanel();
        renderBlitzHud();
        renderBlitzComboText();
        renderUiButtons();
        renderTooltip();
        rewardScreen.render(hand);
        if (shopScreen.isVisible()) shopScreen.render(gold);
        if (blitzRelicShop.isVisible()) blitzRelicShop.render(gold, blitzManager);
        if (evtVisible) renderEvent();
        gameOverScreen.render();
        renderPopup();
        if (gameOverScreen.isVisible()) renderReturnMenuButton();
        if (gameOverScreen.isVisible()) {
            renderUpgradesButton();
            if (upgradesVisible) renderUpgrades();
        }

        renderStatusBorder();
        renderFrenzy();
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (board != null) board.dispose();
        if (player != null) player.dispose();
        if (enemies != null) for (int i = 0; i < enemies.size; i++) enemies.get(i).dispose();
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.dispose();
        }
        if (moveSound != null) moveSound.dispose();
        if (victorySound != null) victorySound.dispose();
        if (gameOverSound != null) gameOverSound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (augmentSound != null) augmentSound.dispose();
        if (arrowSound != null) arrowSound.dispose();
        if (arrowTexture != null) arrowTexture.dispose();
        if (menuButtonTexture != null) menuButtonTexture.dispose();
        if (menuButtonLockedTexture != null) menuButtonLockedTexture.dispose();
        if (menuPanelTexture != null) menuPanelTexture.dispose();
        if (logoChessTexture != null) logoChessTexture.dispose();
        if (logoCloakTexture != null) logoCloakTexture.dispose();
        if (daggerTexture != null) daggerTexture.dispose();
        if (tooltipTexture != null) tooltipTexture.dispose();
        if (tintGreenTexture != null) tintGreenTexture.dispose();
        if (tintRedTexture != null) tintRedTexture.dispose();
        if (circleTex != null) circleTex.dispose();
        for (Texture t : iconTex.values()) t.dispose();
        for (int i = 0; i < extraSfx.size; i++) extraSfx.get(i).dispose();
        Card.disposeTextures();
    }

    // Crumble debris (enemy pieces breaking apart)
    private static class Debris {
        float x, y, vx, vy, rot, vrot, size, life, maxLife;
    }

    // Floating combat text
    private static class FloatText {
        String text;
        float x, y, life, maxLife, r, g, b;
    }

    private void updateMessage(float delta) {
        if (messageTimer <= 0) return;
        messageTimer -= delta;
        if (messageTimer <= 0) {
            messageTimer = 0;
            eventMessage = "";
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (hand != null) positionCards();
    }

    @Override
    public void pause() {
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.pause();
    }

    @Override
    public void resume() {
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.play();
    }

    // ---------- Spell visual effects ----------
    private static class Spark {
        float x, y, vx, vy, life, maxLife, size, r, g, b;
    }

    private static class Bolt {
        float[] xs, ys;
        float timer;
    }
}
