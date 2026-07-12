package com.selim.impostorgame.commands;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ImpStartCommand implements CommandExecutor {

    private final ImpostorGamePlugin plugin;
    private final Random random = new Random();

    public ImpStartCommand(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("impostorgame.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        GameManager gm = plugin.getGameManager();

        if (gm.isGameRunning()) {
            sender.sendMessage(ChatColor.RED + "A game is already running! Use /impostorgame stop to end it first.");
            return true;
        }

        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (online.size() < 2) {
            sender.sendMessage(ChatColor.RED + "You need at least 2 online players to start the game.");
            return true;
        }

        Player impostor = online.get(random.nextInt(online.size()));
        gm.startGame(impostor, online);

        int range = plugin.getConfig().getInt("rtp.range", 300);
        boolean clearInv = plugin.getConfig().getBoolean("game.clear-inventory", true);

        for (Player p : online) {
            if (clearInv) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
            }

            // Random teleport within the configured range
            Location rtpLoc = findSafeRandomLocation(p.getWorld(), p.getLocation(), range);
            p.teleport(rtpLoc);

            // Every player gets the tracker compass
            p.getInventory().addItem(gm.createTrackerCompass());

            // Role reveal via title only - never chat
            if (p.getUniqueId().equals(impostor.getUniqueId())) {
                p.getInventory().addItem(gm.createAbilityDye(false));
                p.sendTitle(gm.msg("messages.impostor-role"), gm.msg("messages.impostor-subtitle"), 10, 70, 20);
            } else {
                p.sendTitle(gm.msg("messages.crewmate-role"), gm.msg("messages.crewmate-subtitle"), 10, 70, 20);
            }
        }

        sender.sendMessage(ChatColor.GREEN + "The Impostor Game has started with " + online.size() + " players!");
        return true;
    }

    /**
     * Looks for a reasonably safe spot on the surface within `range` blocks
     * of the given origin. Falls back to world spawn if nothing is found.
     */
    private Location findSafeRandomLocation(World world, Location origin, int range) {
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = random.nextInt(range * 2 + 1) - range;
            int dz = random.nextInt(range * 2 + 1) - range;
            int x = origin.getBlockX() + dx;
            int z = origin.getBlockZ() + dz;
            int y = world.getHighestBlockYAt(x, z);

            Material below = world.getBlockAt(x, y, z).getType();

            if (below.isSolid() && below != Material.LAVA && below != Material.WATER) {
                return new Location(world, x + 0.5, y + 1, z + 0.5);
            }
        }
        return world.getSpawnLocation();
    }
}
