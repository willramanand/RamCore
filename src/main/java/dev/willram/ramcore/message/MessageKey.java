package dev.willram.ramcore.message;

import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

/**
 * Reusable message identifier with a fallback MiniMessage template.
 */
public final class MessageKey {
    private final String id;
    private final String defaultTemplate;

    private MessageKey(@NotNull String id, @NotNull String defaultTemplate) {
        this.id = requireNonNull(id, "id");
        this.defaultTemplate = requireNonNull(defaultTemplate, "defaultTemplate");

        RamPreconditions.checkArgument(!id.isBlank(), "message id must not be blank", "Use a stable id such as 'command.no-permission'.");
    }

    @NotNull
    public static MessageKey of(@NotNull String id, @NotNull String defaultTemplate) {
        return new MessageKey(id, defaultTemplate);
    }

    @NotNull
    public String id() {
        return this.id;
    }

    @NotNull
    public String defaultTemplate() {
        return this.defaultTemplate;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof MessageKey that)) {
            return false;
        }

        return this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id;
    }
}
