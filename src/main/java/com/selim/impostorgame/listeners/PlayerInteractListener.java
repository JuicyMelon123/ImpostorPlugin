package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerInteractListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public PlayerInteractListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        GameManager gm = plugin.getGameManager();

        if (item == null) return;

        // Tracker compass -> open the tracking GUI
        if (gm.isTrackerCompass(item)) {
            event.setCancelled(true);
            CompassGUIListener.open(player);
            return;
        }

        // Ability dye -> toggle resistance/strength (impostor only, active game only)
        if (gm.isAbilityDye(item)) {
            event.setCancelled(true);

            if (!gm.isGameRunning() || !gm.isImpostor(player.getUniqueId())) {
                return;
            }

            boolean nowActive = !gm.isAbilityActive(player.getUniqueId());
            gm.setAbilityActive(player.getUniqueId(), nowActive);

            PlayerInventory inv = player.getInventory();
            inv.setItemInMainHand(gm.createAbilityDye(nowActive));

            if (nowActive) {
                // amplifier 1 = level II. ambient + particles=false -> no particles shown.
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1, true, false, false));
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, true, false, false));
                com.selim.impostorgame.Utils.sendActionBar(player, ChatColor.GREEN + "Abilities activated!");
            } else {
                player.removePotionEffect(PotionEffectType.RESISTANCE);
                player.removePotionEffect(PotionEffectType.STRENGTH);
                com.selim.impostorgame.Utils.sendActionBar(player, ChatColor.GRAY + "Abilities deactivated.");
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        GameManager gm = plugin.getGameManager();
        ItemStack item = event.getItemDrop().getItemStack();

        if (gm.isTrackerCompass(item) || gm.isAbilityDye(item)) {
            event.setCancelled(true);
        }
    }
}
