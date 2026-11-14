package com.afox.headball;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeadBallCommand implements CommandExecutor, TabCompleter {

    private final HeadBall plugin;
    private final BallManager balls;

    public HeadBallCommand(HeadBall plugin, BallManager balls) {
        this.plugin = plugin;
        this.balls = balls;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            if (!sender.hasPermission("headball.use")) {
                deny(sender);
                return true;
            }
            showHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spawn" -> {
                if (!sender.hasPermission("headball.use")) {
                    deny(sender);
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    plugin.getMessages().send(sender, "spawn-player-only");
                    return true;
                }
                spawn(player, args);
            }
            case "remove" -> {
                if (!sender.hasPermission("headball.remove")) {
                    deny(sender);
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    int removed = balls.removeAll();
                    plugin.getMessages().send(sender, "removed-all", "count", removed);
                } else {
                    int removed = balls.count();
                    balls.clear();
                    plugin.getMessages().send(sender, "removed", "count", removed);
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("headball.reload")) {
                    deny(sender);
                    return true;
                }
                plugin.reloadSettings();
                plugin.getMessages().send(sender, "reloaded");
            }
            default -> {
                if (!sender.hasPermission("headball.use")) {
                    deny(sender);
                    return true;
                }
                showHelp(sender);
            }
        }
        return true;
    }

    private void deny(CommandSender sender) {
        plugin.getMessages().send(sender, "no-permission");
    }

    private void spawn(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(player, "available-balls");
            plugin.getMessages().send(player, "spawn-usage");
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        Location location = spawnLocation(player);
        switch (type) {
            case "ball" -> {
                if (balls.spawn(location, player) == null) {
                    balls.sendLimitMessage(player);
                    return;
                }
                plugin.getMessages().send(player, "spawned-football");
            }
            case "volleyball" -> {
                if (balls.spawnVolleyball(location, player) == null) {
                    balls.sendLimitMessage(player);
                    return;
                }
                plugin.getMessages().send(player, "spawned-volleyball");
            }
            default -> {
                plugin.getMessages().send(player, "unknown-ball", "type", args[1]);
                plugin.getMessages().send(player, "available-balls");
            }
        }
    }

    private Location spawnLocation(Player player) {
        return player.getLocation()
                .add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5))
                .add(0, 0.5, 0);
    }

    private void showHelp(CommandSender sender) {
        plugin.getMessages().send(sender, "help-header");
        plugin.getMessages().send(sender, "help-spawn");
        plugin.getMessages().send(sender, "help-remove");
        plugin.getMessages().send(sender, "help-reload");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("headball.use")) {
                suggestions.add("spawn");
            }
            if (sender.hasPermission("headball.remove")) {
                suggestions.add("remove");
            }
            if (sender.hasPermission("headball.reload")) {
                suggestions.add("reload");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> {
                    if (sender.hasPermission("headball.use")) {
                        suggestions.add("ball");
                        suggestions.add("volleyball");
                    }
                }
                case "remove" -> {
                    if (sender.hasPermission("headball.remove")) {
                        suggestions.add("all");
                    }
                }
                default -> {
                }
            }
        }

        if (suggestions.isEmpty()) {
            return suggestions;
        }

        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        suggestions.removeIf(suggestion -> !suggestion.startsWith(last));
        return suggestions;
    }
}
