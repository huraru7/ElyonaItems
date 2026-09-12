package world.elyona.items.combat;

import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import world.elyona.items.ElyonaItemsPlugin;

public class MobDamageListener implements Listener {

    private final ElyonaItemsPlugin plugin;
    private final HologramManager hologramManager;

    public MobDamageListener(ElyonaItemsPlugin plugin, HologramManager hologramManager) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        if (living instanceof Player) return;
        if (living instanceof ArmorStand) return;

        // ダメージがHPに確定反映されるのを待ってから読み取る（1tick後）
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (living.isValid() && !living.isDead()) {
                hologramManager.notifyDamaged(living);
            }
        });
    }
}
