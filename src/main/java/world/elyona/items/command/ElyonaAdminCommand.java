package world.elyona.items.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.model.ItemDefinition;
import world.elyona.items.model.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * /elyona 管理コマンド。
 * elyona.admin 権限が必要。
 */
public class ElyonaAdminCommand implements CommandExecutor, TabCompleter {

    private final ElyonaItemsPlugin plugin;

    public ElyonaAdminCommand(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elyona.admin")) {
            sender.sendMessage("§cこのコマンドを使用する権限がありません。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sendHelp(sender);
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c使用方法: /elyona give <プレイヤー> <アイテムID> [品質/mythic]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤー " + args[1] + " が見つかりません。");
            return true;
        }

        String itemId = args[2];
        ItemDefinition def = plugin.getItemsConfig().getItem(itemId);
        if (def == null) {
            sender.sendMessage("§cアイテムID '" + itemId + "' が見つかりません。");
            sender.sendMessage("§7利用可能: " + String.join(", ", plugin.getItemsConfig().getItemIds()));
            return true;
        }

        // Mythicアイテムの制限
        if (def.getRarity() == Rarity.MYTHIC && !sender.hasPermission("elyona.admin")) {
            sender.sendMessage("§cMythicアイテムの配布にはelyona.admin権限が必要です。");
            return true;
        }

        ItemStack item;
        if (args.length >= 4 && args[3].equalsIgnoreCase("mythic")) {
            item = plugin.getItemGenerator().generate(itemId, 100);
        } else if (args.length >= 4) {
            try {
                int quality = Integer.parseInt(args[3]);
                quality = Math.max(0, Math.min(100, quality));
                item = plugin.getItemGenerator().generate(itemId, quality);
            } catch (NumberFormatException e) {
                sender.sendMessage("§c品質は0〜100の整数か 'mythic' を指定してください。");
                return true;
            }
        } else {
            item = plugin.getItemGenerator().generate(itemId);
        }

        target.getInventory().addItem(item);
        int quality = plugin.getItemManager().getQuality(item);
        sender.sendMessage("§a" + target.getName() + " に §e" + def.getDisplay()
                + " §7(品質: " + quality + ") §aを付与しました。");
        target.sendMessage("§7[ElyonaItems] §e" + def.getDisplay()
                + " §7(品質: " + quality + ") §fを受け取りました。");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getItemsConfig().load();
        sender.sendMessage("§a[ElyonaItems] 設定をリロードしました。");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6[ElyonaItems] コマンド一覧:");
        sender.sendMessage("§e /elyona give <プレイヤー> <アイテムID> [品質]  §7- アイテムを付与");
        sender.sendMessage("§e /elyona reload  §7- 設定をリロード");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (!sender.hasPermission("elyona.admin")) return List.of();

        if (args.length == 1) {
            return List.of("give", "reload");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            return new ArrayList<>(plugin.getItemsConfig().getItemIds());
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give")) {
            List<String> qualities = new ArrayList<>();
            for (int i = 0; i <= 100; i += 10) qualities.add(String.valueOf(i));
            qualities.add("mythic");
            return qualities;
        }
        return List.of();
    }
}
