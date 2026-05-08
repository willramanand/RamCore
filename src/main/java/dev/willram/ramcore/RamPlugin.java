package dev.willram.ramcore;

import dev.willram.ramcore.scheduler.RamExecutors;
import dev.willram.ramcore.scheduler.Schedulers;
import dev.willram.ramcore.scheduler.TaskContext;
import dev.willram.ramcore.service.ServiceContext;
import dev.willram.ramcore.service.ServiceRegistry;
import dev.willram.ramcore.terminable.composite.CompositeTerminable;
import dev.willram.ramcore.terminable.module.TerminableModule;
import dev.willram.ramcore.time.Time;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import org.jetbrains.annotations.NotNull;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnstableApiUsage")
public abstract class RamPlugin extends JavaPlugin implements ServiceContext {


    // the backing terminable registry
    private CompositeTerminable terminableRegistry;
    private ServiceRegistry serviceRegistry;

    public abstract void enable();
    public abstract void disable();
    public abstract void load();

    @Override
    public void onEnable() {
        long startTime = Time.nowMillis();
        this.log("<gold>===</gold><aqua> ENABLE START </aqua><gold>===</gold>");

        // schedule cleanup of the registry
        Schedulers.runTimer(TaskContext.async(), this.terminableRegistry::cleanup, 200L, 600L, this.terminableRegistry);

        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            this.registerCommands(commands);
        });

        this.serviceRegistry.enableAll();
        this.enable();

        startTime = Time.nowMillis() - startTime;
        this.log("<gold>===</gold> <aqua>ENABLE</aqua> <dark_green>COMPLETE</dark_green> <gold>(</gold><yellow>Took</yellow> <light_purple>" + startTime +"ms</light_purple><gold>) ===</gold>");
    }

    @Override
    public void onDisable() {
        long startTime = Time.nowMillis();
        this.log("<gold>=== <red>DISABLE <aqua>START <gold>===");


        this.disable();
        this.serviceRegistry.close();

        // terminate the registry
        this.terminableRegistry.closeAndReportException();
        Schedulers.shutdown(this);
        RamExecutors.shutdown();

        startTime = Time.nowMillis() - startTime;
        this.log("<gold>=== <red>DISABLE <green>COMPLETE <light_purple>" + startTime + "ms <gold>===");
    }

    @Override
    public void onLoad() {
        this.terminableRegistry = CompositeTerminable.create();
        this.serviceRegistry = ServiceRegistry.create(this);
        this.load();
        this.serviceRegistry.loadAll();
    }

    public void log(String message) {
        Bukkit.getServer().getConsoleSender().sendRichMessage("<gold>[<aqua>"+ this.getName() + "</aqua>]</gold> " + message);
    }

    public <T extends Listener> void registerListener(@NotNull T listener) {
        requireNonNull(listener, "listener");
        getServer().getPluginManager().registerEvents(listener, this);
    }

    @NotNull
    @Override
    public <T extends AutoCloseable> T bind(@NotNull T terminable) {
        return this.terminableRegistry.bind(terminable);
    }

    @NotNull
    @Override
    public <T extends TerminableModule> T bindModule(@NotNull T module) {
        return this.terminableRegistry.bindModule(module);
    }

    @NotNull
    @Override
    public ServiceRegistry services() {
        return this.serviceRegistry;
    }

    public abstract void registerCommands(@NotNull Commands commands);
}
