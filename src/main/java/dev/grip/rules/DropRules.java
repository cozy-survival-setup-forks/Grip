package dev.grip.rules;

import dev.grip.config.Settings;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import io.papermc.paper.datacomponent.item.ItemContainerContents;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Decides whether dropping a stack needs a second press. The order matters: the always-confirm
 * list and the protected traits win over anything on the free list.
 */
public final class DropRules {

    private final Settings settings;

    public DropRules(Settings settings) {
        this.settings = settings;
    }

    public boolean needsConfirmation(ItemStack stack) {
        final Material type = stack.getType();
        if (type.isAir()) {
            return false;
        }
        if (settings.alwaysConfirm().contains(type) || isProtected(stack)) {
            return true;
        }
        return !isFree(type);
    }

    private boolean isFree(Material type) {
        final Settings.FreeDrop free = settings.freeDrop();
        return (free.food() && type.isEdible())
                || (free.blocks() && type.isBlock())
                || free.list().contains(type);
    }

    private boolean isProtected(ItemStack stack) {
        final Settings.Protect protect = settings.protect();
        if (protect.named() && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return true;
        }
        if (protect.customModels() && (stack.isDataOverridden(DataComponentTypes.CUSTOM_MODEL_DATA)
                || stack.isDataOverridden(DataComponentTypes.ITEM_MODEL))) {
            return true;
        }
        if (protect.enchanted() && isEnchanted(stack)) {
            return true;
        }
        return protect.containers() && hasContents(stack);
    }

    private static boolean isEnchanted(ItemStack stack) {
        return !stack.getEnchantments().isEmpty() || stack.hasData(DataComponentTypes.STORED_ENCHANTMENTS)
                && !stack.getData(DataComponentTypes.STORED_ENCHANTMENTS).enchantments().isEmpty();
    }

    private static boolean hasContents(ItemStack stack) {
        final ItemContainerContents box = stack.getData(DataComponentTypes.CONTAINER);
        if (box != null && !box.contents().isEmpty()) {
            return true;
        }
        final BundleContents bundle = stack.getData(DataComponentTypes.BUNDLE_CONTENTS);
        return bundle != null && !bundle.contents().isEmpty();
    }
}
