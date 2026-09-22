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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

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
        final ItemStack stack = event.getItemDrop().getItemStack();
        if (!shouldAsk(player, stack)) {
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
        if (!settings.get().clickOutside() || event.getSlotType() != InventoryType.SlotType.OUTSIDE
                || event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack stack = event.getCursor();
        if (!shouldAsk(player, stack)) {
            return;
        }
        if (!confirm(player, stack)) {
            event.setCancelled(true);
        }
    }

    // Pressing Q (or Ctrl+Q) while hovering a slot in any open inventory drops that slot's item
    // straight away and never fires PlayerDropItemEvent, so it needs its own check here.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGuiDrop(InventoryClickEvent event) {
        if (event.getClick() != ClickType.DROP && event.getClick() != ClickType.CONTROL_DROP) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        final ItemStack stack = event.getCurrentItem();
        if (stack == null || !shouldAsk(player, stack)) {
            return;
        }
        if (!confirm(player, stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        confirmations.forget(event.getPlayer().getUniqueId());
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
