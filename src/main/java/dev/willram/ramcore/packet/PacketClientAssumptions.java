package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Client-version assumptions attached to packet diagnostics.
 */
public record PacketClientAssumptions(
        @Nullable Integer protocolVersion,
        @Nullable String minecraftVersion,
        @NotNull Set<String> notes
) {

    @NotNull
    public static PacketClientAssumptions unknown() {
        return new PacketClientAssumptions(null, null, Set.of("Client protocol is unknown; prefer conservative packet features."));
    }

    public PacketClientAssumptions {
        notes = Set.copyOf(notes);
    }
}
