package com.afox.headball;

import org.bukkit.plugin.java.JavaPlugin;

public final class HeadBall extends JavaPlugin {

    private BallManager balls;
    private Settings settings;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings(this);

        balls = new BallManager(this);
        balls.start();

        getServer().getPluginManager().registerEvents(new KickListener(balls), this);

        if (getCommand("headball") != null) {
            getCommand("headball").setExecutor(new HeadBallCommand(this, balls));
        }
    }

    @Override
    public void onDisable() {
        if (balls != null) {
            balls.stop();
        }
    }

    public Settings getSettings() {
        return settings;
    }

    public void reloadSettings() {
        reloadConfig();
        settings = new Settings(this);
    }
}
