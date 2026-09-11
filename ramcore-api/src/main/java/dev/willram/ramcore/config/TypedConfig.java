package dev.willram.ramcore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;

/**
 * Loaded typed config view.
 */
public interface TypedConfig {

    @NotNull
    Path path();

    @NotNull
    Set<ConfigKey<?>> keys();

    @NotNull
    <T> T get(@NotNull ConfigKey<T> key);

    boolean contains(@NotNull ConfigKey<?> key);

    void reload();

    @NotNull
    FileConfiguration raw();
}
