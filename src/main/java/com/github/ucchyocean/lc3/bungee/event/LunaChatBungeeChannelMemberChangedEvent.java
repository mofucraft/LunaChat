/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.bungee.event;

import com.github.ucchyocean.lc3.member.ChannelMember;

import java.util.List;

/**
 * メンバー変更イベント
 *
 * @author ucchy
 */
public class LunaChatBungeeChannelMemberChangedEvent extends LunaChatBungeeBaseCancellableEvent {

    private final List<ChannelMember> before;
    private final List<ChannelMember> after;

    /**
     * コンストラクタ
     *
     * @param channelName チャンネル名
     * @param before      変更前のメンバー
     * @param after       変更後のメンバー
     */
    public LunaChatBungeeChannelMemberChangedEvent(
            String channelName, List<ChannelMember> before, List<ChannelMember> after) {
        super(channelName);
        this.before = before;
        this.after = after;
    }

    /**
     * 変更前のメンバーリストをかえす
     *
     * @return
     */
    public List<ChannelMember> getMembersBefore() {
        return before;
    }

    /**
     * 変更後のメンバーリストをかえす
     *
     * @return
     */
    public List<ChannelMember> getMembersAfter() {
        return after;
    }
}
