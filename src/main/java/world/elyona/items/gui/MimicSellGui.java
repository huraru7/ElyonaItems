package world.elyona.items.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import world.elyona.economy.ElyonaEconomyPlugin;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.api.ItemManager;
import world.elyona.items.model.ItemDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * MIMICへのアイテム売却GUI。
 */
public class MimicSellGui implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final ItemManager itemManager;
    private static final String GUI_TITLE = "§0§lMIMIC 買取";

    public MimicSellGui(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    /**
     * プレイヤーにMIMIC売却GUIを開く。
     * 手持ちのElyonaItemsアイテムを表示する。
     */
    public void open(Player player) {
        // プレイヤーのインベントリからElyonaItemsを収集
        List<ItemWithSlot> elyonaItems = new ArrayList<>();
        var inv = player.getInventory();

        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            if (!itemManager.isElyonaItem(item)) continue;
            elyonaItems.add(new ItemWithSlot(item, i));
        }

        // GUIを作成 (9の倍数、最大54スロット。所持品が空でも1行だけ開いて空である旨を表示する)
        int rows = elyonaItems.isEmpty() ? 1 : Math.min(6, (elyonaItems.size() / 9) + 1);
        Inventory gui = Bukkit.createInventory(null, rows * 9,
                Component.text(GUI_TITLE));

        if (elyonaItems.isEmpty()) {
            gui.setItem(4, createEmptyNotice());
        } else {
            for (int i = 0; i < elyonaItems.size() && i < rows * 9; i++) {
                ItemStack original = elyonaItems.get(i).item;
                ItemStack displayItem = createDisplayItem(original);
                gui.setItem(i, displayItem);
            }
        }

        player.openInventory(gui);
    }

    /**
     * 売却可能なアイテムを何も所持していない場合に表示する案内アイテム。
     */
    private ItemStack createEmptyNotice() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();
        meta.displayName(ser.deserialize("&c売れるアイテムがありません")
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(ser.deserialize("&7超技術アイテムを所持していないようです")
                .decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * GUI表示用にアイテムに価格情報を追加したコピーを作る。
     */
    private ItemStack createDisplayItem(ItemStack original) {
        ItemStack display = original.clone();
        ItemMeta meta = display.getItemMeta();
        if (meta == null) return display;

        String itemId = itemManager.getItemId(original);
        int quality = itemManager.getQuality(original);
        long price = itemManager.getMimicPrice(itemId, quality);

        ItemDefinition def = itemManager.getDefinition(original);
        String itemName = def != null ? def.getDisplay() : "不明なアイテム";

        List<Component> existingLore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        LegacyComponentSerializer ser = LegacyComponentSerializer.legacyAmpersand();

        existingLore.add(Component.empty());
        existingLore.add(ser.deserialize("§e▶ クリックで売却")
                .decoration(TextDecoration.ITALIC, false));
        existingLore.add(ser.deserialize("§f買取価格: §6Cr " + String.format("%,d", price))
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(existingLore);
        display.setItemMeta(meta);
        return display;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // GUIタイトルチェック
        String title = LegacyComponentSerializer.legacySection()
                .serialize(event.getView().title());
        if (!title.equals(GUI_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!itemManager.isElyonaItem(clicked)) return;

        String itemId = itemManager.getItemId(clicked);
        int quality = itemManager.getQuality(clicked);
        long price = itemManager.getMimicPrice(itemId, quality);

        ItemDefinition def = itemManager.getDefinition(clicked);
        String itemName = def != null ? def.getDisplay() : "不明なアイテム";

        // プレイヤーのインベントリから対象アイテムを探して削除
        boolean sold = removeItemFromPlayer(player, itemId, quality);

        if (!sold) {
            player.sendMessage("§7[MIMIC] §fアイテムが見つかりませんでした。");
            player.closeInventory();
            return;
        }

        // Cred付与
        boolean credited = creditPlayer(player, price);

        // MIMICの口調で通知
        if (credited) {
            player.sendMessage("§7[MIMIC] §f「§e" + itemName
                    + "§f」§7(品質: §e" + quality + "§7) を §6Cr " + String.format("%,d", price)
                    + " §fで買い取りました。");
        } else {
            player.sendMessage("§7[MIMIC] §f買取処理中にエラーが発生しました。運営にお問い合わせください。");
        }

        player.closeInventory();
    }

    /**
     * ElyonaEconomy経由でプレイヤーにCredを付与する。ElyonaEconomyが見つからない場合はfalseを返す。
     */
    private boolean creditPlayer(Player player, long price) {
        var economyPlugin = Bukkit.getPluginManager().getPlugin("ElyonaEconomy");
        if (!(economyPlugin instanceof ElyonaEconomyPlugin economy)) {
            plugin.getLogger().warning("ElyonaEconomyが見つからないため、売却報酬を付与できませんでした。");
            return false;
        }
        economy.getEconomyCache().addBalance(player.getUniqueId(), price);
        return true;
    }

    /**
     * プレイヤーのインベントリから指定IDと品質のアイテムを1つ削除する。
     */
    private boolean removeItemFromPlayer(Player player, String targetId, int targetQuality) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            String id = itemManager.getItemId(item);
            int q = itemManager.getQuality(item);
            if (targetId.equals(id) && targetQuality == q) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inv.setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    private record ItemWithSlot(ItemStack item, int slot) {}
}
