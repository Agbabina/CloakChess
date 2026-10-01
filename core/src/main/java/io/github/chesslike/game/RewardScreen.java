package io.github.chesslike.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import io.github.chesslike.game.Augment;
import io.github.chesslike.game.AugmentType;
import io.github.chesslike.game.Card;

import java.util.Random;

public class RewardScreen {

    public enum State {
        HIDDEN,
        CHOOSING_REWARD,
        CHOOSING_CARD,
        CHOOSING_AUGMENT
    }

    private State state = State.HIDDEN;

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;

    private final Random random = new Random();

    private final Rectangle newCardButton = new Rectangle();
    private final Rectangle augmentButton = new Rectangle();

    private final Array<Rectangle> cardButtons =
        new Array<>();

    private final Array<Rectangle> augmentButtons =
        new Array<>();

    private final Array<Augment> augmentChoices =
        new Array<>();

    private int goldReward;

    private Card selectedCard;

    private Augment selectedAugment;

    // ------------------------------------------------
    // NEW CARD REWARD OUTPUT
    // ------------------------------------------------

    private Card.MovementType grantedCardType;

    // ------------------------------------------------
    // CARD TYPES THE PLAYER CAN ACTUALLY RECEIVE
    // (MADROOK / JESTER / BLINKER / CHAMELEON are
    // enemy-only, so they must never be rewards)
    // ------------------------------------------------

    private static final Card.MovementType[] REWARD_CARD_TYPES = {
        Card.MovementType.KNIGHT,
        Card.MovementType.BISHOP,
        Card.MovementType.ROOK,
        Card.MovementType.QUEEN,
        Card.MovementType.PAWN,
        Card.MovementType.DASH
    };

    // ------------------------------------------------
    // SHARED TEXTURES
    // (static — loaded once, since RewardScreen is
    // only ever instantiated a single time by Main)
    // ------------------------------------------------

    private static Texture newCardButtonTexture;
    private static Texture augmentButtonTexture;
    private static Texture sectionBoxTexture;
    private static NinePatch sectionBox;

    // Margin = RADIUS (22) + BORDER_THICKNESS (6) from
    // the section_box.png generation script — the flat
    // stretchable middle starts/ends 28px in from each edge.
    private static final int BOX_MARGIN = 28;

