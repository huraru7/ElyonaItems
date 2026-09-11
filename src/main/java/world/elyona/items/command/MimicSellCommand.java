package world.elyona.items.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.gui.MimicSellGui;

import java.util.List;

/**
 * /mimic sell コマンド。
 */
public class MimicSellCommand implements CommandExecutor, TabCompleter {

    private final ElyonaItemsPlugin plugin;
    private final MimicSellGui sellGui;

    public MimicSellCommand(ElyonaItemsPlugin plugin, MimicSellGui sellGui) {
        this.plugin = plugin;
        this.sellGui = sellGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用できます。");
            return true;
        }

        if (!player.hasPermission("elyona.mimic.sell")) {
            player.sendMessage("§cこのコマンドを使用する権限がありません。");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("sell")) {
            sellGui.open(player);
            return true;
        }

        player.sendMessage("§7使用方法: /mimic sell");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                       String alias, String[] args) {
        if (args.length == 1) {
            return List.of("sell");
        }
        return List.of();
    }
}
