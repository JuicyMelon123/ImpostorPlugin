package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import com.selim.impostorgame.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CompassGUIListener implements Listener {

    private static final int NEAREST_SLOT = 11;
    private static final int FURTHEST_SLOT = 15;

    /** Marker holder so we can reliably identify our own GUI in the click event. */
    private static class TrackerHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static void open(Player player) {
        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();

        String title = ChatColor.DARK_GRAY + ChatColor.translateAlternateColorCodes('&',
                ImpostorGamePlugin.getInstance().getConfig().getString("messages.tracker-gui-title", "Oyuncu Takip Cihazı"));

        Inventory gui = Bukkit.createInventory(new TrackerHolder(), 27, title);

        ItemStack nearest = new ItemStack(Material.RED_DYE);
        ItemMeta nearestMeta = nearest.getItemMeta();
        nearestMeta.setDisplayName(gm.msg("messages.tracker-nearest-item"));
        nearest.setItemMeta(nearestMeta);

        ItemStack furthest = new ItemStack(Material.LIME_DYE);
        ItemMeta furthestMeta = furthest.getItemMeta();
        furthestMeta.setDisplayName(gm.msg("messages.tracker-furthest-item"));
        furthest.setItemMeta(furthestMeta);

        gui.setItem(NEAREST_SLOT, nearest);
        gui.setItem(FURTHEST_SLOT, furthest);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TrackerHolder)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();

        if (event.getRawSlot() == NEAREST_SLOT) {
            gm.setCompassMode(player.getUniqueId(), GameManager.TrackMode.NEAREST);
            Utils.sendActionBar(player, gm.msg("messages.tracker-nearest-selected"));
            player.closeInventory();
        } else if (event.getRawSlot() == FURTHEST_SLOT) {
            gm.setCompassMode(player.getUniqueId(), GameManager.TrackMode.FURTHEST);
            Utils.sendActionBar(player, gm.msg("messages.tracker-furthest-selected"));
            player.closeInventory();
        }
    }
}
