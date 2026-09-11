package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Deserializes loaded {@link ContentDefinition}s into specs using a {@link ContentDeserializer} per
 * content type. Types with no registered deserializer are ignored (not an error), so a project can
 * load only the types it cares about.
 *
 * <pre>{@code
 * SpecLoadResult specs = SpecLoader.create()
 *         .deserializer("items", ItemSpec::deserialize)
 *         .deserializer("regions", RegionSpec::deserialize)
 *         .load(dir);
 * specs.throwIfErrors();
 * }</pre>
 *
 * <p>Stability: experimental.</p>
 */
public final class SpecLoader {
    private final Map<String, ContentDeserializer<?>> deserializers = new LinkedHashMap<>();

    @NotNull
    public static SpecLoader create() {
        return new SpecLoader();
    }

    /**
     * Registers a deserializer for a content type.
     *
     * @param type         the content type (directory name)
     * @param deserializer the deserializer
     * @return this loader
     */
    @NotNull
    public SpecLoader deserializer(@NotNull String type, @NotNull ContentDeserializer<?> deserializer) {
        requireNonNull(type, "type");
        requireNonNull(deserializer, "deserializer");
        this.deserializers.put(type, deserializer);
        return this;
    }

    /**
     * Loads content from a directory and deserializes it.
     *
     * @param root the content root
     * @return the spec result
     */
    @NotNull
    public SpecLoadResult load(@NotNull Path root) {
        return deserialize(ContentLoader.load(root));
    }

    /**
     * Deserializes an already-loaded content result.
     *
     * @param content the content load result
     * @return the spec result
     */
    @NotNull
    public SpecLoadResult deserialize(@NotNull ContentLoadResult content) {
        requireNonNull(content, "content");
        Map<ContentId, Object> specs = new LinkedHashMap<>();
        Map<ContentId, String> types = new LinkedHashMap<>();
        List<ValidationError> errors = new ArrayList<>(content.errors());

        for (ContentDefinition definition : content.definitions()) {
            ContentDeserializer<?> deserializer = this.deserializers.get(definition.type());
            if (deserializer == null) {
                continue;
            }
            try {
                Object spec = deserializer.deserialize(definition.node());
                specs.put(definition.id(), spec);
                types.put(definition.id(), definition.type());
            } catch (ContentDeserializeException e) {
                errors.add(ValidationError.at(definition.source().file(), sourcePath(definition), e.getMessage()));
            }
        }

        return new SpecLoadResult(specs, types, errors);
    }

    private static String sourcePath(@NotNull ContentDefinition definition) {
        String path = definition.source().path();
        return path.isEmpty() ? definition.id().toString() : path + " (" + definition.id() + ")";
    }
}
