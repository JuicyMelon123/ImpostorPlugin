package com.selim.impostorgame.tasks;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassUpdateTask extends BukkitRunnable {

    private final ImpostorGamePlugin plugin;

    public CompassUpdateTask(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        GameManager gm = plugin.getGameManager();

        for (Player player : Bukkit.getOnlinePlayers()) {
            GameManager.TrackMode mode = gm.getCompassMode(player.getUniqueId());
            if (mode == null) continue;

            Player target = findTarget(player, mode, gm);
            if (target != null) {
                player.setCompassTarget(target.getLocation());
            }
        }
    }

    private Player findTarget(Player viewer, GameManager.TrackMode mode, GameManager gm) {
        Player best = null;
        double bestDistSq = (mode == GameManager.TrackMode.NEAREST) ? Double.MAX_VALUE : -1;

        for (Player other : viewer.getWorld().getPlayers()) {
            if (other.getUniqueId().equals(viewer.getUniqueId())) continue;

            // While a game is active, only track alive participants
            if (gm.isGameRunning() && !gm.getAlive().contains(other.getUniqueId())) continue;

            // Horizontal-only distance - a compass needle can only ever point on the
            // X/Z plane, so factoring in Y (e.g. someone mining straight down nearby)
            // would produce misleading "nearest"/"furthest" results.
            double dx = viewer.getLocation().getX() - other.getLocation().getX();
            double dz = viewer.getLocation().getZ() - other.getLocation().getZ();
            double distSq = dx * dx + dz * dz;

            if (mode == GameManager.TrackMode.NEAREST) {
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = other;
                }
            } else {
                if (distSq > bestDistSq) {
                    bestDistSq = distSq;
                    best = other;
                }
            }
        }
        return best;
    }
}
