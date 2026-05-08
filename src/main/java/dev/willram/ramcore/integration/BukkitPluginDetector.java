package dev.willram.ramcore.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Bukkit-backed optional plugin detector.
 */
public final class BukkitPluginDetector implements PluginDetector {

    @Override
    public boolean present(@NotNull String pluginName) {
        return Bukkit.getPluginManager().getPlugin(pluginName) != null;
    }

    @Override
    public boolean enabled(@NotNull String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin != null && plugin.isEnabled();
    }

    @NotNull
    @Override
    public Optional<String> version(@NotNull String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin == null ? Optional.empty() : Optional.of(plugin.getPluginMeta().getVersion());
    }
}
