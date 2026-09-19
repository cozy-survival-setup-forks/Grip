package dev.grip;

import dev.grip.command.GripCommand;
import dev.grip.command.Perms;
import dev.grip.config.Settings;
import dev.grip.confirm.Confirmations;
import dev.grip.message.Messages;
import dev.grip.player.PlayerPrefs;
import dev.grip.rules.DropRules;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

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
    }

    /** Re-reads config.yml and lang.yml. */
    public void reload() {
        reloadConfig();
        applyConfig();
        messages.load();
    }

    private void applyConfig() {
        final Settings loaded = Settings.load(getConfig(), getLogger());
        settings = loaded;
        rules = new DropRules(loaded);
    }
}
