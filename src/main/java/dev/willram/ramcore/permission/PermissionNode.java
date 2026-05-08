package dev.willram.ramcore.permission;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Named permission with sender-facing denial message.
 */
public final class PermissionNode {
    private final String value;
    private final String denialMessage;

    private PermissionNode(@NotNull String value, @NotNull String denialMessage) {
        this.value = validate(value);
        this.denialMessage = requireNonNull(denialMessage, "denialMessage");
    }

    @NotNull
    public static PermissionNode of(@NotNull String value) {
        return new PermissionNode(value, "<red>You do not have permission to use this command.");
    }

    @NotNull
    public static PermissionNode of(@NotNull String value, @NotNull String denialMessage) {
        return new PermissionNode(value, denialMessage);
    }

    @NotNull
    public PermissionNode child(@NotNull String suffix) {
        return new PermissionNode(this.value + "." + validate(suffix), this.denialMessage);
    }

    @NotNull
    public PermissionNode denialMessage(@NotNull String denialMessage) {
        return new PermissionNode(this.value, denialMessage);
    }

    @NotNull
    public String value() {
        return this.value;
    }

    @NotNull
    public String denialMessage() {
        return this.denialMessage;
    }

    private static String validate(String value) {
        return RamPreconditions.notBlank(requireNonNull(value, "value"), "permission");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PermissionNode that)) {
            return false;
        }

        return this.value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return this.value.hashCode();
    }

    @Override
    public String toString() {
        return this.value;
    }
}
