package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectatorMenuListener implements Listener {

    private static final int IMPOSTOR_SLOT = 11;
    private static final int CREWMATES_SLOT = 15;

    private static class SpectatorMainHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class SpectatorCrewHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();
        if (!gm.isSpectatorItem(event.getItem())) return;

        event.setCancelled(true);

        if (!gm.isGameRunning()) return;

        openMainMenu(event.getPlayer());
    }

    private void openMainMenu(Player player) {
        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();

        String title = ChatColor.translateAlternateColorCodes('&',
                ImpostorGamePlugin.getInstance().getConfig().getString("messages.spectator-gui-title", "İzleyici Menüsü"));

        Inventory gui = Bukkit.createInventory(new SpectatorMainHolder(), 27, title);

        ItemStack impostorItem = new ItemStack(Material.RED_DYE);
        ItemMeta impostorMeta = impostorItem.getItemMeta();
        impostorMeta.setDisplayName(gm.msg("messages.spectator-impostor-item"));
        impostorItem.setItemMeta(impostorMeta);

        ItemStack crewmatesItem = new ItemStack(Material.GREEN_DYE);
        ItemMeta crewmatesMeta = crewmatesItem.getItemMeta();
        crewmatesMeta.setDisplayName(gm.msg("messages.spectator-crewmates-item"));
        crewmatesItem.setItemMeta(crewmatesMeta);

        gui.setItem(IMPOSTOR_SLOT, impostorItem);
        gui.setItem(CREWMATES_SLOT, crewmatesItem);

        player.openInventory(gui);
    }

    private void openCrewmatesMenu(Player player) {
        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();

        List<Player> onlineCrewmates = gm.getAlive().stream()
                .filter(uuid -> !uuid.equals(gm.getImpostorId()))
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toList());

        int size = Math.max(27, (int) (Math.ceil((onlineCrewmates.size() + 9) / 9.0) * 9));
        size = Math.min(size, 54);

        String title = ChatColor.translateAlternateColorCodes('&',
                ImpostorGamePlugin.getInstance().getConfig().getString("messages.spectator-gui-title", "İzleyici Menüsü"));

        Inventory gui = Bukkit.createInventory(new SpectatorCrewHolder(), size, title);

        int slot = 0;
        for (Player crewmate : onlineCrewmates) {
            if (slot >= size - 9) break; // leave the bottom row free for the back button
            gui.setItem(slot, gm.createSpectateTargetItem(crewmate));
            slot++;
        }

        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName(gm.msg("messages.spectator-back-item"));
        back.setItemMeta(backMeta);
        gui.setItem(size - 5, back);

        player.openInventory(gui);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        GameManager gm = ImpostorGamePlugin.getInstance().getGameManager();

        if (event.getInventory().getHolder() instanceof SpectatorMainHolder) {
            event.setCancelled(true);

            if (event.getRawSlot() == IMPOSTOR_SLOT) {
                Player impostor = gm.getImpostorId() != null ? Bukkit.getPlayer(gm.getImpostorId()) : null;
                if (impostor != null && impostor.isOnline()) {
                    player.setSpectatorTarget(impostor);
                    player.closeInventory();
                } else {
                    player.sendMessage(gm.msg("messages.spectator-target-offline"));
                }
            } else if (event.getRawSlot() == CREWMATES_SLOT) {
                openCrewmatesMenu(player);
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof SpectatorCrewHolder) {
            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null) return;

            if (clicked.getType() == Material.BARRIER) {
                openMainMenu(player);
                return;
            }

            UUID targetId = gm.getSpectateTarget(clicked);
            if (targetId == null) return;

            Player target = Bukkit.getPlayer(targetId);
            if (target != null && target.isOnline()) {
                player.setSpectatorTarget(target);
                player.closeInventory();
            } else {
                player.sendMessage(gm.msg("messages.spectator-target-offline"));
            }
        }
    }
}
