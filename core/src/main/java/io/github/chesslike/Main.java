package io.github.chesslike;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

import io.github.chesslike.game.Board;
import io.github.chesslike.game.Card;
import io.github.chesslike.game.Enemy;
import io.github.chesslike.game.EnemyGenerator;
import io.github.chesslike.game.Player;

import java.util.Random;

public class Main extends Game {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Board board;
    private Player player;

    // =========================================
    // CARDS
    // =========================================

    private Array<Card> hand;
    private Card activeCard;

    private static final int MAX_HAND_SIZE = 5;

    private final Random random = new Random();

    private static final float CARD_REWARD_CHANCE = 0.50f;

    // =========================================
    // ENEMIES
    // =========================================

    private Array<Enemy> enemies;
    private EnemyGenerator enemyGenerator;

    private int difficulty = 1;

    // =========================================
    // EVENT MESSAGE
    // =========================================

    private String eventMessage = "";
    private float messageTimer = 0f;

    // =========================================
    // CREATE
    // =========================================

    @Override
    public void create() {

        batch = new SpriteBatch();

        shapeRenderer =
            new ShapeRenderer();

        // -----------------------------------------
        // FONT
        // -----------------------------------------

        FreeTypeFontGenerator generator =
            new FreeTypeFontGenerator(
                Gdx.files.internal("Font.ttf")
            );

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        parameter.size = 22;

        font =
            generator.generateFont(parameter);

        generator.dispose();

        // -----------------------------------------
        // BOARD
        // -----------------------------------------

        board =
            new Board(batch);

        // -----------------------------------------
        // PLAYER
        // -----------------------------------------

        player =
            new Player(2, 2);

        // -----------------------------------------
        // ENEMIES
        // -----------------------------------------

        enemyGenerator =
            new EnemyGenerator();

        enemies =
            enemyGenerator.generateEnemies(
                difficulty
            );

        // -----------------------------------------
        // STARTING HAND
        // -----------------------------------------

        hand =
            new Array<>();

        hand.add(
            createCard(
                Card.MovementType.KNIGHT
            )
        );

        hand.add(
            createCard(
                Card.MovementType.BISHOP
            )
        );

        hand.add(
            createCard(
                Card.MovementType.ROOK
            )
        );

        hand.add(
            createCard(
                Card.MovementType.QUEEN
            )
        );

        hand.add(
            createCard(
                Card.MovementType.PAWN
            )
        );

        positionCards();

        eventMessage =
            "Room " + difficulty;

        messageTimer = 2f;
    }

    // =========================================
    // CARD POSITIONING
    // =========================================

    private void positionCards() {

        if (hand == null || hand.size == 0) {
            return;
        }

        float screenWidth =
            Gdx.graphics.getWidth();

        float screenHeight =
            Gdx.graphics.getHeight();

        // =========================================
        // LANDSCAPE
        // Cards on RIGHT
        // =========================================

        if (screenWidth >= screenHeight) {

            float availableHeight =
                screenHeight - 40f;

            float spacing =
                10f;

            float requiredHeight =
                hand.size * 155f
                    + (hand.size - 1)
                    * spacing;

            float scale = 1f;

            if (requiredHeight > availableHeight) {

                scale =
                    availableHeight
                        / requiredHeight;
            }

            scale =
                MathUtils.clamp(
                    scale,
                    0.50f,
                    0.82f
                );

            float cardWidth =
                115f * scale;

            float cardHeight =
                155f * scale;

            float actualSpacing =
                spacing * scale;

            float totalHeight =
                hand.size * cardHeight
                    + (hand.size - 1)
                    * actualSpacing;

            float startY =
                (screenHeight - totalHeight)
                    / 2f;

            float x =
                screenWidth
                    - cardWidth
                    - 20f;

            for (int i = 0; i < hand.size; i++) {

                Card card =
                    hand.get(i);

                card.setLayoutScale(scale);

                float y =
                    startY
                        + i
                        * (
                        cardHeight
                            + actualSpacing
                    );

                card.setPosition(
                    x,
                    y
                );
            }

        } else {

            // =========================================
            // PORTRAIT
            // Cards on BOTTOM
            // =========================================

            float availableWidth =
                screenWidth - 20f;

            float spacing =
                6f;

            float requiredWidth =
                hand.size * 115f
                    + (hand.size - 1)
                    * spacing;

            float scale = 1f;

            if (requiredWidth > availableWidth) {

                scale =
                    availableWidth
                        / requiredWidth;
            }

            scale =
                MathUtils.clamp(
                    scale,
                    0.42f,
                    0.75f
                );

            float cardWidth =
                115f * scale;

            float cardHeight =
                155f * scale;

            float actualSpacing =
                spacing * scale;

            float totalWidth =
                hand.size * cardWidth
                    + (hand.size - 1)
                    * actualSpacing;

            float startX =
                (screenWidth - totalWidth)
                    / 2f;

            float y =
                15f;

            for (int i = 0; i < hand.size; i++) {

                Card card =
                    hand.get(i);

                card.setLayoutScale(scale);

                float x =
                    startX
                        + i
                        * (
                        cardWidth
                            + actualSpacing
                    );

                card.setPosition(
                    x,
                    y
                );
            }
        }
    }

