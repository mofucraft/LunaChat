/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.command;

import com.github.ucchyocean.lc3.Messages;
import com.github.ucchyocean.lc3.member.ChannelMember;

/**
 * dictionaryコマンドのエイリアス実行クラス、名前のみが異なるが、他は全て一緒。
 *
 * @author ucchy
 */
public class DicCommand extends DictionaryCommand {

    private static final String COMMAND_NAME = "dic";

    /**
     * コマンドを取得します。
     *
     * @return コマンド
     * @see com.github.ucchyocean.lc3.command.LunaChatSubCommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return COMMAND_NAME;
    }

    /**
     * 使用方法に関するメッセージをsenderに送信します。
     *
     * @param sender コマンド実行者
     * @param label  実行ラベル
     * @see com.github.ucchyocean.lc3.command.LunaChatSubCommand#sendUsageMessage()
     */
    @Override
    public void sendUsageMessage(
            ChannelMember sender, String label) {
        sender.sendMessage(Messages.usageDic(label));
    }
}
