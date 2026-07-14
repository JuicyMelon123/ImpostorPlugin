package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerPresenceListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public PlayerPresenceListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("rules.block-join-leave-messages", true)) {
            event.setJoinMessage(null);
        }

        // Hides this player from everyone's tab list and vice versa. Players remain
        // fully visible and interactable in the world - this is tab-list only.
        plugin.getGameManager().applyTabVisibilityForJoin(event.getPlayer());

        plugin.getGameManager().applyNametagVisibility(event.getPlayer());

        // A round is active and this player was already eliminated from it
        // (they were kicked, not banned) -> let them rejoin as a spectator.
        GameManager gm = plugin.getGameManager();
        Player player = event.getPlayer();

        if (gm.isGameRunning() && gm.isParticipant(player.getUniqueId())
                && !gm.getAlive().contains(player.getUniqueId())) {
            // Applying gamemode/flight/potion state synchronously inside PlayerJoinEvent
            // can fail to fully apply client-side since the client hasn't finished
            // processing login packets yet. Delaying by one tick makes sure it sticks.
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    gm.enterSpectatorState(player);
                    player.sendMessage(gm.msg("messages.now-spectating"));
                }
            });
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfig().getBoolean("rules.block-join-leave-messages", true)) {
            event.setQuitMessage(null);
        }
    }
}
