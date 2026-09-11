package world.elyona.items.api;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.model.EffectDefinition;
import world.elyona.items.model.ItemDefinition;

/**
 * ElyonaItemsアイテムの情報取得・計算ユーティリティ。
 */
public class ItemManager {

    private final ElyonaItemsPlugin plugin;

    public ItemManager(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ItemStackがElyonaItemsのアイテムか判定する。
     */
    public boolean isElyonaItem(ItemStack item) {
        return getItemId(item) != null;
    }

    /**
     * ItemStackのアイテムIDを返す。ElyonaItemsでない場合はnull。
     */
    public String getItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, ItemGenerator.NBT_ITEM_ID);
        return pdc.get(key, PersistentDataType.STRING);
    }

    /**
     * ItemStackの品質値を返す。ElyonaItemsでない場合は-1。
     */
    public int getQuality(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, ItemGenerator.NBT_QUALITY);
        Integer q = pdc.get(key, PersistentDataType.INTEGER);
        return q != null ? q : -1;
    }

    /**
     * ItemStackの現在の耐久度を返す。耐久度なしの場合は-1。
     */
    public int getDurability(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return -1;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, ItemGenerator.NBT_DURABILITY);
        Integer dur = pdc.get(key, PersistentDataType.INTEGER);
        return dur != null ? dur : -1;
    }

    /**
     * ItemStackの耐久度を設定する。
     */
    public void setDurability(ItemStack item, int durability) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, ItemGenerator.NBT_DURABILITY);
        pdc.set(key, PersistentDataType.INTEGER, Math.max(0, durability));
        item.setItemMeta(meta);
    }

    /**
     * アイテムIDとエフェクトタイプ名から効果値を計算する。
     */
    public double calculateEffect(String itemId, String effectTypeName, int quality) {
        ItemDefinition def = plugin.getItemsConfig().getItem(itemId);
        if (def == null) return 0;
        for (EffectDefinition eff : def.getEffects()) {
            if (eff.getType().name().equals(effectTypeName)) {
                return eff.calculateValue(quality);
            }
        }
        return 0;
    }

    /**
     * MIMIC買取価格を計算する。
     * 買取価格 = 基本価格 × (0.5 + 品質/100 × 0.5)
     */
    public long getMimicPrice(String itemId, int quality) {
        long base = plugin.getItemsConfig().getMimicBasePrice(itemId);
        if (base <= 0) return 0;
        double multiplier = 0.5 + (quality / 100.0) * 0.5;
        return Math.round(base * multiplier);
    }

    /**
     * ItemStackからMIMIC買取価格を計算する。
     */
    public long getMimicPrice(ItemStack item) {
        String itemId = getItemId(item);
        int quality = getQuality(item);
        if (itemId == null || quality < 0) return 0;
        return getMimicPrice(itemId, quality);
    }

    /**
     * アイテム定義を返す。
     */
    public ItemDefinition getDefinition(ItemStack item) {
        String id = getItemId(item);
        if (id == null) return null;
        return plugin.getItemsConfig().getItem(id);
    }

    /**
     * アイテム定義をIDで返す。
     */
    public ItemDefinition getDefinition(String itemId) {
        return plugin.getItemsConfig().getItem(itemId);
    }
}
