/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.bungee.event;

import com.github.ucchyocean.lc3.member.ChannelMember;

/**
 * Japanize変換が行われた後に呼び出されるイベント
 *
 * @author ucchy
 */
public class LunaChatBungeePostJapanizeEvent extends LunaChatBungeeBaseCancellableEvent {

    private final ChannelMember member;
    private final String original;
    private String japanized;

    /**
     * コンストラクタ
     *
     * @param channelName チャンネル名
     * @param member      発言したプレイヤー
     * @param original    変換前の文字列
     * @param japanized   変換後の文字列
     */
    public LunaChatBungeePostJapanizeEvent(String channelName, ChannelMember member,
                                           String original, String japanized) {
        super(channelName);
        this.member = member;
        this.original = original;
        this.japanized = japanized;
    }

    /**
     * 発言を行ったプレイヤーを取得します。
     *
     * @return 発言したプレイヤー
     */
    public ChannelMember getMember() {
        return member;
    }

    /**
     * Japanize変換後の文字列を返す
     *
     * @return 変換後の文字列
     */
    public String getJapanized() {
        return japanized;
    }

    /**
     * Japanize変換後の文字列を差し替える
     *
     * @param japanized 変換後の文字列
     */
    public void setJapanized(String japanized) {
        this.japanized = japanized;
    }

    /**
     * Japanize変換前の文字列を返す
     *
     * @return 変換前の文字列
     */
    public String getOriginal() {
        return original;
    }
}
