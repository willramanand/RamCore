package dev.willram.ramcore.packet;

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

/**
 * Packet utility diagnostics.
 */
public record PacketDiagnostics(
        boolean protocolLibAvailable,
        @NotNull Set<PacketFeature> supportedFeatures,
        @NotNull PacketClientAssumptions clientAssumptions,
        @NotNull Set<String> warnings
) {

    @NotNull
    public static PacketDiagnostics of(boolean protocolLibAvailable) {
        Set<PacketFeature> supported = EnumSet.of(PacketFeature.PER_PLAYER_VISUAL_STATE);
        Set<String> warnings = EnumSet.allOf(PacketFeature.class).stream()
                .filter(PacketFeature::protocolLibBacked)
                .map(feature -> feature.name() + " requires ProtocolLib packet adapters.")
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        if (protocolLibAvailable) {
            supported = EnumSet.allOf(PacketFeature.class);
            warnings = Set.of("Packet field layouts are client-version sensitive; keep packet factories version guarded.");
        }
        return new PacketDiagnostics(protocolLibAvailable, supported, PacketClientAssumptions.unknown(), warnings);
    }

    public PacketDiagnostics {
        supportedFeatures = Set.copyOf(supportedFeatures);
        warnings = Set.copyOf(warnings);
    }

    public boolean supports(@NotNull PacketFeature feature) {
        return this.supportedFeatures.contains(feature);
    }
}
