/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.bukkit.event;

import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMember;

/**
 * チャンネル作成イベント
 *
 * @author ucchy
 */
public class LunaChatBukkitChannelCreateEvent extends LunaChatBukkitBaseCancellableEvent {

    private final ChannelMember member;

    public LunaChatBukkitChannelCreateEvent(String channelName, ChannelMember member) {
        super(channelName);
        this.member = member;
    }

    /**
     * 作成するチャンネルのチャンネル名を上書き設定する
     *
     * @param channelName 設定するチャンネル名
     */
    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
     * チャンネルを作成した人を取得する。
     *
     * @return チャンネルを作成したChannelMember
     */
    public ChannelMember getMember() {
        return member;
    }

    /**
     * @see com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitBaseEvent#getChannel()
     * @deprecated チャンネル作成イベントは、チャンネルを作成する前に呼び出されるので、
     * このメソッドの戻り値は必ずnullになります。
     */
    @Override
    public Channel getChannel() {
        return super.getChannel();
    }
}
