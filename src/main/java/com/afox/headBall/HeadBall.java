package com.afox.headball;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HeadBall extends JavaPlugin {

    private BallManager balls;
    private Settings settings;
    private Settings volleyballSettings;
    private NamespacedKey heldBallKey;
    private CraftingManager crafting;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        heldBallKey = new NamespacedKey(this, "held-ball");
        messages = new Messages(this);
        messages.load();
        loadSettings();

        crafting = new CraftingManager(this);
        crafting.apply();

        balls = new BallManager(this);
        balls.start();

        getServer().getPluginManager().registerEvents(new KickListener(this, balls), this);

        if (getCommand("headball") != null) {
            HeadBallCommand executor = new HeadBallCommand(this, balls);
            Objects.requireNonNull(getCommand("headball")).setExecutor(executor);
            Objects.requireNonNull(getCommand("headball")).setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (crafting != null) {
            crafting.remove();
        }
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

    public Messages getMessages() {
        return messages;
    }

    public void reloadSettings() {
        reloadConfig();
        messages.load();
        loadSettings();
        crafting.apply();
    }
}
