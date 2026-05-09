package dev.willram.ramcore.selector;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class SelectorTest {

    @Test
    public void entitySelectorFiltersByDistanceAndSortsNearest() {
        World world = world("world");
        World otherWorld = world("nether");
        Player close = player("Close", world, 2, 0, 0);
        Player far = player("Far", world, 8, 0, 0);
        Player tooFar = player("TooFar", world, 20, 0, 0);
        Player elsewhere = player("Elsewhere", otherWorld, 1, 0, 0);

        List<Player> selected = Selectors.players()
                .within(new Location(world, 0, 0, 0), 10)
                .nearest(new Location(world, 0, 0, 0))
                .select(List.of(tooFar, far, elsewhere, close));

        assertEquals(List.of(close, far), selected);
    }

    @Test
    public void playerSelectorFiltersCommonPlayerPredicates() {
        World world = world("world");
        Player admin = player("Admin", world, 0, 0, 0, Set.of("example.admin"), true, GameMode.CREATIVE);
        Player member = player("Member", world, 1, 0, 0, Set.of(), true, GameMode.SURVIVAL);
        Player offlineAdmin = player("Offline", world, 2, 0, 0, Set.of("example.admin"), false, GameMode.CREATIVE);

        List<Player> selected = Selectors.players()
                .permission("example.admin")
                .gameMode(GameMode.CREATIVE)
                .online()
                .limit(1)
                .select(List.of(member, offlineAdmin, admin));

        assertEquals(List.of(admin), selected);
    }

    @Test
    public void selectorFindsByUuidAndName() {
        World world = world("world");
        Player player = player("Target", world, 0, 0, 0);
        UUID id = player.getUniqueId();

        assertEquals(player, Selectors.playerByName(List.of(player), "target").orElseThrow());
        assertEquals(player, Selectors.playerByUuid(List.of(player), id).orElseThrow());
        assertEquals(player, Selectors.entityByUuid(List.of(player), id).orElseThrow());
    }

    @Test
    public void singleReportsMisuseWhenSelectionIsAmbiguous() {
        World world = world("world");
        List<Player> players = List.of(
                player("One", world, 0, 0, 0),
                player("Two", world, 1, 0, 0)
        );

        try {
            Selectors.players().single(players);
            fail("Expected selector misuse");
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("expected exactly one"));
            assertTrue(e.fix().contains("first"));
        }
    }

    private static World world(String name) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> name;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "World{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(World.class, handler);
    }

    private static Player player(String name, World world, double x, double y, double z) {
        return player(name, world, x, y, z, Set.of(), true, GameMode.SURVIVAL);
    }

    private static Player player(String name, World world, double x, double y, double z,
                                 Set<String> permissions, boolean online, GameMode gameMode) {
        UUID uuid = UUID.nameUUIDFromBytes(("player:" + name).getBytes());
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName" -> name;
            case "getWorld" -> world;
            case "getLocation" -> new Location(world, x, y, z);
            case "hasPermission" -> permissions.contains((String) args[0]);
            case "isOnline" -> online;
            case "getGameMode" -> gameMode;
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            case "toString" -> "Player{" + name + "}";
            default -> defaultValue(method.getReturnType());
        };
        return proxy(Player.class, handler);
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
