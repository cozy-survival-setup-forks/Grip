package dev.grip.command;

import dev.grip.DropListener;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

/**
 * Permission nodes. Turning the prompt off for yourself is open to everyone, reloading is for ops.
 */
public final class Perms {

    public static final String ADMIN = "grip.admin";
    public static final String TOGGLE = "grip.toggle";

    private Perms() {
    }

    public static void register(PluginManager manager) {
        add(manager, ADMIN, "Reload Grip", PermissionDefault.OP);
        add(manager, TOGGLE, "Turn the drop prompt on or off for yourself", PermissionDefault.TRUE);
        add(manager, DropListener.BYPASS, "Drop anything with a single press", PermissionDefault.FALSE);
    }

    private static void add(PluginManager manager, String node, String description, PermissionDefault value) {
        if (manager.getPermission(node) == null) {
            manager.addPermission(new Permission(node, description, value));
        }
    }
}
