package world.elyona.items.combat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * プレイヤーごとの攻撃力・防御力ボーナス（フラット加算値）を管理する。
 * 装備由来（付け外しで再計算・恒久的）と消費アイテム由来（一時的・期限付き）を分けて保持し、
 * 合計値をダメージ計算・ステータス表示に使う。
 */
public class PlayerStatManager {

    private final Map<UUID, Double> equipmentAttack = new ConcurrentHashMap<>();
    private final Map<UUID, Double> equipmentDefense = new ConcurrentHashMap<>();
    private final Map<UUID, Double> consumableAttack = new ConcurrentHashMap<>();
    private final Map<UUID, Double> consumableDefense = new ConcurrentHashMap<>();

    /** 装備からの攻撃力ボーナス合計を設定する（装備の付け外しのたびに再計算して呼ぶ） */
    public void setEquipmentAttackBonus(UUID uuid, double value) {
        equipmentAttack.put(uuid, value);
    }

    /** 装備からの防御力ボーナス合計を設定する */
    public void setEquipmentDefenseBonus(UUID uuid, double value) {
        equipmentDefense.put(uuid, value);
    }

    public double getEquipmentAttackBonus(UUID uuid) {
        return equipmentAttack.getOrDefault(uuid, 0.0);
    }

    public double getEquipmentDefenseBonus(UUID uuid) {
        return equipmentDefense.getOrDefault(uuid, 0.0);
    }

    /** 装備+消費アイテムを合わせた攻撃力ボーナス合計 */
    public double getTotalAttackBonus(UUID uuid) {
        return equipmentAttack.getOrDefault(uuid, 0.0) + consumableAttack.getOrDefault(uuid, 0.0);
    }

    /** 装備+消費アイテムを合わせた防御力ボーナス合計 */
    public double getTotalDefenseBonus(UUID uuid) {
        return equipmentDefense.getOrDefault(uuid, 0.0) + consumableDefense.getOrDefault(uuid, 0.0);
    }

    /** 消費アイテムによる一時的な攻撃力ボーナスを付与し、期限が来たら自動で取り消す */
    public void addTemporaryAttackBonus(Plugin plugin, UUID uuid, double value, long durationTicks) {
        consumableAttack.merge(uuid, value, Double::sum);
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeTemporary(consumableAttack, uuid, value), durationTicks);
    }

    /** 消費アイテムによる一時的な防御力ボーナスを付与し、期限が来たら自動で取り消す */
    public void addTemporaryDefenseBonus(Plugin plugin, UUID uuid, double value, long durationTicks) {
        consumableDefense.merge(uuid, value, Double::sum);
        Bukkit.getScheduler().runTaskLater(plugin, () -> removeTemporary(consumableDefense, uuid, value), durationTicks);
    }

    private void removeTemporary(Map<UUID, Double> map, UUID uuid, double value) {
        double remaining = map.merge(uuid, -value, Double::sum);
        if (Math.abs(remaining) < 0.001) map.remove(uuid);
    }

    /** ログアウト時にプレイヤーの保持データを破棄する */
    public void cleanup(UUID uuid) {
        equipmentAttack.remove(uuid);
        equipmentDefense.remove(uuid);
        consumableAttack.remove(uuid);
        consumableDefense.remove(uuid);
    }
}
