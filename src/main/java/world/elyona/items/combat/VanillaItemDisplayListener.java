package world.elyona.items.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.api.ItemManager;

/**
 * プレイヤーの所持品に含まれる「バニラの」武器・防具を検知し、
 * WeaponDisplay/ArmorDisplayでElyona独自の攻撃力・防御力表示に統一する。
 * ElyonaItems製の特殊アイテム（base_itemがDIAMOND_CHESTPLATE等の場合も含む）は
 * ItemGenerator側で既に独自の効果・属性を設定済みのため、ここでは対象外とする
 * （対象外にしないと、素材の見た目だけを見て上書きが二重にかかってしまう）。
 * 入手経路（クラフト・チェストからの取得・拾得・ログイン時の既存所持品）を
 * 個別に追うのではなく、変化が起きうるタイミングでインベントリ全体を再スキャンする。
 */
public class VanillaItemDisplayListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final ItemManager itemManager;
    private final WeaponDisplay weaponDisplay;
    private final ArmorDisplay armorDisplay;

    public VanillaItemDisplayListener(ElyonaItemsPlugin plugin, WeaponDisplay weaponDisplay, ArmorDisplay armorDisplay) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.weaponDisplay = weaponDisplay;
        this.armorDisplay = armorDisplay;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> scanInventory(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> scanInventory(player));
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTask(plugin, () -> scanInventory(player));
    }

    private void scanInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (itemManager.isElyonaItem(item)) continue;
            weaponDisplay.applyElyonaLore(item);
            armorDisplay.applyElyonaLore(item);
        }
    }
}
