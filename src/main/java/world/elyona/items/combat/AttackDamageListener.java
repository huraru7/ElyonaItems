package world.elyona.items.combat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * バニラのダメージ計算（会心・エンチャント込み）が終わった後に、
 * Elyonaの攻撃力/防御力ボーナス（フラット加算）を適用する。
 * 属性ダメージ等の分岐は行わず、ダメージは常に単一の数値のまま。
 */
public class AttackDamageListener implements Listener {

    private final PlayerStatManager statManager;

    public AttackDamageListener(PlayerStatManager statManager) {
        this.statManager = statManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        double damage = event.getDamage();

        // 攻撃側（プレイヤー）の攻撃力ボーナスを加算
        if (event instanceof EntityDamageByEntityEvent byEntity
                && byEntity.getDamager() instanceof Player attacker) {
            damage += statManager.getTotalAttackBonus(attacker.getUniqueId());
        }

        // 被弾側（プレイヤー）の防御力ボーナスを減算
        if (event.getEntity() instanceof Player victim) {
            damage -= statManager.getTotalDefenseBonus(victim.getUniqueId());
        }

        event.setDamage(Math.max(0.0, damage));
    }
}
