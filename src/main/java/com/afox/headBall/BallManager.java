package com.afox.headball;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BallManager {

    private static final int DEAD_TICKS_LIMIT = 100;

    private final HeadBall plugin;
    private final List<Ball> balls = new ArrayList<>();
    private final Map<Ball, int[]> chunks = new HashMap<>();
    private final Map<Ball, Integer> deadTicks = new HashMap<>();
    private final File file;
    private BukkitTask task;
    private int saveTicks;

    public BallManager(HeadBall plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "balls.yml");
    }

    public void start() {
        load();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    private void tickAll() {
        Iterator<Ball> iterator = balls.iterator();
        while (iterator.hasNext()) {
            Ball ball = iterator.next();
            if (!ball.isAlive()) {
                int dead = deadTicks.getOrDefault(ball, 0) + 1;
                if (dead < DEAD_TICKS_LIMIT) {
                    deadTicks.put(ball, dead);
                    continue;
                }
                plugin.getLogger().warning("[HeadBall] Removing dead ball at " + ball.getLocation());
                releaseChunk(ball);
                ball.remove();
                deadTicks.remove(ball);
                iterator.remove();
                continue;
            }
            deadTicks.remove(ball);
            ball.tick();
            updateChunk(ball);
        }

        if (++saveTicks >= 200) {
            saveTicks = 0;
            save();
        }
    }

    public Ball spawn(Location location, Player owner) {
        loadChunk(location);
        Ball ball = new Ball(location, owner, plugin.getSettings());
        register(ball);
        save();
        return ball;
    }

    public VolleyBall spawnVolleyball(Location location, Player owner) {
        loadChunk(location);
        VolleyBall ball = new VolleyBall(location, owner, plugin.getVolleyballSettings());
        register(ball);
        save();
        return ball;
    }

    public void remove(Ball ball) {
        releaseChunk(ball);
        ball.remove();
        balls.remove(ball);
        save();
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
            releaseChunk(ball);
            ball.remove();
        }
        balls.clear();
        save();
    }

    public int removeAll() {
        for (Ball ball : balls) {
            releaseChunk(ball);
        }
        int removed = removeTaggedEntities();
        balls.clear();
        save();
        return removed;
    }

    public void stop() {
        plugin.getLogger().info("[HeadBall] Saving " + balls.size() + " ball(s) on disable");
        save();
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Ball ball : balls) {
            releaseChunk(ball);
            ball.remove();
        }
        balls.clear();
    }

    public int count() {
        return balls.size();
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        List<Map<String, Object>> entries = new ArrayList<>();

        for (Ball ball : balls) {
            if (!ball.isAlive()) {
                continue;
            }
            Location location = ball.getLocation();
            if (location.getWorld() == null) {
                continue;
            }

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", ball instanceof VolleyBall ? "volleyball" : "ball");
            entry.put("world", location.getWorld().getName());
            entry.put("x", location.getX());
            entry.put("y", location.getY());
            entry.put("z", location.getZ());
            entry.put("texture", ball.getTexture());

            Vector velocity = ball.getVelocity();
            entry.put("vx", velocity.getX());
            entry.put("vy", velocity.getY());
            entry.put("vz", velocity.getZ());
            entries.add(entry);
        }

        data.set("balls", entries);
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            data.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить мячи: " + exception.getMessage());
        }
    }

    public void load() {
        removeTaggedEntities();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        for (Map<?, ?> entry : data.getMapList("balls")) {
            String worldName = String.valueOf(entry.get("world"));
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }

            Location location = new Location(
                    world,
                    toDouble(entry.get("x")),
                    toDouble(entry.get("y")),
                    toDouble(entry.get("z"))
            );

            String type = entry.get("type") == null ? "ball" : String.valueOf(entry.get("type"));
            String texture = entry.get("texture") == null ? null : String.valueOf(entry.get("texture"));

            loadChunk(location);
            Ball ball;
            if ("volleyball".equalsIgnoreCase(type)) {
                ball = new VolleyBall(location, null, plugin.getVolleyballSettings(), texture);
            } else {
                ball = new Ball(location, null, plugin.getSettings(), texture);
            }
            ball.setVelocity(new Vector(
                    toDouble(entry.get("vx")),
                    toDouble(entry.get("vy")),
                    toDouble(entry.get("vz"))
            ));
            register(ball);
        }

        plugin.getLogger().info("[HeadBall] Loaded " + balls.size() + " ball(s) from balls.yml");
    }

    private void register(Ball ball) {
        balls.add(ball);
        updateChunk(ball);
    }

    private void loadChunk(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.getChunkAt(location.getBlockX() >> 4, location.getBlockZ() >> 4).load();
    }

    private void updateChunk(Ball ball) {
        Location location = ball.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        int[] current = chunks.get(ball);
        if (current != null && current[0] == chunkX && current[1] == chunkZ) {
            return;
        }

        if (current != null) {
            world.removePluginChunkTicket(current[0], current[1], plugin);
        }
        world.addPluginChunkTicket(chunkX, chunkZ, plugin);
        chunks.put(ball, new int[]{chunkX, chunkZ});
    }

    private void releaseChunk(Ball ball) {
        int[] chunk = chunks.remove(ball);
        if (chunk == null) {
            return;
        }
        World world = ball.getLocation().getWorld();
        if (world != null) {
            world.removePluginChunkTicket(chunk[0], chunk[1], plugin);
        }
    }

    private int removeTaggedEntities() {
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
        return removed;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
