# mofucraft向け LunaChat 改造まとめ

元リポジトリ: [ucchyocean/LunaChat](https://github.com/ucchyocean/LunaChat) (v3.1.7 以降)
改造バージョン: v3.1.10

後継プラグインに同じ改造を行う際の参考資料。

---

## 概要

主な改造は以下の3点。

1. **Adventure API への移行**（BungeeCord Chat API から切り替え）
2. **NameColor プラグイン連携**（`%namecolor_name_minimessage%` によるグラデーション・shadow装飾の保持）
3. **mofucraft 固有のプレースホルダー対応**（`%mofucommunity_prefix%` の扱い）

---

## 1. ビルド設定 (`build.gradle`)

| 変更点 | 変更前 | 変更後 |
|--------|--------|--------|
| Shadow plugin バージョン | `7.1.2` | `8.1.1` |
| API 依存 | `org.spigotmc:spigot-api:1.16.2-R0.1-SNAPSHOT` | `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT` |
| PlaceholderAPI | `2.11.2` | `2.11.6` |
| Java バージョン | `VERSION_1_8` | `VERSION_21` |
| バージョン | `3.1.7` | `3.1.10` |

- Paper リポジトリ (`https://repo.papermc.io/repository/maven-public/`) を追加。
- Multiverse リポジトリの URL を `content/repositories/multiverse/` → `content/groups/public/` に変更。
- `shadowJar` のパッケージリロケーション（`com.google.gson`, `org.apache.commons.lang3`, `org.bstats` 等）を**全て削除**。Java 21 / ASM の互換性問題を避けるため。代わりに `mergeServiceFiles()` を追加。

---

## 2. `LunaChatBukkit.java`

### 2-1. bStats 初期化を try-catch で保護

リロケーション削除に伴い、bStats 初期化が失敗してもプラグインが起動できるよう `try-catch` で囲む。

```java
try {
    Metrics metrics = new Metrics(this, 7936);
    // ...
} catch (Exception e) {
    getLogger().warning("bStats metrics could not be initialized: " + e.getMessage());
}
```

### 2-2. 未解決プレースホルダーの除去メソッド追加

PlaceholderAPI が展開できなかった `%xxx%` 形式の文字列をチャットに残さないためのユーティリティメソッド。

```java
public static String stripUnresolvedPlaceholders(String text) {
    if (text == null) return null;
    return text.replaceAll("%[^%]+%", "");
}
```

---

## 3. Adventure API 対応（全 `ChannelMember` サブクラス）

### 3-1. `ChannelMember.java`（抽象基底クラス）

以下の2つの抽象メソッドを追加。

```java
public abstract void sendMessage(Component message);
public abstract Component getDisplayNameComponent();
```

### 3-2. 各サブクラスへの実装

| クラス | `sendMessage(Component)` | `getDisplayNameComponent()` |
|--------|--------------------------|-----------------------------|
| `ChannelMemberPlayer` | `player.sendMessage(message)` | PlaceholderAPI で `%namecolor_name_minimessage%` を取得し MiniMessage パース（後述） |
| `ChannelMemberBukkitConsole` | `sender.sendMessage(message)` | `Component.text(sender.getName())` |
| `ChannelMemberBlock` | 何もしない | `Component.text(sender.getName())` |
| `ChannelMemberOther` | 何もしない | `Component.text(displayName)` |
| `ChannelMemberSystem` | 何もしない | `Component.text(NAME)` |
| `ChannelMemberProxiedPlayer` | PlainText に変換して BungeeCord API で送信 | `Component.text(getDisplayName())` |
| `ChannelMemberBungeeConsole` | PlainText に変換して BungeeCord API で送信 | `Component.text(sender.getName())` |

> **補足**: BungeeCord 側のクラス（`ProxiedPlayer`, `BungeeConsole`）は Adventure API に非対応のため、`PlainTextComponentSerializer` でプレーンテキストに変換してから送信する。

### 3-3. `ChannelMemberPlayer.getDisplayName()` の変更

```java
// 変更前
String displayName = player.getDisplayName(); // 旧API（String）

// 変更後
String displayName = LegacyComponentSerializer.legacySection().serialize(player.displayName()); // Adventure API
```

### 3-4. `ChannelMemberPlayer.getDisplayNameComponent()` の追加

NameColor プラグインの MiniMessage 形式名前（グラデーション・shadow 含む）を取得するメソッド。

```java
@Override
public Component getDisplayNameComponent() {
    if (LunaChatBukkit.getInstance().enablePlaceholderAPI()) {
        String miniMessageFormat = PlaceholderAPI.setPlaceholders(player, "%namecolor_name_minimessage%");
        if (miniMessageFormat != null && !miniMessageFormat.isEmpty()
                && !miniMessageFormat.contains("%namecolor_name_minimessage%")) {
            return MiniMessage.miniMessage().deserialize(miniMessageFormat);
        }
    }
    return player.displayName(); // フォールバック
}
```

---

## 4. `ClickableFormat.java`（主要変更）

### 4-1. `member` フィールドの追加

`makeFormat()` の結果として生成された `ClickableFormat` インスタンスが発言者の `ChannelMember` を保持できるよう、フィールドと対応コンストラクタを追加。

### 4-2. `%displayname` / `%username` をマーカーに置換

`getDisplayNameComponent()` で得た Adventure Component（shadow等の装飾付き）を後から埋め込むため、プレーンテキストではなくマーカー文字列 `＜DISPLAY_NAME_COMPONENT＞` を中間表現として使用する方式に変更。

```java
// 変更前: member.getDisplayName() をそのまま埋め込む
// 変更後: マーカーを埋め込み、makeAdventureComponent() 内で実際の Component に置換
private static final String DISPLAY_NAME_COMPONENT_PLACEHOLDER = "＜DISPLAY_NAME_COMPONENT＞";
```

### 4-3. PlaceholderAPI による format 文字列の展開

`makeFormat()` 内で、LunaChat 固有キーワード置換後に PlaceholderAPI も実行するよう追加。プレイヤー以外（Bot/コンソール）の場合は `%mofucommunity_prefix%` を空文字に置換する。

### 4-4. `makeAdventureComponent()` メソッドの追加

従来の `makeTextComponent()` (BungeeCord API) に代わる Adventure API 版。以下の機能を含む。

- **マーカー → Component 置換**: `＜DISPLAY_NAME_COMPONENT＞` を `getDisplayNameComponent()` の結果で置き換え。
- **スタッフ判定と色適用**:
  - パーミッション `lunachat.staff` または `mofucraft.staff` を持つ場合、prefix の末尾色をプレイヤー名に適用。
  - ただしグラデーション名（複数の異なる色を持つ子コンポーネント）の場合は色適用をスキップして装飾を保持。
- **shadow 保持**: コンポーネント自体を変更せず、親コンポーネントでラップしてクリック/ホバーイベントを付与する `applyEventsToComponent()` を使用。
- **URL のクリック化**: チャットメッセージ内の `https?://...` を自動的にクリック可能なリンクに変換。

### 4-5. 追加されたヘルパーメソッド

| メソッド | 役割 |
|----------|------|
| `parseTextWithUrls(String)` | テキスト内 URL を検出してクリック可能 Component に変換 |
| `applyEventsToComponent(...)` | Component を変更せず親でラップしてイベントを設定（shadow 保持） |
| `extractLastColor(String)` | prefix テキストから最後の色コード（`§x§...` or `§a` 等）を抽出 |
| `getColorFromCode(char)` | 標準色コード文字 → `TextColor` |
| `isGradientComponent(Component)` | 子コンポーネントが複数の異なる色を持つかを判定 |
| `applyColorToComponent(Component, TextColor)` | 色のみ変更し他のスタイル（shadow等）を再帰的に保持 |

---

## 5. `BukkitChannel.java`

### 5-1. プレイヤー以外の発言での PlaceholderAPI 処理

```java
// 変更前: ChannelMemberBukkit の場合のみ PAPI 展開
// 変更後: プレイヤー以外は %mofucommunity_prefix% を空文字に置換
if (player instanceof ChannelMemberBukkit) {
    message = PlaceholderAPI.setPlaceholders(bukkitPlayer, message);
} else {
    message = message.replace("%mofucommunity_prefix%", "");
}
message = LunaChatBukkit.stripUnresolvedPlaceholders(message);
```

### 5-2. メッセージ送信を Adventure API に変更

```java
// 変更前
BaseComponent[] comps = format.makeTextComponent();
p.sendMessage(comps);

// 変更後
Component adventureComponent = format.makeAdventureComponent();
p.sendMessage(adventureComponent);
```

---

## 6. `BukkitEventListener.java`

### 6-1. `%displayName` の大文字小文字を修正

```java
// 変更前（大文字混在で %displayname と一致しない）
.replace("%1$s", "%displayName")

// 変更後
.replace("%1$s", "%displayname")
```

### 6-2. メッセージ送信・コンソール出力を Adventure API に変更

通常チャット処理（`AsyncPlayerChatEvent`）でも Adventure API の `Component` を使用するよう変更。コンソール出力は `PlainTextComponentSerializer.plainText().serialize()` に変更。

### 6-3. 未解決プレースホルダーの除去

PAPI 展開後に `stripUnresolvedPlaceholders()` を呼び出すよう追加。

---

## 7. `Utility.java`

### `&#RRGGBB` 形式のカラーコード対応

`&` プレフィックス付きの Hex カラーコード（一部プラグインで使用される形式）を追加サポート。

| 追加対応形式 | 例 |
|-------------|-----|
| `&#RRGGBB` | `&#FF0000` |
| `&#RGB` | `&#F00` |

影響する3メソッド:
- `replaceWebColorCode(String)` — 変換処理に `&#...` のパターンを追加
- `stripAltColorCode(String)` — ストリップ処理に `&#...` を追加
- `isAltColorCode(String)` — 判定の正規表現に `&#...` を追加

---

## 変更ファイル一覧

```
build.gradle
src/main/java/com/github/ucchyocean/lc3/LunaChatBukkit.java
src/main/java/com/github/ucchyocean/lc3/bukkit/BukkitEventListener.java
src/main/java/com/github/ucchyocean/lc3/channel/BukkitChannel.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMember.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberBlock.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberBukkitConsole.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberBungeeConsole.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberOther.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberPlayer.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberProxiedPlayer.java
src/main/java/com/github/ucchyocean/lc3/member/ChannelMemberSystem.java
src/main/java/com/github/ucchyocean/lc3/util/ClickableFormat.java
src/main/java/com/github/ucchyocean/lc3/util/Utility.java
```

---

## 後継プラグインへの移植時の注意点

1. **Paper API 前提**: Spigot API ではなく `io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT` (またはそれ以降) が必要。Adventure API と MiniMessage を使用している。
2. **NameColor プラグイン依存**: `getDisplayNameComponent()` の実装は `%namecolor_name_minimessage%` というプレースホルダーに依存している。NameColor プラグインを使用しない場合は `player.displayName()` のみのフォールバック実装に簡略化できる。
3. **`%mofucommunity_prefix%` ハードコード**: `BukkitChannel` と `ClickableFormat` に `%mofucommunity_prefix%` が直書きされている。mofucraft 固有のプレースホルダーであり、別サーバーでは不要なら該当箇所を削除または汎用化する。
4. **パッケージリロケーション無効化**: リロケーションを削除したため、bStats 等の依存ライブラリが他のプラグインのものと衝突する可能性がある。本番環境に応じて再度リロケーションを有効化することを検討すること。
5. **Java 21 必須**: ソースの互換バージョンが Java 21 に引き上げられている（`switch` 式のパターンマッチング等を使用）。
