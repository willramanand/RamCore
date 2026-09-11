package dev.willram.ramcore.permission;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Permission helper entry points.
 */
public final class Permissions {

    @NotNull
    public static PermissionNode node(@NotNull String value) {
        return PermissionNode.of(value);
    }

    @NotNull
    public static PermissionNode node(@NotNull String value, @NotNull String denialMessage) {
        return PermissionNode.of(value, denialMessage);
    }

    @NotNull
    public static PermissionRequirement all(@NotNull PermissionNode... nodes) {
        return PermissionRequirement.all(nodes);
    }

    @NotNull
    public static PermissionRequirement any(@NotNull PermissionNode... nodes) {
        return PermissionRequirement.any(nodes);
    }

    public static boolean has(@NotNull CommandSender sender, @NotNull PermissionNode node) {
        return requireNonNull(sender, "sender").hasPermission(requireNonNull(node, "node").value());
    }

    public static boolean has(@NotNull CommandSender sender, @NotNull PermissionRequirement requirement) {
        return requireNonNull(requirement, "requirement").test(sender);
    }

    private Permissions() {
    }
}
