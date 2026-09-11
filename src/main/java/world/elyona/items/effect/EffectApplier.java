package world.elyona.items.effect;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import world.elyona.items.ElyonaItemsPlugin;
import world.elyona.items.effect.custom.CustomEffectManager;
import world.elyona.items.model.EffectDefinition;
import world.elyona.items.model.EffectType;
import world.elyona.items.model.ItemDefinition;

import java.util.List;

/**
 * ElyonaItemsのエフェクトをプレイヤーに付与・除去するクラス。
 */
public class EffectApplier {

    private static final int PERMANENT_DURATION = Integer.MAX_VALUE;

    /** 装備エフェクトとして付与しうる全PotionEffectType（一律除去時に使用） */
    private static final List<PotionEffectType> MANAGED_POTION_TYPES = List.of(
            PotionEffectType.SPEED, PotionEffectType.JUMP_BOOST, PotionEffectType.SLOW_FALLING,
            PotionEffectType.DOLPHINS_GRACE, PotionEffectType.STRENGTH, PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE, PotionEffectType.NIGHT_VISION, PotionEffectType.REGENERATION,
            PotionEffectType.SATURATION, PotionEffectType.HASTE, PotionEffectType.LUCK
    );

    private final ElyonaItemsPlugin plugin;
    private final CustomEffectManager customEffectManager;

    public EffectApplier(ElyonaItemsPlugin plugin, CustomEffectManager customEffectManager) {
        this.plugin = plugin;
        this.customEffectManager = customEffectManager;
    }

    /**
     * 装備アイテムのエフェクトをプレイヤーに付与する (永続)。
     */
    public void applyEquipmentEffects(Player player, ItemDefinition def, int quality) {
        applyEffects(player, def.getEffects(), quality, PERMANENT_DURATION, def.getId());
    }

    /**
     * 消耗品のエフェクトをプレイヤーに付与する (時間制限)。
     */
    public void applyConsumableEffects(Player player, ItemDefinition def, int quality) {
        int durationTicks = def.calculateDurationTicks(quality);
        applyEffects(player, def.getEffects(), quality, durationTicks, null);
    }

    /**
     * アイテムのエフェクトをプレイヤーから除去する。
     */
    public void removeEquipmentEffects(Player player, ItemDefinition def, int quality) {
        removeEffects(player, def.getEffects(), quality, def.getId());
    }

    /**
     * 装備由来のポーション効果を、現在の防具の中身に関係なく一律で除去する。
     * 外した直後は防具スロットが空になり「何のアイテムの効果だったか」が
     * 特定できないため、装備スロットの再スキャンではなく全種類を対象にする。
     */
    public void removeAllManagedPotionEffects(Player player) {
        for (PotionEffectType type : MANAGED_POTION_TYPES) {
            player.removePotionEffect(type);
        }
    }

    private void applyEffects(Player player, List<EffectDefinition> effects, int quality,
                               int durationTicks, String itemId) {
        for (EffectDefinition effect : effects) {
            EffectType type = effect.getType();

            if (isCustomEffect(type)) {
                applyCustomEffect(player, effect, quality, itemId);
                continue;
            }

            PotionEffectType potionType = getPotionEffectType(type);
            if (potionType == null) continue;

            int amplifier = effect.calculateAmplifier(quality);
            amplifier = Math.max(0, amplifier);

            // 既存の同エフェクトと比較して強い方だけ付与
            PotionEffect existing = player.getPotionEffect(potionType);
            if (existing != null && existing.getAmplifier() >= amplifier
                    && existing.getDuration() >= durationTicks) {
                continue;
            }

            player.addPotionEffect(new PotionEffect(
                    potionType,
                    durationTicks,
                    amplifier,
                    true,   // ambient (パーティクルを薄く)
                    false,  // particles (パーティクル非表示)
                    false   // icon (アイコン非表示)
            ));
        }
    }

    private void removeEffects(Player player, List<EffectDefinition> effects, int quality, String itemId) {
        for (EffectDefinition effect : effects) {
            EffectType type = effect.getType();

            if (isCustomEffect(type)) {
                removeCustomEffect(player, type, itemId);
                continue;
            }

            PotionEffectType potionType = getPotionEffectType(type);
            if (potionType == null) continue;
            player.removePotionEffect(potionType);
        }
    }

    private boolean isCustomEffect(EffectType type) {
        return switch (type) {
            case GLOWING_MOBS, SCAN_PULSE, GRAVITY_PULL, AREA_MINE,
                 INVENTORY_COMPRESS, SLOW_AURA -> true;
            default -> false;
        };
    }

    private void applyCustomEffect(Player player, EffectDefinition effect, int quality, String itemId) {
        switch (effect.getType()) {
            case GLOWING_MOBS -> {
                double radius = effect.calculateRadius(quality);
                customEffectManager.startGlowingMobs(player, (int) Math.round(radius), itemId);
            }
            case SLOW_AURA -> {
                double radius = effect.calculateRadius(quality);
                int level = effect.calculateSlowLevel(quality);
                customEffectManager.startSlowAura(player, (int) Math.round(radius), level, itemId);
            }
            case INVENTORY_COMPRESS -> customEffectManager.enableInventoryCompress(player, itemId);
            case SCAN_PULSE -> {
                double radius = effect.calculateRadius(quality);
                int duration = effect.getDurationSeconds();
                customEffectManager.triggerScanPulse(player, (int) Math.round(radius), duration);
            }
            case GRAVITY_PULL -> {
                double radius = effect.calculateRadius(quality);
                customEffectManager.triggerGravityPull(player, (int) Math.round(radius));
            }
            case AREA_MINE -> customEffectManager.triggerAreaMine(player);
            default -> { }
        }
    }

    private void removeCustomEffect(Player player, EffectType type, String itemId) {
        switch (type) {
            case GLOWING_MOBS -> customEffectManager.stopGlowingMobs(player, itemId);
            case SLOW_AURA -> customEffectManager.stopSlowAura(player, itemId);
            case INVENTORY_COMPRESS -> customEffectManager.disableInventoryCompress(player, itemId);
            default -> { }
        }
    }

    public PotionEffectType getPotionEffectType(EffectType type) {
        return switch (type) {
            case SPEED -> PotionEffectType.SPEED;
            case JUMP_BOOST -> PotionEffectType.JUMP_BOOST;
            case SLOW_FALLING -> PotionEffectType.SLOW_FALLING;
            case DOLPHINS_GRACE -> PotionEffectType.DOLPHINS_GRACE;
            case STRENGTH -> PotionEffectType.STRENGTH;
            case RESISTANCE -> PotionEffectType.RESISTANCE;
            case KNOCKBACK_RESISTANCE -> PotionEffectType.RESISTANCE; // 代替
            case FIRE_RESISTANCE -> PotionEffectType.FIRE_RESISTANCE;
            case NIGHT_VISION -> PotionEffectType.NIGHT_VISION;
            case REGENERATION -> PotionEffectType.REGENERATION;
            case SATURATION -> PotionEffectType.SATURATION;
            case HASTE -> PotionEffectType.HASTE;
            case LUCK -> PotionEffectType.LUCK;
            case INSTANT_HEALTH -> PotionEffectType.INSTANT_HEALTH;
            case SLOWNESS -> PotionEffectType.SLOWNESS;
            default -> null;
        };
    }
}
