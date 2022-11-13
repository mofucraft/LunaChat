/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2013
 */
package com.github.ucchyocean.lc.event;

import com.github.ucchyocean.lc.channel.Channel;
import org.bukkit.command.CommandSender;

/**
 * チャンネル作成イベント
 *
 * @author ucchy
 * @deprecated Legacy Version
 */
public class LunaChatChannelCreateEvent extends LunaChatBaseCancellableEvent {

    private final CommandSender sender;

    public LunaChatChannelCreateEvent(String channelName, CommandSender sender) {
        super(channelName);
        this.sender = sender;
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
     * @return チャンネルを作成したCommandSender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * @see com.github.ucchyocean.lc.event.LunaChatBaseEvent#getChannel()
     * @deprecated チャンネル作成イベントは、チャンネルを作成する前に呼び出されるので、
     * このメソッドの戻り値は必ずnullになります。
     */
    @Override
    public Channel getChannel() {
        return super.getChannel();
    }
}
