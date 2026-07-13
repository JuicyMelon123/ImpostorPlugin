package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class GameListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public GameListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        // No death messages, ever (unless turned off in config)
        if (plugin.getConfig().getBoolean("rules.block-death-messages", true)) {
            event.setDeathMessage(null);
        }

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
            // The impostor gets one respawn instead of being eliminated
            gm.useRespawn(victimId);
            gm.markAlive(victimId);
            return;
        }

        // Everyone else - and the impostor once their respawn is used - gets kicked,
        // and can rejoin to spectate instead of being permanently banned.
        boolean wasImpostor = gm.isImpostor(victimId);
        String reason = gm.msg("messages.eliminated-kick");

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (victim.isOnline()) {
                victim.kickPlayer(reason.isEmpty() ? "Elendin! Tekrar katılarak izleyici olabilirsin." : reason);
            }

            if (wasImpostor) {
                // The impostor is out of respawns and eliminated - crewmates win
                endGame("messages.crewmates-win");
            } else if (gm.getAlive().size() <= 1) {
                // Only the impostor is left alive - impostor wins
                endGame("messages.impostor-win");
            }
        });
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) return;
        if (!(event.getEntity() instanceof EnderDragon)) return;

        endGame("messages.crewmates-win-dragon");
    }

    private void endGame(String winMessageKey) {
        GameManager gm = plugin.getGameManager();
        if (!gm.isGameRunning()) return; // already ended by another trigger this tick

        String message = gm.msg(winMessageKey);
        Bukkit.broadcastMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (gm.isParticipant(player.getUniqueId())) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        gm.stopGame();
    }
}
