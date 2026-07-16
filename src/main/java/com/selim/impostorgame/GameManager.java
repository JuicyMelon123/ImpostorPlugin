package com.selim.impostorgame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

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
    public final NamespacedKey SPECTATOR_ITEM_KEY;
    public final NamespacedKey SPECTATOR_TARGET_KEY;

    private boolean gameRunning = false;
    private UUID impostorId = null;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Map<UUID, Integer> respawnsLeft = new HashMap<>();
    private final Map<UUID, TrackMode> compassMode = new HashMap<>();
    private final Map<UUID, Boolean> abilityActive = new HashMap<>();
    private final Map<UUID, UUID> spectateFollowing = new HashMap<>();

    private int remainingSeconds = 0;
    private boolean timerActive = false;

    public GameManager(ImpostorGamePlugin plugin) {
        this.plugin = plugin;
        this.ABILITY_DYE_KEY = new NamespacedKey(plugin, "ability_dye");
        this.TRACKER_COMPASS_KEY = new NamespacedKey(plugin, "tracker_compass");
        this.SPECTATOR_ITEM_KEY = new NamespacedKey(plugin, "spectator_item");
        this.SPECTATOR_TARGET_KEY = new NamespacedKey(plugin, "spectator_target");
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
        spectateFollowing.clear();
        timerActive = false;
        remainingSeconds = 0;
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

    /** Broadcasts a win message, resets participants, and stops the round. */
    public void endGame(String winMessageKey) {
        if (!gameRunning) return; // already ended by another trigger this tick

        String message = msg(winMessageKey);
        Bukkit.broadcastMessage(message);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isParticipant(player.getUniqueId())) continue;

            if (!alive.contains(player.getUniqueId())) {
                // Was spectating - undo invisibility/flight/invulnerability and clear
                // the leftover nether star. Survivors keep whatever they were carrying.
                exitSpectatorState(player);
            } else {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }

        stopGame();

        // Now that gameRunning is false, chat/tab/nametag hiding turn back off -
        // refresh everyone's visibility immediately instead of waiting for their
        // next join.
        applyTabVisibility();
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyNametagVisibility(player);
        }
    }

    // ---------------- Round timer ----------------

    /** Reads the configured duration and starts the countdown for a new round. */
    public void startTimer() {
        int hours = plugin.getConfig().getInt("timer.duration-hours", 0);
        int minutes = plugin.getConfig().getInt("timer.duration-minutes", 15);
        int seconds = plugin.getConfig().getInt("timer.duration-seconds", 0);

        remainingSeconds = Math.max(0, hours * 3600 + minutes * 60 + seconds);
        timerActive = plugin.getConfig().getBoolean("timer.enabled", true) && remainingSeconds > 0;
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    /** Ticks the timer down by one second. Returns true the moment it hits zero. */
    public boolean tickTimer() {
        if (!timerActive) return false;

        remainingSeconds--;
        if (remainingSeconds <= 0) {
            remainingSeconds = 0;
            timerActive = false;
            return true;
        }
        return false;
    }

    public String formatRemainingTime() {
        int h = remainingSeconds / 3600;
        int m = (remainingSeconds % 3600) / 60;
        int s = remainingSeconds % 60;
        return String.format("%d:%02d:%02d", h, m, s);
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

    /** True while a round is active and this participant has been eliminated (is spectating). */
    public boolean isSpectating(UUID uuid) {
        return gameRunning && isParticipant(uuid) && !alive.contains(uuid);
    }

    // ---------------- Spectate-follow tracking ----------------

    public void setSpectateFollowing(UUID spectatorId, UUID targetId) {
        spectateFollowing.put(spectatorId, targetId);
    }

    public UUID getSpectateFollowing(UUID spectatorId) {
        return spectateFollowing.get(spectatorId);
    }

    // ---------------- Impostor respawns ----------------

    public boolean hasRespawnLeft(UUID uuid) {
        return respawnsLeft.getOrDefault(uuid, 0) > 0;
    }

    public void useRespawn(UUID uuid) {
        respawnsLeft.merge(uuid, -1, Integer::sum);
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

    // ---------------- Tab list visibility ----------------

    /** Applies the current hide-tab-list setting across every pair of online players. */
    public void applyTabVisibility() {
        boolean hide = gameRunning && plugin.getConfig().getBoolean("rules.hide-tab-list", true);
        java.util.Collection<? extends Player> online = Bukkit.getOnlinePlayers();

        for (Player viewer : online) {
            for (Player target : online) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) continue;

                if (hide) {
                    viewer.unlistPlayer(target);
                } else {
                    viewer.listPlayer(target);
                }
            }
        }
    }

    /** Applies the current hide-tab-list setting between a newly-joined player and everyone else. */
    public void applyTabVisibilityForJoin(Player joined) {
        boolean hide = gameRunning && plugin.getConfig().getBoolean("rules.hide-tab-list", true);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(joined.getUniqueId())) continue;

            if (hide) {
                joined.unlistPlayer(other);
                other.unlistPlayer(joined);
            } else {
                joined.listPlayer(other);
                other.listPlayer(joined);
            }
        }
    }

    // ---------------- Nametag hiding ----------------

    private static final String HIDDEN_NAMETAG_TEAM = "impostorgame_hidden";

    /** Creates (or fetches) the team used to hide everyone's nametag, and configures it. */
    public void setupNametagTeam() {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(HIDDEN_NAMETAG_TEAM);
        if (team == null) {
            team = board.registerNewTeam(HIDDEN_NAMETAG_TEAM);
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
    }

    /** Adds or removes a player from the nametag-hiding team based on current config. */
    public void applyNametagVisibility(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam(HIDDEN_NAMETAG_TEAM);
        if (team == null) {
            setupNametagTeam();
            team = board.getTeam(HIDDEN_NAMETAG_TEAM);
        }

        boolean hideNametags = gameRunning && plugin.getConfig().getBoolean("rules.hide-nametags", true);

        if (hideNametags) {
            if (!team.hasEntry(player.getName())) {
                team.addEntry(player.getName());
            }
        } else {
            team.removeEntry(player.getName());
        }
    }

    // ---------------- Messaging helpers ----------------
    public String msg(String path) {
        String raw = plugin.getConfig().getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String msg(String path, String placeholder, String value) {
        return msg(path).replace(placeholder, value);
    }

    // ---------------- Item builders ----------------

    public ItemStack createTrackerCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.compass-item-name", "&e&lTakip Cihazı")));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.compass-item-lore", "&7Bir oyuncuyu takip etmek için sağ tıkla"))));
        // Adds the enchant glint for visual flair without showing any enchantment text
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(TRACKER_COMPASS_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createAbilityDye(boolean active) {
        ItemStack item = new ItemStack(active ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        String colorPrefix = active ? ChatColor.GREEN.toString() : ChatColor.GRAY.toString();
        String name = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.ability-item-name", "&lYetenek"));
        meta.setDisplayName(colorPrefix + ChatColor.stripColor(name));
        meta.setLore(Collections.singletonList(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.ability-item-lore", "&7Yeteneğini açıp kapatmak için sağ tıkla"))));
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

    public ItemStack createSpectatorItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.spectator-item-name", "&d&lOyuncuları izle")));
        meta.getPersistentDataContainer().set(SPECTATOR_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSpectatorItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SPECTATOR_ITEM_KEY, PersistentDataType.BYTE);
    }

    // ---------------- Fake spectator state ----------------
    // Real GameMode.SPECTATOR disables the hotbar entirely (items can't be held or
    // used), which breaks the nether star menu. So instead we build our own
    // "spectator" out of Adventure mode + invisibility + flight + invulnerability.
    // Trade-off: no true noclip through solid blocks (that's a client-only feature
    // of real spectator mode) - flight lets them go around/over obstacles instead.

    public void enterSpectatorState(Player player) {
        player.setGameMode(GameMode.ADVENTURE);
        player.setInvulnerable(true);
        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false, false));
        player.setFoodLevel(20);
        player.setSaturation(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItem(0, createSpectatorItem());
    }

    public void exitSpectatorState(Player player) {
        player.setInvulnerable(false);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
    }

    /** A player head (with their real skin) representing a spectatable player, with their UUID stored for lookup on click. */
    public ItemStack createSpectateTargetItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName(ChatColor.AQUA + target.getName());
        meta.getPersistentDataContainer().set(SPECTATOR_TARGET_KEY, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    /** Reads the target UUID stored on a player head created by createSpectateTargetItem, or null if not one. */
    public UUID getSpectateTarget(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String raw = item.getItemMeta().getPersistentDataContainer().get(SPECTATOR_TARGET_KEY, PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
