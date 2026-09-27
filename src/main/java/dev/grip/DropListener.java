package dev.grip;

import dev.grip.config.Settings;
import dev.grip.confirm.Confirmations;
import dev.grip.message.Messages;
import dev.grip.player.PlayerPrefs;
import dev.grip.rules.DropRules;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Turns the first Q press, or the first click outside the window, into a question and lets the
 * second one through.
 */
public final class DropListener implements Listener {

    public static final String BYPASS = "grip.bypass";

    private final Plugin plugin;
    private final Supplier<Settings> settings;
    private final Supplier<DropRules> rules;
    private final Confirmations confirmations;
    private final PlayerPrefs prefs;
    private final Messages messages;
    // Main thread only. A GUI click that already asked (or was told to skip asking) marks the
    // player here so the PlayerDropItemEvent it fires right after doesn't ask a second time -
    // asking twice means the second answer arrives as a cancel with nothing to hand the item
    // back to (see onDrop/canReturn), which drops it out of a container or deletes it outright.
    private final Set<UUID> clickApproved = new HashSet<>();

    public DropListener(Plugin plugin, Supplier<Settings> settings, Supplier<DropRules> rules,
                        Confirmations confirmations, PlayerPrefs prefs, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.rules = rules;
        this.confirmations = confirmations;
        this.prefs = prefs;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();
        if (clickApproved.remove(player.getUniqueId())) {
            return;
        }
        final ItemStack stack = event.getItemDrop().getItemStack();
        if (!shouldAsk(player, stack)) {
            return;
        }
        if (!canReturn(player, stack)) {
            // Cancelling here would delete the item: Bukkit hands a cancelled drop back to the
            // main hand or inventory and silently drops anything that doesn't fit. Letting it
            // fall is worse than losing nothing, but better than losing the item outright.
            return;
        }
        if (!confirm(player, stack)) {
            event.setCancelled(true);
            // the client already removed the item from its own view, so send the real inventory back
            plugin.getServer().getScheduler().runTask(plugin, player::updateInventory);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClickOutside(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.OUTSIDE
                || (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT
                        && event.getClick() != ClickType.SHIFT_LEFT && event.getClick() != ClickType.SHIFT_RIGHT)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack stack = event.getCursor();
        if (!shouldAsk(player, stack)) {
            return;
        }
        if (!settings.get().clickOutside()) {
            // Setting says don't ask for outside drops, but the drop still happens and still
            // fires PlayerDropItemEvent right after - mark it approved instead of asking there.
            approveNextDrop(player);
            return;
        }
        if (confirm(player, stack)) {
            approveNextDrop(player);
        } else {
            event.setCancelled(true);
        }
    }

    // Pressing Q (or Ctrl+Q) while hovering a slot in any open inventory drops that slot's item
    // right away, and it also fires a PlayerDropItemEvent for that same drop a moment later -
    // approveNextDrop keeps onDrop from asking about it twice.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiDrop(InventoryClickEvent event) {
        if (event.getClick() != ClickType.DROP && event.getClick() != ClickType.CONTROL_DROP) {
            return;
        }
        if (event.getAction() != InventoryAction.DROP_ONE_SLOT && event.getAction() != InventoryAction.DROP_ALL_SLOT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack stack = event.getCurrentItem();
        if (stack == null || !shouldAsk(player, stack)) {
            return;
        }
        if (confirm(player, stack)) {
            approveNextDrop(player);
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        final UUID id = event.getPlayer().getUniqueId();
        confirmations.forget(id);
        clickApproved.remove(id);
    }

    private void approveNextDrop(Player player) {
        final UUID id = player.getUniqueId();
        clickApproved.add(id);
        plugin.getServer().getScheduler().runTask(plugin, () -> clickApproved.remove(id));
    }

    /** Whether the player's inventory has room for the whole stack if a drop of it is cancelled. */
    private static boolean canReturn(Player player, ItemStack drop) {
        int room = 0;
        for (ItemStack slot : player.getInventory().getStorageContents()) {
            if (slot == null || slot.getType().isAir()) {
                room += drop.getMaxStackSize();
            } else if (slot.isSimilar(drop)) {
                room += Math.max(0, slot.getMaxStackSize() - slot.getAmount());
            }
            if (room >= drop.getAmount()) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldAsk(Player player, ItemStack stack) {
        if (!prefs.enabled(player) || player.hasPermission(BYPASS)) {
            return false;
        }
        if (settings.get().ignoreCreative() && player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        return rules.get().needsConfirmation(stack);
    }

    /** True if the drop may go ahead; otherwise the player has just been asked. */
    private boolean confirm(Player player, ItemStack stack) {
        final long window = settings.get().confirmTime().toMillis();
        if (confirmations.confirm(player.getUniqueId(), stack, System.currentTimeMillis(), window)) {
            return true;
        }
        messages.send(player, "prompt",
                Messages.component("item", nameOf(stack)),
                Messages.text("time", settings.get().confirmTime().toSeconds()));
        return false;
    }

    /** The name a player knows the item by: the custom name if it has one, otherwise the game's own. */
    private static Component nameOf(ItemStack stack) {
        final ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        return Component.translatable(stack.getType().translationKey());
    }
}
