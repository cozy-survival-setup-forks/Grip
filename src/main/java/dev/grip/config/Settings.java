package dev.grip.config;

import dev.grip.util.Durations;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * config.yml, parsed once per reload so a drop never has to touch YAML.
 */
public record Settings(Duration confirmTime, boolean ignoreCreative, boolean clickOutside,
                       Set<Material> alwaysConfirm, Protect protect, FreeDrop freeDrop) {

    public record Protect(boolean named, boolean customModels, boolean enchanted, boolean containers) {
    }

    public record FreeDrop(boolean food, boolean blocks, Set<Material> list) {
    }

    public static Settings load(FileConfiguration cfg, Logger log) {
        return new Settings(
                confirmTime(cfg, log),
                cfg.getBoolean("ignore_creative", true),
                cfg.getBoolean("click_outside", true),
                materials(cfg, "always_confirm", log),
                new Protect(
                        cfg.getBoolean("protect.named", true),
                        cfg.getBoolean("protect.custom_models", true),
                        cfg.getBoolean("protect.enchanted", true),
                        cfg.getBoolean("protect.containers", true)),
                new FreeDrop(
                        cfg.getBoolean("free_drop.food", true),
                        cfg.getBoolean("free_drop.blocks", true),
                        materials(cfg, "free_drop.list", log)));
    }

    private static final Duration CONFIRM_TIME_MAX = Duration.ofSeconds(60);

    private static Duration confirmTime(FileConfiguration cfg, Logger log) {
        final String raw = cfg.getString("confirm_time", "3s");
        final Duration time;
        try {
            time = Durations.parse(raw);
        } catch (IllegalArgumentException e) {
            log.warning("confirm_time: '" + raw + "' is not a duration, using 3s");
            return Duration.ofSeconds(3);
        }
        if (time.isZero()) {
            return Duration.ofSeconds(3);
        }
        // A value this large would overflow Duration.toMillis() on every drop (see
        // DropListener.confirm), and nobody wants a "press Q again" window longer than a minute.
        if (time.isNegative() || time.compareTo(CONFIRM_TIME_MAX) > 0) {
            log.warning("confirm_time: '" + raw + "' must be between 1s and 60s, using 3s");
            return Duration.ofSeconds(3);
        }
        return time;
    }

    private static Set<Material> materials(FileConfiguration cfg, String path, Logger log) {
        if (cfg.isSet(path) && !cfg.isList(path)) {
            // getStringList silently returns the bundled default for this instead of the admin's
            // value if it isn't written as a YAML list - worth a warning, since it looks like it worked.
            log.warning(path + " should be a list (e.g. \"[DIAMOND, NETHERITE_INGOT]\"), ignoring it");
        }
        final Set<Material> found = EnumSet.noneOf(Material.class);
        for (String entry : cfg.getStringList(path)) {
            found.addAll(resolve(entry, path, log));
        }
        return found;
    }

    /** A material name, or an item tag written as #minecraft:swords. */
    private static Set<Material> resolve(String entry, String path, Logger log) {
        final String name = entry.trim();
        if (name.startsWith("#")) {
            final NamespacedKey key = NamespacedKey.fromString(name.substring(1).toLowerCase(Locale.ROOT));
            final Tag<Material> tag = key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
            if (tag == null) {
                log.warning(path + ": unknown item tag " + name + ", skipping it");
                return Set.of();
            }
            return tag.getValues();
        }
        final Material material = Material.matchMaterial(name);
        if (material == null) {
            log.warning(path + ": unknown material " + name + ", skipping it");
            return Set.of();
        }
        return Set.of(material);
    }
}
