package dev.willram.ramcore.template;

import org.jetbrains.annotations.NotNull;

/**
 * Registered template plus owner metadata.
 */
public record TemplateEntry<T>(
        @NotNull String owner,
        @NotNull Template<T> template
) {
}
