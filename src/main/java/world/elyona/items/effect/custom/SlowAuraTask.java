package world.elyona.items.effect.custom;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

/**
 * 周囲の敵にSlownessを定期付与するオーラタスク。
 */
public class SlowAuraTask extends BukkitRunnable {

    private static final int SLOW_DURATION = 40; // 2秒
    private final Player player;
    private final int radius;
    private final int level; // 1始まり

    public SlowAuraTask(Player player, int radius, int level) {
        this.player = player;
        this.radius = radius;
        this.level = level;
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

        PotionEffect slowEffect = new PotionEffect(
                PotionEffectType.SLOWNESS,
                SLOW_DURATION,
                level - 1, // amplifierは0始まり
                true,
                false,
                false
        );

        for (Entity entity : nearby) {
            if (entity instanceof LivingEntity living) {
                living.addPotionEffect(slowEffect);
            }
        }
    }
}
