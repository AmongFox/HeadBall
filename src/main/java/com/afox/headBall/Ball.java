package com.afox.headball;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

public class Ball {

    private static final double GRAVITY = 0.045;
    private static final double BOUNCE = 0.6;
    private static final double WALL_BOUNCE = 0.7;
    private static final double ROLL_FRICTION = 0.96;
    private static final double AIR_DRAG = 0.995;
    private static final double MAX_SPEED = 2.0;
    private static final double STOP = 0.02;
    private static final double RADIUS = 0.25;

    private final Settings settings;
    private final ItemDisplay display;
    private final Interaction hitbox;
    private Location location;
    private Vector velocity = new Vector(0, 0, 0);
    private boolean onGround;

    public Ball(Location location, Player owner, Settings settings) {
        this.settings = settings;
        this.location = location.clone();
        World world = location.getWorld();

        this.hitbox = world.spawn(location, Interaction.class);
        this.hitbox.setInteractionWidth(0.6f);
        this.hitbox.setInteractionHeight(0.6f);
        this.hitbox.setResponsive(true);
        this.hitbox.addScoreboardTag("headball");

        this.display = world.spawn(location, ItemDisplay.class);
        this.display.setItemStack(createHead(owner));
        this.display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
        this.display.setTransformation(new Transformation(
                new Vector3f(0f, settings.yOffset, 0f),
                new AxisAngle4f((float) Math.toRadians(settings.pitch), 1f, 0f, 0f),
                new Vector3f(settings.scale, settings.scale, settings.scale),
                new AxisAngle4f()
        ));
        this.display.addScoreboardTag("headball");

        updateEntities();
    }

    private ItemStack createHead(Player owner) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PlayerProfile profile;
        if (settings.texture != null && !settings.texture.isBlank()) {
            profile = Bukkit.createProfile(UUID.randomUUID(), "headball");
            profile.setProperty(new ProfileProperty("textures", settings.texture));
        } else if (owner != null) {
            profile = owner.getPlayerProfile();
        } else {
            return item;
        }

        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    public void kick(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        boolean sprint = player.isSprinting();
        double speed = sprint ? settings.kickSpeedSprint : settings.kickSpeed;
        double lift = sprint ? settings.kickLiftSprint : settings.kickLift;

        Vector kick = direction.multiply(speed);
        kick.setY(kick.getY() + lift);
        if (kick.length() > MAX_SPEED) {
            kick = kick.normalize().multiply(MAX_SPEED);
        }
        velocity = kick;
        onGround = false;
    }

    public void tick() {
        Vector v = velocity.clone();
        v.setY(v.getY() - GRAVITY);
        if (v.length() > MAX_SPEED) {
            v = v.normalize().multiply(MAX_SPEED);
        }

        onGround = false;

        Location nextY = location.clone().add(0, v.getY(), 0);
        if (collides(nextY)) {
            if (v.getY() < 0) {
                onGround = true;
            }
            v.setY(-v.getY() * BOUNCE);
            if (Math.abs(v.getY()) < 0.08) {
                v.setY(0);
            }
        } else {
            location = nextY;
        }

        Location nextX = location.clone().add(v.getX(), 0, 0);
        if (collides(nextX)) {
            v.setX(-v.getX() * WALL_BOUNCE);
        } else {
            location = nextX;
        }

        Location nextZ = location.clone().add(0, 0, v.getZ());
        if (collides(nextZ)) {
            v.setZ(-v.getZ() * WALL_BOUNCE);
        } else {
            location = nextZ;
        }

        if (onGround) {
            v.setX(v.getX() * ROLL_FRICTION);
            v.setZ(v.getZ() * ROLL_FRICTION);
        } else {
            v.setX(v.getX() * AIR_DRAG);
            v.setZ(v.getZ() * AIR_DRAG);
        }

        if (Math.abs(v.getX()) < STOP) {
            v.setX(0);
        }
        if (Math.abs(v.getY()) < STOP) {
            v.setY(0);
        }
        if (Math.abs(v.getZ()) < STOP) {
            v.setZ(0);
        }

        velocity = v;

        if (location.getY() < location.getWorld().getMinHeight() - 10) {
            remove();
            return;
        }

        updateEntities();
    }

    private boolean collides(Location center) {
        double r = RADIUS;
        BoundingBox box = new BoundingBox(
                center.getX() - r, center.getY() - r, center.getZ() - r,
                center.getX() + r, center.getY() + r, center.getZ() + r
        );

        World world = center.getWorld();
        int minX = (int) Math.floor(box.getMinX());
        int minY = (int) Math.floor(box.getMinY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxX = (int) Math.floor(box.getMaxX());
        int maxY = (int) Math.floor(box.getMaxY());
        int maxZ = (int) Math.floor(box.getMaxZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void updateEntities() {
        if (!display.isValid() || !hitbox.isValid()) {
            return;
        }
        display.teleport(location);
        hitbox.teleport(location.clone().add(0, -0.3, 0));
    }

    public void remove() {
        display.remove();
        hitbox.remove();
    }

    public boolean isAlive() {
        return display.isValid() && hitbox.isValid();
    }

    public Interaction getHitbox() {
        return hitbox;
    }
}
