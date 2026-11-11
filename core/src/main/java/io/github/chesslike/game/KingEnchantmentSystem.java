package io.github.chesslike.game;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Run-scoped King enchantments. These are deliberately not written to meta
 * progression: call resetRun() when a new run begins or the King is defeated.
 */
public final class KingEnchantmentSystem {
    public enum Enchantment {
        IRON_CROWN("Iron Crown", "The King gains +1 maximum health.", Rarity.COMMON),
        WARDING_SIGIL("Warding Sigil", "Begin each room with +1 defence.", Rarity.COMMON),
        ARCANE_HEART("Arcane Heart", "Gain +1 maximum mana.", Rarity.RARE),
        BLOOD_OATH("Blood Oath", "The first capture in each room restores 1 HP.", Rarity.RARE),
        PHOENIX_RUNE("Phoenix Rune", "Once per run, survive a fatal hit at 1 HP.", Rarity.LEGENDARY),
        HUNTER_MARK("Hunter's Mark", "Deal +1 damage to enemies above half health.", Rarity.RARE);

        private final String displayName;
        private final String description;
        private final Rarity rarity;

        Enchantment(String displayName, String description, Rarity rarity) {
            this.displayName = displayName;
            this.description = description;
            this.rarity = rarity;
        }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public Rarity getRarity() { return rarity; }
    }

    public enum Rarity { COMMON, RARE, LEGENDARY }

    private final EnumSet<Enchantment> active = EnumSet.noneOf(Enchantment.class);
    private boolean phoenixUsed;
    private boolean bloodOathTriggeredThisRoom;

    public boolean enchant(Enchantment enchantment) {
        return enchantment != null && active.add(enchantment);
    }

    public boolean has(Enchantment enchantment) { return active.contains(enchantment); }
    public int getMaxHealthBonus() { return has(Enchantment.IRON_CROWN) ? 1 : 0; }
    public int getMaxManaBonus() { return has(Enchantment.ARCANE_HEART) ? 1 : 0; }
    public int getRoomStartDefenseBonus() { return has(Enchantment.WARDING_SIGIL) ? 1 : 0; }
    public boolean hasHunterMark() { return has(Enchantment.HUNTER_MARK); }
    public boolean hasPhoenixRune() { return has(Enchantment.PHOENIX_RUNE) && !phoenixUsed; }

    /** Returns true once per room when Blood Oath should heal after a capture. */
    public boolean consumeBloodOathTrigger() {
        if (!has(Enchantment.BLOOD_OATH) || bloodOathTriggeredThisRoom) return false;
        bloodOathTriggeredThisRoom = true;
        return true;
    }

    /** Returns true only once per run when Phoenix Rune prevents a fatal hit. */
    public boolean consumePhoenixSave() {
        if (!hasPhoenixRune()) return false;
        phoenixUsed = true;
        return true;
    }

    public void beginRoom() { bloodOathTriggeredThisRoom = false; }
    public Set<Enchantment> getActiveEnchantments() {
        return Collections.unmodifiableSet(active);
    }

    /** Enchantments are run-only and must never survive a defeat or fresh run. */
    public void resetRun() {
        active.clear();
        phoenixUsed = false;
        bloodOathTriggeredThisRoom = false;
    }
}
