package world.elyona.items.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.effect.custom.CustomEffectManager;

/**
 * INVENTORY_COMPRESSエフェクト: アイテム拾得時に自動スタック圧縮を行うリスナー。
 */
public class InventoryCompressListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final CustomEffectManager customEffectManager;

    public InventoryCompressListener(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.customEffectManager = plugin.getCustomEffectManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!customEffectManager.isInventoryCompressEnabled(player)) return;

        // 1tickあとにインベントリ圧縮を実行 (アイテムがインベントリに入ってから)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            compressInventory(player);
        }, 1L);
    }

    /**
     * インベントリ内の同一アイテムをスタックに圧縮する。
     */
    private void compressInventory(Player player) {
        var inv = player.getInventory();
        ItemStack[] contents = inv.getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;

            int maxStack = item.getMaxStackSize();
            if (item.getAmount() >= maxStack) continue;

            // 同じアイテムを探してスタック
            for (int j = i + 1; j < contents.length; j++) {
                ItemStack other = contents[j];
                if (other == null || other.getType().isAir()) continue;
                if (!item.isSimilar(other)) continue;

                int total = item.getAmount() + other.getAmount();
                if (total <= maxStack) {
                    item.setAmount(total);
                    contents[j] = null;
                } else {
                    item.setAmount(maxStack);
                    other.setAmount(total - maxStack);
                    break;
                }
            }
        }

        inv.setContents(contents);
    }
}
