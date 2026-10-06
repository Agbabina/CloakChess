package io.github.chesslike.game;

public class CardCombination {

    private static class Recipe {
        final Card.MovementType a, b, resultType;
        final String resultName;
        final int uses;

        Recipe(Card.MovementType a, Card.MovementType b,
               String resultName, Card.MovementType resultType, int uses) {
            this.a = a;
            this.b = b;
            this.resultName = resultName;
            this.resultType = resultType;
            this.uses = uses;
        }

        // Order doesn't matter: A+B and B+A both match
        boolean matches(Card.MovementType x, Card.MovementType y) {
            return (a == x && b == y) || (a == y && b == x);
        }
    }

    // ADD NEW COMBOS HERE: (typeA, typeB, resulting name, resulting movement, uses)
    private static final Recipe[] RECIPES = {
        // Original recipes
        new Recipe(Card.MovementType.ROOK,       Card.MovementType.BISHOP,     "Queen",      Card.MovementType.QUEEN,      2),
        new Recipe(Card.MovementType.KNIGHT,     Card.MovementType.DASH,       "Councillor", Card.MovementType.COUNCILLOR, 2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.BISHOP,     "Knight",     Card.MovementType.KNIGHT,     2),
        new Recipe(Card.MovementType.DASH,       Card.MovementType.DASH,       "Rook",       Card.MovementType.ROOK,       2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.PAWN,       "Dash",       Card.MovementType.DASH,       2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.ROOK,       "Bishop",     Card.MovementType.BISHOP,     2),
        new Recipe(Card.MovementType.KNIGHT,     Card.MovementType.BISHOP,     "ArchBishop", Card.MovementType.ARCHBISHOP, 2),

        // Claude recipes
        new Recipe(Card.MovementType.KNIGHT,     Card.MovementType.KNIGHT,     "Claude",     Card.MovementType.CLAUDE,     2),
        new Recipe(Card.MovementType.QUEEN,      Card.MovementType.PAWN,       "Claude",     Card.MovementType.CLAUDE,     2),
        new Recipe(Card.MovementType.COUNCILLOR, Card.MovementType.DASH,       "Claude",     Card.MovementType.CLAUDE,     2),

        // Queen recipes
        new Recipe(Card.MovementType.ROOK,       Card.MovementType.ROOK,       "Queen",      Card.MovementType.QUEEN,      2),
        new Recipe(Card.MovementType.BISHOP,     Card.MovementType.BISHOP,     "Queen",      Card.MovementType.QUEEN,      2),
        new Recipe(Card.MovementType.ARCHBISHOP, Card.MovementType.PAWN,       "Queen",      Card.MovementType.QUEEN,      2),

        // Councillor / ArchBishop recipes
        new Recipe(Card.MovementType.ROOK,       Card.MovementType.KNIGHT,     "Councillor", Card.MovementType.COUNCILLOR, 2),
        new Recipe(Card.MovementType.QUEEN,      Card.MovementType.DASH,       "Councillor", Card.MovementType.COUNCILLOR, 2),
        new Recipe(Card.MovementType.ROOK,       Card.MovementType.DASH,       "ArchBishop", Card.MovementType.ARCHBISHOP, 2),
        new Recipe(Card.MovementType.QUEEN,      Card.MovementType.KNIGHT,     "ArchBishop", Card.MovementType.ARCHBISHOP, 2),

        // Basic piece recipes
        new Recipe(Card.MovementType.BISHOP,     Card.MovementType.DASH,       "Knight",     Card.MovementType.KNIGHT,     2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.KNIGHT,     "Dash",       Card.MovementType.DASH,       2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.DASH,       "Rook",       Card.MovementType.ROOK,       2),
        new Recipe(Card.MovementType.KNIGHT,     Card.MovementType.ROOK,       "Councillor", Card.MovementType.COUNCILLOR, 2),
        new Recipe(Card.MovementType.ROOK,       Card.MovementType.QUEEN,      "Mad Rook",   Card.MovementType.MADROOK,    2),
        new Recipe(Card.MovementType.SHIFTER,    Card.MovementType.KNIGHT,     "Blinker",    Card.MovementType.BLINKER,    2),
        new Recipe(Card.MovementType.SHIFTER,    Card.MovementType.QUEEN,      "Chameleon",  Card.MovementType.CHAMELEON,  2),
        new Recipe(Card.MovementType.PAWN,       Card.MovementType.CLAUDE,     "Shifter",    Card.MovementType.SHIFTER,    3),
        new Recipe(Card.MovementType.CLAUDE,     Card.MovementType.BISHOP,     "Blinker",    Card.MovementType.BLINKER,    2),
        new Recipe(Card.MovementType.DASH,       Card.MovementType.SHIFTER,    "Jester",     Card.MovementType.JESTER,     2),
    };

    /** Returns a new Card if the two cards combine, otherwise null. */
    public static Card combine(Card first, Card second) {
        if (first == null || second == null) return null;

        Card.MovementType t1 = first.getMovementType();
        Card.MovementType t2 = second.getMovementType();

        for (Recipe r : RECIPES) {
            if (r.matches(t1, t2)) {
                return new Card(r.resultName, r.resultType, r.uses);
            }
        }
        return null;
    }
}
