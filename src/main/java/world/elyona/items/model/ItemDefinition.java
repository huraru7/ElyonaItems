package world.elyona.items.model;

import org.bukkit.Material;
import java.util.Collections;
import java.util.List;

public class ItemDefinition {

    private final String id;
    private final String display;
    private final Rarity rarity;
    private final ItemType type;
    private final boolean hasDurability;
    private final int maxDurability;
    private final ItemCategory category;
    private final String loreFlavor;
    private final Material baseItem;
    private final List<EffectDefinition> effects;
    private final List<EffectDefinition> afterEffects;
    // 消耗品の効果時間 (秒)
    private final int durationSecondsMin;
    private final int durationSecondsMax;

    private ItemDefinition(Builder builder) {
        this.id = builder.id;
        this.display = builder.display;
        this.rarity = builder.rarity;
        this.type = builder.type;
        this.hasDurability = builder.hasDurability;
        this.maxDurability = builder.maxDurability;
        this.category = builder.category;
        this.loreFlavor = builder.loreFlavor;
        this.baseItem = builder.baseItem;
        this.effects = Collections.unmodifiableList(builder.effects);
        this.afterEffects = Collections.unmodifiableList(builder.afterEffects);
        this.durationSecondsMin = builder.durationSecondsMin;
        this.durationSecondsMax = builder.durationSecondsMax;
    }

    public String getId() { return id; }
    public String getDisplay() { return display; }
    public Rarity getRarity() { return rarity; }
    public ItemType getType() { return type; }
    public boolean hasDurability() { return hasDurability; }
    public int getMaxDurability() { return maxDurability; }
    public ItemCategory getCategory() { return category; }
    public String getLoreFlavor() { return loreFlavor; }
    public Material getBaseItem() { return baseItem; }
    public List<EffectDefinition> getEffects() { return effects; }
    public List<EffectDefinition> getAfterEffects() { return afterEffects; }
    public int getDurationSecondsMin() { return durationSecondsMin; }
    public int getDurationSecondsMax() { return durationSecondsMax; }

    /**
     * 品質値に基づいて消耗品の効果時間 (ticks) を計算する
     */
    public int calculateDurationTicks(int quality) {
        double duration = durationSecondsMin + (durationSecondsMax - durationSecondsMin) * (quality / 100.0);
        return (int) Math.round(duration) * 20;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static class Builder {
        private final String id;
        private String display = "";
        private Rarity rarity = Rarity.COMMON;
        private ItemType type = ItemType.EQUIPMENT;
        private boolean hasDurability = false;
        private int maxDurability = 0;
        private ItemCategory category = ItemCategory.SPECIAL;
        private String loreFlavor = "";
        private Material baseItem = Material.PAPER;
        private List<EffectDefinition> effects = new java.util.ArrayList<>();
        private List<EffectDefinition> afterEffects = new java.util.ArrayList<>();
        private int durationSecondsMin = 60;
        private int durationSecondsMax = 60;

        public Builder(String id) {
            this.id = id;
        }

        public Builder display(String display) { this.display = display; return this; }
        public Builder rarity(Rarity rarity) { this.rarity = rarity; return this; }
        public Builder type(ItemType type) { this.type = type; return this; }
        public Builder hasDurability(boolean hasDurability) { this.hasDurability = hasDurability; return this; }
        public Builder maxDurability(int maxDurability) { this.maxDurability = maxDurability; return this; }
        public Builder category(ItemCategory category) { this.category = category; return this; }
        public Builder loreFlavor(String loreFlavor) { this.loreFlavor = loreFlavor; return this; }
        public Builder baseItem(Material baseItem) { this.baseItem = baseItem; return this; }
        public Builder effects(List<EffectDefinition> effects) { this.effects = effects; return this; }
        public Builder afterEffects(List<EffectDefinition> afterEffects) { this.afterEffects = afterEffects; return this; }
        public Builder durationSecondsMin(int min) { this.durationSecondsMin = min; return this; }
        public Builder durationSecondsMax(int max) { this.durationSecondsMax = max; return this; }

        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
}
