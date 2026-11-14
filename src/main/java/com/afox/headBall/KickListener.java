package com.afox.headball;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class KickListener implements Listener {

    private static final String FOOTBALL = "ball";
    private static final String VOLLEYBALL = "volleyball";

    private final HeadBall plugin;
    private final BallManager balls;

    public KickListener(HeadBall plugin, BallManager balls) {
        this.plugin = plugin;
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
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            pickup(player, ball);
            return;
        }

        ball.kick(player);
    }

    @EventHandler
    public void onUseHeld(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        String type = Ball.heldBallType(plugin, event.getItem());
        if (type == null) {
            return;
        }

        Player player = event.getPlayer();
        if (balls.isFull(player)) {
            balls.sendLimitMessage(player);
            return;
        }

        event.setCancelled(true);

        if (VOLLEYBALL.equals(type)) {
            boolean strong = !player.isOnGround();
            VolleyBall ball = balls.spawnVolleyball(
                    player.getEyeLocation().add(player.getEyeLocation().getDirection().normalize().multiply(1.2)),
                    player
            );
            if (ball == null) {
                balls.sendLimitMessage(player);
                return;
            }
            consumeOne(player);
            ball.throwBall(player, strong);
        } else {
            if (balls.spawn(player.getLocation().add(0, 0.3, 0), player) == null) {
                balls.sendLimitMessage(player);
                return;
            }
            consumeOne(player);
        }
    }

    private void pickup(Player player, Ball ball) {
        boolean volleyball = ball instanceof VolleyBall;
        String type = volleyball ? VOLLEYBALL : FOOTBALL;
        String name = volleyball ? "Волейбольный мяч" : "Футбольный мяч";
        String texture = ball.getTexture();

        balls.remove(ball);
        giveHeldItem(player, Ball.createHeldItem(plugin, texture, type, name));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }

    private void giveHeldItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private void consumeOne(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }
}
