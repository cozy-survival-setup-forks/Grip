package dev.grip.player;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Players who turned the confirmation off. Kept in the player's own data so it survives restarts,
 * and cached while they are online.
 */
public final class PlayerPrefs implements Listener {

    private final NamespacedKey offKey;
    private final Set<UUID> off = ConcurrentHashMap.newKeySet();

    public PlayerPrefs(Plugin plugin) {
        this.offKey = new NamespacedKey(plugin, "confirmation_off");
        Bukkit.getOnlinePlayers().forEach(this::load);
    }

    public boolean enabled(Player player) {
        return !off.contains(player.getUniqueId());
    }

    /** Flips the switch and returns the new state. */
    public boolean toggle(Player player) {
        final boolean nowEnabled = !enabled(player);
        if (nowEnabled) {
            off.remove(player.getUniqueId());
            player.getPersistentDataContainer().remove(offKey);
        } else {
            off.add(player.getUniqueId());
            player.getPersistentDataContainer().set(offKey, PersistentDataType.BYTE, (byte) 1);
        }
        return nowEnabled;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        load(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        off.remove(event.getPlayer().getUniqueId());
    }

    private void load(Player player) {
        if (player.getPersistentDataContainer().has(offKey, PersistentDataType.BYTE)) {
            off.add(player.getUniqueId());
        }
    }
}
