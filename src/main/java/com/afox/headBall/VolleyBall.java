package com.afox.headball;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class VolleyBall extends Ball {

    public VolleyBall(Location location, Player owner, Settings settings) {
        super(location, owner, settings, 1.2f, 1.2f);
    }

    @Override
    public void kick(Player player) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        boolean jumping = !player.isOnGround();
        double speed = jumping ? settings.kickSpeedJump : settings.kickSpeed;
        double lift = jumping ? settings.kickLiftJump : settings.kickLift;

        World world = location.getWorld();
        if (jumping) {
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
        capFallSpeed();
    }

    @Override
    public void tick() {
        super.tick();
        capFallSpeed();
    }

    private void capFallSpeed() {
        if (velocity.getY() < -settings.fallSpeed) {
            velocity.setY(-settings.fallSpeed);
        }
    }

    public void throwBall(Player player, boolean strong) {
        Vector direction = player.getEyeLocation().getDirection().normalize();
        double speed = strong ? settings.throwSpeedJump : settings.throwSpeed;
        double lift = strong ? settings.throwLiftJump : settings.throwLift;

        Vector throwVelocity = direction.multiply(speed);
        throwVelocity.setY(throwVelocity.getY() + lift);

        this.velocity = throwVelocity;
        this.onGround = false;
        capFallSpeed();
    }
}
