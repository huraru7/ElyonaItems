package world.elyona.items.combat;

import com.google.common.collect.HashMultimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * バニラ防具が本来持つ防御力・耐久・ノックバック耐性等の属性補正を、
 * Elyona独自の防御力数値で完全に上書きする。
 * バニラのAttributeModifiersを明示的に設定することで素材由来のデフォルト補正を無効化し、
 * GENERIC_ARMORにElyonaの数値だけを設定する（ダメージ計算自体はバニラの防具計算式をそのまま使う）。
 */
public class ArmorDisplay {

    private static final LegacyComponentSerializer SER = LegacyComponentSerializer.legacySection();

    /** 素材プレフィックスごとの防御力（1部位あたり）。バランス調整前の叩き台の値。 */
    private static final Map<String, Double> DEFENSE_BY_MATERIAL_PREFIX = Map.ofEntries(
            Map.entry("LEATHER", 1.0),
            Map.entry("GOLDEN", 1.0),
            Map.entry("CHAINMAIL", 2.0),
            Map.entry("IRON", 3.0),
            Map.entry("DIAMOND", 5.0),
            Map.entry("NETHERITE", 7.0),
            Map.entry("TURTLE", 2.0)
    );

    private final Plugin plugin;
    private final NamespacedKey appliedKey;

    public ArmorDisplay(Plugin plugin) {
        this.plugin = plugin;
        this.appliedKey = new NamespacedKey(plugin, "armor_lore_applied");
    }

    public static boolean isArmor(Material type) {
        String name = type.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    /** 未処理の防具であればElyona独自の防御力に上書きする。処理済みなら何もしない。 */
    public void applyElyonaLore(ItemStack item) {
        if (item == null || !isArmor(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (meta.getPersistentDataContainer().has(appliedKey, PersistentDataType.BYTE)) return;

        EquipmentSlotGroup slotGroup = resolveSlotGroup(item.getType());
        if (slotGroup == null) return;

        double defense = resolveDefense(item.getType());

        // バニラの素材由来のデフォルト補正（防御力・耐久・ノックバック耐性等）を
        // 明示的なAttributeModifiers設定によって完全に置き換える（未指定の属性は0になる）
        var modifiers = HashMultimap.<Attribute, AttributeModifier>create();
        modifiers.put(Attribute.GENERIC_ARMOR, new AttributeModifier(
                new NamespacedKey(plugin, "elyona_armor"), defense, AttributeModifier.Operation.ADD_NUMBER, slotGroup));
        meta.setAttributeModifiers(modifiers);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(SER.deserialize("§8━━━━━━━━━━"));
        lore.add(SER.deserialize(String.format("§7防御力: §b%.1f", defense)));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(appliedKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    private EquipmentSlotGroup resolveSlotGroup(Material type) {
        String name = type.name();
        if (name.endsWith("_HELMET")) return EquipmentSlotGroup.HEAD;
        if (name.endsWith("_CHESTPLATE")) return EquipmentSlotGroup.CHEST;
        if (name.endsWith("_LEGGINGS")) return EquipmentSlotGroup.LEGS;
        if (name.endsWith("_BOOTS")) return EquipmentSlotGroup.FEET;
        return null;
    }

    private double resolveDefense(Material type) {
        String name = type.name();
        for (var entry : DEFENSE_BY_MATERIAL_PREFIX.entrySet()) {
            if (name.startsWith(entry.getKey())) return entry.getValue();
        }
        return 0.0;
    }
}
