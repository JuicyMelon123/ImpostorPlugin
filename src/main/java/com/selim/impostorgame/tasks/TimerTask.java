package com.selim.impostorgame.tasks;

import com.selim.impostorgame.GameManager;
import com.selim.impostorgame.ImpostorGamePlugin;
import com.selim.impostorgame.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TimerTask extends BukkitRunnable {

    private final ImpostorGamePlugin plugin;

    public TimerTask(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        GameManager gm = plugin.getGameManager();

        if (!gm.isGameRunning() || !gm.isTimerActive()) return;

        boolean justExpired = gm.tickTimer();

        String text = gm.msg("messages.timer-format", "{time}", gm.formatRemainingTime());
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (gm.isParticipant(player.getUniqueId())) {
                Utils.sendActionBar(player, text);
            }
        }

        if (justExpired) {
            // Crewmates ran out of time to find the impostor
            gm.endGame("messages.impostor-win-timeout");
        }
    }
}
