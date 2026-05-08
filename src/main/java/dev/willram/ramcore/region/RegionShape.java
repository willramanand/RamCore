package dev.willram.ramcore.region;

import dev.willram.ramcore.serialize.Position;
import org.jetbrains.annotations.NotNull;

/**
 * Position containment test for lightweight regions.
 */
@FunctionalInterface
public interface RegionShape {

    boolean contains(@NotNull Position position);
}
