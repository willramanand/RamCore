package dev.willram.ramcore.service;

import dev.willram.ramcore.terminable.TerminableConsumer;
import org.jetbrains.annotations.NotNull;

/**
 * Provides service lookup and cleanup binding during lifecycle callbacks.
 */
public interface ServiceContext extends TerminableConsumer {

    @NotNull
    ServiceRegistry services();

    @NotNull
    default <T> T service(@NotNull ServiceKey<T> key) {
        return services().require(key);
    }
}
