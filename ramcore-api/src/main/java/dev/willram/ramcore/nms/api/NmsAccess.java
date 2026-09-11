package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import dev.willram.ramcore.reflect.ServerReflection;
import org.jetbrains.annotations.NotNull;

/**
 * Facade for the guarded NMS access strategy.
 */
public final class NmsAccess {

    @NotNull
    public static NmsAccessRegistry registry(@NotNull MinecraftVersion minecraftVersion, @NotNull NmsVersion nmsVersion) {
        return NmsAccessRegistry.create(minecraftVersion, nmsVersion);
    }

    @NotNull
    public static NmsAccessRegistry runtimeRegistry() {
        return NmsAccessRegistry.create(MinecraftVersion.getRuntimeVersion(), ServerReflection.getNmsVersion());
    }

    private NmsAccess() {
    }
}
