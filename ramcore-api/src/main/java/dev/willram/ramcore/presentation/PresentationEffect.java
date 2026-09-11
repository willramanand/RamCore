package dev.willram.ramcore.presentation;

import dev.willram.ramcore.terminable.Terminable;
import org.jetbrains.annotations.NotNull;

/**
 * Playable presentation effect.
 */
@FunctionalInterface
public interface PresentationEffect {

    @NotNull
    Terminable play(@NotNull PresentationContext context);
}
