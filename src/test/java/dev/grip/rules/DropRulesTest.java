package dev.grip.rules;

import dev.grip.config.Settings;
import dev.grip.confirm.Confirmations;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.io.InputStreamReader;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DropRulesTest {

    private DropRules rules;

    @BeforeEach
    void setUp() throws Exception {
        MockBukkit.mock();
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(getClass().getResourceAsStream("/config.yml")));
        rules = new DropRules(Settings.load(config, Logger.getAnonymousLogger()));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void toolsAndGearAsk() {
        assertTrue(rules.needsConfirmation(new ItemStack(Material.DIAMOND_SWORD)));
        assertTrue(rules.needsConfirmation(new ItemStack(Material.NETHERITE_CHESTPLATE)));
    }

    @Test
    void cheapThingsDropFreely() {
        assertFalse(rules.needsConfirmation(new ItemStack(Material.COBBLESTONE)));
        assertFalse(rules.needsConfirmation(new ItemStack(Material.BREAD)));
        assertFalse(rules.needsConfirmation(new ItemStack(Material.ROTTEN_FLESH)));
    }

    @Test
    void alwaysConfirmBeatsTheFreeRules() {
        assertTrue(rules.needsConfirmation(new ItemStack(Material.GOLDEN_APPLE)));
        assertTrue(rules.needsConfirmation(new ItemStack(Material.DIAMOND)));
        assertTrue(rules.needsConfirmation(new ItemStack(Material.WHITE_SHULKER_BOX)));
    }

    @Test
    void protectedTraitsBeatTheFreeRules() {
        final ItemStack enchanted = new ItemStack(Material.COBBLESTONE);
        enchanted.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
        assertTrue(rules.needsConfirmation(enchanted));

        final ItemStack renamed = new ItemStack(Material.DIRT);
        renamed.editMeta(meta -> meta.displayName(net.kyori.adventure.text.Component.text("Keepsake")));
        assertTrue(rules.needsConfirmation(renamed));
    }

    @Test
    void airNeverAsks() {
        assertFalse(rules.needsConfirmation(new ItemStack(Material.AIR)));
    }

    @Test
    void secondPressWithinTheWindowConfirms() {
        final Confirmations confirmations = new Confirmations();
        final UUID id = UUID.randomUUID();
        final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);

        assertFalse(confirmations.confirm(id, sword, 1_000, 3_000));
        assertTrue(confirmations.confirm(id, sword, 2_500, 3_000));
        assertFalse(confirmations.confirm(id, sword, 2_600, 3_000));
    }

    @Test
    void lateOrDifferentPressStartsOver() {
        final Confirmations confirmations = new Confirmations();
        final UUID id = UUID.randomUUID();

        assertFalse(confirmations.confirm(id, new ItemStack(Material.DIAMOND_SWORD), 1_000, 3_000));
        assertFalse(confirmations.confirm(id, new ItemStack(Material.DIAMOND_SWORD), 5_000, 3_000));
        assertFalse(confirmations.confirm(id, new ItemStack(Material.DIAMOND_PICKAXE), 5_500, 3_000));
    }
}
