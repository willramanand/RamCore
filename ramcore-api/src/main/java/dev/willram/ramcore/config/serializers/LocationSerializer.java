package dev.willram.ramcore.config.serializers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;

public class LocationSerializer implements TypeSerializer<Location> {
    public static final LocationSerializer INSTANCE = new LocationSerializer();

    private static final String WORLD_NAME = "world_name";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String YAW = "yaw";
    private static final String PITCH = "pitch";

    private ConfigurationNode nonVirtualNode(final ConfigurationNode source, final Object... path) throws SerializationException {
        if (!source.hasChild(path)) {
            throw new SerializationException("Required field " + Arrays.toString(path) + " was not present in node");
        }
        return source.node(path);
    }

    @Override
    public Location deserialize(Type type, ConfigurationNode node) throws SerializationException {
        final String worldName = nonVirtualNode(node, WORLD_NAME).getString();
        final double x = nonVirtualNode(node, X).getDouble();
        final double y = nonVirtualNode(node, Y).getDouble();
        final double z = nonVirtualNode(node, Z).getDouble();
        final float yaw = nonVirtualNode(node, YAW).getFloat();
        final float pitch = nonVirtualNode(node, PITCH).getFloat();

        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    @Override
    public void serialize(Type type, @Nullable Location loc, ConfigurationNode node) throws SerializationException {
        if (loc == null) {
            node.raw(null);
            return;
        }

        node.node(WORLD_NAME).set(loc.getWorld().getName());
        node.node(X).set(loc.getX());
        node.node(Y).set(loc.getY());
        node.node(Z).set(loc.getZ());
        node.node(YAW).set(loc.getYaw());
        node.node(PITCH).set(loc.getPitch());
    }
}
