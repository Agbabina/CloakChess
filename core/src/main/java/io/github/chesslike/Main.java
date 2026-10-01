
    package io.github.chesslike;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.Preferences;
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

    private Music backgroundMusic;
    private Sound moveSound, victorySound, gameOverSound, clickSound, augmentSound, arrowSound;

    private Array<Card> hand;
    private Card activeCard;
    private static final int MAX_HAND_SIZE = 5;
    private final Random random = new Random();
    private static final float CARD_REWARD_CHANCE = 0.50f;

    private final ObjectMap<Card, CardModifier> cardModifiers = new ObjectMap<>();
    private final ObjectMap<Enemy, EnemyCurse> enemyCurses = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> poisonTurns = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> burnTurns = new ObjectMap<>();
    private final ObjectMap<Enemy, Integer> armorShields = new ObjectMap<>();

    private Array<Spell> ownedSpells = new Array<>();
    private Array<Relic> ownedRelics = new Array<>();
    private Spell selectedSpell;
    private ShopScreen shopScreen;

    // Main menu
    private boolean menuVisible = true;
    private boolean bestiaryVisible = false;
    private final Rectangle startButton = new Rectangle();
    private final Rectangle bestiaryButton = new Rectangle();
    private final Rectangle blitzButton = new Rectangle();
    private final Rectangle backButton = new Rectangle();
    private final Rectangle continueButton = new Rectangle();
    private final Rectangle returnMenuButton = new Rectangle();
    private Preferences savePrefs;

    private int playerMaxHp = 3;
    private int playerHp = 3;
    private boolean doubleMoveActive = false;
    private boolean shopWasOpenedThisRoom = false;

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
    private static class Shockwave { float x, y, timer, maxRadius; }
    private final Array<Shockwave> shockwaves = new Array<>();
    private static final float SHOCKWAVE_DURATION = 0.35f;

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

    // On-screen touch buttons: 0 = arrow, 1-3 = spell slots, 4 = cancel
    private final Rectangle[] uiButtons = {new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle(), new Rectangle()};
    private static final float UI_BTN_W = 120f, UI_BTN_H = 55f, UI_BTN_GAP = 10f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        savePrefs = Gdx.app.getPreferences("CloakChessSave");
        moveSound = Gdx.audio.newSound(Gdx.files.internal("freesound_community-ficha-de-ajedrez-34722.mp3"));
        victorySound = Gdx.audio.newSound(Gdx.files.internal("emand_edroff-victory-bell-success-fanfare-576275.mp3"));
        gameOverSound = Gdx.audio.newSound(Gdx.files.internal("gameOver.mp3"));
        clickSound = Gdx.audio.newSound(Gdx.files.internal("dragon-studio-button-press-386165.mp3"));
        augmentSound = Gdx.audio.newSound(Gdx.files.internal("spell.mp3"));
        arrowSound = Gdx.audio.newSound(Gdx.files.internal("arrow.mp3"));

        if (Gdx.files.internal("background.mp3").exists()) {
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("background.mp3"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(0.35f);
            backgroundMusic.play();
        }

        arrowTexture = new Texture(Gdx.files.internal("pixelarrow.png"));
        arrowTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Card.loadTextures();
        RewardScreen.loadTextures();
        shapeRenderer = new ShapeRenderer();

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 22;
        font = generator.generateFont(parameter);
        generator.dispose();

        board = new Board(batch);
        player = new Player(2, 2);
        enemyGenerator = new EnemyGenerator();
        hand = new Array<>();
        rewardScreen = new RewardScreen(batch, shapeRenderer, font);
        gameOverScreen = new GameOverScreen(batch, shapeRenderer, font);
        shopScreen = new ShopScreen(batch, shapeRenderer, font);

        resetPlayerStats();
        buildStartingHand();
        positionCards();

        // Start on the main menu instead of immediately entering Room 1.
        menuVisible = true;
        bestiaryVisible = false;
    }

    private void resetPlayerStats() { playerMaxHp = 3; playerHp = 3; gold = 0; ownedSpells.clear(); ownedRelics.clear(); mana = getMaxMana(); }

    // MANA (relic-aware)
    private int getMaxMana() {
        int m = BASE_MAX_MANA;
        if (hasRelic(Relic.Blessing.ARCANE_WELL)) m += 2;
        if (hasCurse(Relic.Curse.DRAINED)) m -= 2;
        return Math.max(1, m);
    }

    // MANA COSTS (edit the base numbers to tune spell balance)
    private int manaCost(Spell spell) {
        int cost;
        if (spell == Spell.LIGHTNING) cost = 3;
        else if (spell == Spell.POISON) cost = 2;
        else if (spell == Spell.BURN) cost = 2;
        else cost = 2;
        if (hasRelic(Relic.Blessing.THRIFTY_MAGE)) cost -= 1;
        if (hasCurse(Relic.Curse.COSTLY_MAGIC)) cost += 1;
        return Math.max(1, cost);
    }

    private float cardRewardChance() {
        float chance = CARD_REWARD_CHANCE;
        if (hasRelic(Relic.Blessing.LUCKY_DRAW)) chance += 0.25f;
        if (hasCurse(Relic.Curse.STINGY_DECK)) chance -= 0.25f;
        return MathUtils.clamp(chance, 0f, 1f);
    }

    private void buildStartingHand() {
        hand.clear();
        hand.add(createCard(Card.MovementType.KNIGHT));
        hand.add(createCard(Card.MovementType.BISHOP));
        hand.add(createCard(Card.MovementType.ROOK));
        hand.add(createCard(Card.MovementType.QUEEN));
        hand.add(createCard(Card.MovementType.DASH));
    }

    private Card createCard(Card.MovementType type) {
        Card card;
        switch (type) {
            case KNIGHT: card = new Card("Knight", Card.MovementType.KNIGHT, 2); break;
            case BISHOP: card = new Card("Bishop", Card.MovementType.BISHOP, 2); break;
            case ROOK: card = new Card("Rook", Card.MovementType.ROOK, 2); break;
            case QUEEN: card = new Card("Queen", Card.MovementType.QUEEN, 2); break;
            case PAWN: card = new Card("Pawn", Card.MovementType.PAWN, 2); break;
            case DASH: card = new Card("Dash", Card.MovementType.DASH, 2); break;
            default: return createCard(Card.MovementType.KNIGHT);
        }
        if (random.nextFloat() < 0.25f) {
            CardModifier modifier = CardModifier.random(random);
            cardModifiers.put(card, modifier);
        }
        return card;
    }

    private void generateEnemiesForCurrentRoom() {
        enemies = enemyGenerator.generateEnemies(difficulty, player.getX(), player.getY());
        enemyCurses.clear();
        poisonTurns.clear();
        burnTurns.clear();
        armorShields.clear();
        for (int i = 0; i < enemies.size; i++) assignEnemyCurse(enemies.get(i));
        if (hasCurse(Relic.Curse.HARDENED)) {
            for (int i = 0; i < enemies.size; i++) {
                Enemy en = enemies.get(i);
                Integer s = armorShields.get(en);
                armorShields.put(en, (s == null ? 0 : s) + 1);
            }
        }
        startEnemyDrop();
    }

    private void assignEnemyCurse(Enemy enemy) {
        if (random.nextFloat() >= 0.25f) return;
        EnemyCurse curse = random.nextBoolean() ? EnemyCurse.ARMORED : EnemyCurse.VENGEFUL;
        enemyCurses.put(enemy, curse);
        if (curse == EnemyCurse.ARMORED) armorShields.put(enemy, 1);
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
            for (int i = 0; i < hand.size; i++) { Card c = hand.get(i); c.setLayoutScale(scale); c.setPosition(w - cw - 20f, startY + i * (ch + gap)); }
        } else {
            float spacing = 6f;
            float required = hand.size * 115f + (hand.size - 1) * spacing;
            float scale = MathUtils.clamp(w - 20f < required ? (w - 20f) / required : 1f, 0.42f, 0.75f);
            float cw = 115f * scale, ch = 155f * scale, gap = spacing * scale;
            float startX = (w - (hand.size * cw + (hand.size - 1) * gap)) / 2f;
            for (int i = 0; i < hand.size; i++) { Card c = hand.get(i); c.setLayoutScale(scale); c.setPosition(startX + i * (cw + gap), 15f); }
        }
    }

    private void giveRandomCard() {
        if (hand.size >= MAX_HAND_SIZE) { message("Hand full!", 2f); return; }
        if (random.nextFloat() > cardRewardChance()) { message("No card this turn...", 2f); return; }
        Card.MovementType[] types = {Card.MovementType.KNIGHT, Card.MovementType.BISHOP, Card.MovementType.ROOK, Card.MovementType.QUEEN, Card.MovementType.PAWN, Card.MovementType.DASH};
        Card c = createCard(types[random.nextInt(types.length)]);
        hand.add(c); positionCards(); message("You received a " + c.getName() + "!", 2f);
    }

    private void updateCardHover() {
        float mx = Gdx.input.getX(), my = Gdx.graphics.getHeight() - Gdx.input.getY();
        for (Card c : hand) c.setHovered(c.isClicked(mx, my));
    }

    private void startEnemyDrop() { for (int i = 0; i < enemies.size; i++) enemies.get(i).startFall(0.15f + i * DROP_STAGGER); }
    private boolean anyEnemyFalling() { for (int i = 0; i < enemies.size; i++) { Enemy e = enemies.get(i); if (e.isAlive() && e.isFalling()) return true; } return false; }
    private void checkEnemyLandings() { for (int i = 0; i < enemies.size; i++) { Enemy e = enemies.get(i); if (e.consumeLanded()) { spawnShockwave(e.getX(), e.getY(), 0.9f); moveSound.play(); } } }

    private void spawnShockwave(int x, int y, float radius) { Shockwave w = new Shockwave(); w.x = tileCenterX(x); w.y = tileCenterY(y); w.maxRadius = radius * Player.TILE_SIZE; shockwaves.add(w); }
    private void updateShockwaves(float delta) { for (int i = shockwaves.size - 1; i >= 0; i--) { Shockwave w = shockwaves.get(i); w.timer += delta; if (w.timer >= SHOCKWAVE_DURATION) shockwaves.removeIndex(i); } }
    private void renderShockwaves() {
        if (shockwaves.size == 0) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < shockwaves.size; i++) { Shockwave w = shockwaves.get(i); float p = MathUtils.clamp(w.timer / SHOCKWAVE_DURATION, 0f, 1f); float r = w.maxRadius * (1f - (1f-p)*(1f-p)); shapeRenderer.setColor(1f, .85f, .4f, 1f-p); shapeRenderer.circle(w.x,w.y,r,48); }
        shapeRenderer.end();
    }

    private Enemy getEnemyAt(int x, int y, Enemy ignore) {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e != ignore && e.isAlive() && e.getX() == x && e.getY() == y) return e;
        }
        return null;
    }

    private boolean damageEnemy(Enemy enemy, int amount) {
        if (enemy == null || !enemy.isAlive()) return false;
        Integer shield = armorShields.get(enemy);
        if (shield != null && shield > 0) {
            armorShields.put(enemy, shield - 1);
            message("Armored blocked 1 damage!", 1.2f);
            return false;
        }
        boolean died = enemy.takeDamage(amount);
        if (died) enemyCurses.remove(enemy);
        return died;
    }

    private void pushEnemy(Enemy enemy, int dx, int dy) {
        if (enemy == null || !enemy.isAlive()) return;

        int nx = enemy.getX() + dx;
        int ny = enemy.getY() + dy;

        // Check if the target push tile is blocked by board boundaries, another enemy, or the player
        boolean outOfBounds = nx < 0 || nx >= Player.BOARD_SIZE || ny < 0 || ny >= Player.BOARD_SIZE;
        boolean occupiedByEnemy = getEnemyAt(nx, ny, enemy) != null;
        boolean occupiedByPlayer = (nx == player.getX() && ny == player.getY());

        if (outOfBounds || occupiedByEnemy || occupiedByPlayer) {
            // Slam damage if pushed into a wall/obstacle
            spawnShockwave(enemy.getX(), enemy.getY(), 0.6f);
            boolean died = damageEnemy(enemy, 1);
            message(died ? "Enemy crushed!" : "Enemy slammed!", 1.5f);
        } else {
            // Clean push outward
            enemy.pushTo(nx, ny);
        }
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

    private boolean isDashCard(Card c){ return c.getName().equalsIgnoreCase("Dash"); }
    private void pushEnemiesAlongDash(int tx,int ty){ int dx=tx-player.getX(),dy=ty-player.getY(); if(dx!=0&&dy!=0&&Math.abs(dx)!=Math.abs(dy))return; int sx=Integer.signum(dx),sy=Integer.signum(dy),steps=Math.max(Math.abs(dx),Math.abs(dy)); boolean any=false; for(int i=steps-1;i>=1;i--){Enemy e=getEnemyAt(player.getX()+sx*i,player.getY()+sy*i,null);if(e!=null&&!e.isFalling()){pushEnemy(e,sx,sy);any=true;}} if(any)message("Dash shove!",1.5f); }

    private Enemy findFirstEnemyInLine(int tx,int ty){ int dx=Integer.signum(tx-player.getX()),dy=Integer.signum(ty-player.getY()); int x=player.getX()+dx,y=player.getY()+dy; while(x>=0&&x<Player.BOARD_SIZE&&y>=0&&y<Player.BOARD_SIZE){Enemy e=getEnemyAt(x,y,null);if(e!=null)return e;if(x==tx&&y==ty)break;x+=dx;y+=dy;}return null; }
    private float tileCenterX(int x){return Player.getBoardX()+x*Player.TILE_SIZE+Player.TILE_SIZE/2f;}
    private float tileCenterY(int y){return Player.getBoardY()+y*Player.TILE_SIZE+Player.TILE_SIZE/2f;}

    private void updateArrowAnimation(float delta){if(!arrowInFlight)return;arrowProgress+=delta/ARROW_FLIGHT_DURATION;if(arrowProgress>=1f){arrowProgress=1f;resolveArrowHit();}}
    private void resolveArrowHit(){if(pendingHitEnemy!=null&&pendingHitEnemy.isAlive())damageEnemy(pendingHitEnemy,999);pendingHitEnemy=null;arrowInFlight=false;if(allEnemiesDefeated())winRoom();else moveEnemies();}

    private void message(String s,float t){eventMessage=s;messageTimer=t;}

    // Blessings apply whether or not the relic is cursed; curses apply only on cursed relics
    private boolean hasRelic(Relic.Blessing b){for(int i=0;i<ownedRelics.size;i++)if(ownedRelics.get(i).getBlessing()==b)return true;return false;}
    private boolean hasCurse(Relic.Curse c){for(int i=0;i<ownedRelics.size;i++){Relic r=ownedRelics.get(i);if(r.isCursed()&&r.getCurse()==c)return true;}return false;}

    // ---------- Touch UI buttons ----------
    private void layoutUiButtons(){
        float h=Gdx.graphics.getHeight();
        float x=20f, y=h-130f-UI_BTN_H;
        for(int i=0;i<uiButtons.length;i++){
            uiButtons[i].set(x,y,UI_BTN_W,UI_BTN_H);
            y-=UI_BTN_H+UI_BTN_GAP;
        }
    }

    private boolean handleUiTap(float x,float y){
        layoutUiButtons();
        for(int i=0;i<uiButtons.length;i++){
            if(!uiButtons[i].contains(x,y))continue;
            clickSound.play();
            if(i==0)toggleArrowMode();
            else if(i>=1&&i<=3)selectSpell(i-1);
            else cancelAction();
            return true;
        }
        return false;
    }

    private void toggleArrowMode(){
        if(player.hasArrows()){
            shootMode=!shootMode;
            if(activeCard!=null)activeCard.setSelected(false);
            activeCard=null;
            selectedSpell=null;
            player.setSelected(shootMode);
            message(shootMode?"Arrow mode: choose a target":"Arrow mode cancelled",1.5f);
        } else message("No arrows left!",1.5f);
    }

    private void cancelAction(){
        selectedSpell=null;
        if(shootMode){shootMode=false;player.setSelected(false);}
        message("Cancelled",1f);
    }

    private void renderUiButtons(){
        if(gameOverScreen.isVisible()||shopScreen.isVisible()||rewardScreen.isVisible())return;
        layoutUiButtons();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for(int i=0;i<uiButtons.length;i++){
            boolean active=(i==0&&shootMode)||(i>=1&&i<=3&&selectedSpell!=null&&i-1<ownedSpells.size&&ownedSpells.get(i-1)==selectedSpell);
            if(active)shapeRenderer.setColor(.3f,.8f,1f,.9f);else shapeRenderer.setColor(.15f,.17f,.25f,.85f);
            Rectangle r=uiButtons[i];
            shapeRenderer.rect(r.x,r.y,r.width,r.height);
        }
        shapeRenderer.end();
        batch.begin();
        font.getData().setScale(.5f);
        for(int i=0;i<uiButtons.length;i++){
            String label;
            if(i==0)label="ARROW ("+player.getArrowsRemaining()+")";
            else if(i<=3)label=(i-1<ownedSpells.size)?ownedSpells.get(i-1).getLabel()+" ("+manaCost(ownedSpells.get(i-1))+")":"-";
            else label="CANCEL";
            Rectangle r=uiButtons[i];
            font.setColor(Color.WHITE);
            font.draw(batch,label,r.x+8f,r.y+r.height/2f+font.getCapHeight()/2f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
    // --------------------------------------


    // ---------- Main Menu ----------
    // ---------- Autosave ----------
    private boolean hasAutosave() {
        return savePrefs != null && savePrefs.getBoolean("hasSave", false);
    }

    private void autosave() {
        if (savePrefs == null || menuVisible || gameOverScreen.isVisible()) return;

        savePrefs.putBoolean("hasSave", true);
        savePrefs.putInteger("difficulty", difficulty);
        savePrefs.putInteger("playerHp", playerHp);
        savePrefs.putInteger("playerMaxHp", playerMaxHp);
        savePrefs.putInteger("gold", gold);
        savePrefs.putInteger("mana", mana);

        savePrefs.putInteger("handSize", hand.size);
        for (int i = 0; i < hand.size; i++) {
            Card c = hand.get(i);
            savePrefs.putString("cardType" + i, c.getMovementType().name());
            savePrefs.putInteger("cardUses" + i, c.getUsesRemaining());
            CardModifier modifier = cardModifiers.get(c);
            savePrefs.putString("cardModifier" + i, modifier == null ? "" : modifier.name());
            savePrefs.putBoolean("cardRebound" + i, c.hasRebound());
            savePrefs.putBoolean("cardPierce" + i, c.hasPierce());
            savePrefs.putBoolean("cardDoubleMove" + i, c.hasDoubleMove());
            savePrefs.putBoolean("cardTeleport" + i, c.hasTeleportationInfusion());
            savePrefs.putBoolean("cardBurn" + i, c.hasBurn());
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

    private void clearAutosave() {
        if (savePrefs == null) return;
        savePrefs.clear();
        savePrefs.flush();
    }

    private void continueGameFromMenu() {
        if (!hasAutosave()) {
            startGameFromMenu();
            return;
        }

        menuVisible = false;
        bestiaryVisible = false;
        difficulty = Math.max(1, savePrefs.getInteger("difficulty", 1));
        playerHp = Math.max(1, savePrefs.getInteger("playerHp", 3));
        playerMaxHp = Math.max(playerHp, savePrefs.getInteger("playerMaxHp", 3));
        gold = Math.max(0, savePrefs.getInteger("gold", 0));
        mana = Math.max(1, savePrefs.getInteger("mana", BASE_MAX_MANA));

        if (player != null) player.dispose();
        player = new Player(2, 2);

        hand.clear();
        cardModifiers.clear();
        int savedHandSize = Math.min(MAX_HAND_SIZE, Math.max(0, savePrefs.getInteger("handSize", 0)));
        for (int i = 0; i < savedHandSize; i++) {
            try {
                Card.MovementType type = Card.MovementType.valueOf(savePrefs.getString("cardType" + i, "KNIGHT"));
                int uses = Math.max(0, savePrefs.getInteger("cardUses" + i, 2));
                Card c = new Card(cardName(type), type, uses);
                hand.add(c);
                String modifierName = savePrefs.getString("cardModifier" + i, "");
                if (!modifierName.isEmpty()) {
                    try { cardModifiers.put(c, CardModifier.valueOf(modifierName)); } catch (Exception ignored) {}
                }
                restoreCardAugments(c, i);
            } catch (Exception ignored) {}
        }

        ownedSpells.clear();
        int spellCount = Math.max(0, savePrefs.getInteger("spellCount", 0));
        for (int i = 0; i < spellCount; i++) {
            try { ownedSpells.add(Spell.valueOf(savePrefs.getString("spell" + i, "LIGHTNING"))); }
            catch (Exception ignored) {}
        }

        ownedRelics.clear();
        int relicCount = Math.max(0, savePrefs.getInteger("relicCount", 0));
        for (int i = 0; i < relicCount; i++) {
            Relic relic = findRelicByName(savePrefs.getString("relic" + i, ""));
            if (relic != null) ownedRelics.add(relic);
        }

        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        doubleMoveActive = false;
        rewardScreen.hide();
        gameOverScreen.hide();
        shopScreen.hide();

        generateEnemiesForCurrentRoom();
        positionCards();
        if (backgroundMusic != null && !backgroundMusic.isPlaying()) backgroundMusic.play();
        message("Autosave loaded - Room " + difficulty, 2f);
    }

    private void restoreCardAugments(Card card, int index) {
        try {
            java.lang.reflect.Field f = Card.class.getDeclaredField("hasRebound");
            f.setAccessible(true); f.setBoolean(card, savePrefs.getBoolean("cardRebound" + index, false));
            f = Card.class.getDeclaredField("hasPierce");
            f.setAccessible(true); f.setBoolean(card, savePrefs.getBoolean("cardPierce" + index, false));
            f = Card.class.getDeclaredField("hasDoubleMove");
            f.setAccessible(true); f.setBoolean(card, savePrefs.getBoolean("cardDoubleMove" + index, false));
            f = Card.class.getDeclaredField("hasTeleportationInfusion");
            f.setAccessible(true); f.setBoolean(card, savePrefs.getBoolean("cardTeleport" + index, false));
            f = Card.class.getDeclaredField("hasBurn");
            f.setAccessible(true); f.setBoolean(card, savePrefs.getBoolean("cardBurn" + index, false));
        } catch (Exception ignored) {}
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
        } catch (Exception ignored) {}
        return null;
    }

    private String cardName(Card.MovementType type) {
        switch (type) {
            case KNIGHT: return "Knight";
            case BISHOP: return "Bishop";
            case ROOK: return "Rook";
            case QUEEN: return "Queen";
            case PAWN: return "Pawn";
            case DASH: return "Dash";
            default: return "Knight";
        }
    }

    private void returnToMainMenu() {
        autosave();
        if (backgroundMusic != null && backgroundMusic.isPlaying()) backgroundMusic.stop();
        rewardScreen.hide();
        shopScreen.hide();
        gameOverScreen.hide();
        selectedSpell = null;
        shootMode = false;
        arrowInFlight = false;
        pendingHitEnemy = null;
        activeCard = null;
        if (player != null) player.setSelected(false);
        menuVisible = true;
        bestiaryVisible = false;
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

        if (hasAutosave()) {
            continueButton.set(x, startY - (buttonH + gap), buttonW, buttonH);
            bestiaryButton.set(x, startY - 2f * (buttonH + gap), buttonW, buttonH);
            blitzButton.set(x, startY - 3f * (buttonH + gap), buttonW, buttonH);
        } else {
            bestiaryButton.set(x, startY - (buttonH + gap), buttonW, buttonH);
            blitzButton.set(x, startY - 2f * (buttonH + gap), buttonW, buttonH);
        }

        backButton.set(25f, 25f, 150f, 55f);
        returnMenuButton.set(w - 210f, 25f, 185f, 58f);
    }

    private void startGameFromMenu() {
        clearAutosave();
        menuVisible = false;
        bestiaryVisible = false;

        difficulty = 1;
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

        if (!Gdx.input.justTouched()) return;

        // Get the actual touch position.
        float touchX = Gdx.input.getX();
        float touchY = Gdx.graphics.getHeight() - Gdx.input.getY();

        // Small touch padding makes buttons easier to press on phones.
        float padding = 20f;

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
            message("Blitz is coming in a future version!", 2f);
        }
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



    private void renderMenu() {
        layoutMenuButtons();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (bestiaryVisible) {
            shapeRenderer.setColor(.10f, .11f, .16f, 1f);
            shapeRenderer.rect(0, 0, w, h);

            shapeRenderer.setColor(.15f, .17f, .25f, 1f);
            shapeRenderer.rect(backButton.x, backButton.y, backButton.width, backButton.height);

            shapeRenderer.end();

            batch.begin();

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

        shapeRenderer.setColor(.08f, .09f, .12f, 1f);
        shapeRenderer.rect(0, 0, w, h);

        drawMenuButton(startButton, false);
        if (hasAutosave()) drawMenuButton(continueButton, false);
        drawMenuButton(bestiaryButton, false);
        drawMenuButton(blitzButton, true);

        shapeRenderer.end();

        batch.begin();

        font.setColor(new Color(.3f, .8f, 1f, 1f));
        font.getData().setScale(2.6f);
        String title = "CLOAKCHESS";
        float titleWidth = font.getSpaceXadvance() * title.length();
        font.draw(batch, title, (w - titleWidth) / 2f, h * .78f);

        font.getData().setScale(.8f);
        font.setColor(new Color(.65f, .68f, .75f, 1f));
        String subtitle = "Chess. Cards. Roguelike.";
        float subtitleWidth = font.getSpaceXadvance() * subtitle.length();
        font.draw(batch, subtitle, (w - subtitleWidth) / 2f, h * .70f);

        drawMenuLabel(batch, "START", startButton, Color.WHITE);
        if (hasAutosave()) drawMenuLabel(batch, "CONTINUE", continueButton, Color.WHITE);
        drawMenuLabel(batch, "BESTIARY", bestiaryButton, Color.WHITE);

        font.setColor(new Color(.45f, .47f, .52f, 1f));
        font.getData().setScale(.8f);
        String locked = "BLITZ  [LOCKED]";
        font.draw(batch, locked, blitzButton.x + blitzButton.width / 2f - 65f,
            blitzButton.y + blitzButton.height / 2f + 10f);

        font.getData().setScale(.55f);
        font.setColor(new Color(.45f, .47f, .52f, 1f));
        font.draw(batch, "COMING IN A FUTURE VERSION",
            blitzButton.x + blitzButton.width / 2f - 92f,
            blitzButton.y + 23f);

        font.getData().setScale(.55f);
        font.setColor(new Color(.38f, .40f, .46f, 1f));
        String version = "CloakChess";
        font.draw(batch, version, 20f, 25f);

        font.getData().setScale(1f);
        batch.end();
    }

    private void drawMenuButton(Rectangle r, boolean locked) {
        if (locked) {
            shapeRenderer.setColor(.12f, .13f, .18f, .95f);
        } else {
            shapeRenderer.setColor(.15f, .17f, .25f, .95f);
        }
        shapeRenderer.rect(r.x, r.y, r.width, r.height);
    }

    private void drawMenuLabel(SpriteBatch targetBatch, String text, Rectangle r, Color color) {
        font.setColor(color);
        font.getData().setScale(1.1f);
        float approximateWidth = font.getSpaceXadvance() * text.length();
        font.draw(targetBatch, text,
            r.x + r.width / 2f - approximateWidth / 2f,
            r.y + r.height / 2f + 13f);
    }

    private void handleInput(){
        if (menuVisible) {
            handleMenuInput();
            renderMenu();
            return;
        }

        if(gameOverScreen.isVisible()){
            if(!Gdx.input.justTouched())return;
            float x=Gdx.input.getX(),y=Gdx.graphics.getHeight()-Gdx.input.getY();
            layoutMenuButtons();
            if(returnMenuButton.contains(x,y)){clickSound.play();returnToMainMenu();return;}
            if(gameOverScreen.handleClick(x,y)){clickSound.play();restartGame();}
            return;
        }
        if(shopScreen.isVisible()){handleShopInput();return;}
        if(rewardScreen.isVisible()){if(!Gdx.input.justTouched())return;float x=Gdx.input.getX(),y=Gdx.graphics.getHeight()-Gdx.input.getY();clickSound.play();rewardScreen.handleClick(x,y,hand);return;}
        if(arrowInFlight||anyEnemyFalling())return;

        if(rewardScreen.hasGrantedNewCard()){Card.MovementType type=rewardScreen.getGrantedCardType();if(hand.size<MAX_HAND_SIZE){Card c=createCard(type);hand.add(c);positionCards();message("You received a "+c.getName()+"!",2f);}else message("Hand full!",2f);rewardScreen.hide();generateNextRoom();return;}
        if(rewardScreen.hasSelectedAugment()){augmentSound.play();Card c=rewardScreen.getSelectedCard();Augment a=rewardScreen.getSelectedAugment();c.applyAugment(a);message(c.getName()+" gained "+a.getName()+"!",2f);rewardScreen.hide();generateNextRoom();return;}

        if(Gdx.input.justTouched()){float ux=Gdx.input.getX(),uy=Gdx.graphics.getHeight()-Gdx.input.getY();if(handleUiTap(ux,uy))return;}

        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) selectSpell(0);
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) selectSpell(1);
        if(Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) selectSpell(2);
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)){cancelAction();return;}

        if(Gdx.input.isKeyJustPressed(Input.Keys.A)){toggleArrowMode();return;}
        if(shootMode){handleArrowInput();return;}
        if(selectedSpell!=null){handleSpellInput();return;}
        if(!Gdx.input.justTouched())return;

        float touchX=Gdx.input.getX(),touchY=Gdx.graphics.getHeight()-Gdx.input.getY();
        if(activeCard==null){for(int i=hand.size-1;i>=0;i--){Card c=hand.get(i);if(c.isClicked(touchX,touchY)){clickSound.play();activeCard=c;c.setSelected(true);message(c.getName()+" selected",1.5f);break;}}return;}
        if(activeCard.isClicked(touchX,touchY)){clickSound.play();activeCard.setSelected(false);activeCard=null;player.setSelected(false);return;}
        if(!player.isSelected()){if(player.isClicked(touchX,touchY)){clickSound.play();player.setSelected(true);message("Choose a destination",1.5f);}return;}

        int tx=(int)((touchX-Player.getBoardX())/Player.TILE_SIZE),ty=(int)((touchY-Player.getBoardY())/Player.TILE_SIZE);
        boolean valid=activeCard.hasTeleportationInfusion()?tx>=0&&tx<Player.BOARD_SIZE&&ty>=0&&ty<Player.BOARD_SIZE:player.isValidMove(tx,ty,activeCard.getMovementRules(),board);
        if(!valid){message("Invalid move!",1.5f);return;}

        if(isDashCard(activeCard))pushEnemiesAlongDash(tx,ty);
        if(activeCard.hasPierce()){Enemy e=findFirstEnemyInLine(tx,ty);if(e!=null)damageEnemy(e,1);}
        player.setPosition(tx,ty);moveSound.play();
        boolean captured=checkPlayerCapture();
        Card played=activeCard; player.setSelected(false); activeCard=null;
        playCardWithModifier(played,captured);
    }

    private void playCardWithModifier(Card card, boolean captured){
        CardModifier modifier=cardModifiers.get(card);
        boolean free=modifier==CardModifier.FRUGAL&&random.nextBoolean();
        if(modifier==CardModifier.BLOODPRICE){if(!damagePlayer(1)){message("Bloodprice took 1 HP",1.5f);}}
        if(!free){card.use();if(modifier==CardModifier.BRITTLE&&!card.isUsed())card.use();}
        if(card.hasBurn()){triggerBurnAugment();card.consumeBurn();}
        if(modifier==CardModifier.LEECH&&captured)healPlayer(1);
        if(card.isUsed())hand.removeValue(card,true);
        mana=Math.min(getMaxMana(),mana+MANA_REGEN_PER_CARD+(hasRelic(Relic.Blessing.MANA_SPRING)?1:0));
        positionCards();giveRandomCard();
        if(captured&&card.hasRebound()){message("Rebound! Extra action!",1.5f);if(allEnemiesDefeated())winRoom();return;}
        if(card.hasDoubleMove()&&!doubleMoveActive){doubleMoveActive=true;message("Double Move! Act again.",1.5f);return;}
        doubleMoveActive=false;
        moveEnemies();
        if(allEnemiesDefeated())winRoom();
    }

    private void triggerBurnAugment(){Array<Enemy> alive=new Array<>();for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(e.isAlive())alive.add(e);}if(alive.size>0){Enemy target=alive.get(random.nextInt(alive.size));damageEnemy(target,1);message("Burn hit an enemy!",1.5f);if(augmentSound!=null)augmentSound.play();}}

    private void selectSpell(int index){
        if(index<0||index>=ownedSpells.size){message("No spell in that slot",1.2f);return;}
        Spell spell=ownedSpells.get(index);
        if(mana<manaCost(spell)){message("Not enough mana! ("+mana+"/"+manaCost(spell)+")",1.5f);return;}
        selectedSpell=spell;shootMode=false;
        if(activeCard!=null){activeCard.setSelected(false);activeCard=null;}
        player.setSelected(false);
        message(selectedSpell.getLabel()+": tap an enemy",1.5f);
    }
    private void handleSpellInput(){if(!Gdx.input.justTouched())return;float x=Gdx.input.getX(),y=Gdx.graphics.getHeight()-Gdx.input.getY();Enemy target=null;for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(e.isAlive()&&e.isClicked(x,y)){target=e;break;}}if(target==null){message("Choose an enemy",1.2f);return;}castSpell(selectedSpell,target);selectedSpell=null;}
    private void castSpell(Spell spell,Enemy target){
        if(spell==null)return;
        int cost=manaCost(spell);
        if(mana<cost){message("Not enough mana!",1.5f);return;}
        mana-=cost;
        if(spell==Spell.LIGHTNING){damageEnemy(target,2);for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(e!=target&&e.isAlive()&&Math.abs(e.getX()-target.getX())<=1&&Math.abs(e.getY()-target.getY())<=1)damageEnemy(e,1);}}
        else if(spell==Spell.POISON)poisonTurns.put(target,4);
        else if(spell==Spell.BURN){burnTurns.put(target,2);}
        if(augmentSound!=null)augmentSound.play();message(spell.getLabel()+" cast! (-"+cost+" mana)",1.5f);moveEnemies();if(allEnemiesDefeated())winRoom();}

    private boolean checkPlayerCapture() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.isAlive() && e.getX() == player.getX() && e.getY() == player.getY()) {
                if (enemyCurses.get(e) == EnemyCurse.VENGEFUL) {
                    damagePlayer(1);
                    message("Vengeful! -1 HP", 1.5f);
                }
                damageEnemy(e, 999);
                groundPound(player.getX(), player.getY());
                return true;
            }
        }
        return false;
    }

    private void moveEnemies() {
        applyEnemyStatuses();
        if (allEnemiesDefeated()) return;

        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.isAlive()) continue;

            e.moveTowards(player.getX(), player.getY());

            // Check if enemy moved onto player's tile
            if (enemyCapturedPlayer(e)) {
                // Apply bonus damage for Vengeful curse if present
                if (enemyCurses.get(e) == EnemyCurse.VENGEFUL) {
                    damagePlayer(1);
                    message("Vengeful! Extra damage!", 1.5f);
                }

                // Normal capture damage (-1 HP)
                damagePlayer(1);
                message("Ouch! Enemy hit you! (-1 HP)", 1.5f);

                // Destroy the enemy that hit you so they don't sit on your tile
                damageEnemy(e, 999);

                // Only trigger game over if HP reaches 0
                if (playerHp <= 0) {
                    gameOver();
                    return;
                }
            }
        }
    }

    private void applyEnemyStatuses(){
        for(int i=0;i<enemies.size;i++){Enemy e=enemies.get(i);if(!e.isAlive())continue;Integer p=poisonTurns.get(e);if(p!=null&&p>0){damageEnemy(e,1);if(p-1<=0)poisonTurns.remove(e);else poisonTurns.put(e,p-1);}Integer b=burnTurns.get(e);if(b!=null&&b>0){damageEnemy(e,1);if(b-1<=0)burnTurns.remove(e);else burnTurns.put(e,b-1);}}
    }

    private boolean enemyCapturedPlayer(Enemy e){return e.getX()==player.getX()&&e.getY()==player.getY();}
    private boolean damagePlayer(int amount){playerHp=Math.max(0,playerHp-amount);if(playerHp<=0){gameOver();return false;}return true;}
    private void healPlayer(int amount){playerHp=Math.min(playerMaxHp,playerHp+amount);}

    private boolean allEnemiesDefeated(){for(int i=0;i<enemies.size;i++)if(enemies.get(i).isAlive())return false;return true;}

    private void winRoom(){
        if(backgroundMusic!=null&&backgroundMusic.isPlaying())backgroundMusic.stop();
        victorySound.play();
        int reward=25+(hasRelic(Relic.Blessing.GOLD_MAGNET)?10:0)-(hasCurse(Relic.Curse.TOLL)?10:0);
        reward=Math.max(0,reward);
        gold+=reward;
        healPlayer(hasRelic(Relic.Blessing.SECOND_WIND)?1:0);
        rewardScreen.show(reward);
    }

    private void generateNextRoom(){
        difficulty++;
        for(int i=0;i<enemies.size;i++)enemies.get(i).dispose();
        shockwaves.clear();
        generateEnemiesForCurrentRoom();
        player.resetArrows();shootMode=false;arrowInFlight=false;pendingHitEnemy=null;selectedSpell=null;doubleMoveActive=false;
        mana=hasCurse(Relic.Curse.MANA_LEAK)?Math.max(1,getMaxMana()/2):getMaxMana();
        if(backgroundMusic!=null&&!backgroundMusic.isPlaying())backgroundMusic.play();
        eventMessage="ROOM "+difficulty;messageTimer=2f;
        if(difficulty%3==0)openShop();
        autosave();
    }

    // ---------- Shop (direct access, no reflection) ----------
    private void openShop(){
        Array<ShopScreen.Item> items=new Array<>();
        items.add(new ShopScreen.Item(ShopScreen.Kind.POTION,"Healing Potion","Heal 2 HP",25,null,null));
        Spell s=Spell.values()[random.nextInt(Spell.values().length)];
        items.add(new ShopScreen.Item(ShopScreen.Kind.SPELL,s.getLabel(),s.getDescription(),s.getCost(),s,null));
        Relic r=Relic.random(random);
        items.add(new ShopScreen.Item(ShopScreen.Kind.RELIC,r.getName(),r.getDescription(),r.getPrice(),null,r));
        Spell s2=Spell.values()[random.nextInt(Spell.values().length)];
        items.add(new ShopScreen.Item(ShopScreen.Kind.SPELL,s2.getLabel(),s2.getDescription(),s2.getCost(),s2,null));
        shopScreen.show(items);
        shopWasOpenedThisRoom=true;
        message("SHOP",2f);
    }

    private void handleShopInput(){
        if(!Gdx.input.justTouched())return;
        float x=Gdx.input.getX(),y=Gdx.graphics.getHeight()-Gdx.input.getY();
        int result=shopScreen.handleClick(x,y);
        if(result==ShopScreen.LEAVE){clickSound.play();shopScreen.hide();message("Shop closed",1.2f);return;}
        if(result==ShopScreen.NONE)return;
        ShopScreen.Item item=shopScreen.getItem(result);
        if(item==null)return;
        if(item.sold){shopScreen.setMessage("Already sold");return;}
        if(gold<item.cost){shopScreen.setMessage("Not enough gold");return;}
        gold-=item.cost;
        clickSound.play();
        if(item.kind==ShopScreen.Kind.POTION){
            healPlayer(2+(hasRelic(Relic.Blessing.HEARTY_BREW)?1:0));
            message("Potion bought!",1.5f);
        }else if(item.kind==ShopScreen.Kind.SPELL){
            ownedSpells.add(item.spell);
            message(item.spell.getLabel()+" learned!",1.5f);
        }else if(item.kind==ShopScreen.Kind.RELIC){
            ownedRelics.add(item.relic);
            applyRelic(item.relic);
            message(item.relic.getName()+" acquired!",1.5f);
        }
        shopScreen.markSold(result);
        autosave();
    }
    // ---------------------------------------------------------

    private void applyRelic(Relic relic){
        if(relic==null)return;
        if(relic.getBlessing()==Relic.Blessing.IRON_HEART)playerMaxHp++;
        if(relic.isCursed()&&relic.getCurse()==Relic.Curse.FRAGILE){playerMaxHp=Math.max(1,playerMaxHp-1);playerHp=Math.min(playerHp,playerMaxHp);}
        if(relic.getBlessing()==Relic.Blessing.ARCANE_WELL)mana=Math.min(getMaxMana(),mana+2);
        mana=Math.min(mana,getMaxMana());
    }

    private void handleArrowInput(){if(!Gdx.input.justTouched())return;float x=Gdx.input.getX(),y=Gdx.graphics.getHeight()-Gdx.input.getY();int tx=(int)((x-Player.getBoardX())/Player.TILE_SIZE),ty=(int)((y-Player.getBoardY())/Player.TILE_SIZE);if(!player.isValidArrowTarget(tx,ty)){message("Invalid arrow direction!",1.5f);return;}Enemy hit=findFirstEnemyInLine(tx,ty);if(hit==null){message("No target in line!",1.5f);return;}pendingHitEnemy=hit;arrowStartX=tileCenterX(player.getX());arrowStartY=tileCenterY(player.getY());arrowTargetX=tileCenterX(hit.getX());arrowTargetY=tileCenterY(hit.getY());arrowProgress=0;arrowInFlight=true;player.useArrow();arrowSound.play();shootMode=false;player.setSelected(false);}

    private void gameOver(){if(gameOverScreen.isVisible())return;clearAutosave();if(backgroundMusic!=null&&backgroundMusic.isPlaying())backgroundMusic.stop();gameOverSound.play();message("You were captured!",3f);gameOverScreen.show();}

    private void restartGame(){difficulty=1;cardModifiers.clear();enemyCurses.clear();poisonTurns.clear();burnTurns.clear();armorShields.clear();doubleMoveActive=false;selectedSpell=null;shootMode=false;arrowInFlight=false;pendingHitEnemy=null;if(player!=null)player.dispose();player=new Player(2,2);resetPlayerStats();buildStartingHand();if(enemies!=null)for(int i=0;i<enemies.size;i++)enemies.get(i).dispose();generateEnemiesForCurrentRoom();rewardScreen.hide();gameOverScreen.hide();shopScreen.hide();positionCards();if(backgroundMusic!=null){backgroundMusic.stop();backgroundMusic.play();}message("Room 1",2f);}

    private void renderReturnMenuButton() {
        layoutMenuButtons();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(.15f, .17f, .25f, .96f);
        shapeRenderer.rect(returnMenuButton.x, returnMenuButton.y, returnMenuButton.width, returnMenuButton.height);
        shapeRenderer.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.getData().setScale(.62f);
        font.draw(batch, "MAIN MENU", returnMenuButton.x + 34f, returnMenuButton.y + 36f);
        font.getData().setScale(1f);
        batch.end();
    }

    private void updateMessage(float delta){if(messageTimer<=0)return;messageTimer-=delta;if(messageTimer<=0){messageTimer=0;eventMessage="";}}

    @Override public void resize(int width,int height){super.resize(width,height);if(hand!=null)positionCards();}
    @Override public void pause(){if(backgroundMusic!=null&&backgroundMusic.isPlaying())backgroundMusic.pause();}
    @Override public void resume(){if(backgroundMusic!=null&&!backgroundMusic.isPlaying())backgroundMusic.play();}

    @Override public void render(){
        float delta=Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(.08f,.09f,.12f,1f);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA,GL20.GL_ONE_MINUS_SRC_ALPHA);

        if (menuVisible) {
            handleMenuInput();
            renderMenu();
            return;
        }

        for(Card c:hand)c.update(delta);if(!gameOverScreen.isVisible()&&!shopScreen.isVisible())updateCardHover();checkEnemyLandings();handleInput();updateMessage(delta);updateArrowAnimation(delta);updateShockwaves(delta);
        board.render();
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);for(int i=0;i<enemies.size;i++)enemies.get(i).renderShadow(shapeRenderer);if(activeCard!=null&&player.isSelected())player.renderLegalMoves(shapeRenderer,activeCard,board);if(shootMode)player.renderArrowRange(shapeRenderer);shapeRenderer.end();
        batch.begin();for(Card c:hand)c.drawShape(batch);for(int i=0;i<enemies.size;i++)enemies.get(i).render(batch);player.render(batch);
        if(arrowInFlight){float x=MathUtils.lerp(arrowStartX,arrowTargetX,arrowProgress),y=MathUtils.lerp(arrowStartY,arrowTargetY,arrowProgress);float angle=MathUtils.radiansToDegrees*MathUtils.atan2(arrowTargetY-arrowStartY,arrowTargetX-arrowStartX);batch.draw(arrowTexture,x-ARROW_DRAW_WIDTH,y-ARROW_DRAW_HEIGHT,ARROW_DRAW_WIDTH,ARROW_DRAW_HEIGHT,ARROW_DRAW_WIDTH,ARROW_DRAW_HEIGHT,2f,2f,angle,0,0,arrowTexture.getWidth(),arrowTexture.getHeight(),false,false);}
        for(Card c:hand)c.drawText(batch,font);
        font.getData().setScale(1f);font.setColor(new Color(.3f,.8f,1f,1f));font.draw(batch,eventMessage,20,Gdx.graphics.getHeight()-20);
        font.setColor(Color.WHITE);font.getData().setScale(.7f);font.draw(batch,"HP: "+playerHp+"/"+playerMaxHp+"   Mana: "+mana+"/"+getMaxMana()+"   Gold: "+gold+"   Room: "+difficulty,20,Gdx.graphics.getHeight()-50);font.draw(batch,"Arrows: "+player.getArrowsRemaining()+"   Spells: "+ownedSpells.size,20,Gdx.graphics.getHeight()-75);if(selectedSpell!=null)font.draw(batch,"Selected: "+selectedSpell.getLabel()+" - tap target",20,Gdx.graphics.getHeight()-100);font.getData().setScale(1f);font.setColor(Color.WHITE);batch.end();
        renderShockwaves();renderUiButtons();rewardScreen.render(hand);if(shopScreen.isVisible())shopScreen.render(gold);gameOverScreen.render();
        if (gameOverScreen.isVisible()) renderReturnMenuButton();
    }

    @Override public void dispose(){if(batch!=null)batch.dispose();if(shapeRenderer!=null)shapeRenderer.dispose();if(font!=null)font.dispose();if(board!=null)board.dispose();if(player!=null)player.dispose();if(enemies!=null)for(int i=0;i<enemies.size;i++)enemies.get(i).dispose();if(backgroundMusic!=null){backgroundMusic.stop();backgroundMusic.dispose();}if(moveSound!=null)moveSound.dispose();if(victorySound!=null)victorySound.dispose();if(gameOverSound!=null)gameOverSound.dispose();if(clickSound!=null)clickSound.dispose();if(augmentSound!=null)augmentSound.dispose();if(arrowSound!=null)arrowSound.dispose();if(arrowTexture!=null)arrowTexture.dispose();Card.disposeTextures();}
}