    // =========================================
    // CREATE CARD
    // =========================================

    private Card createCard(
        Card.MovementType type
    ) {

        switch (type) {

            case KNIGHT:
                return new Card(
                    "Knight",
                    Card.MovementType.KNIGHT,
                    2
                );

            case BISHOP:
                return new Card(
                    "Bishop",
                    Card.MovementType.BISHOP,
                    2
                );

            case ROOK:
                return new Card(
                    "Rook",
                    Card.MovementType.ROOK,
                    2
                );

            case QUEEN:
                return new Card(
                    "Queen",
                    Card.MovementType.QUEEN,
                    2
                );

            case PAWN:
                return new Card(
                    "Pawn",
                    Card.MovementType.PAWN,
                    2
                );

            default:
                throw new IllegalArgumentException(
                    "Unknown card type: " + type
                );
        }
    }

    // =========================================
    // RANDOM CARD REWARD
    // =========================================

    private void giveRandomCard() {

        if (hand.size >= MAX_HAND_SIZE) {

            eventMessage =
                "Hand full!";

            messageTimer = 2f;

            return;
        }

        if (
            random.nextFloat()
                > CARD_REWARD_CHANCE
        ) {

            eventMessage =
                "No card this turn...";

            messageTimer = 2f;

            return;
        }

        Card.MovementType[] types = {

            Card.MovementType.KNIGHT,
            Card.MovementType.BISHOP,
            Card.MovementType.ROOK,
            Card.MovementType.QUEEN,
            Card.MovementType.PAWN
        };

        Card.MovementType randomType =
            types[
                random.nextInt(
                    types.length
                )
                ];

        Card newCard =
            createCard(randomType);

        hand.add(newCard);

        positionCards();

        eventMessage =
            "You received a "
                + newCard.getName()
                + "!";

        messageTimer = 2f;
    }

    // =========================================
    // CARD HOVER
    // =========================================

    private void updateCardHover() {

        float mouseX =
            Gdx.input.getX();

        float mouseY =
            Gdx.graphics.getHeight()
                - Gdx.input.getY();

        for (Card card : hand) {

            card.setHovered(
                card.isClicked(
                    mouseX,
                    mouseY
                )
            );
        }
    }

    // =========================================
    // INPUT
    // =========================================

