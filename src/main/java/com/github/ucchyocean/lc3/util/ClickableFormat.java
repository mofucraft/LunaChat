/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.util;

import com.github.ucchyocean.lc3.LunaChat;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.github.ucchyocean.lc3.LunaChatMode;
import com.github.ucchyocean.lc3.Messages;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMember;
import com.github.ucchyocean.lc3.member.ChannelMemberBukkit;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * チャットのフォーマットを作成するユーティリティクラス
 *
 * @author ucchy
 */
public class ClickableFormat {

    private static final String JOIN_COMMAND_TEMPLATE = "/lunachat join %s";
    private static final String TELL_COMMAND_TEMPLATE = "/tell %s";

    private static final String PLACEHOLDER_RUN_COMMAND =
            "＜type=RUN_COMMAND text=\"%s\" hover=\"%s\" command=\"%s\"＞";
    private static final String PLACEHOLDER_SUGGEST_COMMAND =
            "＜type=SUGGEST_COMMAND text=\"%s\" hover=\"%s\" command=\"%s\"＞";
    private static final String PLACEHOLDER_PATTERN =
            "＜type=(SUGGEST_COMMAND|RUN_COMMAND) text=\"([^\"]*)\" hover=\"([^\"]*)\" command=\"([^\"]*)\"＞";
    // Adventure API用のプレースホルダー（displayNameComponentを後から埋め込むため）
    private static final String DISPLAY_NAME_COMPONENT_PLACEHOLDER = "＜DISPLAY_NAME_COMPONENT＞";

    private final KeywordReplacer message;
    private ChannelMember member;

    private ClickableFormat(KeywordReplacer message) {
        this.message = message;
    }

    private ClickableFormat(KeywordReplacer message, ChannelMember member) {
        this.message = message;
        this.member = member;
    }

    /**
     * チャットフォーマット内のキーワードを置き換えする
     *
     * @param format チャットフォーマット
     * @param member 発言者
     * @return 置き換え結果
     */
    public static ClickableFormat makeFormat(String format, @Nullable ChannelMember member) {
        return makeFormat(format, member, null, true);
    }

