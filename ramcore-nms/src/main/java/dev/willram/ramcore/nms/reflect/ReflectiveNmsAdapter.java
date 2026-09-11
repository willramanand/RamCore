package dev.willram.ramcore.nms.reflect;

import dev.willram.ramcore.nms.api.NmsAccessTier;
import dev.willram.ramcore.nms.api.NmsAdapter;
import dev.willram.ramcore.nms.api.NmsCapability;
import dev.willram.ramcore.nms.api.NmsCapabilityCheck;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Small adapter backed by reflective class probes.
 */
public final class ReflectiveNmsAdapter implements NmsAdapter {
    private final String id;
    private final GuardedNmsLookup lookup;
    private final Map<NmsCapability, String> requiredClasses = new EnumMap<>(NmsCapability.class);

    private ReflectiveNmsAdapter(@NotNull String id, @NotNull GuardedNmsLookup lookup) {
        this.id = Objects.requireNonNull(id, "id");
        this.lookup = Objects.requireNonNull(lookup, "lookup");
    }

    @NotNull
    public static ReflectiveNmsAdapter create(@NotNull String id, @NotNull GuardedNmsLookup lookup) {
        return new ReflectiveNmsAdapter(id, lookup);
    }

    @NotNull
    public ReflectiveNmsAdapter capability(@NotNull NmsCapability capability, @NotNull String requiredClass) {
        this.requiredClasses.put(Objects.requireNonNull(capability, "capability"), Objects.requireNonNull(requiredClass, "requiredClass"));
        return this;
    }

    @Override
    @NotNull
    public String id() {
        return this.id;
    }

    @Override
    @NotNull
    public NmsAccessTier tier() {
        return NmsAccessTier.GUARDED_REFLECTION;
    }

    @Override
    @NotNull
    public Set<NmsCapability> capabilities() {
        return Set.copyOf(this.requiredClasses.keySet());
    }

    @Override
    @NotNull
    public NmsCapabilityCheck check(@NotNull NmsCapability capability) {
        String requiredClass = this.requiredClasses.get(Objects.requireNonNull(capability, "capability"));
        if (requiredClass == null) {
            return NmsCapabilityCheck.unknown(capability);
        }
        if (this.lookup.hasClass(requiredClass)) {
            return NmsCapabilityCheck.supported(capability, tier(), this.id, "Found required class " + requiredClass + ".");
        }
        return NmsCapabilityCheck.unsupported(capability, tier(), this.id, "Missing required class " + requiredClass + ".");
    }
}
