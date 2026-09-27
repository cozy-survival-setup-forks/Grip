package dev.grip;

import dev.grip.command.GripCommand;
import dev.grip.command.Perms;
import dev.grip.config.Settings;
import dev.grip.confirm.Confirmations;
import dev.grip.message.Messages;
import dev.grip.player.PlayerPrefs;
import dev.grip.rules.DropRules;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Grip: asks for a second Q press before a valuable item leaves the player's inventory.
 *
 * @author Groovified, Blockie Studios
 */
public class GripPlugin extends JavaPlugin {

    private volatile Settings settings;
    private volatile DropRules rules;
    private Messages messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyConfig();

        Perms.register(getServer().getPluginManager());

        final PlayerPrefs prefs = new PlayerPrefs(this);
        messages = new Messages(this);
        messages.load();

        final Confirmations confirmations = new Confirmations();
        getServer().getPluginManager().registerEvents(prefs, this);
        getServer().getPluginManager().registerEvents(
                new DropListener(this, () -> settings, () -> rules, confirmations, prefs, messages), this);

        final GripCommand command = new GripCommand(this, prefs, messages);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(command.build().build(), "Drop confirmation settings", List.of()));
        Metrics.start(this);
        Banner.print(this, "Thanks for keeping a fumbled Q from costing anyone their gear.");
    }

    /**
     * Re-reads config.yml and lang.yml. Returns false and leaves the settings already loaded in
     * place if config.yml doesn't parse - {@link #reloadConfig()} would otherwise silently swap
     * in an empty config (and so the bundled defaults) with no error beyond a console log.
     */
    public boolean reload() {
        final File configFile = new File(getDataFolder(), "config.yml");
        try {
            new YamlConfiguration().load(configFile);
        } catch (InvalidConfigurationException | IOException e) {
            getLogger().warning("config.yml is broken, keeping the settings already loaded: " + e.getMessage());
            return false;
        }
        reloadConfig();
        applyConfig();
        messages.load();
        return true;
    }

    private void applyConfig() {
        final Settings loaded = Settings.load(getConfig(), getLogger());
        settings = loaded;
        rules = new DropRules(loaded);
    }
}
