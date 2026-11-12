package com.afox.headball;

import org.bukkit.configuration.file.FileConfiguration;

public class Settings {

    public final String texture;
    public final float scale;
    public final float yOffset;
    public final float pitch;
    public final double kickSpeed;
    public final double kickSpeedSprint;
    public final double kickLift;
    public final double kickLiftSprint;

    public Settings(HeadBall plugin) {
        FileConfiguration config = plugin.getConfig();
        this.texture = config.getString("ball.texture", "");
        this.scale = (float) config.getDouble("ball.scale", 1.1);
        this.yOffset = (float) config.getDouble("ball.y-offset", 0.3);
        this.pitch = (float) config.getDouble("ball.pitch", 20.0);
        this.kickSpeed = config.getDouble("ball.kick-speed", 0.8);
        this.kickSpeedSprint = config.getDouble("ball.kick-speed-sprint", 1.5);
        this.kickLift = config.getDouble("ball.kick-lift", 0.2);
        this.kickLiftSprint = config.getDouble("ball.kick-lift-sprint", 0.35);
    }
}
