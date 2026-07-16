package com.selim.impostorgame;

import com.selim.impostorgame.commands.ImpStartCommand;
import com.selim.impostorgame.commands.ImpostorGameCommand;
import com.selim.impostorgame.listeners.ChatBlockListener;
import com.selim.impostorgame.listeners.CommandBlockListener;
import com.selim.impostorgame.listeners.CompassGUIListener;
import com.selim.impostorgame.listeners.GameListener;
import com.selim.impostorgame.listeners.PlayerInteractListener;
import com.selim.impostorgame.listeners.PlayerPresenceListener;
import com.selim.impostorgame.listeners.SpectatorMenuListener;
import com.selim.impostorgame.listeners.SpectatorProtectionListener;
import com.selim.impostorgame.tasks.CompassUpdateTask;
import com.selim.impostorgame.tasks.SpectateFollowTask;
import com.selim.impostorgame.tasks.TimerTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ImpostorGamePlugin extends JavaPlugin {

    private static ImpostorGamePlugin instance;
    private GameManager gameManager;
    private CompassUpdateTask compassUpdateTask;
    private TimerTask timerTask;
    private SpectateFollowTask spectateFollowTask;

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
        getServer().getPluginManager().registerEvents(new PlayerPresenceListener(this), this);
        getServer().getPluginManager().registerEvents(new SpectatorMenuListener(), this);
        getServer().getPluginManager().registerEvents(new SpectatorProtectionListener(this), this);

        // In case of a reload with players already online, apply tab-hiding and
        // nametag-hiding right away
        this.gameManager.setupNametagTeam();
        this.gameManager.applyTabVisibility();
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.gameManager.applyNametagVisibility(player);
        }

        // Compass tracking runs once a second
        this.compassUpdateTask = new CompassUpdateTask(this);
        this.compassUpdateTask.runTaskTimer(this, 20L, 20L);

        // Round timer also runs once a second
        this.timerTask = new TimerTask(this);
        this.timerTask.runTaskTimer(this, 20L, 20L);

        // Spectator follow-teleport also runs once a second
        this.spectateFollowTask = new SpectateFollowTask(this);
        this.spectateFollowTask.runTaskTimer(this, 20L, 20L);

        getLogger().info("ImpostorGame has been enabled!");
    }

    @Override
    public void onDisable() {
        if (compassUpdateTask != null) {
            compassUpdateTask.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (spectateFollowTask != null) {
            spectateFollowTask.cancel();
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
