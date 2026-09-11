package dev.willram.ramcore.nms.api;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Public boundary for an internal implementation provider.
 */
public interface NmsAdapter {

    @NotNull
    String id();

    @NotNull
    NmsAccessTier tier();

    @NotNull
    Set<NmsCapability> capabilities();

    @NotNull
    NmsCapabilityCheck check(@NotNull NmsCapability capability);
}
