package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class SpectatorProtectionListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public SpectatorProtectionListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    private GameManager gm() {
        return plugin.getGameManager();
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (gm().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        // Prevent a spectating player from dealing damage to anyone else too
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (gm().isSpectating(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (gm().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        if (gm().isSpectating(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!gm().isSpectating(player.getUniqueId())) return;

        // Let the spectator nether star's own right-click GUI logic still fire -
        // SpectatorMenuListener doesn't check event.isCancelled(), so cancelling
        // here doesn't block it. This only stops interaction with the world itself
        // (doors, chests, buttons, etc.) which Adventure mode doesn't block on its own.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
        }
    }
}
