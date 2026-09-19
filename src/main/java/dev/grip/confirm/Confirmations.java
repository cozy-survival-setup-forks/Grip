package dev.grip.confirm;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers which item each player was just asked about, and for how long the answer counts.
 */
public final class Confirmations {

    private record Pending(ItemStack item, long expiresAt) {
    }

    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    /**
     * Called for a drop that needs confirming. Returns true if this press is the second one for
     * the same item; otherwise starts a new window and returns false.
     */
    public boolean confirm(UUID player, ItemStack stack, long nowMillis, long windowMillis) {
        final Pending previous = pending.get(player);
        if (previous != null && previous.expiresAt() > nowMillis && previous.item().isSimilar(stack)) {
            pending.remove(player);
            return true;
        }
        pending.put(player, new Pending(stack.asOne(), nowMillis + windowMillis));
        return false;
    }

    public void forget(UUID player) {
        pending.remove(player);
    }
}