    /**
     * チャットフォーマット内のキーワードを置き換えする
     *
     * @param format         チャットフォーマット
     * @param member         発言者
     * @param channel        チャンネル
     * @param withPlayerLink プレイヤー名の箇所にクリック可能なプレースホルダーを挿入するか
     * @return 置き換え結果
     */
    public static ClickableFormat makeFormat(String format,
                                             @Nullable ChannelMember member, @Nullable Channel channel, boolean withPlayerLink) {

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

        LunaChatAPI api = LunaChat.getAPI();

        KeywordReplacer msg = new KeywordReplacer(format);

        //msg.replace("%msg", message);

        if (channel != null) {

            // テンプレートのキーワードを、まず最初に置き換える
            for (int i = 0; i <= 9; i++) {
                String key = "%" + i;
                if (msg.contains(key)) {
                    if (api.getTemplate("" + i) != null) {
                        msg.replace(key, api.getTemplate("" + i));
                        break;
                    }
                }
            }

            // チャンネル関連のキーワード置き換え
            msg.replace("%ch", String.format(
                    PLACEHOLDER_RUN_COMMAND,
                    channel.getName(),
                    Messages.hoverChannelName(channel.getName()),
                    String.format(JOIN_COMMAND_TEMPLATE, channel.getName())));
            msg.replace("%color", channel.getColorCode());
            if (channel.getPrivateMessageTo() != null) {
                msg.replace("%to", String.format(
                        PLACEHOLDER_SUGGEST_COMMAND,
                        channel.getPrivateMessageTo().getDisplayName(),
                        Messages.hoverPlayerName(channel.getPrivateMessageTo().getName()),
                        String.format(TELL_COMMAND_TEMPLATE, channel.getPrivateMessageTo().getName())));
                msg.replace("%recieverserver", channel.getPrivateMessageTo().getServerName());
            }
        }

        if (msg.contains("%date")) {
            msg.replace("%date", dateFormat.format(new Date()));
        }
        if (msg.contains("%time")) {
            msg.replace("%time", timeFormat.format(new Date()));
        }

        if (member != null) {

            // ChannelMember関連のキーワード置き換え
            if (withPlayerLink) {
                String playerPMPlaceHolder = String.format(
                        PLACEHOLDER_SUGGEST_COMMAND,
                        member.getDisplayName(),
                        Messages.hoverPlayerName(member.getName()),
                        String.format(TELL_COMMAND_TEMPLATE, member.getName()));
                msg.replace("%displayname", playerPMPlaceHolder);
                msg.replace("%username", playerPMPlaceHolder);
                msg.replace("%player", String.format(
                        PLACEHOLDER_SUGGEST_COMMAND,
                        member.getName(),
                        Messages.hoverPlayerName(member.getName()),
                        String.format(TELL_COMMAND_TEMPLATE, member.getName())));
            } else {
                msg.replace("%displayname", member.getDisplayName());
                msg.replace("%username", member.getDisplayName());
                msg.replace("%player", member.getName());
            }

            if (msg.contains("%prefix") || msg.contains("%suffix")) {
                msg.replace("%prefix", member.getPrefix());
                msg.replace("%suffix", member.getSuffix());
            }

            msg.replace("%world", member.getWorldName());
            msg.replace("%server", member.getServerName());
        }

        // LunaChatの独自キーワード置換後、PlaceholderAPIのプレースホルダーを置換
        // これにより、フォーマット内に直接記述されたPlaceholderAPIのプレースホルダーが正しく展開される
        if (LunaChat.getMode() == LunaChatMode.BUKKIT) {
            try {
                if (LunaChatBukkit.getInstance().enablePlaceholderAPI()) {
                    String result = msg.toString();
                    if (member instanceof ChannelMemberBukkit) {
                        // プレイヤーの場合：PlaceholderAPI で展開
                        Player bukkitPlayer = ((ChannelMemberBukkit) member).getPlayer();
                        if (bukkitPlayer != null) {
                            result = PlaceholderAPI.setPlaceholders(bukkitPlayer, result);
                        }
                    }
                    // 取得できなかったプレースホルダーを空文字に置換
                    result = LunaChatBukkit.stripUnresolvedPlaceholders(result);
                    msg = new KeywordReplacer(result);
                }
            } catch (NoClassDefFoundError e) {
                // PlaceholderAPIが利用できない場合はスキップ
            }
        }

        return new ClickableFormat(msg, member);
    }

    /**
     * チャンネルチャットのメッセージ用のフォーマットを置き換えする
     *
     * @param format      フォーマット
     * @param channelName チャンネル名
     * @return 置き換え結果
     */
    public static ClickableFormat makeChannelClickableMessage(String format, String channelName) {

        KeywordReplacer msg = new KeywordReplacer(format);
        String stripped = Utility.stripColorCode(channelName);
        msg.replace("%channel%", String.format(
                PLACEHOLDER_RUN_COMMAND,
                channelName,
                Messages.hoverChannelName(stripped),
                String.format(JOIN_COMMAND_TEMPLATE, stripped)));

        return new ClickableFormat(msg);
    }

    /**
     * チャットフォーマット内のキーワードをBukkitの通常チャットイベント用に置き換えする
     *
     * @param format 置き換え元のチャットフォーマット
     * @param member 発言者
     * @return 置き換え結果
     */
    public static String replaceForNormalChatFormat(String format, ChannelMember member) {
        format = format.replace("%displayName", "%1$s");
        format = format.replace("%username", "%1$s");
        format = format.replace("%msg", "%2$s");
        return makeFormat(format, member, null, false).toLegacyText();
    }

