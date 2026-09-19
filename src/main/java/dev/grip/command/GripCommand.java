package dev.grip.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.grip.GripPlugin;
import dev.grip.message.Messages;
import dev.grip.player.PlayerPrefs;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /grip toggle for players, /grip reload for admins.
 */
public final class GripCommand {

    private final GripPlugin plugin;
    private final PlayerPrefs prefs;
    private final Messages messages;

    public GripCommand(GripPlugin plugin, PlayerPrefs prefs, Messages messages) {
        this.plugin = plugin;
        this.prefs = prefs;
        this.messages = messages;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("grip")
                .then(Commands.literal("toggle")
                        .requires(source -> source.getSender().hasPermission(Perms.TOGGLE))
                        .executes(this::toggle))
                .then(Commands.literal("reload")
                        .requires(source -> source.getSender().hasPermission(Perms.ADMIN))
                        .executes(this::reload));
    }

    private int toggle(CommandContext<CommandSourceStack> ctx) {
        final CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player player) {
            messages.send(player, "toggle", messages.state(prefs.toggle(player)));
        } else {
            messages.send(sender, "players_only");
        }
        return Command.SINGLE_SUCCESS;
    }

    private int reload(CommandContext<CommandSourceStack> ctx) {
        plugin.reload();
        messages.send(ctx.getSource().getSender(), "reload");
        return Command.SINGLE_SUCCESS;
    }
}
