package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The result of deserializing a {@link ContentLoadResult} into specs: the specs keyed by id, plus
 * every error (content errors carried over, plus deserialization errors).
 */
public final class SpecLoadResult {
    private final Map<ContentId, Object> specs;
    private final Map<ContentId, String> types;
    private final List<ValidationError> errors;

    SpecLoadResult(@NotNull Map<ContentId, Object> specs, @NotNull Map<ContentId, String> types, @NotNull List<ValidationError> errors) {
        this.specs = Map.copyOf(specs);
        this.types = Map.copyOf(types);
        this.errors = List.copyOf(errors);
    }

    public boolean successful() {
        return this.errors.isEmpty();
    }

    @NotNull
    public List<ValidationError> errors() {
        return this.errors;
    }

    /**
     * A spec by id and expected type.
     *
     * @param id   the id
     * @param type the spec class
     * @param <T>  the spec type
     * @return the spec, or empty when absent or of another type
     */
    @NotNull
    public <T> Optional<T> get(@NotNull ContentId id, @NotNull Class<T> type) {
        Object spec = this.specs.get(requireNonNull(id, "id"));
        return type.isInstance(spec) ? Optional.of(type.cast(spec)) : Optional.empty();
    }

    /**
     * Every spec of a content type.
     *
     * @param contentType the content type (directory name)
     * @param type        the spec class
     * @param <T>         the spec type
     * @return matching specs
     */
    @NotNull
    public <T> List<T> ofType(@NotNull String contentType, @NotNull Class<T> type) {
        requireNonNull(contentType, "contentType");
        requireNonNull(type, "type");
        List<T> result = new ArrayList<>();
        this.specs.forEach((id, spec) -> {
            if (contentType.equals(this.types.get(id)) && type.isInstance(spec)) {
                result.add(type.cast(spec));
            }
        });
        return result;
    }

    @NotNull
    public SpecLoadResult throwIfErrors() {
        if (!this.errors.isEmpty()) {
            throw new ContentValidationException(this.errors);
        }
        return this;
    }
}
