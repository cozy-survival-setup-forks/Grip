package dev.grip;

import dev.grip.command.GripCommand;
import dev.grip.command.Perms;
import dev.grip.config.Settings;
import dev.grip.confirm.Confirmations;
import dev.grip.message.Messages;
import dev.grip.player.PlayerPrefs;
import dev.grip.rules.DropRules;
import dev.grip.safe.ConfigMigrator;
import dev.grip.safe.Doctor;
import dev.grip.safe.FileBackups;
import dev.grip.safe.Guard;
import dev.grip.safe.Health;
import dev.grip.safe.Prep;
import dev.grip.safe.ServerId;
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

    private static final int CONFIG_VERSION = 1;
    private static final int LANG_VERSION = 1;

    private final List<Prep.Spec> files = List.of(
            new Prep.Spec("config.yml", "config-version", CONFIG_VERSION, Prep.configMigrator(CONFIG_VERSION), rules -> {
                rules.duration("confirm_time");
                rules.materials("always_confirm");
                rules.materials("free_drop.list");
            }),
            new Prep.Spec("lang.yml", "lang-version", LANG_VERSION, new ConfigMigrator("lang-version", LANG_VERSION), null));

    private volatile Settings settings;
    private volatile DropRules rules;
    private Messages messages;

    @Override
    public void onEnable() {
        try {
            enableInner();
        } catch (RuntimeException e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Grip failed to start and will be disabled. This is "
                    + "usually a bad config.yml or lang.yml - check the warnings above this, or delete the whole "
                    + "plugins/Grip folder to regenerate defaults.", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void enableInner() {
        saveDefaultConfig();
        Health.storage("YAML files in the plugin folder (config.yml, lang.yml, data.yml)");
        Prep.startup(this, files);
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
        final boolean beacon = getConfig().getBoolean("metrics.enabled", true);
        Metrics.start(this, ServerId.resolve(getDataFolder().toPath(),
                ServerId.inYaml(new File(getDataFolder(), "data.yml").toPath(), getLogger()), beacon, getLogger()));
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
        final List<Guard.Problem> problems = Prep.validate(this, files);
        if (!problems.isEmpty()) {
            Prep.logRejected(this, problems);
            return false;
        }
        reloadConfig();
        applyConfig();
        messages.load();
        return true;
    }

    /** The text of /grip doctor. */
    public List<String> doctor() {
        final List<String> extra = new java.util.ArrayList<>(Prep.versionLines(this, files));
        extra.add("Pending writes: 0 (this plugin keeps no queued saves)");
        return Doctor.report(getName(), getPluginMeta().getVersion(), extra);
    }

    /** /grip backup now: a verified copy of the settings and data files. */
    public boolean backupNow() {
        final List<String> names = new java.util.ArrayList<>(Prep.fileNames(files));
        names.add("data.yml");
        return FileBackups.snapshot(getDataFolder().toPath(), names, 5, getLogger());
    }

    private void applyConfig() {
        final Settings loaded = Settings.load(getConfig(), getLogger());
        settings = loaded;
        rules = new DropRules(loaded);
    }
}
