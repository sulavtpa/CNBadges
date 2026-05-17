package net.cn.badges;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class CNBadges extends JavaPlugin implements Listener {

    private CfgManager configManager;
    private DbManager databaseManager;
    private BCache badgeCache;

    @Override
    public void onEnable() {
        configManager = new CfgManager(this);
        configManager.loadConfig();

        badgeCache = new BCache(this);

        databaseManager = new DbManager(this);
        databaseManager.connect();

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("badge").setExecutor(new BCmd(this));

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BadgePlaceholder().register();
        } else {
            getLogger().warning("PlaceholderAPI not found. Placeholders will not work.");
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        if (badgeCache != null) {
            badgeCache.clear();
        }
    }

    public CfgManager getCfgManager() {
        return configManager;
    }

    public DbManager getDbManager() {
        return databaseManager;
    }

    public BCache getBadgeCache() {
        return badgeCache;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        databaseManager.loadplayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        badgeCache.autoremover(event.getPlayer().getUniqueId());
    }

    private class BadgePlaceholder extends PlaceholderExpansion {
        @Override
        public String getIdentifier() {
            return "cnbadges";
        }

        @Override
        public String getAuthor() {
            return "Antigravity";
        }

        @Override
        public String getVersion() {
            return "1.0";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public String onRequest(OfflinePlayer player, String params) {
            if (player == null || badgeCache == null)
                return "";

            Map<String, Integer> pBadges = badgeCache.getBadge(player.getUniqueId());
            if (pBadges == null || pBadges.isEmpty())
                return "";

            Map<String, CfgManager.BDef> definitions = configManager.getBadgeDefs();
            List<Map.Entry<String, Integer>> targets = new ArrayList<>();

            for (Map.Entry<String, Integer> entry : pBadges.entrySet()) {
                if (definitions.containsKey(entry.getKey())) {
                    targets.add(entry);
                }
            }

            if (targets.isEmpty())
                return "";

            targets.sort((e1, e2) -> Integer.compare(
                    definitions.get(e2.getKey()).weight,
                    definitions.get(e1.getKey()).weight));

            StringJoiner result = new StringJoiner(" ");
            for (Map.Entry<String, Integer> entry : targets) {
                String rawDisplay = definitions.get(entry.getKey()).tiers.get(entry.getValue());
                if (rawDisplay != null) {
                    String legacy = LegacyComponentSerializer.legacySection()
                            .serialize(MiniMessage.miniMessage().deserialize(rawDisplay));
                    result.add(legacy);
                }
            }

            return result.toString();
        }
    }
}