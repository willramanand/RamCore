package dev.willram.ramcore.permission;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Grouped permission requirement.
 */
@SuppressWarnings("UnstableApiUsage")
public final class PermissionRequirement {
    private enum Mode {
        ALL,
        ANY
    }

    private final Mode mode;
    private final List<PermissionNode> nodes;
    private final String denialMessage;

    private PermissionRequirement(@NotNull Mode mode, @NotNull List<PermissionNode> nodes, @NotNull String denialMessage) {
        this.mode = requireNonNull(mode, "mode");
        this.nodes = List.copyOf(requireNonNull(nodes, "nodes"));
        this.denialMessage = requireNonNull(denialMessage, "denialMessage");

        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("permission requirement must include at least one node");
        }
    }

    @NotNull
    public static PermissionRequirement all(@NotNull PermissionNode... nodes) {
        return all(Arrays.asList(nodes));
    }

    @NotNull
    public static PermissionRequirement all(@NotNull List<PermissionNode> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("permission requirement must include at least one node");
        }
        return new PermissionRequirement(Mode.ALL, nodes, nodes.getFirst().denialMessage());
    }

    @NotNull
    public static PermissionRequirement any(@NotNull PermissionNode... nodes) {
        return any(Arrays.asList(nodes));
    }

    @NotNull
    public static PermissionRequirement any(@NotNull List<PermissionNode> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("permission requirement must include at least one node");
        }
        return new PermissionRequirement(Mode.ANY, nodes, nodes.getFirst().denialMessage());
    }

    @NotNull
    public PermissionRequirement denialMessage(@NotNull String denialMessage) {
        return new PermissionRequirement(this.mode, this.nodes, denialMessage);
    }

    public boolean test(@NotNull CommandSender sender) {
        requireNonNull(sender, "sender");
        return switch (this.mode) {
            case ALL -> this.nodes.stream().allMatch(node -> sender.hasPermission(node.value()));
            case ANY -> this.nodes.stream().anyMatch(node -> sender.hasPermission(node.value()));
        };
    }

    @NotNull
    public Predicate<CommandSourceStack> asCommandRequirement() {
        return source -> test(source.getSender());
    }

    @NotNull
    public List<PermissionNode> nodes() {
        return this.nodes;
    }

    @NotNull
    public String denialMessage() {
        return this.denialMessage;
    }
}
