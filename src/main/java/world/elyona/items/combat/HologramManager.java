package world.elyona.items.combat;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitTask;
import world.elyona.items.ElyonaItemsPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 敵の頭上にHPバーのみのホログラムを表示する。
 * 状態異常がかかっている敵の表示（記号付き）は属性システム実装時に拡張予定。
 *
 * 表示条件: 直近でHPが変動した敵のみ（常時全mob表示はしない）。
 * 一定時間ダメージが無い、または敵が死亡・無効化された場合はホログラムを消す。
 */
public class HologramManager {

    private static final long TIMEOUT_TICKS = 100L; // 無更新5秒でホログラムを消す
    private static final int BAR_LENGTH = 10;

    private final ElyonaItemsPlugin plugin;
    private final Map<UUID, ArmorStand> holograms = new HashMap<>();
    private final Map<UUID, Long> lastDamageTick = new HashMap<>();
    private BukkitTask task;

    public HologramManager(ElyonaItemsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 2L);
    }

    public void stop() {
        if (task != null) task.cancel();
        holograms.values().forEach(Entity::remove);
        holograms.clear();
        lastDamageTick.clear();
    }

    /** ダメージを受けた敵の通知を受け、ホログラムを生成/更新する。ダメージ確定後（1tick後）に呼ぶこと。 */
    public void notifyDamaged(LivingEntity entity) {
        UUID id = entity.getUniqueId();
        lastDamageTick.put(id, entity.getWorld().getFullTime());

        ArmorStand stand = holograms.get(id);
        if (stand == null || !stand.isValid()) {
            stand = spawnHologram(entity);
            holograms.put(id, stand);
        }
        updateText(stand, entity);
        stand.teleport(hologramLocation(entity));
    }

    private void tick() {
        Iterator<Map.Entry<UUID, ArmorStand>> it = holograms.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ArmorStand> entry = it.next();
            UUID mobId = entry.getKey();
            ArmorStand stand = entry.getValue();

            Entity mob = Bukkit.getEntity(mobId);
            if (!(mob instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
                stand.remove();
                it.remove();
                lastDamageTick.remove(mobId);
                continue;
            }

            long last = lastDamageTick.getOrDefault(mobId, 0L);
            if (living.getWorld().getFullTime() - last > TIMEOUT_TICKS) {
                stand.remove();
                it.remove();
                lastDamageTick.remove(mobId);
                continue;
            }

            stand.teleport(hologramLocation(living));
        }
    }

    private ArmorStand spawnHologram(LivingEntity entity) {
        return entity.getWorld().spawn(hologramLocation(entity), ArmorStand.class, as -> {
            as.setMarker(true);
            as.setInvisible(true);
            as.setGravity(false);
            as.setSmall(true);
            as.setBasePlate(false);
            as.setInvulnerable(true);
            as.setPersistent(false);
            as.setCustomNameVisible(true);
        });
    }

    private Location hologramLocation(LivingEntity entity) {
        return entity.getLocation().add(0, entity.getHeight() + 0.3, 0);
    }

    private void updateText(ArmorStand stand, LivingEntity entity) {
        double maxHealth = 20.0;
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) maxHealth = attr.getValue();

        double health = Math.max(0, entity.getHealth());
        int filled = maxHealth > 0
                ? Math.max(0, Math.min(BAR_LENGTH, (int) Math.round(BAR_LENGTH * (health / maxHealth))))
                : 0;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < BAR_LENGTH; i++) {
            bar.append(i < filled ? "§a▮" : "§7▯");
        }

        String text = bar + " §f" + Math.round(health) + "§7/§f" + Math.round(maxHealth);
        stand.customName(LegacyComponentSerializer.legacySection().deserialize(text));
    }
}
