package com.selim.impostorgame.commands;

import com.selim.impostorgame.ImpostorGamePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ImpostorGameCommand implements CommandExecutor {

    private final ImpostorGamePlugin plugin;

    public ImpostorGameCommand(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("impostorgame.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /impostorgame <reload|stop>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GREEN + "ImpostorGame config reloaded.");
                break;
            case "stop":
                plugin.getGameManager().stopGame();
                sender.sendMessage(ChatColor.GREEN + "The Impostor Game has been stopped.");
                break;
            default:
                sender.sendMessage(ChatColor.YELLOW + "Usage: /impostorgame <reload|stop>");
        }
        return true;
    }
}
