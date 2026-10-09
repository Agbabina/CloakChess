package io.github.chesslike.game;

public enum CaptureCurse {
    ARROWPROOF("Arrowproof", "Arrows cannot affect this enemy.", "ARROWPROOF"),
    PAWNLOCK("Pawnlocked", "Cannot be captured with a Pawn.", "PAWN LOCK"),
    KNIGHTLOCK("Knightlocked", "Cannot be captured with a Knight.", "KNIGHT LOCK"),
    BISHOPLOCK("Bishoplocked", "Cannot be captured with a Bishop.", "BISHOP LOCK"),
    ROOKLOCK("Rooklocked", "Cannot be captured with a Rook.", "ROOK LOCK"),
    QUEENLOCK("Queenlocked", "Cannot be captured with a Queen.", "QUEEN LOCK"),
    DASHLOCK("Dashlocked", "Cannot be captured with Dash.", "DASH LOCK"),
    ASSASSINATIONPROOF("Veiled", "Cloak assassination cannot kill this enemy.", "ASSASSINATION PROOF");

    private final String label;




    private final String description;
    private final String shortLabel;

    CaptureCurse(String label, String description, String shortLabel) {
        this.label = label;
        this.description = description;
        this.shortLabel = shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public boolean blocks(Card card, boolean assassination, int ignoredCaptureDamage) {
        if (card == null) return false;
        if (assassination) return this == ASSASSINATIONPROOF;
        switch (this) {
            case PAWNLOCK: return card.getMovementType() == Card.MovementType.PAWN;
            case KNIGHTLOCK: return card.getMovementType() == Card.MovementType.KNIGHT;
            case BISHOPLOCK: return card.getMovementType() == Card.MovementType.BISHOP;
            case ROOKLOCK: return card.getMovementType() == Card.MovementType.ROOK;
            case QUEENLOCK: return card.getMovementType() == Card.MovementType.QUEEN;
            case DASHLOCK: return card.getMovementType() == Card.MovementType.DASH;
            case ASSASSINATIONPROOF: return assassination;
            default: return false;
        }
    }
}
