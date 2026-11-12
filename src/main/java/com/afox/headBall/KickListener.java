package com.afox.headball;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class KickListener implements Listener {

    private final BallManager balls;

    public KickListener(BallManager balls) {
        this.balls = balls;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Ball ball = balls.byHitbox(event.getRightClicked());
        if (ball == null) {
            return;
        }

        event.setCancelled(true);
        ball.kick(event.getPlayer());
    }
}
