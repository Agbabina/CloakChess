
package io.github.chesslike.game;

public class Augment {

    private final AugmentType type;
    private final String name;
    private final String description;
    private final int value;

    public Augment(
        AugmentType type,
        String name,
        String description,
        int value
    ) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.value = value;
    }

    public AugmentType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return name + ": " + description;
    }
}

