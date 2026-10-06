package io.github.chesslike.game;

/**
 * A card with its own movement rules. It is never dealt randomly:
 * the only way to get one is through CardCombination.
 * baseType is only used for the card's look/icon.
 */
public class CustomCard extends Card {

    private final MovementRules rules;

    public CustomCard(String name, Card.MovementType baseType, int uses, MovementRules rules) {
        super(name, baseType, uses);
        this.rules = rules;
    }

    @Override
    public MovementRules getMovementRules() {
        return rules;
    }
}
