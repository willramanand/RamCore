package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentDefinition;
import org.jetbrains.annotations.NotNull;

/**
 * Turns a reloaded {@link ContentDefinition} into the domain object that live objects rebind to
 * (e.g. deserialize a spec and build the template). May throw; the reload service catches and records
 * the failure.
 */
@FunctionalInterface
public interface ContentResolver {

    /**
     * Resolves a definition to its domain object.
     *
     * @param definition the reloaded definition
     * @return the resolved object passed to {@link TemplateBound#rebind(Object)}
     * @throws Exception if resolution fails
     */
    @NotNull
    Object resolve(@NotNull ContentDefinition definition) throws Exception;
}
