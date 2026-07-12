package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class GameListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public GameListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        // No death messages, ever
        event.setDeathMessage(null);

        Player victim = event.getEntity();
        UUID victimId = victim.getUniqueId();
        GameManager gm = plugin.getGameManager();

        if (!gm.isGameRunning() || !gm.isParticipant(victimId)) {
            return;
        }

        gm.markDead(victimId);

        // Let the impostor know they got a kill (only the impostor sees this)
        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victimId) && gm.isImpostor(killer.getUniqueId())) {
            killer.sendMessage(gm.msg("messages.impostor-kill"));
        }

        if (gm.isImpostor(victimId) && gm.hasRespawnLeft(victimId)) {
            // The impostor gets one respawn instead of being banned
            gm.useRespawn(victimId);
            gm.markAlive(victimId);
        } else {
            // Everyone else - and the impostor once their respawn is used - gets banned
            gm.queueBan(victimId);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        GameManager gm = plugin.getGameManager();

        if (!gm.isPendingBan(player.getUniqueId())) {
            return;
        }

        gm.clearPendingBan(player.getUniqueId());

        String banMessage = gm.msg("ban.message");
        String reason = banMessage.isEmpty() ? "You were eliminated." : banMessage;

        // Ban + kick a tick later, after the respawn has fully processed
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), reason, null, "ImpostorGame");
            player.kickPlayer(reason);
        });
    }
}
