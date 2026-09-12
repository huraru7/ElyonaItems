package world.elyona.items.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.model.EffectDefinition;
import world.elyona.items.model.ItemDefinition;
import world.elyona.items.model.Rarity;
import world.elyona.items.quality.QualityGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * ElyonaItemsのアイテムを生成するクラス。
 */
public class ItemGenerator {

    private final ElyonaItemsPlugin plugin;

    public static final String NBT_ITEM_ID = "ElyonaItemId";
    public static final String NBT_QUALITY = "ElyonaQuality";
    public static final String NBT_DURABILITY = "ElyonaDurability";

    public ItemGenerator(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * ランダム品質でアイテムを生成する。
     */
    public ItemStack generate(String itemId) {
        ItemDefinition def = plugin.getItemsConfig().getItem(itemId);
        if (def == null) {
            throw new IllegalArgumentException("アイテムIDが見つかりません: " + itemId);
        }
        int quality = QualityGenerator.generate(def.getRarity());
        return generate(def, quality);
    }

    /**
     * 品質を指定してアイテムを生成する。
     */
    public ItemStack generate(String itemId, int quality) {
        ItemDefinition def = plugin.getItemsConfig().getItem(itemId);
        if (def == null) {
            throw new IllegalArgumentException("アイテムIDが見つかりません: " + itemId);
        }
        quality = QualityGenerator.clamp(def.getRarity(), quality);
        return generate(def, quality);
    }

    /**
     * ItemDefinitionと品質値からItemStackを生成する。
     */
    public ItemStack generate(ItemDefinition def, int quality) {
        ItemStack item = new ItemStack(def.getBaseItem());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // 表示名設定
        String rarityColor = def.getRarity().getColorCode();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(
                rarityColor + "§l" + def.getDisplay()
        ).decoration(TextDecoration.ITALIC, false));

        // Lore設定
        meta.lore(buildLore(def, quality));

        // NBTタグ設定
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey idKey = new NamespacedKey(plugin, NBT_ITEM_ID);
        NamespacedKey qualityKey = new NamespacedKey(plugin, NBT_QUALITY);
        pdc.set(idKey, PersistentDataType.STRING, def.getId());
        pdc.set(qualityKey, PersistentDataType.INTEGER, quality);

        // 耐久度設定
        if (def.hasDurability()) {
            NamespacedKey durKey = new NamespacedKey(plugin, NBT_DURABILITY);
            pdc.set(durKey, PersistentDataType.INTEGER, def.getMaxDurability());
        }

        // バニラの耐久度表示を無効化
        meta.setUnbreakable(true);

        // ベース素材（ダイヤ防具・ネザライト防具等）が本来持つ防御力・耐久・ノックバック耐性等の
        // 属性補正を無効化する。効果はElyonaが定義したエフェクトのみにする。
        // 注: 空のMultimapを渡すだけだとPaperがデフォルト属性を使う扱いにしてしまうため、
        // 関係する属性それぞれに明示的に0を設定する必要がある。
        meta.setAttributeModifiers(zeroOutDefaultAttributes(def.getBaseItem()));

        item.setItemMeta(meta);
        return item;
    }

