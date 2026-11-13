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
                    sender.sendMessage("§cСоздавать мячи может только игрок.");
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
                    sender.sendMessage("§aУдалено мячей во всех мирах: " + removed);
                } else {
                    int removed = balls.count();
                    balls.clear();
                    sender.sendMessage("§aУдалено мячей: " + removed);
                }
            }
            case "reload" -> {
                if (!sender.hasPermission("headball.reload")) {
                    deny(sender);
                    return true;
                }
                plugin.reloadSettings();
                sender.sendMessage("§aКонфиг перезагружен. Пересоздай мяч.");
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
        sender.sendMessage("§cНедостаточно прав.");
    }

    private void spawn(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eДоступные мячи: §f[ball | volleyball]");
            player.sendMessage("§7Использование: §e/headball spawn <ball | volleyball>");
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);
        Location location = spawnLocation(player);
        switch (type) {
            case "ball" -> {
                if (balls.spawn(location, player) == null) {
                    limitReached(player);
                    return;
                }
                player.sendMessage("§aФутбольный мяч создан.");
            }
            case "volleyball" -> {
                if (balls.spawnVolleyball(location, player) == null) {
                    limitReached(player);
                    return;
                }
                player.sendMessage("§aВолейбольный мяч создан.");
            }
            default -> {
                player.sendMessage("§cНеизвестный мяч: §f" + args[1]);
                player.sendMessage("§eДоступные мячи: §f[ball | volleyball]");
            }
        }
    }

    private void limitReached(Player player) {
        player.sendMessage("§cДостигнут лимит мячей: §f" + balls.maxBalls());
    }

    private Location spawnLocation(Player player) {
        return player.getLocation()
                .add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5))
                .add(0, 0.5, 0);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§e§lHeadBall §7— команды:");
        sender.sendMessage("§e/headball spawn <ball|volleyball> §7— создать мяч");
        sender.sendMessage("§e/headball remove [all] §7— удалить мячи");
        sender.sendMessage("§e/headball reload §7— перезагрузить конфиг");
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
