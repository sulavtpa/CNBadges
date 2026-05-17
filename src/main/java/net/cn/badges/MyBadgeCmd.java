package net.cn.badges;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MyBadgeCmd implements TabExecutor {
    private final CNBadges plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_TIME = 5000L;
    private static final List<String> SYMBOLS = Arrays.asList("✧", "✦", "★");
    private static final List<String> COLORS = Arrays.asList(
            "black", "dark_gray", "dark_blue", "blue", "dark_green", "green",
            "dark_aqua", "aqua", "dark_red", "red", "dark_purple", "light_purple",
            "gold", "yellow", "gray", "white"
    );

    public MyBadgeCmd(CNBadges plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID uuid = player.getUniqueId();
        SpecialBadgeData data = plugin.getBadgeCache().getSpecialBadge(uuid);

        if (data == null || !data.hasPermission()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You do not have permission to use the special badge."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Usage: /mybadge <badge> <color>"));
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (cooldowns.containsKey(uuid)) {
            long timeLeft = (cooldowns.get(uuid) + COOLDOWN_TIME) - currentTime;
            if (timeLeft > 0) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please wait " + (timeLeft / 1000) + " seconds before using this command again."));
                return true;
            }
        }

        String symbol = args[0];
        String color = args[1].toLowerCase();

        if (!SYMBOLS.contains(symbol)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid badge symbol."));
            return true;
        }

        if (!COLORS.contains(color)) {
            player.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid color."));
            return true;
        }

        plugin.getDbManager().setSpecialBadge(uuid, symbol, color);
        cooldowns.put(uuid, currentTime);
        player.sendMessage(MiniMessage.miniMessage().deserialize("<green>Your special badge has been updated to <" + color + ">" + symbol + "</" + color + ">"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        SpecialBadgeData data = plugin.getBadgeCache().getSpecialBadge(player.getUniqueId());
        
        if (data == null || !data.hasPermission()) {
            return null;
        }

        if (args.length == 1) {
            return SYMBOLS.stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return COLORS.stream()
                    .filter(c -> c.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        return null;
    }
}
