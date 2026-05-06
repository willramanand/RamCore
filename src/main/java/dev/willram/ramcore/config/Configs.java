package dev.willram.ramcore.config;

import dev.willram.ramcore.config.serializers.LocationSerializer;
import org.bukkit.Location;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;

/**
 * Misc utilities for working with Configurate
 */
public abstract class Configs {

    private static final TypeSerializerCollection TYPE_SERIALIZERS;
    static {
        TYPE_SERIALIZERS = TypeSerializerCollection.builder()
                .register(Location.class, LocationSerializer.INSTANCE)
                .build();
    }

    @NotNull
    public static TypeSerializerCollection typeSerializers() {
        return TYPE_SERIALIZERS;
    }

    private Configs() {}
}