/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.lc.event;

import com.github.ucchyocean.lc.channel.ChannelPlayer;

import java.util.List;

/**
 * メンバー変更イベント
 *
 * @author ucchy
 * @deprecated Legacy Version
 */
public class LunaChatChannelMemberChangedEvent extends LunaChatBaseCancellableEvent {

    private final List<ChannelPlayer> before;
    private final List<ChannelPlayer> after;

    /**
     * コンストラクタ
     *
     * @param channelName チャンネル名
     * @param before      変更前のメンバー
     * @param after       変更後のメンバー
     */
    public LunaChatChannelMemberChangedEvent(
            String channelName, List<ChannelPlayer> before, List<ChannelPlayer> after) {
        super(channelName);
        this.before = before;
        this.after = after;
    }

    /**
     * 変更前のメンバーリストをかえす
     *
     * @return
     */
    public List<ChannelPlayer> getMembersBefore() {
        return before;
    }

    /**
     * 変更後のメンバーリストをかえす
     *
     * @return
     */
    public List<ChannelPlayer> getMembersAfter() {
        return after;
    }
}
