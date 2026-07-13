package com.selim.impostorgame.listeners;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.GameMode;
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

        if (plugin.getConfig().getBoolean("rules.hide-tab-list", true)) {
            // Hides this player from everyone's tab list. They remain fully
            // visible and interactable in the world - this is tab-list only.
            event.getPlayer().setListed(false);
        }

        plugin.getGameManager().applyNametagVisibility(event.getPlayer());

        // A round is active and this player was already eliminated from it
        // (they were kicked, not banned) -> let them rejoin as a spectator.
        GameManager gm = plugin.getGameManager();
        if (gm.isGameRunning() && gm.isParticipant(event.getPlayer().getUniqueId())
                && !gm.getAlive().contains(event.getPlayer().getUniqueId())) {
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            event.getPlayer().sendMessage(gm.msg("messages.now-spectating"));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (plugin.getConfig().getBoolean("rules.block-join-leave-messages", true)) {
            event.setQuitMessage(null);
        }
    }
}
