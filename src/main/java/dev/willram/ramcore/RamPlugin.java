package dev.willram.ramcore;

import dev.willram.ramcore.time.Time;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public abstract class RamPlugin extends JavaPlugin {


    public abstract void enable();
    public abstract void disable();
    public abstract void load();

    @Override
    public void onEnable() {
        long startTime = Time.nowMillis();
        this.log("<gold>===</gold><aqua> ENABLE START </aqua><gold>===</gold>");

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            this.registerCommands(commands);
        });

        this.enable();

        startTime = Time.nowMillis() - startTime;
        this.log("<gold>===</gold> <aqua>ENABLE</aqua> <dark_green>COMPLETE</dark_green> <gold>(</gold><yellow>Took</yellow> <light_purple>" + startTime +"ms</light_purple><gold>) ===</gold>");
    }

    @Override
    public void onDisable() {
        long startTime = Time.nowMillis();
        this.log("<gold>=== <red>DISABLE <aqua>START <gold>===");

        this.disable();

        startTime = Time.nowMillis() - startTime;
        this.log("<gold>=== <red>DISABLE <green>COMPLETE <light_purple>" + startTime + "ms <gold>===");
    }

    @Override
    public void onLoad() {
        this.load();
    }

    public void log(String message) {
        Bukkit.getServer().getConsoleSender().sendRichMessage("<gold>[<aqua>"+ this.getName() + "</aqua>]</gold> " + message);
    }

    public <T extends Listener> void registerListener(@Nonnull T listener) {
        requireNonNull(listener, "listener");
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public abstract void registerCommands(@Nonnull Commands commands);
}
