package dev.willram.ramcore.world;

import dev.willram.ramcore.nms.api.NmsAccessRegistry;
import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import dev.willram.ramcore.nms.api.NmsSupportStatus;
import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.serialize.BlockPosition;
import dev.willram.ramcore.serialize.BlockRegion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Static entry point for Paper/Folia-aware world and block helpers.
 */
public final class WorldBlocks {
    private static final BlockEntityNbtCodec UNSUPPORTED_NBT =
            new UnsupportedBlockEntityNbtCodec("Raw block-entity SNBT requires a guarded NMS adapter.");

    @NotNull
    public static BlockEntityNbtCodec unsupportedNbtCodec() {
        return UNSUPPORTED_NBT;
    }

    @NotNull
    public static BlockEntitySnapshot snapshot(@NotNull BlockState state) {
        return BlockEntitySnapshot.capture(state);
    }

    @NotNull
    public static Promise<BlockEntitySnapshot> snapshot(@NotNull Block block) {
        Objects.requireNonNull(block, "block");
        return Schedulers.call(block, () -> BlockEntitySnapshot.capture(block.getState()));
    }

    @NotNull
    public static Promise<StructureSnapshot> capture(@NotNull BlockRegion region) {
        Objects.requireNonNull(region, "region");
        return Schedulers.call(region.getMin().toLocation(), () -> StructureSnapshot.capture(region));
    }

    @NotNull
    public static Promise<Boolean> edit(@NotNull BlockState state, @NotNull Consumer<BlockState> editor,
                                        boolean force, boolean physics) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(editor, "editor");
        return Schedulers.call(state, () -> {
            editor.accept(state);
            return state.update(force, physics);
        });
    }

    @NotNull
    public static Promise<Boolean> configureSpawner(@NotNull CreatureSpawner spawner, @NotNull SpawnerConfig config,
                                                    boolean force, boolean physics) {
        Objects.requireNonNull(spawner, "spawner");
        Objects.requireNonNull(config, "config");
        return Schedulers.call(spawner, () -> {
            config.apply(spawner);
            return spawner.update(force, physics);
        });
    }

    @NotNull
    public static Promise<StructureApplyResult> restore(@NotNull Location origin, @NotNull StructureSnapshot snapshot) {
        return restore(origin, snapshot, StructureApplyOptions.DEFAULT);
    }

    @NotNull
    public static Promise<StructureApplyResult> restore(@NotNull Location origin, @NotNull StructureSnapshot snapshot,
                                                        @NotNull StructureApplyOptions options) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(options, "options");
        Location target = origin.clone();
        return Schedulers.call(target, () -> snapshot.restore(BlockPosition.of(target), options));
    }

    @NotNull
    public static NmsAccessRegistry registerPaperCapability(@NotNull NmsAccessRegistry registry) {
        Objects.requireNonNull(registry, "registry")
                .override(new NmsCapabilityCheck(
                NmsCapability.BLOCK_ENTITY_NBT,
                NmsSupportStatus.PARTIAL,
                NmsAccessTier.PAPER_API,
                "paper-block-state",
                null,
                null,
                "Paper exposes typed block states for signs, containers, spawners, skulls, banners, lecterns, command blocks, PDC, snapshots, and Folia region scheduling; raw block-entity SNBT requires a guarded NMS adapter."
        ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.BLOCK_ENTITY_SNAPSHOTS,
                        NmsAccessTier.PAPER_API,
                        "paper-block-state",
                        "Paper exposes typed block states and PDC for common block entities."
                ))
                .override(NmsCapabilityCheck.unsupported(
                        NmsCapability.RAW_BLOCK_ENTITY_SNBT,
                        NmsAccessTier.GUARDED_REFLECTION,
                        "none",
                        "Raw block-entity SNBT import/export requires a guarded NMS adapter."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.STRUCTURE_SNAPSHOTS,
                        NmsAccessTier.RAMCORE_ADAPTER,
                        "ramcore-structure-snapshots",
                        "RamCore captures and restores typed block snapshots; raw block entity SNBT remains adapter-gated."
                ))
                .override(NmsCapabilityCheck.partial(
                        NmsCapability.SPAWNER_CONFIGURATION,
                        NmsAccessTier.PAPER_API,
                        "paper-block-state",
                        "Paper exposes spawner delays, ranges, counts, and spawn potentials; hidden internals remain adapter-gated."
                ));
        return registry;
    }

    private WorldBlocks() {
    }
}
