package world.elyona.items.model;

public class EffectDefinition {

    private final EffectType type;
    // 標準ポーションエフェクト用
    private final int min;
    private final int max;
    // カスタムエフェクト用 (GLOWING_MOBS, SLOW_AURA等)
    private final int radiusMin;
    private final int radiusMax;
    // SLOW_AURA用
    private final int slowLevelMin;
    private final int slowLevelMax;
    // SCAN_PULSE用
    private final int durationSeconds;

    private EffectDefinition(Builder builder) {
        this.type = builder.type;
        this.min = builder.min;
        this.max = builder.max;
        this.radiusMin = builder.radiusMin;
        this.radiusMax = builder.radiusMax;
        this.slowLevelMin = builder.slowLevelMin;
        this.slowLevelMax = builder.slowLevelMax;
        this.durationSeconds = builder.durationSeconds;
    }

    public EffectType getType() { return type; }
    public int getMin() { return min; }
    public int getMax() { return max; }
    public int getRadiusMin() { return radiusMin; }
    public int getRadiusMax() { return radiusMax; }
    public int getSlowLevelMin() { return slowLevelMin; }
    public int getSlowLevelMax() { return slowLevelMax; }
    public int getDurationSeconds() { return durationSeconds; }

    /**
     * 品質値に基づいてエフェクト値を計算する
     * effectValue = min + (max - min) * (quality / 100.0)
     */
    public double calculateValue(int quality) {
        return min + (max - min) * (quality / 100.0);
    }

    /**
     * 品質値に基づいてamplifier (0始まり) を計算する
     */
    public int calculateAmplifier(int quality) {
        return (int) Math.round(calculateValue(quality)) - 1;
    }

    /**
     * 品質値に基づいて半径を計算する
     */
    public double calculateRadius(int quality) {
        return radiusMin + (radiusMax - radiusMin) * (quality / 100.0);
    }

    /**
     * 品質値に基づいてスロウレベルを計算する
     */
    public int calculateSlowLevel(int quality) {
        return (int) Math.round(slowLevelMin + (slowLevelMax - slowLevelMin) * (quality / 100.0));
    }

    public static Builder builder(EffectType type) {
        return new Builder(type);
    }

    public static class Builder {
        private final EffectType type;
        private int min = 1;
        private int max = 1;
        private int radiusMin = 0;
        private int radiusMax = 0;
        private int slowLevelMin = 1;
        private int slowLevelMax = 1;
        private int durationSeconds = 0;

        public Builder(EffectType type) {
            this.type = type;
        }

        public Builder min(int min) { this.min = min; return this; }
        public Builder max(int max) { this.max = max; return this; }
        public Builder radiusMin(int radiusMin) { this.radiusMin = radiusMin; return this; }
        public Builder radiusMax(int radiusMax) { this.radiusMax = radiusMax; return this; }
        public Builder slowLevelMin(int slowLevelMin) { this.slowLevelMin = slowLevelMin; return this; }
        public Builder slowLevelMax(int slowLevelMax) { this.slowLevelMax = slowLevelMax; return this; }
        public Builder durationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; return this; }

        public EffectDefinition build() {
            return new EffectDefinition(this);
        }
    }
}
