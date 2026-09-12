# ElyonaItems

Elyona World の独自アイテム・戦闘システムを管理するプラグイン。カスタムアイテム生成、消耗品効果、装備の攻撃力・防御力管理、MIMICへのアイテム売却などを提供する。

## 主な機能

- カスタムアイテム生成(`ItemGenerator` / `ItemManager`)
- 消耗品によるカスタム効果付与(`EffectApplier` / `CustomEffectManager`)
- 独自の攻撃力・防御力管理(`PlayerStatManager`) — バニラのダメージ計算を介さない固定値ベースのステータス
- 敵HPホログラム表示(`HologramManager`)
- 武器・防具のダメージ/防御力表示のElyona仕様への統一(`WeaponDisplay` / `ArmorDisplay`)
- 耐久度・装備・インベントリ圧縮関連の各種リスナー
- MIMIC端末でのアイテム売却GUI

## コマンド

| コマンド | 説明 | 権限 |
|---|---|---|
| `/mimic sell` | MIMICへアイテムを売却する | `elyona.mimic.sell`(デフォルト全員) |
| `/elyona <give\|reload> [args...]` | 管理コマンド | `elyona.admin` |

## 依存関係

- Paper 1.21.1
- [ElyonaCore](https://github.com/huraru7/ElyonaCore)
- [ElyonaEconomy](https://github.com/huraru7/ElyonaEconomy)

## ビルド

```
./gradlew build
```

Java 21 / Paper 1.21.1 (paperweight userdev) を使用。
