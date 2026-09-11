package dev.willram.ramcore.dialogue;

import dev.willram.ramcore.permission.PermissionNode;
import dev.willram.ramcore.permission.PermissionRequirement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Built-in dialogue conditions ({@link Predicate}{@code <}{@link DialogueContext}{@code >}). Combine
 * with the standard {@link Predicate#and}, {@link Predicate#or}, {@link Predicate#negate}.
 */
public final class DialogueConditions {

    private DialogueConditions() {
    }

    /** Always available. */
    @NotNull
    public static Predicate<DialogueContext> always() {
        return context -> true;
    }

    /** Requires the given permission requirement. */
    @NotNull
    public static Predicate<DialogueContext> permission(@NotNull PermissionRequirement requirement) {
        requireNonNull(requirement, "requirement");
        return context -> requirement.test(context.player());
    }

    /** Requires the given permission node. */
    @NotNull
    public static Predicate<DialogueContext> permission(@NotNull String node) {
        return permission(PermissionRequirement.all(PermissionNode.of(node)));
    }

    /** Requires a metadata entry to equal a value. */
    @NotNull
    public static Predicate<DialogueContext> metadataEquals(@NotNull String key, @NotNull Object value) {
        requireNonNull(key, "key");
        requireNonNull(value, "value");
        return context -> Objects.equals(context.metadata().get(key), value);
    }
}
