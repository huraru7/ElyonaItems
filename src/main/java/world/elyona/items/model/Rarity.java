package world.elyona.items.model;

public enum Rarity {
    COMMON(0, 100, 10, 60, "§7"),
    UNCOMMON(0, 100, 20, 70, "§a"),
    RARE(10, 100, 30, 80, "§9"),
    EPIC(20, 100, 50, 90, "§5"),
    LEGENDARY(40, 100, 60, 100, "§6"),
    MYTHIC(100, 100, 100, 100, "§d");

    private final int minQuality;
    private final int maxQuality;
    private final int avgMin;
    private final int avgMax;
    private final String colorCode;

    Rarity(int minQuality, int maxQuality, int avgMin, int avgMax, String colorCode) {
        this.minQuality = minQuality;
        this.maxQuality = maxQuality;
        this.avgMin = avgMin;
        this.avgMax = avgMax;
        this.colorCode = colorCode;
    }

    public int getMinQuality() { return minQuality; }
    public int getMaxQuality() { return maxQuality; }
    public int getAvgMin() { return avgMin; }
    public int getAvgMax() { return avgMax; }
    public String getColorCode() { return colorCode; }

    public String getDisplayName() {
        return colorCode + switch (this) {
            case COMMON -> "コモン";
            case UNCOMMON -> "アンコモン";
            case RARE -> "レア";
            case EPIC -> "エピック";
            case LEGENDARY -> "レジェンダリー";
            case MYTHIC -> "ミシック";
        };
    }
}
