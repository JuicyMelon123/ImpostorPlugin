package com.selim.impostorgame;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

    private boolean gameRunning = false;
    private UUID impostorId = null;

    private final Set<UUID> participants = new HashSet<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Map<UUID, Integer> respawnsLeft = new HashMap<>();
    private final Map<UUID, TrackMode> compassMode = new HashMap<>();
    private final Map<UUID, Boolean> abilityActive = new HashMap<>();

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
        boolean hide = plugin.getConfig().getBoolean("rules.hide-tab-list", true);
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
        boolean hide = plugin.getConfig().getBoolean("rules.hide-tab-list", true);

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

        boolean hideNametags = plugin.getConfig().getBoolean("rules.hide-nametags", true);

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
}
