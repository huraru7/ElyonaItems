package world.elyona.items.model;

public enum EffectType {
    // 標準ポーションエフェクト
    SPEED,
    JUMP_BOOST,
    SLOW_FALLING,
    DOLPHINS_GRACE,
    STRENGTH,
    RESISTANCE,
    KNOCKBACK_RESISTANCE,
    FIRE_RESISTANCE,
    NIGHT_VISION,
    REGENERATION,
    SATURATION,
    HASTE,
    LUCK,
    INSTANT_HEALTH,
    SLOWNESS,

    // カスタムエフェクト
    GLOWING_MOBS,       // 周囲のMobにGlowingを付与し続ける
    SCAN_PULSE,         // 一時的に広範囲のmob・chestにGlowingを付与
    GRAVITY_PULL,       // 周囲の敵を中心に引き寄せる
    AREA_MINE,          // 3x3x3範囲採掘・ドロップ量2倍
    INVENTORY_COMPRESS, // アイテム拾得時に自動スタック圧縮
    SLOW_AURA           // 周囲の敵にSlownessを付与し続ける
}
