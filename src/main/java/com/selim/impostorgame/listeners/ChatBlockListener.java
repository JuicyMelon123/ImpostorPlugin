package com.selim.impostorgame.listeners;

import com.selim.impostorgame.ImpostorGamePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatBlockListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public ChatBlockListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getGameManager().isGameRunning()) return;
        if (!plugin.getConfig().getBoolean("rules.block-chat", true)) return;
        event.setCancelled(true);
    }
}
