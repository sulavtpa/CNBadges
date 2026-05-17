package net.cn.badges;

import org.bukkit.Bukkit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BCache {
    private final Map<UUID, Map<String, Integer>> playerBadges = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> taskIds = new ConcurrentHashMap<>();
    private final CNBadges plugin;

    public BCache(CNBadges plugin) {
        this.plugin = plugin;
    }

    public void loadplayer(UUID uuid, Map<String, Integer> badges) {
        playerBadges.put(uuid, new ConcurrentHashMap<>(badges));
        cancelRemovalTask(uuid);
    }

    public void autoremover(UUID uuid) {
        int taskId = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            playerBadges.remove(uuid);
            taskIds.remove(uuid);
        }, 200L).getTaskId();
        taskIds.put(uuid, taskId);
    }

    public void cancelRemovalTask(UUID uuid) {
        Integer taskId = taskIds.remove(uuid);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public void addBadge(UUID uuid, String badge, int tier) {
        playerBadges.computeIfAbsent(uuid, key -> new ConcurrentHashMap<>()).put(badge, tier);
    }

    public void remBadge(UUID uuid, String badge) {
        Map<String, Integer> badges = playerBadges.get(uuid);
        if (badges != null) {
            badges.remove(badge);
        }
    }

    public Map<String, Integer> getBadge(UUID uuid) {
        return playerBadges.get(uuid);
    }

    public void clear() {
        playerBadges.clear();
        for (int taskId : taskIds.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        taskIds.clear();
    }
}