    public BaseComponent[] makeTextComponent() {

        message.translateColorCode();

        List<BaseComponent> components = new ArrayList<>();
        Matcher matcher = Pattern.compile(PLACEHOLDER_PATTERN).matcher(message.getStringBuilder());
        int lastIndex = 0;

        while (matcher.find()) {

            // マッチする箇所までの文字列を取得する
            if (lastIndex < matcher.start()) {
                Collections.addAll(components, TextComponent.fromLegacyText(message.substring(lastIndex, matcher.start())));
            }

            // マッチした箇所の文字列を解析して追加する
            String type = matcher.group(1);
            String text = matcher.group(2);
            String hover = matcher.group(3);
            String command = matcher.group(4);
            TextComponent tc = new TextComponent(TextComponent.fromLegacyText(text));
            if (!hover.isEmpty()) {
                @SuppressWarnings("deprecation")
                HoverEvent event = new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create());
                // bungeecord-chat v1.16-R0.3 style.
//                HoverEvent event = new HoverEvent(
//                        HoverEvent.Action.SHOW_TEXT, new Text(hover));
                tc.setHoverEvent(event);
            }
            if (type.equals("RUN_COMMAND")) {
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            } else { // type.equals("SUGGEST_COMMAND")
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
            }

            // componentsの最後の要素のカラーコードを、TextComponentにも反映させる。 see issue #202
            if (components.size() > 0) {
                BaseComponent last = components.get(components.size() - 1);
                tc.setColor(last.getColor());
            }

            components.add(tc);

            lastIndex = matcher.end();
        }

        if (lastIndex < message.length() - 1) {
            // 残りの部分の文字列を取得する
            Collections.addAll(components, TextComponent.fromLegacyText(message.substring(lastIndex)));
        }

