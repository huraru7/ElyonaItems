package world.elyona.items.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.api.ItemManager;
import world.elyona.items.effect.EffectApplier;
import world.elyona.items.model.ItemDefinition;
import world.elyona.items.model.ItemType;

/**
 * 装備スロットのElyonaItemsアイテムを監視し、エフェクトを管理するリスナー。
 */
public class EquipmentListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final ItemManager itemManager;
    private final EffectApplier effectApplier;

    public EquipmentListener(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.effectApplier = plugin.getEffectApplier();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 遅延実行でインベントリが完全にロードされた後に処理
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                refreshAllEquipmentEffects(player);
            }
        }, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // 全エフェクトを除去してカスタムエフェクトをクリーンアップ
        removeAllEquipmentEffects(player);
        plugin.getCustomEffectManager().cleanup(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // アーマースロットへのクリックを検知
        boolean isArmorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean isArmorCraft = event.getInventory().getType() == InventoryType.CRAFTING
                && isArmorSlotIndex(event.getSlot());

        if (!isArmorSlot && !isArmorCraft) return;

        // 変更後にエフェクトを再適用 (1tick後)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                refreshAllEquipmentEffects(player);
            }
        }, 1L);
    }

    /**
     * プレイヤーの全装備スロットを確認してエフェクトを再適用する。
     */
    public void refreshAllEquipmentEffects(Player player) {
        // まず全装備エフェクトを除去
        removeAllEquipmentEffects(player);

        PlayerInventory inv = player.getInventory();
        ItemStack[] armorContents = inv.getArmorContents();

        for (ItemStack item : armorContents) {
            if (!itemManager.isElyonaItem(item)) continue;
            ItemDefinition def = itemManager.getDefinition(item);
            if (def == null || def.getType() != ItemType.EQUIPMENT) continue;
            int quality = itemManager.getQuality(item);
            effectApplier.applyEquipmentEffects(player, def, quality);
        }
    }

    /**
     * プレイヤーの全ElyonaItemsエフェクトを除去する。
     */
    private void removeAllEquipmentEffects(Player player) {
        PlayerInventory inv = player.getInventory();
        ItemStack[] armorContents = inv.getArmorContents();

        for (ItemStack item : armorContents) {
            if (!itemManager.isElyonaItem(item)) continue;
            ItemDefinition def = itemManager.getDefinition(item);
            if (def == null || def.getType() != ItemType.EQUIPMENT) continue;
            int quality = itemManager.getQuality(item);
            effectApplier.removeEquipmentEffects(player, def, quality);
        }
    }

    private boolean isArmorSlotIndex(int slot) {
        return slot >= 36 && slot <= 39;
    }
}
