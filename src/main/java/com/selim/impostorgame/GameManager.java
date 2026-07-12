package com.selim.impostorgame;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    public enum TrackMode {
        NEAREST,
        FURTHEST
    }

    private final ImpostorGamePlugin plugin;

    // Item identification keys, stored in each item's PersistentDataContainer
    public final NamespacedKey ABILITY_DYE_KEY;
    public final NamespacedKey TRACKER_COMPASS_KEY;

    private boolean gameRunning = false;
    private UUID impostorId = null;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Map<UUID, Integer> respawnsLeft = new HashMap<>();
    private final Map<UUID, TrackMode> compassMode = new HashMap<>();
    private final Map<UUID, Boolean> abilityActive = new HashMap<>();
    private final Set<UUID> pendingBan = new HashSet<>();

    public GameManager(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
        this.ABILITY_DYE_KEY = new NamespacedKey(plugin, "ability_dye");
        this.TRACKER_COMPASS_KEY = new NamespacedKey(plugin, "tracker_compass");
    }

    // ---------------- Game state ----------------

    public boolean isGameRunning() {
        return gameRunning;
    }

    public void resetGame() {
        gameRunning = false;
        impostorId = null;
        participants.clear();
        alive.clear();
        respawnsLeft.clear();
        abilityActive.clear();
        pendingBan.clear();
        // compassMode is intentionally kept - trackers keep working between rounds
    }

    public void startGame(Player impostor, Collection<? extends Player> allParticipants) {
        resetGame();
        gameRunning = true;
        impostorId = impostor.getUniqueId();

        for (Player p : allParticipants) {
            participants.add(p.getUniqueId());
            alive.add(p.getUniqueId());
        }

        respawnsLeft.put(impostorId, plugin.getConfig().getInt("respawn.impostor-respawns", 1));
        abilityActive.put(impostorId, false);
    }

    public void stopGame() {
        resetGame();
    }

    public boolean isImpostor(UUID uuid) {
        return gameRunning && impostorId != null && impostorId.equals(uuid);
    }

    public UUID getImpostorId() {
        return impostorId;
    }

    public boolean isParticipant(UUID uuid) {
        return participants.contains(uuid);
    }

    public Set<UUID> getAlive() {
        return alive;
    }

    public void markDead(UUID uuid) {
        alive.remove(uuid);
    }

    public void markAlive(UUID uuid) {
        alive.add(uuid);
    }

    // ---------------- Impostor respawns ----------------

    public boolean hasRespawnLeft(UUID uuid) {
        return respawnsLeft.getOrDefault(uuid, 0) > 0;
    }

    public void useRespawn(UUID uuid) {
        respawnsLeft.merge(uuid, -1, Integer::sum);
    }

    // ---------------- Pending ban (applied right after respawn) ----------------

    public void queueBan(UUID uuid) {
        pendingBan.add(uuid);
    }

    public boolean isPendingBan(UUID uuid) {
        return pendingBan.contains(uuid);
    }

    public void clearPendingBan(UUID uuid) {
        pendingBan.remove(uuid);
    }

    // ---------------- Ability toggle ----------------

    public boolean isAbilityActive(UUID uuid) {
        return abilityActive.getOrDefault(uuid, false);
    }

    public void setAbilityActive(UUID uuid, boolean active) {
        abilityActive.put(uuid, active);
    }

    // ---------------- Compass tracking ----------------

    public void setCompassMode(UUID uuid, TrackMode mode) {
        compassMode.put(uuid, mode);
    }

    public TrackMode getCompassMode(UUID uuid) {
        return compassMode.get(uuid);
    }

    // ---------------- Messaging helpers ----------------

    public String msg(String path) {
        String raw = plugin.getConfig().getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    // ---------------- Item builders ----------------

    public ItemStack createTrackerCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Tracker");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Right click to track a player"));
        meta.getPersistentDataContainer().set(TRACKER_COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createAbilityDye(boolean active) {
        ItemStack item = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((active ? ChatColor.GREEN : ChatColor.GRAY) + "" + ChatColor.BOLD + "Ability");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Right click to toggle your ability"));
        meta.getPersistentDataContainer().set(ABILITY_DYE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isTrackerCompass(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(TRACKER_COMPASS_KEY, PersistentDataType.BYTE);
    }

    public boolean isAbilityDye(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(ABILITY_DYE_KEY, PersistentDataType.BYTE);
    }
}
