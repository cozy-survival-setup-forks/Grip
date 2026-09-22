package dev.grip;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropListenerTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.load(GripPlugin.class);
        player = server.addPlayer();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private PlayerDropItemEvent drop(Material material) {
        final Location where = player.getLocation();
        final Item item = player.getWorld().dropItem(where, new ItemStack(material));
        final PlayerDropItemEvent event = new PlayerDropItemEvent(player, item);
        server.getPluginManager().callEvent(event);
        return event;
    }

    @Test
    void firstPressIsHeldBackAndSecondGoesThrough() {
        assertTrue(drop(Material.DIAMOND_SWORD).isCancelled());
        assertFalse(drop(Material.DIAMOND_SWORD).isCancelled());
    }

    @Test
    void cheapItemsDropOnTheFirstPress() {
        assertFalse(drop(Material.COBBLESTONE).isCancelled());
    }

    @Test
    void anotherItemInBetweenRestartsTheQuestion() {
        assertTrue(drop(Material.DIAMOND_SWORD).isCancelled());
        assertTrue(drop(Material.DIAMOND_PICKAXE).isCancelled());
        assertTrue(drop(Material.DIAMOND_SWORD).isCancelled());
    }

    // Q on a hovered slot inside an open inventory never fires PlayerDropItemEvent, so it needs
    // its own guard; this is the bypass that let valuables out with no prompt at all.
    @Test
    void qOnAHoveredSlotInAnOpenInventoryAlsoAsks() {
        player.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        final InventoryView view = player.openInventory(player.getInventory());

        final InventoryClickEvent first = new InventoryClickEvent(
                view, InventoryType.SlotType.QUICKBAR, 0, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        server.getPluginManager().callEvent(first);
        assertTrue(first.isCancelled());

        final InventoryClickEvent second = new InventoryClickEvent(
                view, InventoryType.SlotType.QUICKBAR, 0, ClickType.DROP, InventoryAction.DROP_ONE_SLOT);
        server.getPluginManager().callEvent(second);
        assertFalse(second.isCancelled());
    }
}
