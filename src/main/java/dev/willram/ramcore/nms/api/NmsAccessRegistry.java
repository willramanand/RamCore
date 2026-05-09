package dev.willram.ramcore.nms.api;

import dev.willram.ramcore.reflect.MinecraftVersion;
import dev.willram.ramcore.reflect.NmsVersion;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Capability registry for guarded internal access.
 */
public final class NmsAccessRegistry {
    private final MinecraftVersion minecraftVersion;
    private final NmsVersion nmsVersion;
    private final List<NmsAdapter> adapters = new ArrayList<>();
    private final Map<NmsCapability, NmsCapabilityCheck> overrides = new EnumMap<>(NmsCapability.class);

    private NmsAccessRegistry(@NotNull MinecraftVersion minecraftVersion, @NotNull NmsVersion nmsVersion) {
        this.minecraftVersion = Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        this.nmsVersion = Objects.requireNonNull(nmsVersion, "nmsVersion");
    }

    @NotNull
    public static NmsAccessRegistry create(@NotNull MinecraftVersion minecraftVersion, @NotNull NmsVersion nmsVersion) {
        return new NmsAccessRegistry(minecraftVersion, nmsVersion);
    }

    @NotNull
    public MinecraftVersion minecraftVersion() {
        return this.minecraftVersion;
    }

    @NotNull
    public NmsVersion nmsVersion() {
        return this.nmsVersion;
    }

    @NotNull
    public NmsAccessRegistry register(@NotNull NmsAdapter adapter) {
        this.adapters.add(Objects.requireNonNull(adapter, "adapter"));
        this.adapters.sort(Comparator.comparing(NmsAdapter::tier));
        return this;
    }

    @NotNull
    public NmsAccessRegistry override(@NotNull NmsCapabilityCheck check) {
        this.overrides.put(check.capability(), check);
        return this;
    }

    @NotNull
    public Optional<NmsAdapter> adapter(@NotNull String id) {
        Objects.requireNonNull(id, "id");
        return this.adapters.stream().filter(adapter -> adapter.id().equals(id)).findFirst();
    }

    @NotNull
    public NmsCapabilityCheck check(@NotNull NmsCapability capability) {
        Objects.requireNonNull(capability, "capability");
        NmsCapabilityCheck override = this.overrides.get(capability);
        if (override != null) {
            return override;
        }

        for (NmsAdapter adapter : this.adapters) {
            if (!adapter.capabilities().contains(capability)) {
                continue;
            }
            NmsCapabilityCheck check = adapter.check(capability);
            if (check.usable()) {
                return check;
            }
        }
        return NmsCapabilityCheck.unknown(capability);
    }

    @NotNull
    public NmsCapabilityCheck require(@NotNull NmsCapability capability) {
        NmsCapabilityCheck check = check(capability);
        if (!check.usable()) {
            throw new NmsUnsupportedException(capability, check);
        }
        return check;
    }

    @NotNull
    public NmsDiagnostics diagnostics() {
        List<NmsCapabilityCheck> checks = new ArrayList<>();
        for (NmsCapability capability : NmsCapability.values()) {
            checks.add(check(capability));
        }
        return new NmsDiagnostics(
                this.minecraftVersion,
                this.nmsVersion,
                this.adapters.stream().map(NmsAdapter::id).toList(),
                checks
        );
    }

    @NotNull
    public NmsSelfTestReport selfTest(@NotNull NmsSelfTestPlan plan) {
        return Objects.requireNonNull(plan, "plan").run(this);
    }

    @NotNull
    public NmsSelfTestReport selfTest() {
        return selfTest(NmsSelfTestPlan.optionalAllCapabilities());
    }

    @NotNull
    public NmsCompatibilityMatrix compatibilityMatrix(@NotNull List<MinecraftVersion> supportedVersions) {
        return NmsCompatibilityMatrix.from(this, supportedVersions);
    }
}
