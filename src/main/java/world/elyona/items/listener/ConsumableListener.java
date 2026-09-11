package world.elyona.items.listener;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.api.ItemManager;
import world.elyona.items.effect.EffectApplier;
import world.elyona.items.model.EffectDefinition;
import world.elyona.items.model.EffectType;
import world.elyona.items.model.ItemDefinition;
import world.elyona.items.model.ItemType;

/**
 * 消耗品アイテムの使用を検知し、エフェクトを適用するリスナー。
 */
public class ConsumableListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final ItemManager itemManager;
    private final EffectApplier effectApplier;

    public ConsumableListener(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.effectApplier = plugin.getEffectApplier();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!itemManager.isElyonaItem(item)) return;

        ItemDefinition def = itemManager.getDefinition(item);
        if (def == null || def.getType() != ItemType.CONSUMABLE) return;

        event.setCancelled(true);

        int quality = itemManager.getQuality(item);

        // AREA_MINEは特別処理
        boolean hasAreaMine = def.getEffects().stream()
                .anyMatch(e -> e.getType() == EffectType.AREA_MINE);
        if (hasAreaMine) {
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock != null) {
                handleAreaMine(player, targetBlock);
            }
        }

        // GRAVITY_PULLは特別処理
        boolean hasGravityPull = def.getEffects().stream()
                .anyMatch(e -> e.getType() == EffectType.GRAVITY_PULL);
        if (hasGravityPull) {
            for (EffectDefinition eff : def.getEffects()) {
                if (eff.getType() == EffectType.GRAVITY_PULL) {
                    double radius = eff.calculateRadius(quality);
                    plugin.getCustomEffectManager().triggerGravityPull(player, (int) Math.round(radius));
                }
            }
        }

        // SCAN_PULSEは特別処理
        boolean hasScanPulse = def.getEffects().stream()
                .anyMatch(e -> e.getType() == EffectType.SCAN_PULSE);
        if (hasScanPulse) {
            for (EffectDefinition eff : def.getEffects()) {
                if (eff.getType() == EffectType.SCAN_PULSE) {
                    double radius = eff.calculateRadius(quality);
                    int duration = eff.getDurationSeconds();
                    plugin.getCustomEffectManager().triggerScanPulse(player, (int) Math.round(radius), duration);
                }
            }
        }

        // 通常エフェクトを付与
        effectApplier.applyConsumableEffects(player, def, quality);

        // アイテムを1つ消費
        item.setAmount(item.getAmount() - 1);

        // アフターエフェクト処理
        if (!def.getAfterEffects().isEmpty()) {
            scheduleAfterEffects(player, def, quality);
        }
    }

    /**
     * 3x3x3範囲採掘の処理。
     */
    private void handleAreaMine(Player player, Block center) {
        int range = 1;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    Block block = center.getRelative(dx, dy, dz);
                    if (block.getType().isAir() || !block.getType().isSolid()) continue;
                    // 天然ブロックとして破壊 (ドロップ発生)
                    block.breakNaturally(player.getInventory().getItemInMainHand(), true);
                    // ドロップ量2倍はbreakNaturallyの引数trueで幸運ツール相当
                }
            }
        }
    }

    /**
     * アフターエフェクトを遅延実行する。
     */
    private void scheduleAfterEffects(Player player, ItemDefinition def, int quality) {
        // アフターエフェクトの付与: メインエフェクト終了後
        int mainDurationTicks = def.calculateDurationTicks(quality);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            for (EffectDefinition afterEff : def.getAfterEffects()) {
                var potionType = effectApplier.getPotionEffectType(afterEff.getType());
                if (potionType == null) continue;
                int amp = afterEff.calculateAmplifier(quality);
                int duration = afterEff.getDurationSeconds() > 0
                        ? afterEff.getDurationSeconds() * 20
                        : 600; // デフォルト30秒

                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        potionType, duration, Math.max(0, amp),
                        true, true, true
                ));
            }
        }, mainDurationTicks);
    }
}
