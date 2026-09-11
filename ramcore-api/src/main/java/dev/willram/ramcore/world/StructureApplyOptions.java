package dev.willram.ramcore.world;

/**
 * Restore options for structure snapshots.
 */
public record StructureApplyOptions(boolean force, boolean physics, boolean applyBlockEntities, boolean requireSingleChunk) {

    public static final StructureApplyOptions DEFAULT = new StructureApplyOptions(true, false, true, true);

    public StructureApplyOptions withRequireSingleChunk(boolean requireSingleChunk) {
        return new StructureApplyOptions(this.force, this.physics, this.applyBlockEntities, requireSingleChunk);
    }

    public StructureApplyOptions withPhysics(boolean physics) {
        return new StructureApplyOptions(this.force, physics, this.applyBlockEntities, this.requireSingleChunk);
    }
}
