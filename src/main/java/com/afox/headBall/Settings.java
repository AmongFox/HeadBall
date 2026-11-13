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
    public final double kickSpeedJump;
    public final double kickLiftJump;
    public final double fallSpeed;
    public final double throwSpeed;
    public final double throwSpeedJump;
    public final double throwLift;
    public final double throwLiftJump;

    public Settings(HeadBall plugin) {
        this(plugin, "ball");
    }

    public Settings(HeadBall plugin, String section) {
        FileConfiguration config = plugin.getConfig();
        this.texture = config.getString(section + ".texture", "");
        this.scale = (float) config.getDouble(section + ".scale", 1.1);
        this.yOffset = (float) config.getDouble(section + ".y-offset", 0.25);
        this.pitch = (float) config.getDouble(section + ".pitch", 0.0);
        this.kickSpeed = config.getDouble(section + ".kick-speed", 0.8);
        this.kickSpeedSprint = config.getDouble(section + ".kick-speed-sprint", 1.5);
        this.kickLift = config.getDouble(section + ".kick-lift", 0.2);
        this.kickLiftSprint = config.getDouble(section + ".kick-lift-sprint", 0.35);
        this.kickSpeedJump = config.getDouble(section + ".kick-speed-jump", kickSpeedSprint);
        this.kickLiftJump = config.getDouble(section + ".kick-lift-jump", kickLiftSprint);
        this.fallSpeed = config.getDouble(section + ".fall-speed", 0.5);
        this.throwSpeed = config.getDouble(section + ".throw-speed", 1.2);
        this.throwSpeedJump = config.getDouble(section + ".throw-speed-jump", 2.0);
        this.throwLift = config.getDouble(section + ".throw-lift", 0.15);
        this.throwLiftJump = config.getDouble(section + ".throw-lift-jump", 0.3);
    }
}
