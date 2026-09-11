package world.elyona.items.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.model.*;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class ItemsConfig {

    private final ElyonaItemsPlugin plugin;
    private final Logger logger;
    private final Map<String, ItemDefinition> items = new HashMap<>();
    private final Map<String, Long> mimicPrices = new HashMap<>();

    public ItemsConfig(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public void load() {
        items.clear();
        mimicPrices.clear();
        loadItems();
        loadMimicPrices();
        logger.info("アイテム定義を " + items.size() + " 件ロードしました");
    }

    private void loadItems() {
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(itemsFile);
        ConfigurationSection itemsSection = cfg.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("items.yml に items セクションが見つかりません");
            return;
        }

        for (String id : itemsSection.getKeys(false)) {
            ConfigurationSection sec = itemsSection.getConfigurationSection(id);
            if (sec == null) continue;
            try {
                ItemDefinition def = parseItem(id, sec);
                items.put(id, def);
            } catch (Exception e) {
                logger.warning("アイテム '" + id + "' のパースに失敗: " + e.getMessage());
            }
        }
    }

    private ItemDefinition parseItem(String id, ConfigurationSection sec) {
        String display = sec.getString("display", id);
        Rarity rarity = parseEnum(Rarity.class, sec.getString("rarity", "COMMON"), Rarity.COMMON);
        ItemType type = parseEnum(ItemType.class, sec.getString("type", "EQUIPMENT"), ItemType.EQUIPMENT);
        boolean durability = sec.getBoolean("durability", false);
        int maxDurability = sec.getInt("max_durability", 0);
        ItemCategory category = parseEnum(ItemCategory.class, sec.getString("category", "SPECIAL"), ItemCategory.SPECIAL);
        String loreFlavor = sec.getString("lore_flavor", "");
        Material baseItem = parseMaterial(sec.getString("base_item", "PAPER"));
        int durationSecondsMin = sec.getInt("duration_seconds_min", 60);
        int durationSecondsMax = sec.getInt("duration_seconds_max", 60);

        List<EffectDefinition> effects = parseEffects(sec.getMapList("effects"));
        List<EffectDefinition> afterEffects = parseEffects(sec.getMapList("after_effects"));

        return ItemDefinition.builder(id)
                .display(display)
                .rarity(rarity)
                .type(type)
                .hasDurability(durability)
                .maxDurability(maxDurability)
                .category(category)
                .loreFlavor(loreFlavor)
                .baseItem(baseItem)
                .effects(effects)
                .afterEffects(afterEffects)
                .durationSecondsMin(durationSecondsMin)
                .durationSecondsMax(durationSecondsMax)
                .build();
    }

    private List<EffectDefinition> parseEffects(List<Map<?, ?>> effectList) {
        List<EffectDefinition> result = new ArrayList<>();
        if (effectList == null) return result;
        for (Map<?, ?> map : effectList) {
            try {
                String typeStr = getString(map, "type", "SPEED");
                EffectType effectType = parseEnum(EffectType.class, typeStr, EffectType.SPEED);

                EffectDefinition.Builder builder = EffectDefinition.builder(effectType)
                        .min(getInt(map, "min", 1))
                        .max(getInt(map, "max", 1))
                        .radiusMin(getInt(map, "radius_min", 0))
                        .radiusMax(getInt(map, "radius_max", 0))
                        .slowLevelMin(getInt(map, "slow_level_min", 1))
                        .slowLevelMax(getInt(map, "slow_level_max", 1))
                        .durationSeconds(getInt(map, "duration_seconds", 0));

                result.add(builder.build());
            } catch (Exception e) {
                logger.warning("エフェクト定義のパースに失敗: " + e.getMessage());
            }
        }
        return result;
    }

    private void loadMimicPrices() {
        FileConfiguration cfg = plugin.getConfig();
        ConfigurationSection priceSection = cfg.getConfigurationSection("mimic_buy_prices");
        if (priceSection == null) {
            logger.warning("config.yml に mimic_buy_prices セクションが見つかりません");
            return;
        }
        for (String id : priceSection.getKeys(false)) {
            mimicPrices.put(id, (long) priceSection.getInt(id, 0));
        }
    }

    public ItemDefinition getItem(String id) {
        return items.get(id);
    }

    public Map<String, ItemDefinition> getAllItems() {
        return Collections.unmodifiableMap(items);
    }

    public long getMimicBasePrice(String itemId) {
        return mimicPrices.getOrDefault(itemId, 0L);
    }

    public Set<String> getItemIds() {
        return Collections.unmodifiableSet(items.keySet());
    }

    // ユーティリティメソッド
    private <T extends Enum<T>> T parseEnum(Class<T> cls, String value, T defaultValue) {
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(cls, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private Material parseMaterial(String name) {
        if (name == null) return Material.PAPER;
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.PAPER;
    }

    private String getString(Map<?, ?> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    private int getInt(Map<?, ?> map, String key, int def) {
        Object val = map.get(key);
        if (val == null) return def;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return def; }
    }
}
