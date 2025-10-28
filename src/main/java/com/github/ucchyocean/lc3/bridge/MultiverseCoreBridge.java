/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package com.github.ucchyocean.lc3.bridge;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

/**
 * Multiverse-Core 連携クラス（v4/v5 両対応）。
 *
 * - v5: org.mvplugins.multiverse.core.MultiverseCoreApi 経由で取得
 * - v4: 旧APIをリフレクションで呼び出し
 * - 未導入: null を返すため、呼び出し側でフォールバック（従来通り）
 */
public class MultiverseCoreBridge {

    private final Adapter adapter;

    private MultiverseCoreBridge(Adapter adapter) {
        this.adapter = adapter;
    }

    /**
     * MultiverseCore API ブリッジをロードする。
     *
     * @param plugin Multiverse-Core のプラグインインスタンス（v5 でも名前は同じ）
     * @return 利用可能ならブリッジ、不可なら null
     */
    public static MultiverseCoreBridge load(Plugin plugin) {
        Adapter adapter = AdapterFactory.create(plugin);
        return adapter.isAvailable() ? new MultiverseCoreBridge(adapter) : null;
    }

    /**
     * 指定されたワールド名のエイリアス名を取得する。
     * 取得できない場合は、元のワールド名を返す。
     */
    public String getWorldAlias(String worldName) {
        return adapter.getWorldAlias(worldName);
    }

    /**
     * 指定されたワールドのエイリアス名を取得する。
     * 取得できない場合は、元のワールド名を返す。
     */
    public String getWorldAlias(World world) {
        return adapter.getWorldAlias(world);
    }

    /**
     * 内部アダプタのファクトリ。
     */
    static class AdapterFactory {
        static Adapter create(Plugin plugin) {
            // v5 の API クラスが存在するかをまず確認
            try {
                Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
                Adapter v5 = new V5Adapter();
                if (v5.isAvailable()) return v5;
            } catch (ClassNotFoundException ignore) {
                // fallthrough
            }

            // v4 のクラスが見える/利用可能かリフレクションで確認
            Adapter v4 = new V4Adapter(plugin);
            if (v4.isAvailable()) return v4;

            return new NoOpAdapter();
        }
    }

    /**
     * 実装切り替え用インターフェース。
     */
    interface Adapter {
        boolean isAvailable();

        String getWorldAlias(String worldName);

        String getWorldAlias(World world);
    }

    /**
     * Multiverse-Core v5 アダプタ。
     * 依存は compileOnly。Option などは反射で扱う。
     */
    static class V5Adapter implements Adapter {
        @Override
        public boolean isAvailable() {
            try {
                Class<?> apiClazz = Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
                // 呼び出し可能か（get() が例外を投げない状態か）を軽く確認
                Object api = apiClazz.getMethod("get").invoke(null);
                return api != null;
            } catch (Throwable t) {
                return false;
            }
        }

        @Override
        public String getWorldAlias(String worldName) {
            try {
                // MultiverseCoreApi api = MultiverseCoreApi.get();
                Class<?> apiClazz = Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
                Object api = apiClazz.getMethod("get").invoke(null);

                // WorldManager wm = api.getWorldManager();
                Object wm = apiClazz.getMethod("getWorldManager").invoke(api);

                // Option<MultiverseWorld> opt = wm.getWorldByNameOrAlias(worldName)
                Object opt = wm.getClass()
                        .getMethod("getWorldByNameOrAlias", String.class)
                        .invoke(wm, worldName);

                // if (opt.isDefined()) alias = opt.get().getAliasOrName(); else worldName
                Boolean defined = (Boolean) opt.getClass().getMethod("isDefined").invoke(opt);
                if (defined) {
                    Object mvw = opt.getClass().getMethod("get").invoke(opt);
                    String aliasOrName = (String) mvw.getClass().getMethod("getAliasOrName").invoke(mvw);
                    return (aliasOrName != null && !aliasOrName.isEmpty()) ? aliasOrName : worldName;
                }
            } catch (Throwable ignore) {
                // fall through to fallback
            }
            return worldName;
        }

        @Override
        public String getWorldAlias(World world) {
            if (world == null) return "";
            try {
                // MultiverseCoreApi api = MultiverseCoreApi.get();
                Class<?> apiClazz = Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi");
                Object api = apiClazz.getMethod("get").invoke(null);

                // WorldManager wm = api.getWorldManager();
                Object wm = apiClazz.getMethod("getWorldManager").invoke(api);

                // Option<LoadedMultiverseWorld> opt = wm.getLoadedWorld(world)
                Object opt = wm.getClass()
                        .getMethod("getLoadedWorld", Class.forName("org.bukkit.World"))
                        .invoke(wm, world);

                Boolean defined = (Boolean) opt.getClass().getMethod("isDefined").invoke(opt);
                if (defined) {
                    Object mvw = opt.getClass().getMethod("get").invoke(opt);
                    String aliasOrName = (String) mvw.getClass().getMethod("getAliasOrName").invoke(mvw);
                    if (aliasOrName != null && !aliasOrName.isEmpty()) return aliasOrName;
                } else {
                    // 未ロードの場合は名前で取得
                    return getWorldAlias(world.getName());
                }
            } catch (Throwable ignore) {
                // fall through
            }
            return world.getName();
        }
    }

    /**
     * Multiverse-Core v4 アダプタ（リフレクション使用）。
     */
    static class V4Adapter implements Adapter {
        private final Plugin plugin;

        V4Adapter(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean isAvailable() {
            try {
                if (plugin == null) return false;
                // 旧パッケージが存在するかどうか
                Class.forName("com.onarandombox.MultiverseCore.MultiverseCore");
                // getMVWorldManager が呼べるか簡易チェック
                plugin.getClass().getMethod("getMVWorldManager");
                return true;
            } catch (Throwable t) {
                return false;
            }
        }

        @Override
        public String getWorldAlias(String worldName) {
            try {
                Object mgr = plugin.getClass().getMethod("getMVWorldManager").invoke(plugin);
                Object mvworld = mgr.getClass().getMethod("getMVWorld", String.class).invoke(mgr, worldName);
                if (mvworld != null) {
                    String alias = (String) mvworld.getClass().getMethod("getAlias").invoke(mvworld);
                    if (alias != null && alias.length() > 0) return alias;
                    String name = (String) mvworld.getClass().getMethod("getName").invoke(mvworld);
                    if (name != null && name.length() > 0) return name;
                }
            } catch (Throwable ignore) {
                // fall through
            }
            return worldName;
        }

        @Override
        public String getWorldAlias(World world) {
            if (world == null) return "";
            try {
                Object mgr = plugin.getClass().getMethod("getMVWorldManager").invoke(plugin);
                Object mvworld = mgr.getClass().getMethod("getMVWorld", Class.forName("org.bukkit.World"))
                        .invoke(mgr, world);
                if (mvworld != null) {
                    String alias = (String) mvworld.getClass().getMethod("getAlias").invoke(mvworld);
                    if (alias != null && alias.length() > 0) return alias;
                    String name = (String) mvworld.getClass().getMethod("getName").invoke(mvworld);
                    if (name != null && name.length() > 0) return name;
                }
            } catch (Throwable ignore) {
                // fall through
            }
            return world.getName();
        }
    }

    /**
     * Multiverse-Core 未導入時のダミー。
     */
    static class NoOpAdapter implements Adapter {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public String getWorldAlias(String worldName) {
            return worldName;
        }

        @Override
        public String getWorldAlias(World world) {
            return world == null ? "" : world.getName();
        }
    }
}
