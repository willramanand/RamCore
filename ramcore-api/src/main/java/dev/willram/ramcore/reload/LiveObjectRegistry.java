package dev.willram.ramcore.reload;

import dev.willram.ramcore.content.ContentId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Objects.requireNonNull;

/**
 * Tracks {@link TemplateBound} live objects by their template id so {@link ContentReloadService} can
 * find and rebuild them on reload. Thread-safe.
 */
public final class LiveObjectRegistry {
    private final Map<ContentId, List<TemplateBound>> byTemplate = new ConcurrentHashMap<>();

    /** Registers a live object. */
    public void register(@NotNull TemplateBound object) {
        requireNonNull(object, "object");
        this.byTemplate.computeIfAbsent(object.templateId(), k -> new CopyOnWriteArrayList<>()).add(object);
    }

    /** Unregisters a live object. */
    public void unregister(@NotNull TemplateBound object) {
        requireNonNull(object, "object");
        List<TemplateBound> list = this.byTemplate.get(object.templateId());
        if (list != null) {
            list.remove(object);
            if (list.isEmpty()) {
                this.byTemplate.remove(object.templateId());
            }
        }
    }

    /** The live objects bound to a template. */
    @NotNull
    public List<TemplateBound> boundTo(@NotNull ContentId templateId) {
        List<TemplateBound> list = this.byTemplate.get(requireNonNull(templateId, "templateId"));
        return list == null ? List.of() : List.copyOf(list);
    }

    /** Total live objects tracked. */
    public int size() {
        return this.byTemplate.values().stream().mapToInt(List::size).sum();
    }

    /** Drops all tracked objects. */
    public void clear() {
        this.byTemplate.clear();
    }
}
