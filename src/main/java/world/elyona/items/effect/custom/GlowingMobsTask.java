package world.elyona.items.effect.custom;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 周囲のMobにGlowingエフェクトを定期付与するタスク。
 */
public class GlowingMobsTask extends BukkitRunnable {

    private static final int GLOW_DURATION = 40; // 2秒
    private final Player player;
    private final int radius;

    public GlowingMobsTask(Player player, int radius) {
        this.player = player;
        this.radius = radius;
    }

    @Override
    public void run() {
        if (!player.isOnline() || player.isDead()) {
            cancel();
            return;
        }

        Collection<Entity> nearby = player.getWorld().getNearbyEntities(
                player.getLocation(), radius, radius, radius,
                e -> e instanceof LivingEntity && e != player
        );

        PotionEffect glowEffect = new PotionEffect(
                PotionEffectType.GLOWING,
                GLOW_DURATION,
                0,
                true,
                false,
                false
        );

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity living) {
                living.addPotionEffect(glowEffect);
            }
        }
    }
}
