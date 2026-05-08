package dev.willram.ramcore.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Typed Bukkit YAML config backed by {@link YamlConfiguration}.
 */
public final class BukkitConfig implements TypedConfig {
    private final Path path;
    private final Set<ConfigKey<?>> keys;
    private FileConfiguration configuration = new YamlConfiguration();

    private BukkitConfig(@NotNull Path path, @NotNull Collection<ConfigKey<?>> keys) {
        this.path = requireNonNull(path, "path");
        this.keys = new LinkedHashSet<>(requireNonNull(keys, "keys"));
    }

    @NotNull
    public static BukkitConfig load(@NotNull Path path, @NotNull Collection<ConfigKey<?>> keys) {
        BukkitConfig config = new BukkitConfig(path, keys);
        config.reload();
        return config;
    }

    @NotNull
    public static BukkitConfig load(@NotNull Path path, @NotNull ConfigKey<?>... keys) {
        return load(path, List.of(keys));
    }

    @NotNull
    @Override
    public Path path() {
        return this.path;
    }

    @NotNull
    @Override
    public Set<ConfigKey<?>> keys() {
        return Set.copyOf(this.keys);
    }

    @NotNull
    @Override
    public <T> T get(@NotNull ConfigKey<T> key) {
        if (!this.keys.contains(requireNonNull(key, "key"))) {
            throw new IllegalArgumentException("config key not registered: " + key.path());
        }

        T value = readValue(key);
        if (value == null) {
            throw new ConfigException("config value missing: " + key.path());
        }
        return value;
    }

    @Override
    public boolean contains(@NotNull ConfigKey<?> key) {
        requireNonNull(key, "key");
        return this.configuration.contains(key.path(), true);
    }

    @Override
    public void reload() {
        try {
            Path parent = this.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.notExists(this.path)) {
                Files.createFile(this.path);
            }

            YamlConfiguration loaded = new YamlConfiguration();
            loaded.load(this.path.toFile());
            this.configuration = loaded;
            applyDefaults();
            validate();
            this.configuration.save(this.path.toFile());
        } catch (IOException | InvalidConfigurationException e) {
            throw new ConfigException("failed to load config " + this.path, e);
        }
    }

    @NotNull
    @Override
    public FileConfiguration raw() {
        return this.configuration;
    }

    private void applyDefaults() {
        for (ConfigKey<?> key : this.keys) {
            if (key.defaultValue() != null) {
                this.configuration.addDefault(key.path(), key.defaultValue());
            }
        }
        this.configuration.options().copyDefaults(true);
    }

    private void validate() {
        List<String> errors = new ArrayList<>();
        for (ConfigKey<?> key : this.keys) {
            validateKey(key, errors);
        }

        if (!errors.isEmpty()) {
            throw new ConfigValidationException(errors);
        }
    }

    private <T> void validateKey(ConfigKey<T> key, List<String> errors) {
        if (!this.configuration.contains(key.path(), true)) {
            if (key.required()) {
                errors.add(key.path() + ": required value missing");
            }
            return;
        }

        T value;
        try {
            value = readValue(key);
        } catch (RuntimeException e) {
            errors.add(key.path() + ": expected " + key.type().getSimpleName());
            return;
        }

        if (value == null) {
            errors.add(key.path() + ": expected " + key.type().getSimpleName());
            return;
        }

        errors.addAll(key.validateValue(value));
    }

    private <T> T readValue(ConfigKey<T> key) {
        return this.configuration.getObject(key.path(), key.type(), key.defaultValue());
    }
}
