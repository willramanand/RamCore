package dev.willram.ramcore.item.component;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import io.papermc.paper.datacomponent.DataComponentType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * Static entry point for item data component helpers.
 */
public final class ItemComponents {

    @NotNull
    public static ItemComponentBackend edit(@NotNull ItemStack item) {
        return new PaperItemComponentBackend(item);
    }

    @NotNull
    public static InMemoryItemComponentBackend memoryBackend() {
        return new InMemoryItemComponentBackend();
    }

    @NotNull
    public static ItemComponentPatch.Builder patch() {
        return ItemComponentPatch.builder();
    }

    @NotNull
    public static ItemComponentProfile.Builder profile() {
        return ItemComponentProfile.builder();
    }

    @NotNull
    public static ItemComponentPatch diff(@NotNull ItemComponentBackend before, @NotNull ItemComponentBackend after) {
        return before.snapshot().diff(after.snapshot());
    }

    public static void copy(@NotNull ItemComponentBackend source, @NotNull ItemComponentBackend target,
                            @NotNull java.util.function.Predicate<DataComponentType> predicate) {
        target.copyFrom(source, predicate);
    }

    public static boolean matchesIgnoring(@NotNull ItemComponentBackend first, @NotNull ItemComponentBackend second,
                                          @NotNull Set<DataComponentType> ignored) {
        return first.matchesWithout(second, ignored, false);
    }

    @NotNull
    public static ItemComponentSerializedPatch serialize(@NotNull ItemComponentPatch patch) {
        return ItemComponentSerializedPatch.of(patch);
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.ITEM_DATA_COMPONENTS,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-data-components",
                null,
                null,
                "Paper exposes experimental item data components; RamCore wraps reads, patches, copy/diff, and common builders while preserving a versioned boundary."
        ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.ITEM_COMPONENT_PATCHES,
                        NmsAccessTier.PAPER_API,
                        "paper-data-components",
                        "RamCore wraps Paper component reads, patches, diffs, copies, and resets behind stable value objects."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.ITEM_COMPONENT_PROFILES,
                        NmsAccessTier.PAPER_API,
                        "paper-data-components",
                        "RamCore high-level profiles target Paper experimental component types and remain version-sensitive."
                ));
        return registry;
    }

    private ItemComponents() {
    }
}
