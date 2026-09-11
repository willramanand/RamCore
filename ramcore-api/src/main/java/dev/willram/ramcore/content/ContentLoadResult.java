package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * The outcome of a {@link ContentLoader} run: the definitions that loaded cleanly and every error
 * that was found. The loader never throws mid-load; call {@link #throwIfErrors()} to fail fast.
 *
 * @param definitions successfully loaded and merged definitions
 * @param errors      every error found, with source and path
 */
public record ContentLoadResult(@NotNull List<ContentDefinition> definitions, @NotNull List<ValidationError> errors) {

    public ContentLoadResult {
        definitions = List.copyOf(requireNonNull(definitions, "definitions"));
        errors = List.copyOf(requireNonNull(errors, "errors"));
    }

    public boolean successful() {
        return this.errors.isEmpty();
    }

    /**
     * The definitions of one type.
     *
     * @param type the type (directory name)
     * @return matching definitions
     */
    @NotNull
    public List<ContentDefinition> ofType(@NotNull String type) {
        requireNonNull(type, "type");
        return this.definitions.stream().filter(definition -> definition.type().equals(type)).toList();
    }

    /**
     * A definition by id.
     *
     * @param id the id
     * @return the definition, or empty
     */
    @NotNull
    public Optional<ContentDefinition> definition(@NotNull ContentId id) {
        requireNonNull(id, "id");
        return this.definitions.stream().filter(definition -> definition.id().equals(id)).findFirst();
    }

    /**
     * Throws a {@link ContentValidationException} if any error was found.
     *
     * @return this result, for chaining, when clean
     */
    @NotNull
    public ContentLoadResult throwIfErrors() {
        if (!this.errors.isEmpty()) {
            throw new ContentValidationException(this.errors);
        }
        return this;
    }
}
