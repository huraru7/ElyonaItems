package world.elyona.items;

import org.bukkit.plugin.java.JavaPlugin;
import world.elyona.items.api.ItemGenerator;
import world.elyona.items.api.ItemManager;
import world.elyona.items.combat.ArmorDisplay;
import world.elyona.items.combat.AttackDamageListener;
import world.elyona.items.combat.HologramManager;
import world.elyona.items.combat.MobDamageListener;
import world.elyona.items.combat.PlayerStatManager;
import world.elyona.items.combat.VanillaItemDisplayListener;
import world.elyona.items.combat.WeaponDisplay;
import world.elyona.items.command.ElyonaAdminCommand;
import world.elyona.items.command.MimicSellCommand;
import world.elyona.items.config.ItemsConfig;
import world.elyona.items.effect.EffectApplier;
import world.elyona.items.effect.custom.CustomEffectManager;
import world.elyona.items.gui.MimicSellGui;
import world.elyona.items.listener.ConsumableListener;
import world.elyona.items.listener.DurabilityListener;
import world.elyona.items.listener.EquipmentListener;
import world.elyona.items.listener.InventoryCompressListener;

import java.io.File;

public class ElyonaItemsPlugin extends JavaPlugin {

    private ItemsConfig itemsConfig;
    private ItemGenerator itemGenerator;
    private ItemManager itemManager;
    private CustomEffectManager customEffectManager;
    private EffectApplier effectApplier;
    private EquipmentListener equipmentListener;
    private MimicSellGui mimicSellGui;
    private HologramManager hologramManager;
    private PlayerStatManager playerStatManager;

    @Override
    public void onEnable() {
        // データフォルダ初期化
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        saveDefaultConfig();

        // libs フォルダ確認
        File libsDir = new File(getDataFolder().getParentFile().getParentFile(), "libs");
        // 注: libsはプロジェクトのルートにあるため、サーバー起動時は classpath にある

        // コンポーネント初期化
        this.itemsConfig = new ItemsConfig(this);
        this.itemsConfig.load();

        this.itemGenerator = new ItemGenerator(this);
        this.itemManager = new ItemManager(this);
        this.customEffectManager = new CustomEffectManager(this);
        this.playerStatManager = new PlayerStatManager();
        this.effectApplier = new EffectApplier(this, customEffectManager, playerStatManager);

        // リスナー登録
        this.equipmentListener = new EquipmentListener(this);
        getServer().getPluginManager().registerEvents(equipmentListener, this);
        getServer().getPluginManager().registerEvents(new ConsumableListener(this), this);
        getServer().getPluginManager().registerEvents(new DurabilityListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryCompressListener(this), this);

        // GUIリスナー登録
        this.mimicSellGui = new MimicSellGui(this);
        getServer().getPluginManager().registerEvents(mimicSellGui, this);

        // 敵HPホログラム
        this.hologramManager = new HologramManager(this);
        this.hologramManager.start();
        getServer().getPluginManager().registerEvents(new MobDamageListener(this, hologramManager), this);

        // バニラ武器・防具のダメージ/防御力表記統一（防具はバニラの素材由来補正をElyonaの数値で上書き）
        WeaponDisplay weaponDisplay = new WeaponDisplay(this);
        ArmorDisplay armorDisplay = new ArmorDisplay(this);
        getServer().getPluginManager().registerEvents(
                new VanillaItemDisplayListener(this, weaponDisplay, armorDisplay), this);

        // 攻撃力・防御力ボーナスのダメージ計算への適用
        getServer().getPluginManager().registerEvents(new AttackDamageListener(playerStatManager), this);

        // コマンド登録
        var mimicCmd = getCommand("mimic");
        if (mimicCmd != null) {
            var mimicSellCmd = new MimicSellCommand(this, mimicSellGui);
            mimicCmd.setExecutor(mimicSellCmd);
            mimicCmd.setTabCompleter(mimicSellCmd);
        }

        var elyonaCmd = getCommand("elyona");
        if (elyonaCmd != null) {
            var adminCmd = new ElyonaAdminCommand(this);
            elyonaCmd.setExecutor(adminCmd);
            elyonaCmd.setTabCompleter(adminCmd);
        }

        getLogger().info("ElyonaItems が有効化されました。アイテム数: " + itemsConfig.getAllItems().size());
    }

    @Override
    public void onDisable() {
        // カスタムエフェクトのタスクを全キャンセル
        if (customEffectManager != null) {
            customEffectManager.cancelAll();
        }
        // ホログラムを全て除去
        if (hologramManager != null) {
            hologramManager.stop();
        }
        getLogger().info("ElyonaItems が無効化されました。");
    }

    // ━━ Getter ━━

    public ItemsConfig getItemsConfig() { return itemsConfig; }
    public ItemGenerator getItemGenerator() { return itemGenerator; }
    public ItemManager getItemManager() { return itemManager; }
    public CustomEffectManager getCustomEffectManager() { return customEffectManager; }
    public EffectApplier getEffectApplier() { return effectApplier; }
    public EquipmentListener getEquipmentListener() { return equipmentListener; }
    public MimicSellGui getMimicSellGui() { return mimicSellGui; }
    public PlayerStatManager getPlayerStatManager() { return playerStatManager; }
}
