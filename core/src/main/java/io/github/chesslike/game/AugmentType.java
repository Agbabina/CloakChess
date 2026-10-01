
package io.github.chesslike.game;

public enum AugmentType {

    // Card can be used additional times.
    EXTRA_USES,

    // After using this card, another enemy piece is burned.
    BURN,

    // Allows the player to teleport to any square on the board.
    TELEPORTATION_INFUSION,

    // After capturing an enemy, the player may continue with another move.
    REBOUND,

    // Allows the card to interact with enemies in a special way.
    PIERCE,

    // Allows the card to trigger another move.
    DOUBLE_MOVE
}

