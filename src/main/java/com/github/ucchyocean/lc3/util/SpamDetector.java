/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.util;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * スパム検出ユーティリティクラス
 *
 * プレイヤーの連続したメッセージの類似度を計算し、スパムを検出します。
 * AntiSpamプラグイン（https://github.com/mofucraft/AntiSpam）の機能を統合しています。
 *
 * @author ucchy
 */
public class SpamDetector {

    /** プレイヤー名と最後のメッセージのマップ */
    private static final Map<String, String> lastMessages = new HashMap<>();

    /** スパムフラグが立っているプレイヤーのセット */
    private static final Set<String> spamFlagged = new HashSet<>();

    /** Jaro-Winkler距離計算機 */
    private static final JaroWinklerDistance jaroWinklerDistance = new JaroWinklerDistance();

    /** Levenshtein距離計算機 */
    private static final LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

    /** 挨拶パターン（15文字以内でこれらのキーワードを含む場合は挨拶と判定） */
    private static final Pattern GREETING_PATTERN = Pattern.compile(
        ".*(こん|hello|hi|yoro|kon|よろ|oha|おは).*",
        Pattern.CASE_INSENSITIVE
    );

    /** 挨拶メッセージの最大長 */
    private static final int GREETING_MAX_LENGTH = 15;

    /**
     * メッセージがスパムかどうかを判定します。
     *
     * @param playerName プレイヤー名
     * @param message メッセージ
     * @param threshold 類似度の閾値（0.0-1.0、デフォルトは0.8）
     * @param useJaroWinkler Jaro-Winkler距離を使用するかどうか（falseの場合はLevenshtein距離）
     * @param excludeGreetings 挨拶メッセージを除外するかどうか
     * @return スパム判定結果
     */
    public static SpamCheckResult checkSpam(
            String playerName,
            String message,
            double threshold,
            boolean useJaroWinkler,
            boolean excludeGreetings) {

        // 挨拶メッセージの除外
        if (excludeGreetings && isGreeting(message)) {
            return new SpamCheckResult(false, false, 0.0);
        }

        // 前回のメッセージを取得
        String lastMessage = lastMessages.get(playerName);

        // 初回メッセージの場合
        if (lastMessage == null) {
            lastMessages.put(playerName, message);
            return new SpamCheckResult(false, false, 0.0);
        }

        // 類似度を計算
        double similarity = calculateSimilarity(lastMessage, message, useJaroWinkler);

        // スパム判定
        boolean isSpam = similarity >= threshold;
        boolean isRepeatOffender = spamFlagged.contains(playerName);

        if (isSpam) {
            if (isRepeatOffender) {
                // 繰り返し違反者の場合、メッセージを更新しない
                return new SpamCheckResult(true, true, similarity);
            } else {
                // 初回違反の場合、フラグを立てる
                spamFlagged.add(playerName);
                lastMessages.put(playerName, message);
                return new SpamCheckResult(true, false, similarity);
            }
        } else {
            // スパムでない場合、フラグをクリアしてメッセージを更新
            spamFlagged.remove(playerName);
            lastMessages.put(playerName, message);
            return new SpamCheckResult(false, false, similarity);
        }
    }

    /**
     * 2つの文字列の類似度を計算します。
     *
     * @param str1 文字列1
     * @param str2 文字列2
     * @param useJaroWinkler Jaro-Winkler距離を使用するかどうか
     * @return 類似度（0.0-1.0）
     */
    public static double calculateSimilarity(String str1, String str2, boolean useJaroWinkler) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        if (str1.equals(str2)) {
            return 1.0;
        }

        if (useJaroWinkler) {
            // Jaro-Winkler距離を使用（0.0-1.0、1.0が完全一致）
            return jaroWinklerDistance.apply(str1, str2);
        } else {
            // Levenshtein距離を使用し、正規化して類似度に変換
            int distance = levenshteinDistance.apply(str1, str2);
            int maxLength = Math.max(str1.length(), str2.length());
            if (maxLength == 0) {
                return 1.0;
            }
            return 1.0 - ((double) distance / maxLength);
        }
    }

    /**
     * メッセージが挨拶かどうかを判定します。
     *
     * @param message メッセージ
     * @return 挨拶の場合true
     */
    public static boolean isGreeting(String message) {
        if (message == null || message.length() > GREETING_MAX_LENGTH) {
            return false;
        }

        return GREETING_PATTERN.matcher(message).matches();
    }

    /**
     * 特定のプレイヤーのスパムデータをクリアします。
     *
     * @param playerName プレイヤー名
     */
    public static void clearPlayerData(String playerName) {
        lastMessages.remove(playerName);
        spamFlagged.remove(playerName);
    }

    /**
     * 全てのスパムデータをクリアします。
     */
    public static void clearAllData() {
        lastMessages.clear();
        spamFlagged.clear();
    }

    /**
     * スパムチェック結果を保持するクラス
     */
    public static class SpamCheckResult {
        private final boolean isSpam;
        private final boolean isRepeatOffender;
        private final double similarity;

        public SpamCheckResult(boolean isSpam, boolean isRepeatOffender, double similarity) {
            this.isSpam = isSpam;
            this.isRepeatOffender = isRepeatOffender;
            this.similarity = similarity;
        }

        /**
         * スパムかどうかを返します。
         *
         * @return スパムの場合true
         */
        public boolean isSpam() {
            return isSpam;
        }

        /**
         * 繰り返し違反者かどうかを返します。
         *
         * @return 繰り返し違反者の場合true
         */
        public boolean isRepeatOffender() {
            return isRepeatOffender;
        }

        /**
         * 類似度を返します。
         *
         * @return 類似度（0.0-1.0）
         */
        public double getSimilarity() {
            return similarity;
        }
    }
}
