package com.afox.headball;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HeadBallCommand implements CommandExecutor {

    private final HeadBall plugin;
    private final BallManager balls;

    public HeadBallCommand(HeadBall plugin, BallManager balls) {
        this.plugin = plugin;
        this.balls = balls;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Команда только для игроков.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§e/headball spawn §7| §e/headball remove §7| §e/headball reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                Location location = player.getLocation()
                        .add(player.getLocation().getDirection().setY(0).normalize().multiply(1.5))
                        .add(0, 0.5, 0);
                balls.spawn(location, player);
                player.sendMessage("§aМяч создан.");
            }
            case "remove" -> {
                int removed = balls.count();
                balls.clear();
                player.sendMessage("§aУдалено мячей: " + removed);
            }
            case "reload" -> {
                plugin.reloadSettings();
                player.sendMessage("§aКонфиг перезагружен. Пересоздай мяч.");
            }
            default -> player.sendMessage("§e/headball spawn §7| §e/headball remove §7| §e/headball reload");
        }
        return true;
    }
}
