package net.cn.badges;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CfgManager {
    private final CNBadges plugin;
    private Toml config;
    private final File configFile;

    private String dbHost, dbName, dbUser, dbPassword;
    private long dbPort;

    private final Map<String, BDef> badgeDefinitions = new HashMap<>();

    public CfgManager(CNBadges plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.toml");
    }

    public void loadConfig() {
        if (!plugin.getDataFolder().exists())
            plugin.getDataFolder().mkdirs();
        if (!configFile.exists()) {
            plugin.saveResource("config.toml", false);
        }

        config = new Toml().read(configFile);

        dbHost = config.getString("database.host", "127.0.0.1");
        dbPort = config.getLong("database.port", 3306L);
        dbName = config.getString("database.database", "badges_db");
        dbUser = config.getString("database.username", "root");
        dbPassword = config.getString("database.password", "password");

        badgeDefinitions.clear();
        Toml badgesToml = config.getTable("badges");
        if (badgesToml != null) {
            for (String badgeId : badgesToml.toMap().keySet()) {
                Toml badgeData = badgesToml.getTable(badgeId);
                if (badgeData == null)
                    continue;

                long weight = badgeData.getLong("weight", 1L);
                BDef def = new BDef((int) weight);

                Toml tiersToml = badgeData.getTable("tiers");
                if (tiersToml != null) {
                    for (Map.Entry<String, Object> tEntry : tiersToml.toMap().entrySet()) {
                        try {
                            def.tiers.put(Integer.parseInt(tEntry.getKey()), tEntry.getValue().toString());
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning(
                                    "Invalid tier key in config for badge " + badgeId + ": " + tEntry.getKey());
                        }
                    }
                }
                badgeDefinitions.put(badgeId, def);
            }
        }
    }

    public void saveConfig() {
        Map<String, Object> map = config.toMap();
        Map<String, Object> badgesMap = new HashMap<>();
        for (Map.Entry<String, BDef> entry : badgeDefinitions.entrySet()) {
            Map<String, Object> bData = new HashMap<>();
            bData.put("weight", entry.getValue().weight);
            Map<String, String> stringTiers = new HashMap<>();
            for (Map.Entry<Integer, String> t : entry.getValue().tiers.entrySet()) {
                stringTiers.put(String.valueOf(t.getKey()), t.getValue());
            }
            bData.put("tiers", stringTiers);
            badgesMap.put(entry.getKey(), bData);
        }
        map.put("badges", badgesMap);

        try {
            new TomlWriter().write(map, configFile);
            config = new Toml().read(configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save config.toml");
        }
    }

    public void createBadge(String name, String display) {
        BDef def = new BDef(1);
        def.tiers.put(1, display);
        badgeDefinitions.put(name, def);
        saveConfig();
    }

    public void setTier(String name, int tier, String display) {
        BDef def = badgeDefinitions.computeIfAbsent(name, level -> new BDef(1));
        def.tiers.put(tier, display);
        saveConfig();
    }

    public void removeTier(String name, int tier) {
        BDef def = badgeDefinitions.get(name);
        if (def != null) {
            def.tiers.remove(tier);
            saveConfig();
        }
    }

    public void removeBadge(String name) {
        badgeDefinitions.remove(name);
        saveConfig();
    }

    public String getDbHost() {
        return dbHost;
    }

    public long getDbPort() {
        return dbPort;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public Map<String, BDef> getBadgeDefs() {
        return badgeDefinitions;
    }

    public static class BDef {
        public int weight;
        public final Map<Integer, String> tiers = new HashMap<>();

        public BDef(int weight) {
            this.weight = weight;
        }
    }
}