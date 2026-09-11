package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Compatibility matrix for NMS-adjacent capabilities across supported Minecraft versions.
 */
public record NmsCompatibilityMatrix(@NotNull List<NmsCompatibilityCell> cells) {

    public NmsCompatibilityMatrix {
        cells = List.copyOf(Objects.requireNonNull(cells, "cells"));
    }

    @NotNull
    public static NmsCompatibilityMatrix from(@NotNull NmsAccessRegistry registry,
                                              @NotNull List<MinecraftVersion> versions) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(versions, "versions");
        List<NmsCompatibilityCell> cells = new ArrayList<>();
        for (MinecraftVersion version : versions) {
            Objects.requireNonNull(version, "version");
            for (NmsCapability capability : NmsCapability.values()) {
                NmsCapabilityCheck check = registry.check(capability);
                cells.add(cell(version, check));
            }
        }
        cells.sort(Comparator.comparing(NmsCompatibilityCell::minecraftVersion)
                .thenComparing(NmsCompatibilityCell::capability));
        return new NmsCompatibilityMatrix(cells);
    }

    @NotNull
    public List<NmsCompatibilityCell> cells(@NotNull NmsCapability capability) {
        Objects.requireNonNull(capability, "capability");
        return this.cells.stream()
                .filter(cell -> cell.capability() == capability)
                .toList();
    }

    @NotNull
    public List<String> markdownLines() {
        List<String> lines = new ArrayList<>();
        lines.add("| Minecraft | Capability | Status | Tier | Adapter | Reason |");
        lines.add("| --- | --- | --- | --- | --- | --- |");
        for (NmsCompatibilityCell cell : this.cells) {
            lines.add("| " + cell.minecraftVersion().getVersion()
                    + " | `" + cell.capability() + "`"
                    + " | `" + cell.status() + "`"
                    + " | `" + cell.tier() + "`"
                    + " | `" + cell.adapterId() + "`"
                    + " | " + cell.reason().replace("|", "/") + " |");
        }
        return List.copyOf(lines);
    }

    private static NmsCompatibilityCell cell(MinecraftVersion version, NmsCapabilityCheck check) {
        if (!inRange(version, check)) {
            return new NmsCompatibilityCell(version, check.capability(), NmsSupportStatus.UNSUPPORTED,
                    check.tier(), check.adapterId(), "Outside adapter version range.");
        }
        return new NmsCompatibilityCell(version, check.capability(), check.status(), check.tier(), check.adapterId(), check.reason());
    }

    private static boolean inRange(MinecraftVersion version, NmsCapabilityCheck check) {
        return (check.minimumVersion() == null || version.isAfterOrEq(check.minimumVersion()))
                && (check.maximumVersion() == null || version.isBeforeOrEq(check.maximumVersion()));
    }
}
