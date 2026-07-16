package com.selim.impostorgame.tasks;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class SpectateFollowTask extends BukkitRunnable {

    private final ImpostorGamePlugin plugin;

    public SpectateFollowTask(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) return;

        for (Player spectator : Bukkit.getOnlinePlayers()) {
            if (!gm.isSpectating(spectator.getUniqueId())) continue;

            UUID targetId = gm.getSpectateFollowing(spectator.getUniqueId());
            if (targetId == null) continue;

            Player target = Bukkit.getPlayer(targetId);
            if (target == null || !target.isOnline()) continue;

            spectator.teleport(target.getLocation());
        }
    }
}