    public static void loadTextures() {

        if (newCardButtonTexture == null) {
            newCardButtonTexture =
                new Texture(
                    Gdx.files.internal("button.png")
                );

            newCardButtonTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        if (augmentButtonTexture == null) {
            augmentButtonTexture =
                new Texture(
                    Gdx.files.internal("button_augment.png")
                );

            augmentButtonTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        // Load the texture if necessary
        if (sectionBoxTexture == null) {
            sectionBoxTexture =
                new Texture(
                    Gdx.files.internal("section_box.png")
                );

            sectionBoxTexture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        // Always make sure the NinePatch exists
        if (sectionBox == null) {
            sectionBox =
                new NinePatch(
                    sectionBoxTexture,
                    BOX_MARGIN,
                    BOX_MARGIN,
                    BOX_MARGIN,
                    BOX_MARGIN
                );
        }
    }

    public static void disposeTextures() {

        if (newCardButtonTexture != null) {
            newCardButtonTexture.dispose();
            newCardButtonTexture = null;
        }

        if (augmentButtonTexture != null) {
            augmentButtonTexture.dispose();
            augmentButtonTexture = null;
        }

        if (sectionBoxTexture != null) {
            sectionBoxTexture.dispose();
            sectionBoxTexture = null;
            sectionBox = null;
        }
    }

    public RewardScreen(
        SpriteBatch batch,
        ShapeRenderer shapeRenderer,
        BitmapFont font
    ) {
        this.batch = batch;
        this.shapeRenderer = shapeRenderer;
        this.font = font;
    }

    // ------------------------------------------------
    // SHOW
    // ------------------------------------------------

    public void show(int goldReward) {

        this.goldReward = goldReward;

        selectedCard = null;
        selectedAugment = null;
        grantedCardType = null;

        augmentChoices.clear();
        cardButtons.clear();
        augmentButtons.clear();

        state = State.CHOOSING_REWARD;
    }

    // ------------------------------------------------
    // HIDE
    // ------------------------------------------------

    public void hide() {

        state = State.HIDDEN;

        selectedCard = null;
        selectedAugment = null;
        grantedCardType = null;

        augmentChoices.clear();
        cardButtons.clear();
        augmentButtons.clear();
    }

    // ------------------------------------------------
    // STATE
    // ------------------------------------------------

    public boolean isVisible() {
        return state != State.HIDDEN;
    }

    public State getState() {
        return state;
    }

    // ------------------------------------------------
    // RENDER
    // ------------------------------------------------

    public void render(Array<Card> hand) {

        if (!isVisible()) {
            return;
        }
        if(sectionBox==null){
            loadTextures();
        }
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        // --------------------------------------------
        // DARK OVERLAY
        // (flat dim behind the panel — stays as a
        // ShapeRenderer fill, no texture needed here)
        // --------------------------------------------

        shapeRenderer.begin(
            ShapeRenderer.ShapeType.Filled
        );

        shapeRenderer.setColor(
            new Color(0f, 0f, 0f, 0.78f)
        );

        shapeRenderer.rect(
            0,
            0,
            width,
            height
        );

        shapeRenderer.end();

        // --------------------------------------------
        // PANEL (now drawn from section_box via NinePatch
        // instead of a flat ShapeRenderer rect, so it
        // keeps crisp rounded corners at any size)
        // --------------------------------------------

        float panelWidth =
            Math.min(width - 40f, 760f);

        float panelHeight =
            Math.min(height - 40f, 560f);

        float panelX =
            (width - panelWidth) / 2f;

        float panelY =
            (height - panelHeight) / 2f;

        batch.begin();

        sectionBox.draw(
            batch,
            panelX,
            panelY,
            panelWidth,
            panelHeight
        );

        batch.end();

        // --------------------------------------------
        // STATE-SPECIFIC UI
        // --------------------------------------------

        if (state == State.CHOOSING_REWARD) {

            renderRewardChoices(
                panelX,
                panelY,
                panelWidth,
                panelHeight
            );

        } else if (state == State.CHOOSING_CARD) {

            renderCardChoices(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                hand
            );

        } else if (state == State.CHOOSING_AUGMENT) {

            renderAugmentChoices(
                panelX,
                panelY,
                panelWidth,
                panelHeight
            );
        }
    }

    // ------------------------------------------------
    // REWARD CHOICE
    // ------------------------------------------------

    private void renderRewardChoices(
        float panelX,
        float panelY,
        float panelWidth,
        float panelHeight
    ) {

        float buttonWidth =
            Math.min(280f, panelWidth * 0.40f);

        // Height is derived from each texture's own
        // aspect ratio so the art isn't stretched/
        // squished to fit an arbitrary fixed height.
        float newCardButtonHeight =
            buttonWidth *
                ((float) newCardButtonTexture.getHeight() /
                    newCardButtonTexture.getWidth());

        float augmentButtonHeight =
            buttonWidth *
                ((float) augmentButtonTexture.getHeight() /
                    augmentButtonTexture.getWidth());

        float buttonHeight =
            Math.max(
                newCardButtonHeight,
                augmentButtonHeight
            );

        float gap = 30f;

        float totalWidth =
            buttonWidth * 2f + gap;

        float startX =
            panelX +
                (panelWidth - totalWidth) / 2f;

        // Both buttons still sit on a shared baseline
        // (bottom-aligned) even if their heights differ.
        float rowBottomY =
            panelY +
                panelHeight / 2f -
                buttonHeight / 2f;

        newCardButton.set(
            startX,
            rowBottomY,
            buttonWidth,
            newCardButtonHeight
        );

        augmentButton.set(
            startX + buttonWidth + gap,
            rowBottomY,
            buttonWidth,
            augmentButtonHeight
        );

        batch.begin();

        // -----------------------------------------
        // BUTTON IMAGES
        // (labels are baked into these textures —
        // no separate "NEW CARD"/"AUGMENT" text draw)
        // -----------------------------------------

        batch.draw(
            newCardButtonTexture,
            newCardButton.x,
            newCardButton.y,
            newCardButton.width,
            newCardButton.height
        );

        batch.draw(
            augmentButtonTexture,
            augmentButton.x,
            augmentButton.y,
            augmentButton.width,
            augmentButton.height
        );

        // -----------------------------------------
        // HEADER TEXT
        // -----------------------------------------

        font.getData().setScale(1.4f);
        font.setColor(Color.WHITE);

        font.draw(
            batch,
            "ROOM CLEARED",
            panelX + 35f,
            panelY + panelHeight - 40f
        );

        font.getData().setScale(0.8f);

        font.setColor(
            new Color(
                0.65f,
                0.70f,
                0.78f,
                1f
            )
        );

        font.draw(
            batch,
            "Choose your reward",
            panelX + 35f,
            panelY + panelHeight - 78f
        );

        // -----------------------------------------
        // SUBTITLES UNDER EACH BUTTON
        // -----------------------------------------

        font.getData().setScale(0.55f);
        font.setColor(Color.WHITE);

        font.draw(
            batch,
            "Receive a random card",
            newCardButton.x + 30f,
            newCardButton.y - 12f
        );

        font.draw(
            batch,
            "Improve an existing card",
            augmentButton.x + 30f,
            augmentButton.y - 12f
        );

        // -----------------------------------------
        // GOLD
        // -----------------------------------------

        font.getData().setScale(1f);

        font.setColor(
            new Color(
                1f,
                0.78f,
                0.25f,
                1f
            )
        );

        font.draw(
            batch,
            "+" + goldReward + " GOLD",
            panelX + 35f,
            panelY + 35f
        );

        resetFont();

        batch.end();
    }

    // ------------------------------------------------
    // CARD CHOICE
    // ------------------------------------------------

    private void renderCardChoices(
        float panelX,
        float panelY,
        float panelWidth,
        float panelHeight,
        Array<Card> hand
    ) {

        cardButtons.clear();

        float cardWidth = 120f;
        float cardHeight = 170f;
        float gap = 15f;

        float totalWidth =
            hand.size * cardWidth +
                Math.max(0, hand.size - 1) * gap;

        float startX =
            panelX +
                (panelWidth - totalWidth) / 2f;

        float y =
            panelY +
                panelHeight / 2f -
                cardHeight / 2f;

        for (int i = 0; i < hand.size; i++) {

            Rectangle rect = new Rectangle(
                startX + i * (cardWidth + gap),
                y,
                cardWidth,
                cardHeight
            );

            cardButtons.add(rect);
        }

        batch.begin();

        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);

        font.draw(
            batch,
            "CHOOSE A CARD TO AUGMENT",
            panelX + 30f,
            panelY + panelHeight - 35f
        );

        font.getData().setScale(0.65f);

        font.setColor(
            new Color(
                0.65f,
                0.70f,
                0.78f,
                1f
            )
        );

        font.draw(
            batch,
            "Select one of your cards",
            panelX + 30f,
            panelY + panelHeight - 70f
        );

        // -----------------------------------------
        // CARD SLOT BOXES (NinePatch, replaces the
        // old flat ShapeRenderer rects)
        // -----------------------------------------

        for (int i = 0; i < hand.size; i++) {

            Rectangle rect =
                cardButtons.get(i);

            sectionBox.draw(
                batch,
                rect.x,
                rect.y,
                rect.width,
                rect.height
            );
        }

        // -----------------------------------------
        // CARD TEXT
        // -----------------------------------------

        for (int i = 0; i < hand.size; i++) {

            Card card = hand.get(i);

            Rectangle rect =
                cardButtons.get(i);

            font.getData().setScale(0.75f);

            font.setColor(Color.WHITE);

            font.draw(
                batch,
                card.getName(),
                rect.x + 12f,
                rect.y + rect.height - 25f
            );

            font.getData().setScale(0.55f);

            font.setColor(
                new Color(
                    0.95f,
                    0.75f,
                    0.25f,
                    1f
                )
            );

            font.draw(
                batch,
                "USES: " +
                    card.getUsesRemaining(),
                rect.x + 12f,
                rect.y + rect.height - 55f
            );

            font.setColor(
                new Color(
                    0.40f,
                    0.70f,
                    1f,
                    1f
                )
            );

            font.draw(
                batch,
                card.getMovementType().toString(),
                rect.x + 12f,
                rect.y + 55f
            );
        }

        resetFont();

        batch.end();
    }

    // ------------------------------------------------
    // AUGMENT CHOICE
    // ------------------------------------------------

    private void renderAugmentChoices(
        float panelX,
        float panelY,
        float panelWidth,
        float panelHeight
    ) {

        augmentButtons.clear();

        float buttonWidth = 190f;
        float buttonHeight = 150f;
        float gap = 20f;

        float totalWidth =
            buttonWidth * augmentChoices.size +
                gap * (augmentChoices.size - 1);

        float startX =
            panelX +
                (panelWidth - totalWidth) / 2f;

        float y =
            panelY +
                panelHeight / 2f -
                buttonHeight / 2f;

        for (int i = 0;
             i < augmentChoices.size;
             i++) {

            Rectangle rect =
                new Rectangle(
                    startX +
                        i * (buttonWidth + gap),
                    y,
                    buttonWidth,
                    buttonHeight
                );

            augmentButtons.add(rect);
        }

        batch.begin();

        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);

        font.draw(
            batch,
            "CHOOSE AN AUGMENT",
            panelX + 30f,
            panelY + panelHeight - 35f
        );

        font.getData().setScale(0.65f);

        font.setColor(
            new Color(
                0.65f,
                0.70f,
                0.78f,
                1f
            )
        );

        font.draw(
            batch,
            selectedCard.getName() +
                " will receive one of these",
            panelX + 30f,
            panelY + panelHeight - 70f
        );

        // -----------------------------------------
        // AUGMENT TILE BOXES (NinePatch)
        // -----------------------------------------

        for (int i = 0;
             i < augmentChoices.size;
             i++) {

            Rectangle rect =
                augmentButtons.get(i);

            sectionBox.draw(
                batch,
                rect.x,
                rect.y,
                rect.width,
                rect.height
            );
        }

        // -----------------------------------------
        // AUGMENT TEXT
        // -----------------------------------------

        for (int i = 0;
             i < augmentChoices.size;
             i++) {

            Augment augment =
                augmentChoices.get(i);

            Rectangle rect =
                augmentButtons.get(i);

            font.getData().setScale(0.75f);

            font.setColor(
                new Color(
                    1f,
                    0.78f,
                    0.25f,
                    1f
                )
            );

            font.draw(
                batch,
                augment.getName(),
                rect.x + 15f,
                rect.y + rect.height - 25f
            );

            font.getData().setScale(0.55f);

            font.setColor(Color.WHITE);

            font.draw(
                batch,
                augment.getDescription(),
                rect.x + 15f,
                rect.y + rect.height - 60f
            );
        }

        resetFont();

        batch.end();
    }

    // ------------------------------------------------
    // CLICK HANDLING
    // ------------------------------------------------

    public boolean handleClick(
        float x,
        float y,
        Array<Card> hand
    ) {

        if (!isVisible()) {
            return false;
        }

        // --------------------------------------------
        // CHOOSE REWARD
        // --------------------------------------------

        if (state == State.CHOOSING_REWARD) {

            if (newCardButton.contains(x, y)) {

                grantedCardType = randomCardType();

                state = State.HIDDEN;

                return true;
            }

            if (augmentButton.contains(x, y)) {

                if (hand.size == 0) {
                    return true;
                }

                state = State.CHOOSING_CARD;

                return true;
            }
        }

        // --------------------------------------------
        // CHOOSE CARD (only reachable via AUGMENT)
        // --------------------------------------------

        else if (state == State.CHOOSING_CARD) {

            for (int i = 0;
                 i < cardButtons.size;
                 i++) {

                if (
                    cardButtons.get(i)
                        .contains(x, y)
                ) {

                    selectedCard =
                        hand.get(i);

                    generateAugmentChoices();

                    state =
                        State.CHOOSING_AUGMENT;

                    return true;
                }
            }
        }

        // --------------------------------------------
        // CHOOSE AUGMENT
        // --------------------------------------------

        else if (
            state == State.CHOOSING_AUGMENT
        ) {

            for (int i = 0;
                 i < augmentButtons.size;
                 i++) {

                if (
                    augmentButtons.get(i)
                        .contains(x, y)
                ) {

                    selectedAugment =
                        augmentChoices.get(i);

                    // FIX: this branch used to leave
                    // state on CHOOSING_AUGMENT forever,
                    // unlike the new-card path (which
                    // sets HIDDEN). That meant nothing
                    // ever observed the augment as
                    // "done" and applied it.
                    state = State.HIDDEN;

                    return true;
                }
            }
        }

        return false;
    }

    // ------------------------------------------------
    // RANDOM CARD TYPE
    // ------------------------------------------------

    private Card.MovementType randomCardType() {

        return REWARD_CARD_TYPES[
            random.nextInt(REWARD_CARD_TYPES.length)
            ];
    }

    // ------------------------------------------------
    // RANDOM AUGMENTS
    // ------------------------------------------------

    private void generateAugmentChoices() {

        augmentChoices.clear();

        Array<Augment> pool =
            createAugmentPool();

        while (
            augmentChoices.size < 3
                && pool.size > 0
        ) {

            int index =
                random.nextInt(pool.size);

            augmentChoices.add(
                pool.removeIndex(index)
            );
        }
    }

    private Array<Augment> createAugmentPool() {

        Array<Augment> pool =
            new Array<>();

        pool.add(
            new Augment(
                AugmentType.EXTRA_USES,
                "+1 USE",
                "This card gets one extra use.",
                1
            )
        );

        pool.add(
            new Augment(
                AugmentType.EXTRA_USES,
                "+2 USES",
                "This card gets two extra uses.",
                2
            )
        );

        pool.add(
            new Augment(
                AugmentType.BURN,
                "Burn",
                "Remove another enemy Piece from the board",
                1
            )
        );

        pool.add(
            new Augment(
                AugmentType.REBOUND,
                "REBOUND",
                "Capture and continue moving.",
                1
            )
        );

        pool.add(
            new Augment(
                AugmentType.PIERCE,
                "PIERCE",
                "Gain special enemy movement.",
                1
            )
        );
        pool.add(
            new Augment(
                AugmentType.TELEPORTATION_INFUSION,
                "Teleportation",
                "Teleport to a square on the board when  used",
                2
            )
        );

        pool.add(
            new Augment(
                AugmentType.DOUBLE_MOVE,
                "DOUBLE MOVE",
                "The card can trigger another move.",
                1
            )
        );

        return pool;
    }

    // ------------------------------------------------
    // SELECTED AUGMENT
    // ------------------------------------------------

    public boolean hasSelectedAugment() {
        return selectedAugment != null;
    }

    public Augment getSelectedAugment() {
        return selectedAugment;
    }

    public Card getSelectedCard() {
        return selectedCard;
    }

    // ------------------------------------------------
    // GRANTED NEW CARD
    // ------------------------------------------------

    public boolean hasGrantedNewCard() {
        return grantedCardType != null;
    }

    public Card.MovementType getGrantedCardType() {
        return grantedCardType;
    }

    // ------------------------------------------------
    // FONT RESET
    // ------------------------------------------------

    private void resetFont() {

        font.getData().setScale(1f);

        font.setColor(Color.WHITE);
    }
}
