package dev.willram.ramcore.template;

import org.jetbrains.annotations.NotNull;

/**
 * Merges inherited parent values with child overrides.
 */
@FunctionalInterface
public interface TemplateComposer<T> {

    @NotNull
    T compose(@NotNull T parent, @NotNull T child);
}
