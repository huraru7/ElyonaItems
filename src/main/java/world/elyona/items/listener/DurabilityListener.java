package world.elyona.items.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.api.ItemGenerator;
import world.elyona.items.api.ItemManager;
import world.elyona.items.model.ItemDefinition;
import world.elyona.items.model.Rarity;

/**
 * ElyonaItemsアイテムの耐久度を独自管理するリスナー。
 * バニラの耐久度システムをキャンセルし、NBTで管理する。
 */
public class DurabilityListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final ItemManager itemManager;

    public DurabilityListener(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!itemManager.isElyonaItem(item)) return;

        ItemDefinition def = itemManager.getDefinition(item);
        if (def == null) return;

        // バニラ耐久度処理をキャンセル
        event.setCancelled(true);

        // 耐久度なしアイテムは処理しない
        if (!def.hasDurability()) return;

        int currentDurability = itemManager.getDurability(item);
        if (currentDurability < 0) return; // 初期化されていない

        int newDurability = currentDurability - event.getDamage();

        if (newDurability <= 0) {
            // 耐久度切れ: アイテムを破壊
            item.setAmount(0);
            player.sendMessage("§c[ElyonaItems] §7" + def.getDisplay() + " §cが破損して消失しました。");

            // 装備していたら効果を除去
            plugin.getEquipmentListener().refreshAllEquipmentEffects(player);
            return;
        }

        // 耐久度更新
        itemManager.setDurability(item, newDurability);
        plugin.getItemGenerator().updateDurabilityLore(item, newDurability, def.getMaxDurability());
    }
}
