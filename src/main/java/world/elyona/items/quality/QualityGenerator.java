package world.elyona.items.quality;

import world.elyona.items.model.Rarity;
import java.util.concurrent.ThreadLocalRandom;

/**
 * レアリティに応じた品質値を生成する。
 * 正規分布近似（複数の乱数の平均）を使用して分布を作る。
 */
public class QualityGenerator {

    private static final int SAMPLE_COUNT = 3;

    /**
     * 指定したレアリティで品質値を生成する。
     */
    public static int generate(Rarity rarity) {
        if (rarity == Rarity.MYTHIC) {
            return 100;
        }

        int minQ = rarity.getMinQuality();
        int maxQ = rarity.getMaxQuality();
        int avgMin = rarity.getAvgMin();
        int avgMax = rarity.getAvgMax();

        ThreadLocalRandom rand = ThreadLocalRandom.current();

        // 複数サンプルの平均で正規分布を近似
        double sum = 0;
        for (int i = 0; i < SAMPLE_COUNT; i++) {
            sum += avgMin + rand.nextDouble() * (avgMax - avgMin);
        }
        double avg = sum / SAMPLE_COUNT;

        // ばらつきを加える
        double spread = (avgMax - avgMin) * 0.3;
        double raw = avg + (rand.nextDouble() - 0.5) * spread;

        // クランプ
        int quality = (int) Math.round(raw);
        quality = Math.max(minQ, Math.min(maxQ, quality));
        return quality;
    }

    /**
     * 品質値を検証してクランプする。
     */
    public static int clamp(Rarity rarity, int quality) {
        return Math.max(rarity.getMinQuality(), Math.min(rarity.getMaxQuality(), quality));
    }
}