        BaseComponent[] result = new BaseComponent[components.size()];
        components.toArray(result);
        return result;
    }

    public String toLegacyText() {

        StringBuilder msg = new StringBuilder(message.toString());
        Matcher matcher = Pattern.compile(PLACEHOLDER_PATTERN).matcher(msg);

        while (matcher.find(0)) {
            String text = matcher.group(2);
            msg.replace(matcher.start(), matcher.end(), text);
        }

        return msg.toString();
    }

    @Override
    public String toString() {
        return message.toString();
    }

    public void replace(String keyword, String value) {
        message.replace(keyword, value);
    }

    /**
     * Adventure API用のComponentを作成する
     * displayNameComponentを直接埋め込み、shadowなどの装飾を保持する
     *
     * @return Adventure Component
     */
    public Component makeAdventureComponent() {
        message.translateColorCode();

        String text = message.toString();
        net.kyori.adventure.text.TextComponent.Builder builder = Component.text();
        Matcher matcher = Pattern.compile(PLACEHOLDER_PATTERN).matcher(text);
        int lastIndex = 0;

        while (matcher.find()) {
            // マッチする箇所までの文字列を追加
            String beforeText = "";
            if (lastIndex < matcher.start()) {
                beforeText = text.substring(lastIndex, matcher.start());
                builder.append(LegacyComponentSerializer.legacySection().deserialize(beforeText));
            }

            // マッチした箇所の文字列を解析して追加
            String type = matcher.group(1);
            String displayText = matcher.group(2);
            String hover = matcher.group(3);
            String command = matcher.group(4);

            // displayTextがmemberのdisplayNameと一致するか確認
            // 一致する場合はdisplayNameComponentを使用（shadow等を保持）
            Component clickableComponent;
            if (member != null && displayText.equals(member.getDisplayName())) {
                // memberのdisplayNameComponent（shadow付き）を使用
                Component displayNameComp = member.getDisplayNameComponent();

                // スタッフ権限を持っているかチェック
                // スタッフ権限あり: prefixの色を適用
                // スタッフ権限なし（一般プレイヤー）: 白ベース+shadowを維持
                boolean isStaff = false;
                if (member instanceof ChannelMemberBukkit) {
                    Player player = ((ChannelMemberBukkit) member).getPlayer();
                    if (player != null) {
                        isStaff = player.hasPermission("lunachat.staff")
                                || player.hasPermission("mofucraft.staff");
                    }
                }

                if (isStaff) {
                    // スタッフの場合、prefixの色を適用
                    net.kyori.adventure.text.format.TextColor prefixColor = extractLastColor(text.substring(0, matcher.start()));
                    if (prefixColor != null) {
                        displayNameComp = applyColorToComponent(displayNameComp, prefixColor);
                    }
                }
                // 一般プレイヤーの場合は白ベース+shadowをそのまま維持

                clickableComponent = displayNameComp;
            } else {
                // 通常のテキスト
                clickableComponent = LegacyComponentSerializer.legacySection().deserialize(displayText);
            }

            // クリックイベントとホバーイベントを設定
            if (!hover.isEmpty()) {
                clickableComponent = clickableComponent.hoverEvent(
                        net.kyori.adventure.text.event.HoverEvent.showText(Component.text(hover)));
            }
            if (type.equals("RUN_COMMAND")) {
                clickableComponent = clickableComponent.clickEvent(
                        net.kyori.adventure.text.event.ClickEvent.runCommand(command));
            } else {
                clickableComponent = clickableComponent.clickEvent(
                        net.kyori.adventure.text.event.ClickEvent.suggestCommand(command));
            }

            builder.append(clickableComponent);
            lastIndex = matcher.end();
        }

        // 残りの部分を追加
        if (lastIndex < text.length()) {
            String remaining = text.substring(lastIndex);
            builder.append(LegacyComponentSerializer.legacySection().deserialize(remaining));
        }

        return builder.build();
    }

    /**
     * 文字列から最後の色コードを抽出する
     *
     * @param text 対象文字列
     * @return 最後の色コード、見つからない場合はnull
     */
    private net.kyori.adventure.text.format.TextColor extractLastColor(String text) {
        if (text == null || text.isEmpty()) return null;

        // §x§R§R§G§G§B§B 形式（RGB）を検索
        Pattern rgbPattern = Pattern.compile("§x§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])§([0-9a-fA-F])");
        Matcher rgbMatcher = rgbPattern.matcher(text);
        String lastRgb = null;
        while (rgbMatcher.find()) {
            lastRgb = rgbMatcher.group(1) + rgbMatcher.group(2) +
                      rgbMatcher.group(3) + rgbMatcher.group(4) +
                      rgbMatcher.group(5) + rgbMatcher.group(6);
        }
        if (lastRgb != null) {
            return net.kyori.adventure.text.format.TextColor.fromHexString("#" + lastRgb);
        }

        // §[0-9a-f] 形式（標準色）を検索
        Pattern standardPattern = Pattern.compile("§([0-9a-fA-F])");
        Matcher standardMatcher = standardPattern.matcher(text);
        Character lastColor = null;
        while (standardMatcher.find()) {
            lastColor = standardMatcher.group(1).charAt(0);
        }
        if (lastColor != null) {
            return getColorFromCode(lastColor);
        }

        return null;
    }

    /**
     * 標準色コードからTextColorを取得
     */
    private net.kyori.adventure.text.format.TextColor getColorFromCode(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> net.kyori.adventure.text.format.NamedTextColor.BLACK;
            case '1' -> net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE;
            case '2' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN;
            case '3' -> net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA;
            case '4' -> net.kyori.adventure.text.format.NamedTextColor.DARK_RED;
            case '5' -> net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE;
            case '6' -> net.kyori.adventure.text.format.NamedTextColor.GOLD;
            case '7' -> net.kyori.adventure.text.format.NamedTextColor.GRAY;
            case '8' -> net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
            case '9' -> net.kyori.adventure.text.format.NamedTextColor.BLUE;
            case 'a' -> net.kyori.adventure.text.format.NamedTextColor.GREEN;
            case 'b' -> net.kyori.adventure.text.format.NamedTextColor.AQUA;
            case 'c' -> net.kyori.adventure.text.format.NamedTextColor.RED;
            case 'd' -> net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
            case 'e' -> net.kyori.adventure.text.format.NamedTextColor.YELLOW;
            case 'f' -> net.kyori.adventure.text.format.NamedTextColor.WHITE;
            default -> null;
        };
    }

    /**
     * Componentに色を適用する（shadowなどの装飾は保持）
     */
    private Component applyColorToComponent(Component component, net.kyori.adventure.text.format.TextColor color) {
        if (component instanceof net.kyori.adventure.text.TextComponent textComp) {
            // 現在のスタイルを取得し、色だけ変更
            net.kyori.adventure.text.format.Style currentStyle = textComp.style();
            net.kyori.adventure.text.format.Style newStyle = currentStyle.color(color);

            // 子コンポーネントも再帰的に処理
            java.util.List<Component> newChildren = new java.util.ArrayList<>();
            for (Component child : textComp.children()) {
                newChildren.add(applyColorToComponent(child, color));
            }

            return textComp.style(newStyle).children(newChildren);
        }
        return component.color(color);
    }
}