    private void handleInput() {

        if (!Gdx.input.justTouched()) {
            return;
        }

        float touchX =
            Gdx.input.getX();

        float touchY =
            Gdx.graphics.getHeight()
                - Gdx.input.getY();

        // =========================================
        // SELECT CARD
        // =========================================

        if (activeCard == null) {

            for (
                int i = hand.size - 1;
                i >= 0;
                i--
            ) {

                Card card =
                    hand.get(i);

                if (
                    card.isClicked(
                        touchX,
                        touchY
                    )
                ) {

                    // THIS was the bug before:
                    // activeCard was null here.

                    activeCard = card;

                    card.setSelected(true);

                    eventMessage =
                        card.getName()
                            + " selected";

                    messageTimer = 1.5f;

                    break;
                }
            }

            return;
        }

        // =========================================
        // CANCEL CARD
        // =========================================

        if (
            activeCard.isClicked(
                touchX,
                touchY
            )
        ) {

            activeCard.setSelected(false);

            activeCard = null;

            player.setSelected(false);

            eventMessage = "";
            messageTimer = 0f;

            return;
        }

        // =========================================
        // SELECT PLAYER
        // =========================================

        if (!player.isSelected()) {

            if (
                player.isClicked(
                    touchX,
                    touchY
                )
            ) {

                player.setSelected(true);

                eventMessage =
                    "Choose a destination";

                messageTimer = 1.5f;
            }

            return;
        }

        // =========================================
        // BOARD COORDINATES
        // =========================================

        final float TILE_SIZE =
            Player.TILE_SIZE;

        float boardX =
            Player.getBoardX();

        float boardY =
            Player.getBoardY();

        int targetX =
            (int)(
                (touchX - boardX)
                    / TILE_SIZE
            );

        int targetY =
            (int)(
                (touchY - boardY)
                    / TILE_SIZE
            );

        // =========================================
        // MOVE PLAYER
        // =========================================

        if (
            player.moveTo(
                targetX,
                targetY,
                activeCard.getMovementRules()
            )
        ) {

            // -----------------------------------------
            // CAPTURE
            // -----------------------------------------

            checkPlayerCapture();

            // -----------------------------------------
            // PLAYER DESELECT
            // -----------------------------------------

            player.setSelected(false);

            // -----------------------------------------
            // USE CARD
            // -----------------------------------------

            Card playedCard =
                activeCard;

            playedCard.use();

            playedCard.setSelected(false);

            activeCard = null;

            // -----------------------------------------
            // REMOVE IF USED UP
            // -----------------------------------------

            if (playedCard.isUsed()) {

                eventMessage =
                    playedCard.getName()
                        + " used up!";

                messageTimer = 1.5f;

                hand.removeValue(
                    playedCard,
                    true
                );
            }

            // -----------------------------------------
            // REPOSITION HAND
            // -----------------------------------------

            positionCards();

            // -----------------------------------------
            // CARD REWARD
            // -----------------------------------------

            giveRandomCard();

            // -----------------------------------------
            // ENEMY TURN
            // -----------------------------------------

            moveEnemies();

            // -----------------------------------------
            // NEXT ROOM
            // -----------------------------------------

            if (allEnemiesDefeated()) {

                generateNextRoom();
            }

        } else {

            eventMessage =
                "Invalid move!";

            messageTimer = 1.5f;
        }
    }

    // =========================================
    // ENEMY CAPTURE PLAYER
    // =========================================

    private boolean enemyCapturedPlayer(
        Enemy enemy
    ) {

        return enemy.getX()
            == player.getX()

            && enemy.getY()
            == player.getY();
    }

    // =========================================
    // ENEMY TURN
    // =========================================

    private void moveEnemies() {

        for (Enemy enemy : enemies) {

            if (!enemy.isAlive()) {
                continue;
            }

            enemy.moveTowards(
                player.getX(),
                player.getY()
            );

            if (
                enemyCapturedPlayer(
                    enemy
                )
            ) {

                eventMessage =
                    "You were captured!";

                messageTimer = 3f;

                // TODO:
                // Game over screen

                return;
            }
        }
    }

    // =========================================
    // CHECK ROOM
    // =========================================

    private boolean allEnemiesDefeated() {

        for (Enemy enemy : enemies) {

            if (enemy.isAlive()) {
                return false;
            }
        }

        return true;
    }

    // =========================================
    // NEXT ROOM
    // =========================================

