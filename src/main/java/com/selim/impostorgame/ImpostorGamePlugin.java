package com.selim.impostorgame;

import com.selim.impostorgame.commands.ImpStartCommand;
import com.selim.impostorgame.commands.ImpostorGameCommand;
import com.selim.impostorgame.listeners.ChatBlockListener;
import com.selim.impostorgame.listeners.CommandBlockListener;
import com.selim.impostorgame.listeners.CompassGUIListener;
import com.selim.impostorgame.listeners.GameListener;
import com.selim.impostorgame.listeners.PlayerInteractListener;
import com.selim.impostorgame.tasks.CompassUpdateTask;
import org.bukkit.plugin.java.JavaPlugin;

public class ImpostorGamePlugin extends JavaPlugin {

    private static ImpostorGamePlugin instance;
    private GameManager gameManager;
    private CompassUpdateTask compassUpdateTask;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gameManager = new GameManager(this);

        // Commands
        getCommand("impstart").setExecutor(new ImpStartCommand(this));
        getCommand("impostorgame").setExecutor(new ImpostorGameCommand(this));

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new CompassGUIListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatBlockListener(this), this);
        getServer().getPluginManager().registerEvents(new CommandBlockListener(this), this);

        // Compass tracking runs once a second
        this.compassUpdateTask = new CompassUpdateTask(this);
        this.compassUpdateTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("ImpostorGame has been enabled!");
    }

    @Override
    public void onDisable() {
        if (compassUpdateTask != null) {
            compassUpdateTask.cancel();
        }
        getLogger().info("ImpostorGame has been disabled!");
    }

    public static ImpostorGamePlugin getInstance() {
        return instance;
    }

    public GameManager getGameManager() {
        return gameManager;
    }
}
