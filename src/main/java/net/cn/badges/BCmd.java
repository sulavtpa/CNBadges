package net.cn.badges;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BCmd implements TabExecutor {
    private final CNBadges plugin;

    public BCmd(CNBadges plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cn.badges.admin")) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>You do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(
                    MiniMessage.miniMessage().deserialize("<red>Usage: /badge <create|tier|edit|remove|give|reload [database]>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                if (args.length < 3) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Usage: /badge give <player> <badge> [tier]"));
                    return true;
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                if (!player.hasPlayedBefore() && !player.isOnline()) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + args[1] + " has never joined the server."));
                    return true;
                }
                String badge = args[2];
                int tier = 1;
                if (args.length >= 4) {
                    try {
                        tier = Integer.parseInt(args[3]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Invalid tier number: " + args[3]));
                        return true;
                    }
                }
                plugin.getDbManager().giveBadge(player.getUniqueId(), badge, tier);
                sender.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<green>Gave " + badge + " tier " + tier + " to " + args[1] + "."));
                break;

            case "remove":
                if (args.length == 2) {
                    plugin.getDbManager().removeBadgeFromAll(args[1]);
                    plugin.getCfgManager().removeBadge(args[1]);
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<green>Removed badge " + args[1] + " entirely."));
                } else if (args.length == 3) {
                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                    if (!target.hasPlayedBefore() && !target.isOnline()) {
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Player " + args[1] + " has never joined the server."));
                        return true;
                    }
                    plugin.getDbManager().removeBadge(target.getUniqueId(), args[2]);
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<green>Removed badge " + args[2] + " from " + args[1] + "."));
                } else if (args.length == 4 && args[2].equalsIgnoreCase("tier")) {
                    try {
                        int rTier = Integer.parseInt(args[3]);
                        plugin.getCfgManager().removeTier(args[1], rTier);
                        sender.sendMessage(MiniMessage.miniMessage()
                                .deserialize("<green>Removed tier " + rTier + " from badge " + args[1] + "."));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(
                                MiniMessage.miniMessage().deserialize("<red>Invalid tier number: " + args[3]));
                    }
                } else {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<red>Usage: /badge remove <name> OR /badge remove <name> tier <tier> OR /badge remove <player> <name>"));
                }
                break;

            case "create":
                if (args.length < 3) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Usage: /badge create <name> <display>"));
                    return true;
                }
                String display = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                plugin.getCfgManager().createBadge(args[1], display);
                sender.sendMessage(MiniMessage.miniMessage()
                        .deserialize("<green>Created badge " + args[1] + " with display " + display));
                break;

            case "tier":
                if (args.length < 4) {
                    sender.sendMessage(
                            MiniMessage.miniMessage().deserialize("<red>Usage: /badge tier <name> <tier> <display>"));
                    return true;
                }
                try {
                    int t = Integer.parseInt(args[2]);
                    String tDisplay = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    plugin.getCfgManager().setTier(args[1], t, tDisplay);
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<green>Added/updated tier " + t + " for badge " + args[1] + "."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid tier number: " + args[2]));
                }
                break;

            case "edit":
                if (args.length < 5 || !args[2].equalsIgnoreCase("tier")) {
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<red>Usage: /badge edit <name> tier <tier> <display>"));
                    return true;
                }
                try {
                    int editT = Integer.parseInt(args[3]);
                    String editDisplay = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
                    plugin.getCfgManager().setTier(args[1], editT, editDisplay);
                    sender.sendMessage(MiniMessage.miniMessage()
                            .deserialize("<green>Edited tier " + editT + " for badge " + args[1] + "."));
                } catch (NumberFormatException e) {
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid tier number: " + args[3]));
                }
                break;

            case "reload":
                if (args.length > 1 && args[1].equalsIgnoreCase("database")) {
                    plugin.getDbManager().reload();
                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>CN-Badges database reload initiated."));
                    return true;
                }
                plugin.getCfgManager().loadConfig();
                plugin.getDbManager().reload();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>CN-Badges full reload initiated."));
                break;

            default:
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown command."));
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("cn.badges.admin"))
            return Collections.emptyList();

        List<String> completions = new ArrayList<>();
        List<String> commands = Arrays.asList("create", "tier", "edit", "remove", "give", "reload");

        if (args.length == 1) {
            completions.addAll(commands);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give":
                    return null;
                case "remove":
                    completions.addAll(plugin.getCfgManager().getBadgeDefs().keySet());
                    Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    break;
                case "tier":
                case "edit":
                    completions.addAll(plugin.getCfgManager().getBadgeDefs().keySet());
                    break;
                case "create":
                    completions.add("<name>");
                    break;
                case "reload":
                    completions.add("database");
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "give":
                    completions.addAll(plugin.getCfgManager().getBadgeDefs().keySet());
                    break;
                case "remove":
                    if (plugin.getCfgManager().getBadgeDefs().containsKey(args[1])) {
                        completions.add("tier");
                    }
                    if (Bukkit.getPlayerExact(args[1]) != null) {
                        completions.addAll(plugin.getCfgManager().getBadgeDefs().keySet());
                    }
                    break;
                case "tier":
                    completions.add("<tier>");
                    break;
                case "edit":
                    completions.add("tier");
                    break;
                case "create":
                    completions.add("<display>");
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "give":
                    completions.add("[tier]");
                    break;
                case "remove":
                    if (args[2].equalsIgnoreCase("tier")) {
                        CfgManager.BDef def = plugin.getCfgManager().getBadgeDefs().get(args[1]);
                        if (def != null) {
                            def.tiers.keySet().forEach(t -> completions.add(String.valueOf(t)));
                        }
                    }
                    break;
                case "tier":
                    completions.add("<display>");
                    break;
                case "edit":
                    if (args[2].equalsIgnoreCase("tier")) {
                        CfgManager.BDef def = plugin.getCfgManager().getBadgeDefs().get(args[1]);
                        if (def != null) {
                            def.tiers.keySet().forEach(t -> completions.add(String.valueOf(t)));
                        }
                    }
                    break;
            }
        } else if (args.length == 5) {
            if (args[0].equalsIgnoreCase("edit")) {
                completions.add("<display>");
            }
        }

        if (completions.isEmpty())
            return Collections.emptyList();

        String lastWord = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastWord))
                .collect(Collectors.toList());
    }
}