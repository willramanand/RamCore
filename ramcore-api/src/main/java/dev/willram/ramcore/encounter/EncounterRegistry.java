package dev.willram.ramcore.encounter;

import dev.willram.ramcore.content.ContentId;
import dev.willram.ramcore.exception.RamPreconditions;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Registry for encounter definitions and runtime instances.
 */
public final class EncounterRegistry {
    private final Map<ContentId, EncounterDefinition> definitions = new LinkedHashMap<>();
    private final List<EncounterListener> listeners;

    EncounterRegistry(@NotNull List<EncounterListener> listeners) {
        this.listeners = List.copyOf(listeners);
    }

    @NotNull
    public EncounterRegistry register(@NotNull EncounterDefinition definition) {
        requireNonNull(definition, "definition");
        RamPreconditions.checkArgument(!this.definitions.containsKey(definition.id()), "encounter already registered", "Use a unique encounter id.");
        this.definitions.put(definition.id(), definition);
        return this;
    }

    @NotNull
    public Optional<EncounterDefinition> definition(@NotNull ContentId id) {
        return Optional.ofNullable(this.definitions.get(requireNonNull(id, "id")));
    }

    @NotNull
    public List<EncounterDefinition> definitions() {
        return List.copyOf(this.definitions.values());
    }

    @NotNull
    public EncounterInstance create(@NotNull ContentId id) {
        EncounterDefinition definition = definition(id).orElseThrow(() -> new IllegalArgumentException("Unknown encounter: " + id));
        EncounterInstance instance = new EncounterInstance(definition);
        this.listeners.forEach(instance::listener);
        return instance;
    }
}
