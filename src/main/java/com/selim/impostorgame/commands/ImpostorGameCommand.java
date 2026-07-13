package com.selim.impostorgame.commands;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ImpostorGameCommand implements CommandExecutor {

    private final ImpostorGamePlugin plugin;

    public ImpostorGameCommand(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        GameManager gm = plugin.getGameManager();

        if (!sender.hasPermission("impostorgame.admin")) {
            sender.sendMessage(gm.msg("messages.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(gm.msg("messages.usage-impostorgame"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();

                boolean hideTab = plugin.getConfig().getBoolean("rules.hide-tab-list", true);
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setListed(!hideTab);
                    gm.applyNametagVisibility(player);
                }

                sender.sendMessage(gm.msg("messages.config-reloaded"));
                break;
            case "stop":
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (gm.isParticipant(player.getUniqueId())) {
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                    }
                }
                gm.stopGame();
                sender.sendMessage(gm.msg("messages.game-stopped"));
                break;
            default:
                sender.sendMessage(gm.msg("messages.usage-impostorgame"));
        }
        return true;
    }
}
