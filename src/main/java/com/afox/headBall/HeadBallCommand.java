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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда только для игроков.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> spawn(player, args);
            case "remove" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("all")) {
                    int removed = balls.removeAll();
                    player.sendMessage("§aУдалено мячей во всех мирах: " + removed);
                } else {
                    int removed = balls.count();
                    balls.clear();
                    player.sendMessage("§aУдалено мячей: " + removed);
                }
            }
            case "reload" -> {
                plugin.reloadSettings();
                player.sendMessage("§aКонфиг перезагружен. Пересоздай мяч.");
            }
            default -> showHelp(player);
        }
        return true;
    }

    private void spawn(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§eДоступные мячи: §f[ball | volleyball]");
            player.sendMessage("§7Использование: §e/headball spawn <ball|volleyball>");
            return;
        }

        String type = args[1].toLowerCase();
        Location location = spawnLocation(player);
        switch (type) {
            case "ball" -> {
                balls.spawn(location, player);
                player.sendMessage("§aФутбольный мяч создан.");
            }
            case "volleyball" -> {
                balls.spawnVolleyball(location, player);
                player.sendMessage("§aВолейбольный мяч создан.");
            }
            default -> {
                player.sendMessage("§cНеизвестный мяч: §f" + args[1]);
                player.sendMessage("§eДоступные мячи: §f[ball | volleyball]");
            }
        }
    }

    private Location spawnLocation(Player player) {
        return player.getLocation()
                .add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5))
                .add(0, 0.5, 0);
    }

    private void showHelp(Player player) {
        player.sendMessage("§e§lHeadBall §7— команды:");
        player.sendMessage("§e/headball spawn <ball|volleyball> §7— создать мяч");
        player.sendMessage("§e/headball remove [all] §7— удалить мячи");
        player.sendMessage("§e/headball reload §7— перезагрузить конфиг");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            suggestions.add("spawn");
            suggestions.add("remove");
            suggestions.add("reload");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> {
                    suggestions.add("ball");
                    suggestions.add("volleyball");
                }
                case "remove" -> suggestions.add("all");
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
