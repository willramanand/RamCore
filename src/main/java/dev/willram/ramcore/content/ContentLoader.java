package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ValidationError;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Loads {@code content/<type>/*.yml|*.yaml|*.conf} into {@link ContentDefinition}s.
 *
 * <p>Each file holds one entry (a map with an {@code id}) or many (a top-level list of such maps).
 * An entry has {@code id: ns:value}, an optional {@code extends: ns:parent}, then its type fields.
 * The type is the directory name. Inheritance is resolved by deep-merging the parent's node into the
 * child (child scalar wins, child list replaces, maps merge). Missing parents and inheritance cycles
 * are reported as errors with their source, and the loader collects every error rather than failing
 * on the first.</p>
 *
 * <p>Stability: experimental. Not Folia-thread-sensitive but does blocking file I/O; run off the
 * main thread.</p>
 */
public final class ContentLoader {
    private static final Set<String> YAML = Set.of(".yml", ".yaml");
    private static final String HOCON = ".conf";

    private ContentLoader() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Loads all content under the root directory.
     *
     * @param root the content root (its subdirectories are the types)
     * @return the load result (never throws; inspect {@link ContentLoadResult#errors()})
     */
    @NotNull
    public static ContentLoadResult load(@NotNull Path root) {
        requireNonNull(root, "root");
        List<ValidationError> errors = new ArrayList<>();
        Map<ContentId, Parsed> parsed = new LinkedHashMap<>();

        if (!Files.isDirectory(root)) {
            errors.add(ValidationError.at(root.toString(), "", "content root is not a directory"));
            return new ContentLoadResult(List.of(), errors);
        }

        try (Stream<Path> types = Files.list(root)) {
            types.filter(Files::isDirectory).sorted().forEach(typeDir -> {
                String type = typeDir.getFileName().toString();
                try (Stream<Path> files = Files.list(typeDir)) {
                    files.filter(ContentLoader::isContentFile).sorted().forEach(file -> parseFile(file, type, parsed, errors));
                } catch (IOException e) {
                    errors.add(ValidationError.at(typeDir.toString(), "", "could not list files: " + e.getMessage()));
                }
            });
        } catch (IOException e) {
            errors.add(ValidationError.at(root.toString(), "", "could not list types: " + e.getMessage()));
            return new ContentLoadResult(List.of(), errors);
        }

        Set<ContentId> invalid = validateInheritance(parsed, errors);
        List<ContentDefinition> definitions = resolve(parsed, invalid);
        return new ContentLoadResult(definitions, errors);
    }

    private static boolean isContentFile(@NotNull Path file) {
        if (!Files.isRegularFile(file)) {
            return false;
        }
        String name = file.getFileName().toString().toLowerCase();
        return name.endsWith(HOCON) || YAML.stream().anyMatch(name::endsWith);
    }

    private static void parseFile(@NotNull Path file, @NotNull String type, @NotNull Map<ContentId, Parsed> parsed, @NotNull List<ValidationError> errors) {
        String fileName = file.getFileName().toString();
        ConfigurationNode root;
        try {
            root = loaderFor(file).load();
        } catch (ConfigurateException e) {
            errors.add(ValidationError.at(fileName, "", "could not parse: " + e.getMessage()));
            return;
        }

        if (root.isList()) {
            List<? extends ConfigurationNode> children = root.childrenList();
            for (int i = 0; i < children.size(); i++) {
                parseEntry(children.get(i), type, fileName, "[" + i + "]", parsed, errors);
            }
        } else if (!root.node("id").virtual()) {
            parseEntry(root, type, fileName, "", parsed, errors);
        } else if (root.isMap() && !root.childrenMap().isEmpty()) {
            errors.add(ValidationError.at(fileName, "", "top-level map has no 'id'; use a single entry with an id or a list of entries"));
        } else {
            errors.add(ValidationError.at(fileName, "", "empty or unreadable content file"));
        }
    }

    private static void parseEntry(@NotNull ConfigurationNode node, @NotNull String type, @NotNull String file, @NotNull String path,
                                   @NotNull Map<ContentId, Parsed> parsed, @NotNull List<ValidationError> errors) {
        String idString = node.node("id").getString();
        if (idString == null || idString.isBlank()) {
            errors.add(ValidationError.at(file, path, "entry is missing 'id'"));
            return;
        }
        ContentId id;
        try {
            id = ContentId.parse(idString.trim());
        } catch (RuntimeException invalid) {
            errors.add(ValidationError.at(file, path, "invalid id '" + idString + "': " + invalid.getMessage()));
            return;
        }

        ContentId parent = null;
        String parentString = node.node("extends").getString();
        if (parentString != null && !parentString.isBlank()) {
            try {
                parent = ContentId.parse(parentString.trim());
            } catch (RuntimeException invalid) {
                errors.add(ValidationError.at(file, path, "invalid extends '" + parentString + "': " + invalid.getMessage()));
                return;
            }
        }

        SourceRef source = SourceRef.of(file, path);
        Parsed existing = parsed.get(id);
        if (existing != null) {
            errors.add(ValidationError.at(file, path, "duplicate id " + id + " (also at " + existing.source() + ")"));
            return;
        }
        parsed.put(id, new Parsed(id, type, parent, node, source));
    }

    private static Set<ContentId> validateInheritance(@NotNull Map<ContentId, Parsed> parsed, @NotNull List<ValidationError> errors) {
        Set<ContentId> invalid = new LinkedHashSet<>();
        for (Parsed entry : parsed.values()) {
            if (entry.parent() == null) {
                continue;
            }
            if (!parsed.containsKey(entry.parent())) {
                errors.add(ValidationError.at(entry.source().file(), entry.source().path(), "missing parent " + entry.parent()));
                invalid.add(entry.id());
                continue;
            }
            List<ContentId> chain = new ArrayList<>();
            Set<ContentId> seen = new LinkedHashSet<>();
            ContentId current = entry.id();
            while (current != null) {
                chain.add(current);
                if (!seen.add(current)) {
                    errors.add(ValidationError.at(entry.source().file(), entry.source().path(),
                            "cyclic inheritance: " + chain.stream().map(ContentId::toString).reduce((a, b) -> a + " -> " + b).orElse("")));
                    invalid.add(entry.id());
                    break;
                }
                Parsed node = parsed.get(current);
                current = node == null ? null : node.parent();
            }
        }
        return invalid;
    }

    private static List<ContentDefinition> resolve(@NotNull Map<ContentId, Parsed> parsed, @NotNull Set<ContentId> invalid) {
        List<ContentDefinition> definitions = new ArrayList<>();
        for (Parsed entry : parsed.values()) {
            if (hasInvalidAncestor(entry, parsed, invalid)) {
                continue;
            }
            ConfigurationNode merged = resolveNode(entry, parsed);
            definitions.add(new ContentDefinition(entry.id(), entry.type(), entry.parent(), merged, entry.source()));
        }
        return definitions;
    }

    private static boolean hasInvalidAncestor(@NotNull Parsed entry, @NotNull Map<ContentId, Parsed> parsed, @NotNull Set<ContentId> invalid) {
        Parsed current = entry;
        Set<ContentId> seen = new LinkedHashSet<>();
        while (current != null) {
            if (invalid.contains(current.id()) || !seen.add(current.id())) {
                return true;
            }
            current = current.parent() == null ? null : parsed.get(current.parent());
        }
        return false;
    }

    private static ConfigurationNode resolveNode(@NotNull Parsed entry, @NotNull Map<ContentId, Parsed> parsed) {
        ConfigurationNode result = entry.node().copy();
        if (entry.parent() != null) {
            Parsed parent = parsed.get(entry.parent());
            if (parent != null) {
                result.mergeFrom(resolveNode(parent, parsed));
            }
        }
        return result;
    }

    @NotNull
    private static org.spongepowered.configurate.loader.AbstractConfigurationLoader<? extends ConfigurationNode> loaderFor(@NotNull Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(HOCON)) {
            return HoconConfigurationLoader.builder().path(file).build();
        }
        return YamlConfigurationLoader.builder().path(file).build();
    }

    private record Parsed(@NotNull ContentId id, @NotNull String type, @Nullable ContentId parent,
                          @NotNull ConfigurationNode node, @NotNull SourceRef source) {
    }
}
