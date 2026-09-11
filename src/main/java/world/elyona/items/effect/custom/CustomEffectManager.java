package world.elyona.items.effect.custom;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import world.elyona.items.ElyonaItemsPlugin;

import java.util.*;

/**
 * カスタムエフェクト（GLOWING_MOBS, SLOW_AURA等）の管理クラス。
 * プレイヤーごとにタスクをトラッキングし、重複実行を防ぐ。
 */
public class CustomEffectManager {

    private final ElyonaItemsPlugin plugin;

    // プレイヤーUUID → (アイテムID → タスク)
    private final Map<UUID, Map<String, BukkitTask>> glowingMobsTasks = new HashMap<>();
    private final Map<UUID, Map<String, BukkitTask>> slowAuraTasks = new HashMap<>();
    // プレイヤーUUID → 有効なアイテムIDセット
    private final Map<UUID, Set<String>> inventoryCompressEnabled = new HashMap<>();

    public CustomEffectManager(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
    }

    // ━━ GLOWING_MOBS ━━

    public void startGlowingMobs(Player player, int radius, String itemId) {
        UUID uuid = player.getUniqueId();
        glowingMobsTasks.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<String, BukkitTask> tasks = glowingMobsTasks.get(uuid);

        // 既存タスクのラジウスを確認 - 同じitemIdのタスクが既にあればスキップ
        if (tasks.containsKey(itemId)) return;

        int interval = plugin.getConfig().getInt("custom_effect_interval", 20);
        GlowingMobsTask task = new GlowingMobsTask(player, radius);
        BukkitTask bukkitTask = task.runTaskTimer(plugin, 0L, interval);
        tasks.put(itemId, bukkitTask);
    }

    public void stopGlowingMobs(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> tasks = glowingMobsTasks.get(uuid);
        if (tasks == null) return;
        BukkitTask task = tasks.remove(itemId);
        if (task != null) task.cancel();
    }

    public void stopAllGlowingMobs(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> tasks = glowingMobsTasks.remove(uuid);
        if (tasks == null) return;
        tasks.values().forEach(BukkitTask::cancel);
    }

    // ━━ SLOW_AURA ━━

    public void startSlowAura(Player player, int radius, int level, String itemId) {
        UUID uuid = player.getUniqueId();
        slowAuraTasks.computeIfAbsent(uuid, k -> new HashMap<>());
        Map<String, BukkitTask> tasks = slowAuraTasks.get(uuid);

        if (tasks.containsKey(itemId)) return;

        int interval = plugin.getConfig().getInt("custom_effect_interval", 20);
        SlowAuraTask task = new SlowAuraTask(player, radius, level);
        BukkitTask bukkitTask = task.runTaskTimer(plugin, 0L, interval);
        tasks.put(itemId, bukkitTask);
    }

    public void stopSlowAura(Player player, String itemId) {
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> tasks = slowAuraTasks.get(uuid);
        if (tasks == null) return;
        BukkitTask task = tasks.remove(itemId);
        if (task != null) task.cancel();
    }

    public void stopAllSlowAura(Player player) {
        UUID uuid = player.getUniqueId();
        Map<String, BukkitTask> tasks = slowAuraTasks.remove(uuid);
        if (tasks == null) return;
        tasks.values().forEach(BukkitTask::cancel);
    }

    // ━━ INVENTORY_COMPRESS ━━

    public void enableInventoryCompress(Player player, String itemId) {
        inventoryCompressEnabled
                .computeIfAbsent(player.getUniqueId(), k -> new HashSet<>())
                .add(itemId);
    }

    public void disableInventoryCompress(Player player, String itemId) {
        Set<String> set = inventoryCompressEnabled.get(player.getUniqueId());
        if (set != null) set.remove(itemId);
    }

    public boolean isInventoryCompressEnabled(Player player) {
        Set<String> set = inventoryCompressEnabled.get(player.getUniqueId());
        return set != null && !set.isEmpty();
    }

    // ━━ SCAN_PULSE (一回限り) ━━

    public void triggerScanPulse(Player player, int radius, int durationSeconds) {
        int durationTicks = durationSeconds * 20;
        PotionEffect glowEffect = new PotionEffect(
                PotionEffectType.GLOWING,
                durationTicks,
                0, true, false, false
        );

        player.getWorld().getNearbyEntities(
                player.getLocation(), radius, radius, radius,
                e -> e instanceof LivingEntity && e != player
        ).forEach(e -> {
            if (e instanceof LivingEntity living) {
                living.addPotionEffect(glowEffect);
            }
        });
    }

    // ━━ GRAVITY_PULL (一回限り) ━━

    public void triggerGravityPull(Player player, int radius) {
        Location center = player.getLocation();
        player.getWorld().getNearbyEntities(
                center, radius, radius, radius,
                e -> e instanceof LivingEntity && e != player
        ).forEach(e -> {
            if (e instanceof LivingEntity living) {
                Location entityLoc = living.getLocation();
                // 中心方向へのベクトル
                var velocity = center.toVector()
                        .subtract(entityLoc.toVector())
                        .normalize()
                        .multiply(1.5);
                velocity.setY(Math.max(velocity.getY(), 0.3));
                living.setVelocity(velocity);
            }
        });
    }

    // ━━ AREA_MINE (3x3x3, リスナーから呼ばれる) ━━

    public void triggerAreaMine(Player player) {
        // AreaMineの実際の処理はConsumableListenerで実装
        // (ブロックのbreakNaturallyが必要)
    }

    // ━━ クリーンアップ ━━

    public void cleanup(Player player) {
        stopAllGlowingMobs(player);
        stopAllSlowAura(player);
        inventoryCompressEnabled.remove(player.getUniqueId());
    }

    public void cancelAll() {
        glowingMobsTasks.values().forEach(map -> map.values().forEach(BukkitTask::cancel));
        slowAuraTasks.values().forEach(map -> map.values().forEach(BukkitTask::cancel));
        glowingMobsTasks.clear();
        slowAuraTasks.clear();
        inventoryCompressEnabled.clear();
    }
}
