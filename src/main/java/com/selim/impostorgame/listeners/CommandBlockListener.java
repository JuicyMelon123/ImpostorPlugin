package com.selim.impostorgame.listeners;

import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandBlockListener implements Listener {

    private final ImpostorGamePlugin plugin;

    public CommandBlockListener(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("rules.block-msg-commands", true)) return;

        String message = event.getMessage().toLowerCase();
        String label = message.substring(1).split(" ")[0];

        // Strip a plugin prefix like "essentials:msg"
        if (label.contains(":")) {
            label = label.substring(label.indexOf(':') + 1);
        }

        List<String> blocked = plugin.getConfig().getStringList("rules.blocked-commands");

        if (blocked.contains(label)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Private messages are disabled. Please use the voice chat.");
        }
    }
}
