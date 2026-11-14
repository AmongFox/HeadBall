package com.afox.headball;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Ball {

    private static final double GRAVITY = 0.045;
    private static final double BOUNCE = 0.6;
    private static final double WALL_BOUNCE = 0.7;
    private static final double ROLL_FRICTION = 0.96;
    private static final double AIR_DRAG = 0.995;
    protected static final double MAX_SPEED = 2.0;
    private static final double STOP = 0.02;
    private static final double RADIUS = 0.25;

    private static final double WATER_GRAVITY = 0.01;
    private static final double WATER_BUOYANCY = 0.05;
    private static final double WATER_MAX_RISE = 0.15;
    private static final double WATER_FLOW_PUSH = 0.03;
    private static final double WATER_DRAG = 0.98;
    private static final BlockFace[] HORIZONTAL = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    protected final Settings settings;
    protected final String texture;
    private UUID ownerId;
    private final ItemDisplay display;
    private final Interaction hitbox;
    private final UUID hitboxId;
    protected Location location;
    protected Vector velocity = new Vector(0, 0, 0);
    protected boolean onGround;

    private boolean rendered;
    private double lastRenderX;
    private double lastRenderY;
    private double lastRenderZ;

    public Ball(Location location, Player owner, Settings settings) {
        this(location, owner, settings, null);
    }

    public Ball(Location location, Player owner, Settings settings, String texture) {
        this(location, owner, settings, texture, 0.6f, 0.6f);
    }

    protected Ball(Location location, Player owner, Settings settings, float hitboxWidth, float hitboxHeight) {
        this(location, owner, settings, null, hitboxWidth, hitboxHeight);
    }

    protected Ball(Location location, Player owner, Settings settings, String texture, float hitboxWidth, float hitboxHeight) {
        this.settings = settings;
        this.ownerId = owner == null ? null : owner.getUniqueId();
        this.texture = (texture == null || texture.isBlank()) ? settings.randomTexture() : texture;
        location = location.clone();
        location.setYaw(0f);
        location.setPitch(0f);
        this.location = location;
        World world = location.getWorld();

        this.hitbox = world.spawn(location, Interaction.class, entity -> {
            entity.setInteractionWidth(hitboxWidth);
            entity.setInteractionHeight(hitboxHeight);
            entity.setResponsive(true);
            entity.addScoreboardTag("headball");
        });
        this.hitboxId = hitbox.getUniqueId();

        this.display = world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(createHeadItem(this.texture, owner));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setTransformation(new Transformation(
                    new Vector3f(0f, settings.yOffset, 0f),
                    new AxisAngle4f((float) Math.toRadians(settings.pitch), 1f, 0f, 0f),
                    new Vector3f(settings.scale, settings.scale, settings.scale),
                    new AxisAngle4f()
            ));
            entity.addScoreboardTag("headball");
        });

        updateEntities();
    }

    public static ItemStack createHeadItem(String texture, Player owner) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }

        PlayerProfile profile;
        if (texture != null && !texture.isBlank()) {
            profile = Bukkit.createProfile(UUID.randomUUID());
            profile.getProperties().add(new ProfileProperty("textures", texture));
        } else if (owner != null) {
            profile = owner.getPlayerProfile();
        } else {
            return item;
        }

        meta.setPlayerProfile(profile);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createHeldItem(HeadBall plugin, String texture, String type, String name) {
        ItemStack item = createHeadItem(texture, null);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE)
                    .decoration(TextDecoration.ITALIC, false));
            meta.getPersistentDataContainer().set(plugin.getHeldBallKey(), PersistentDataType.STRING, type);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String heldBallType(HeadBall plugin, ItemStack item) {
        if (item == null || item.getType() != Material.PLAYER_HEAD) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(plugin.getHeldBallKey(), PersistentDataType.STRING);
    }

    public void kick(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        boolean sprint = player.isSprinting();
        double speed = sprint ? settings.kickSpeedSprint : settings.kickSpeed;
        double lift = sprint ? settings.kickLiftSprint : settings.kickLift;

        World world = location.getWorld();
        if (sprint) {
            world.spawnParticle(Particle.SWEEP_ATTACK, location, 1, 0, 0, 0, 0);
        } else {
            world.spawnParticle(Particle.CRIT, location, 6, 0.6, 0.6, 0.6, 0.5);
        }
        float pitch = 0.8f + ThreadLocalRandom.current().nextFloat() * 0.4f;
        world.playSound(location, Sound.ENTITY_PLAYER_SMALL_FALL, 1.0f, pitch);

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

        Block water = waterAt();
        boolean inWater = water != null;
        v.setY(v.getY() - (inWater ? WATER_GRAVITY : GRAVITY));

        if (inWater) {
            v.setY(v.getY() + WATER_BUOYANCY);
            if (v.getY() > WATER_MAX_RISE) {
                v.setY(WATER_MAX_RISE);
            }
            Vector flow = waterFlow(water);
            v.setX(v.getX() + flow.getX() * WATER_FLOW_PUSH);
            v.setZ(v.getZ() + flow.getZ() * WATER_FLOW_PUSH);
            v.setX(v.getX() * WATER_DRAG);
            v.setZ(v.getZ() * WATER_DRAG);
        }

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

    private Block waterAt() {
        Block block = location.getBlock();
        if (block.getType() == Material.WATER) {
            return block;
        }
        Block below = location.clone().subtract(0, 0.4, 0).getBlock();
        if (below.getType() == Material.WATER) {
            return below;
        }
        return null;
    }

    private Vector waterFlow(Block block) {
        int level = waterLevel(block);
        Vector flow = new Vector();
        for (BlockFace face : HORIZONTAL) {
            int neighborLevel = waterLevel(block.getRelative(face));
            if (neighborLevel > level) {
                flow.add(new Vector(face.getModX(), 0, face.getModZ())
                        .multiply(neighborLevel - level));
            }
        }
        if (flow.lengthSquared() > 0) {
            flow.normalize();
        }
        return flow;
    }

    private int waterLevel(Block block) {
        if (block.getType() != Material.WATER) {
            return -1;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Levelled levelled) {
            return levelled.getLevel();
        }
        return 0;
    }

    private void updateEntities() {
        if (!display.isValid() || !hitbox.isValid()) {
            return;
        }

        double dx = location.getX() - lastRenderX;
        double dy = location.getY() - lastRenderY;
        double dz = location.getZ() - lastRenderZ;
        if (rendered && velocity.lengthSquared() < 1.0E-8 && dx * dx + dy * dy + dz * dz < 1.0E-8) {
            return;
        }

        display.teleport(location);
        hitbox.teleport(location.clone().add(0, -0.3, 0));
        rendered = true;
        lastRenderX = location.getX();
        lastRenderY = location.getY();
        lastRenderZ = location.getZ();
    }

    public void remove() {
        display.remove();
        hitbox.remove();
    }

    public boolean isAlive() {
        return display.isValid() && hitbox.isValid();
    }

    public UUID getHitboxId() {
        return hitboxId;
    }

    public Location getLocation() {
        return location.clone();
    }

    public Vector getVelocity() {
        return velocity.clone();
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity.clone();
    }

    public String getTexture() {
        return texture;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }
}