    /**
     * ベース素材が本来持つデフォルト属性（防具の防御力・防具強度・ノックバック耐性、
     * 武器の攻撃力・攻撃速度）を、該当スロットに対して明示的に0で上書きするMultimapを作る。
     * 防具・武器のいずれでもない素材（ポーション等）はそのまま空のMultimapを返す。
     */
    private Multimap<Attribute, AttributeModifier> zeroOutDefaultAttributes(Material baseItem) {
        Multimap<Attribute, AttributeModifier> modifiers = HashMultimap.create();
        String name = baseItem.name();

        EquipmentSlotGroup slotGroup;
        List<Attribute> attributesToZero = new ArrayList<>();

        if (name.endsWith("_HELMET")) {
            slotGroup = EquipmentSlotGroup.HEAD;
            attributesToZero.add(Attribute.GENERIC_ARMOR);
            attributesToZero.add(Attribute.GENERIC_ARMOR_TOUGHNESS);
            attributesToZero.add(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        } else if (name.endsWith("_CHESTPLATE")) {
            slotGroup = EquipmentSlotGroup.CHEST;
            attributesToZero.add(Attribute.GENERIC_ARMOR);
            attributesToZero.add(Attribute.GENERIC_ARMOR_TOUGHNESS);
            attributesToZero.add(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        } else if (name.endsWith("_LEGGINGS")) {
            slotGroup = EquipmentSlotGroup.LEGS;
            attributesToZero.add(Attribute.GENERIC_ARMOR);
            attributesToZero.add(Attribute.GENERIC_ARMOR_TOUGHNESS);
            attributesToZero.add(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        } else if (name.endsWith("_BOOTS")) {
            slotGroup = EquipmentSlotGroup.FEET;
            attributesToZero.add(Attribute.GENERIC_ARMOR);
            attributesToZero.add(Attribute.GENERIC_ARMOR_TOUGHNESS);
            attributesToZero.add(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        } else if (name.endsWith("_SWORD") || name.endsWith("_AXE")
                || baseItem == Material.TRIDENT || baseItem == Material.MACE) {
            slotGroup = EquipmentSlotGroup.HAND;
            attributesToZero.add(Attribute.GENERIC_ATTACK_DAMAGE);
            attributesToZero.add(Attribute.GENERIC_ATTACK_SPEED);
        } else {
            // 防具・武器以外（ポーション・蜂蜜瓶・TNT等）はデフォルト属性を持たないため何もしない
            return modifiers;
        }

        for (Attribute attribute : attributesToZero) {
            modifiers.put(attribute, new AttributeModifier(
                    new NamespacedKey(plugin, "elyona_zero_" + attribute.name().toLowerCase()),
                    0.0, AttributeModifier.Operation.ADD_NUMBER, slotGroup));
        }
        return modifiers;
    }

    private List<Component> buildLore(ItemDefinition def, int quality) {
        List<Component> lore = new ArrayList<>();
        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();

        // レアリティ
        lore.add(ser.deserialize("§8レアリティ: " + def.getRarity().getDisplayName())
                .decoration(TextDecoration.ITALIC, false));

        // 品質値
        String qualityColor = getQualityColor(quality);
        lore.add(ser.deserialize("§8品質: " + qualityColor + quality)
                .decoration(TextDecoration.ITALIC, false));

        // カテゴリ
        lore.add(ser.deserialize("§8カテゴリ: §7" + getCategoryDisplay(def))
                .decoration(TextDecoration.ITALIC, false));

        lore.add(Component.empty());

        // エフェクト説明
        for (EffectDefinition effect : def.getEffects()) {
            String effectLine = buildEffectLine(effect, quality);
            if (!effectLine.isEmpty()) {
                lore.add(ser.deserialize("§b• " + effectLine).decoration(TextDecoration.ITALIC, false));
            }
        }

        // アフターエフェクト
        if (!def.getAfterEffects().isEmpty()) {
            lore.add(Component.empty());
            lore.add(ser.deserialize("§c副作用:").decoration(TextDecoration.ITALIC, false));
            for (EffectDefinition effect : def.getAfterEffects()) {
                String effectLine = buildEffectLine(effect, quality);
                if (!effectLine.isEmpty()) {
                    lore.add(ser.deserialize("§c• " + effectLine).decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        lore.add(Component.empty());

        // 耐久度
        if (def.hasDurability()) {
            lore.add(ser.deserialize("§7耐久度: §f" + def.getMaxDurability() + "/" + def.getMaxDurability())
                    .decoration(TextDecoration.ITALIC, false));
        }

        // フレーバーテキスト
        if (!def.getLoreFlavor().isEmpty()) {
            lore.add(Component.empty());
            lore.add(ser.deserialize("§o§8" + def.getLoreFlavor()).decoration(TextDecoration.ITALIC, false));
        }

        return lore;
    }

    private String buildEffectLine(EffectDefinition effect, int quality) {
        return switch (effect.getType()) {
            case SPEED -> {
                int val = (int) Math.round(effect.calculateValue(quality));
                yield "移動速度 +" + val + "%";
            }
            case JUMP_BOOST -> {
                int amp = effect.calculateAmplifier(quality);
                yield "跳躍力 Lv." + (amp + 1);
            }
            case SLOW_FALLING -> "落下軽減";
            case DOLPHINS_GRACE -> "水中加速";
            case STRENGTH -> {
                int val = (int) Math.round(effect.calculateValue(quality));
                yield "攻撃力 +" + val;
            }
            case RESISTANCE -> {
                int val = (int) Math.round(effect.calculateValue(quality));
                yield "防御力 +" + val;
            }
            case KNOCKBACK_RESISTANCE -> "ノックバック耐性";
            case FIRE_RESISTANCE -> "火炎耐性";
            case NIGHT_VISION -> "暗視";
            case REGENERATION -> {
                int amp = effect.calculateAmplifier(quality);
                yield "体力再生 Lv." + (amp + 1);
            }
            case SATURATION -> "満腹度維持";
            case HASTE -> {
                int amp = effect.calculateAmplifier(quality);
                yield "採掘速度 Lv." + (amp + 1);
            }
            case LUCK -> {
                int amp = effect.calculateAmplifier(quality);
                yield "幸運 Lv." + (amp + 1);
            }
            case INSTANT_HEALTH -> {
                int val = (int) Math.round(effect.calculateValue(quality));
                yield "即時回復 +" + val + "HP";
            }
            case SLOWNESS -> {
                int amp = effect.calculateAmplifier(quality);
                yield "移動低下 Lv." + (amp + 1);
            }
            case GLOWING_MOBS -> {
                double radius = effect.calculateRadius(quality);
                yield String.format("生体探知 (半径 %.0fm)", radius);
            }
            case SCAN_PULSE -> {
                double radius = effect.calculateRadius(quality);
                yield String.format("エリアスキャン (半径 %.0fm)", radius);
            }
            case GRAVITY_PULL -> {
                double radius = effect.calculateRadius(quality);
                yield String.format("重力引き寄せ (半径 %.0fm)", radius);
            }
            case AREA_MINE -> "3x3x3 範囲採掘 (ドロップ2倍)";
            case INVENTORY_COMPRESS -> "アイテム自動スタック圧縮";
            case SLOW_AURA -> {
                double radius = effect.calculateRadius(quality);
                int level = effect.calculateSlowLevel(quality);
                yield String.format("スロウオーラ Lv.%d (半径 %.0fm)", level, radius);
            }
        };
    }

    private String getQualityColor(int quality) {
        if (quality >= 90) return "§d";
        if (quality >= 70) return "§6";
        if (quality >= 50) return "§a";
        if (quality >= 30) return "§e";
        return "§7";
    }

    private String getCategoryDisplay(ItemDefinition def) {
        return switch (def.getCategory()) {
            case MOVEMENT -> "移動";
            case COMBAT -> "戦闘";
            case SENSING -> "感知";
            case PRODUCTION -> "生産";
            case SURVIVAL -> "生存";
            case SPECIAL -> "特殊";
        };
    }

    /**
     * Loreの耐久度表示を更新する。
     */
    public void updateDurabilityLore(ItemStack item, int current, int max) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        List<Component> lore = meta.lore();
        if (lore == null) return;

        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();
        Component durabilityEntry = ser.deserialize("§7耐久度: §f" + current + "/" + max)
                .decoration(TextDecoration.ITALIC, false);

        for (int i = 0; i < lore.size(); i++) {
            String plain = LegacyComponentSerializer.legacySection().serialize(lore.get(i));
            if (plain.contains("耐久度:")) {
                lore.set(i, durabilityEntry);
                meta.lore(lore);
                item.setItemMeta(meta);
                return;
            }
        }
    }
}
