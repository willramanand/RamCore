package dev.willram.ramcore.world;

import dev.willram.ramcore.serialize.BlockPosition;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Banner;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Lectern;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.TileState;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.sign.Side;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stable block-state snapshot for Paper-exposed block entity data.
 */
public record BlockEntitySnapshot(
        @NotNull BlockPosition position,
        @NotNull String material,
        @NotNull String blockData,
        @NotNull BlockEntityKind kind,
        @NotNull Set<String> pdcKeys,
        @NotNull Map<String, String> properties,
        @NotNull Optional<String> rawNbt
) {

    public BlockEntitySnapshot {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(blockData, "blockData");
        Objects.requireNonNull(kind, "kind");
        pdcKeys = Set.copyOf(Objects.requireNonNull(pdcKeys, "pdcKeys"));
        properties = Map.copyOf(Objects.requireNonNull(properties, "properties"));
        Objects.requireNonNull(rawNbt, "rawNbt");
    }

    @NotNull
    public static BlockEntitySnapshot capture(@NotNull BlockState state) {
        return capture(state, WorldBlocks.unsupportedNbtCodec());
    }

    @NotNull
    public static BlockEntitySnapshot capture(@NotNull BlockState state, @NotNull BlockEntityNbtCodec nbtCodec) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(nbtCodec, "nbtCodec");
        Set<String> pdcKeys = new LinkedHashSet<>();
        Map<String, String> properties = new LinkedHashMap<>();
        BlockEntityKind kind = BlockEntityKind.of(state);

        if (state instanceof TileState tileState) {
            for (NamespacedKey key : tileState.getPersistentDataContainer().getKeys()) {
                pdcKeys.add(key.asString());
            }
        }
        collectTypedProperties(state, properties);

        Optional<String> rawNbt = nbtCodec.exportSnbt(state).value();
        return new BlockEntitySnapshot(
                BlockPosition.of(state.getLocation()),
                state.getType().key().asString(),
                state.getBlockData().getAsString(),
                kind,
                pdcKeys,
                properties,
                rawNbt
        );
    }

    @NotNull
    public static Builder builder(@NotNull BlockPosition position, @NotNull String material, @NotNull String blockData) {
        return new Builder(position, material, blockData);
    }

    private static void collectTypedProperties(BlockState state, Map<String, String> properties) {
        if (state instanceof Sign sign) {
            properties.put("sign.editable", Boolean.toString(sign.isEditable()));
            properties.put("sign.waxed", Boolean.toString(sign.isWaxed()));
            properties.put("sign.color", color(sign.getColor()));
            for (Side side : Side.values()) {
                String prefix = "sign." + side.name().toLowerCase() + ".";
                properties.put(prefix + "glowing", Boolean.toString(sign.getSide(side).isGlowingText()));
                String[] lines = sign.getSide(side).getLines();
                for (int i = 0; i < lines.length; i++) {
                    properties.put(prefix + "line." + i, lines[i]);
                }
            }
        }
        if (state instanceof Container container) {
            properties.put("container.inventorySize", Integer.toString(container.getSnapshotInventory().getSize()));
            properties.put("container.locked", Boolean.toString(container.isLocked()));
            if (container.getLock() != null && !container.getLock().isEmpty()) {
                properties.put("container.lock", container.getLock());
            }
            if (container.getCustomName() != null) {
                properties.put("container.customName", container.getCustomName());
            }
        }
        if (state instanceof CreatureSpawner spawner) {
            properties.putAll(SpawnerConfig.capture(spawner).describe());
        }
        if (state instanceof Skull skull) {
            properties.put("skull.rotation", skull.getRotation().name());
            if (skull.getOwningPlayer() != null) {
                properties.put("skull.owner", skull.getOwningPlayer().getUniqueId().toString());
            }
            if (skull.getNoteBlockSound() != null) {
                properties.put("skull.noteBlockSound", skull.getNoteBlockSound().asString());
            }
        }
        if (state instanceof Banner banner) {
            properties.put("banner.baseColor", color(banner.getBaseColor()));
            List<Pattern> patterns = banner.getPatterns();
            properties.put("banner.patterns", Integer.toString(patterns.size()));
            for (int i = 0; i < patterns.size(); i++) {
                Pattern pattern = patterns.get(i);
                properties.put("banner.pattern." + i, pattern.getColor().name() + ":" + pattern.getPattern());
            }
        }
        if (state instanceof Lectern lectern) {
            properties.put("lectern.page", Integer.toString(lectern.getPage()));
            properties.put("lectern.inventorySize", Integer.toString(lectern.getSnapshotInventory().getSize()));
        }
        if (state instanceof CommandBlock commandBlock) {
            properties.put("commandBlock.command", commandBlock.getCommand());
            properties.put("commandBlock.name", commandBlock.getName());
        }
    }

    private static String color(DyeColor color) {
        return color == null ? "none" : color.name();
    }

    public static final class Builder {
        private final BlockPosition position;
        private final String material;
        private final String blockData;
        private BlockEntityKind kind = BlockEntityKind.BLOCK_STATE;
        private final Set<String> pdcKeys = new LinkedHashSet<>();
        private final Map<String, String> properties = new LinkedHashMap<>();
        private Optional<String> rawNbt = Optional.empty();

        private Builder(BlockPosition position, String material, String blockData) {
            this.position = Objects.requireNonNull(position, "position");
            this.material = Objects.requireNonNull(material, "material");
            this.blockData = Objects.requireNonNull(blockData, "blockData");
        }

        @NotNull
        public Builder kind(@NotNull BlockEntityKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        @NotNull
        public Builder pdcKey(@NotNull String pdcKey) {
            this.pdcKeys.add(Objects.requireNonNull(pdcKey, "pdcKey"));
            return this;
        }

        @NotNull
        public Builder property(@NotNull String key, @NotNull String value) {
            this.properties.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
            return this;
        }

        @NotNull
        public Builder rawNbt(@NotNull String rawNbt) {
            this.rawNbt = Optional.of(Objects.requireNonNull(rawNbt, "rawNbt"));
            return this;
        }

        @NotNull
        public BlockEntitySnapshot build() {
            return new BlockEntitySnapshot(this.position, this.material, this.blockData, this.kind, this.pdcKeys, this.properties, this.rawNbt);
        }
    }
}
