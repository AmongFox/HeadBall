package com.afox.headball;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class HeadBall extends JavaPlugin {

    private BallManager balls;
    private Settings settings;
    private Settings volleyballSettings;
    private NamespacedKey heldBallKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        heldBallKey = new NamespacedKey(this, "held-ball");
        loadSettings();

        balls = new BallManager(this);
        balls.start();

        getServer().getPluginManager().registerEvents(new KickListener(this, balls), this);

        if (getCommand("headball") != null) {
            HeadBallCommand executor = new HeadBallCommand(this, balls);
            getCommand("headball").setExecutor(executor);
            getCommand("headball").setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (balls != null) {
            balls.stop();
        }
    }

    private void loadSettings() {
        settings = new Settings(this);
        volleyballSettings = new Settings(this, "volleyball");
    }

    public Settings getSettings() {
        return settings;
    }

    public Settings getVolleyballSettings() {
        return volleyballSettings;
    }

    public NamespacedKey getHeldBallKey() {
        return heldBallKey;
    }

    public void reloadSettings() {
        reloadConfig();
        loadSettings();
    }
}
