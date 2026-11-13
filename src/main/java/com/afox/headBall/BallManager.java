package com.afox.headball;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BallManager {

    private final HeadBall plugin;
    private final List<Ball> balls = new ArrayList<>();
    private BukkitTask task;

    public BallManager(HeadBall plugin) {
        this.plugin = plugin;
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    private void tickAll() {
        Iterator<Ball> iterator = balls.iterator();
        while (iterator.hasNext()) {
            Ball ball = iterator.next();
            if (!ball.isAlive()) {
                ball.remove();
                iterator.remove();
                continue;
            }
            ball.tick();
        }
    }

    public Ball spawn(Location location, Player owner) {
        Ball ball = new Ball(location, owner, plugin.getSettings());
        balls.add(ball);
        return ball;
    }

    public VolleyBall spawnVolleyball(Location location, Player owner) {
        VolleyBall ball = new VolleyBall(location, owner, plugin.getVolleyballSettings());
        balls.add(ball);
        return ball;
    }

    public void remove(Ball ball) {
        ball.remove();
        balls.remove(ball);
    }

    public Ball byHitbox(Entity entity) {
        for (Ball ball : balls) {
            if (ball.getHitbox().getUniqueId().equals(entity.getUniqueId())) {
                return ball;
            }
        }
        return null;
    }

    public void clear() {
        for (Ball ball : balls) {
            ball.remove();
        }
        balls.clear();
    }

    public int removeAll() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!entity.getScoreboardTags().contains("headball")) {
                    continue;
                }
                if (entity instanceof ItemDisplay) {
                    removed++;
                }
                entity.remove();
            }
        }
        balls.clear();
        return removed;
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        clear();
    }

    public int count() {
        return balls.size();
    }
}
