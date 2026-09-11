package dev.willram.ramcore.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Detects optional plugins without tying tests to Bukkit.
 */
public interface PluginDetector {

    boolean present(@NotNull String pluginName);

    boolean enabled(@NotNull String pluginName);

    @NotNull
    Optional<String> version(@NotNull String pluginName);
}
