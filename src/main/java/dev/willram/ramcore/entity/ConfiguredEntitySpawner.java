package dev.willram.ramcore.entity;

import dev.willram.ramcore.promise.Promise;
import dev.willram.ramcore.scheduler.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Region-safe helpers for spawning configured living entities.
 */
public final class ConfiguredEntitySpawner {

    @NotNull
    public static <T extends LivingEntity> Promise<EntitySpawnHandle<T>> spawn(@NotNull Location location,
                                                                               @NotNull EntityTemplate<T> template) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(template, "template");
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        Location spawnLocation = location.clone();
        return Schedulers.call(spawnLocation, () -> spawnNow(world, spawnLocation, template));
    }

    @NotNull
    public static <T extends LivingEntity> EntitySpawnHandle<T> spawnNow(@NotNull World world, @NotNull Location location,
                                                                         @NotNull EntityTemplate<T> template) {
        Objects.requireNonNull(world, "world");
        Location spawnLocation = Objects.requireNonNull(location, "location").clone();
        Objects.requireNonNull(template, "template");
        List<LivingEntity> passengers = new ArrayList<>();
        T entity = world.spawn(spawnLocation, template.type(), template.spawnReason(), template.randomizeData(), template::apply);
        for (EntityTemplate<? extends LivingEntity> passengerTemplate : template.passengers()) {
            LivingEntity passenger = spawnPassenger(world, spawnLocation, passengerTemplate);
            entity.addPassenger(passenger);
            passengers.add(passenger);
        }
        return new EntitySpawnHandle<>(entity, template, passengers);
    }

    private static <T extends LivingEntity> T spawnPassenger(World world, Location location, EntityTemplate<T> template) {
        return world.spawn(location, template.type(), template.spawnReason(), template.randomizeData(), template::apply);
    }

    private ConfiguredEntitySpawner() {
    }
}
