package io.github.chesslike.game;

/**
 * Data-first run item catalogue. Artwork is supplied separately under
 * assets/items/<artKey>.png; the game can use the key as a stable asset ID.
 */
public enum RunItem {
    WOODEN_SHIELD("Wooden Shield", "Block the next 1 damage.", Category.DEFENSE, Rarity.COMMON, false, "wooden_shield"),
    TOWER_SHIELD("Tower Shield", "Gain 2 defence immediately.", Category.DEFENSE, Rarity.RARE, true, "tower_shield"),
    ROYAL_AEGIS("Royal Aegis", "Prevent the next 3 damage this room.", Category.DEFENSE, Rarity.EPIC, true, "royal_aegis"),
    SMOKE_BOMB("Smoke Bomb", "Enemies lose their next movement step.", Category.CONSUMABLE, Rarity.RARE, true, "smoke_bomb"),
    FROST_VIAL("Frost Vial", "Freeze the nearest enemy for 2 turns.", Category.CONSUMABLE, Rarity.COMMON, true, "frost_vial"),
    RIFT_SHARD("Rift Shard", "Teleport the King to a chosen empty tile.", Category.CONSUMABLE, Rarity.RARE, true, "rift_shard"),
    BLOOD_POTION("Blood Potion", "Restore 2 HP.", Category.CONSUMABLE, Rarity.COMMON, true, "blood_potion"),
    MANA_FLASK("Mana Flask", "Restore 2 mana.", Category.CONSUMABLE, Rarity.COMMON, true, "mana_flask"),
    PHOENIX_ASH("Phoenix Ash", "Survive one fatal hit at 1 HP this room.", Category.CONSUMABLE, Rarity.EPIC, true, "phoenix_ash"),
    EXECUTIONERS_SEAL("Executioner's Seal", "Your next capture deals +2 damage.", Category.OFFENSE, Rarity.RARE, true, "executioners_seal"),
    ECHO_STONE("Echo Stone", "Repeat the effect of your last consumable.", Category.UTILITY, Rarity.EPIC, true, "echo_stone"),
    GOLDEN_COMPASS("Golden Compass", "Reveal the safest route through the room.", Category.UTILITY, Rarity.COMMON, true, "golden_compass");

    public enum Category { DEFENSE, CONSUMABLE, OFFENSE, UTILITY }
    public enum Rarity { COMMON, RARE, EPIC }

    private final String displayName, description, artKey;
    private final Category category;
    private final Rarity rarity;
    private final boolean consumable;

    RunItem(String displayName, String description, Category category, Rarity rarity,
            boolean consumable, String artKey) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.rarity = rarity;
        this.consumable = consumable;
        this.artKey = artKey;
    }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public Rarity getRarity() { return rarity; }
    public boolean isConsumable() { return consumable; }
    public String getArtKey() { return artKey; }
}
