package dev.willram.ramcore.world;

import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Spawner entity template with an optional Paper entity snapshot or raw SNBT.
 */
public record SpawnerEntityTemplate(
        @NotNull EntityType type,
        @NotNull Optional<EntitySnapshot> snapshot,
        @NotNull Optional<String> rawSnbt
) {

    @NotNull
    public static SpawnerEntityTemplate of(@NotNull EntityType type) {
        return new SpawnerEntityTemplate(type, Optional.empty(), Optional.empty());
    }

    @NotNull
    public static SpawnerEntityTemplate snapshot(@NotNull EntitySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new SpawnerEntityTemplate(snapshot.getEntityType(), Optional.of(snapshot), Optional.empty());
    }

    @NotNull
    public static SpawnerEntityTemplate rawSnbt(@NotNull EntityType type, @NotNull String rawSnbt) {
        return new SpawnerEntityTemplate(type, Optional.empty(), Optional.of(rawSnbt));
    }

    public SpawnerEntityTemplate {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(rawSnbt, "rawSnbt");
    }

    public boolean paperEntryReady() {
        return this.snapshot.isPresent();
    }
}
