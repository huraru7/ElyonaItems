package world.elyona.items.combat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * バニラ武器（品質・レアリティは付与しない）の攻撃力表記を、
 * ElyonaItemsの他アイテムと統一されたlore装飾スタイルに変更する。
 * バニラ標準のAttributeツールチップは隠し、代わりに独自lore行を追加する。
 */
public class WeaponDisplay {

    private static final LegacyComponentSerializer SER = LegacyComponentSerializer.legacySection();

    private final NamespacedKey appliedKey;

    public WeaponDisplay(Plugin plugin) {
        this.appliedKey = new NamespacedKey(plugin, "weapon_lore_applied");
    }

    public static boolean isWeapon(Material type) {
        String name = type.name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE")
                || type == Material.BOW || type == Material.CROSSBOW
                || type == Material.TRIDENT || type == Material.MACE;
    }

    /** 未処理の武器であればElyona独自のlore表示に置き換える。処理済みなら何もしない。 */
    public void applyElyonaLore(ItemStack item) {
        if (item == null || !isWeapon(item.getType())) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (meta.getPersistentDataContainer().has(appliedKey, PersistentDataType.BYTE)) return;

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(SER.deserialize("§8━━━━━━━━━━"));

        if (hasAttribute(item.getType(), Attribute.GENERIC_ATTACK_DAMAGE)) {
            double damage = sumAttribute(item.getType(), Attribute.GENERIC_ATTACK_DAMAGE, 1.0);
            double speed = sumAttribute(item.getType(), Attribute.GENERIC_ATTACK_SPEED, 4.0);
            lore.add(SER.deserialize(String.format("§7攻撃力: §6%.1f", damage)));
            lore.add(SER.deserialize(String.format("§7攻撃速度: §6%.1f", speed)));
        } else {
            // 弓・クロスボウ等: バニラに固定の攻撃力属性が無いため種別表示のみ
            lore.add(SER.deserialize("§7種別: §b遠距離武器"));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(appliedKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    /** 武器の基礎攻撃力（バニラ属性値）を返す。攻撃力属性を持たない武器（弓等）は0を返す。 */
    public static double getBaseAttackDamage(Material type) {
        if (!hasAttribute(type, Attribute.GENERIC_ATTACK_DAMAGE)) return 0.0;
        return sumAttribute(type, Attribute.GENERIC_ATTACK_DAMAGE, 1.0);
    }

    private static boolean hasAttribute(Material type, Attribute attribute) {
        var mods = type.getDefaultAttributeModifiers(EquipmentSlot.HAND).get(attribute);
        return mods != null && !mods.isEmpty();
    }

    private static double sumAttribute(Material type, Attribute attribute, double base) {
        var mods = type.getDefaultAttributeModifiers(EquipmentSlot.HAND).get(attribute);
        double total = base;
        if (mods != null) {
            for (AttributeModifier mod : mods) {
                if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                    total += mod.getAmount();
                }
            }
        }
        return total;
    }
}
