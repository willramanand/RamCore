package dev.willram.ramcore.commands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CommandSuggestion {
    private final String value;
    private final String tooltip;

    private CommandSuggestion(@NotNull String value, @Nullable String tooltip) {
        this.value = Objects.requireNonNull(value, "value");
        this.tooltip = tooltip;
    }

    @NotNull
    public static CommandSuggestion of(@NotNull String value) {
        return new CommandSuggestion(value, null);
    }

    @NotNull
    public static CommandSuggestion withTooltip(@NotNull String value, @NotNull String tooltip) {
        return new CommandSuggestion(value, Objects.requireNonNull(tooltip, "tooltip"));
    }

    @NotNull
    public String value() {
        return this.value;
    }

    @Nullable
    public String tooltip() {
        return this.tooltip;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CommandSuggestion other)) {
            return false;
        }
        return this.value.equals(other.value) && Objects.equals(this.tooltip, other.tooltip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.value, this.tooltip);
    }

    @Override
    public String toString() {
        return this.tooltip == null ? this.value : this.value + " (" + this.tooltip + ")";
    }
}