    private void generateNextRoom() {

        difficulty++;

        // -----------------------------------------
        // DISPOSE OLD ENEMIES
        // -----------------------------------------

        for (Enemy enemy : enemies) {
            enemy.dispose();
        }

        enemies.clear();

        // -----------------------------------------
        // GENERATE NEW ENEMIES
        // -----------------------------------------

        enemies =
            enemyGenerator.generateEnemies(
                difficulty
            );

        eventMessage =
            "ROOM " + difficulty;

        messageTimer = 2f;
    }

    // =========================================
    // CHECK PLAYER CAPTURE
    // =========================================

    private void checkPlayerCapture() {

        for (Enemy enemy : enemies) {

            if (!enemy.isAlive()) {
                continue;
            }

            if (
                enemy.getX()
                    == player.getX()

                    && enemy.getY()
                    == player.getY()
            ) {

                enemy.kill();

                eventMessage =
                    "Enemy captured!";

                messageTimer = 1.5f;

                break;
            }
        }
    }

    // =========================================
    // UPDATE MESSAGE
    // =========================================

    private void updateMessage(
        float delta
    ) {

        if (messageTimer <= 0f) {
            return;
        }

        messageTimer -= delta;

        if (messageTimer <= 0f) {

            messageTimer = 0f;
            eventMessage = "";
        }
    }

    // =========================================
    // RESIZE
    // =========================================

    @Override
    public void resize(
        int width,
        int height
    ) {

        super.resize(width, height);

        // Recalculate card positions
        // when screen orientation/size changes.
        if (hand != null) {
            positionCards();
        }
    }

    // =========================================
    // RENDER
    // =========================================

    @Override
    public void render() {

        float delta =
            Gdx.graphics.getDeltaTime();

        // =========================================
        // BACKGROUND
        // =========================================

        ScreenUtils.clear(
            0.08f,
            0.09f,
            0.12f,
            1f
        );

        // =========================================
        // CARD ANIMATIONS
        // =========================================

        for (Card card : hand) {

            card.update(delta);
        }

        // =========================================
        // CARD HOVER
        // =========================================

        updateCardHover();

        // =========================================
        // INPUT
        // =========================================

        handleInput();

        // =========================================
        // MESSAGE
        // =========================================

        updateMessage(delta);

        // =========================================
        // BOARD
        // =========================================

        board.render();

        // =========================================
        // LEGAL MOVE HIGHLIGHTS
        // =========================================

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        if (
            activeCard != null
                && player.isSelected()
        ) {

            player.renderLegalMoves(
                shapeRenderer,
                activeCard.getMovementRules()
            );
        }

        shapeRenderer.end();

        // =========================================
        // CARDS
        // =========================================

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        for (Card card : hand) {

            card.drawShape(
                shapeRenderer
            );
        }

        shapeRenderer.end();

        // =========================================
        // ENEMIES + PLAYER + TEXT
        // =========================================

        batch.begin();

        // -----------------------------------------
        // ENEMIES FIRST
        // -----------------------------------------

        for (Enemy enemy : enemies) {

            enemy.render(batch);
        }

        // -----------------------------------------
        // PLAYER
        // -----------------------------------------

        player.render(batch);

        // -----------------------------------------
        // CARD TEXT
        // -----------------------------------------

        for (Card card : hand) {

            card.drawText(
                batch,
                font
            );
        }

        // =========================================
        // EVENT MESSAGE
        // =========================================

        if (!eventMessage.isEmpty()) {

            font.getData().setScale(1f);

            font.setColor(
                new Color(
                    0.30f,
                    0.80f,
                    1f,
                    1f
                )
            );

            font.draw(
                batch,
                eventMessage,
                20f,
                Gdx.graphics.getHeight()
                    - 20f
            );
        }

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);

        batch.end();
    }

    // =========================================
    // CLEANUP
    // =========================================

    @Override
    public void dispose() {

        if (batch != null) {
            batch.dispose();
        }

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }

        if (font != null) {
            font.dispose();
        }

        if (board != null) {
            board.dispose();
        }

        if (player != null) {
            player.dispose();
        }

        if (enemies != null) {

            for (Enemy enemy : enemies) {
                enemy.dispose();
            }
        }
    }
}
