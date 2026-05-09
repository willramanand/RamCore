package dev.willram.ramcore.text;

import dev.willram.ramcore.message.MessageCatalog;
import dev.willram.ramcore.message.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;

public final class TextFormattingTest {

    @Test
    public void typedContextRendersParsedUnparsedAndComponentValues() {
        TextPlaceholder<Integer> level = TextPlaceholder.parsed("level", Integer.class, String::valueOf);
        TextContext context = Texts.context()
                .put(level, 7)
                .unparsed("unsafe", "<red>literal")
                .component("label", Component.text("Rank"))
                .build();

        String plain = Texts.plain("<label>: <level> <unsafe>", context);

        assertEquals("Rank: 7 <red>literal", plain);
        assertEquals(Integer.valueOf(7), context.get(level).orElseThrow());
    }

    @Test
    public void commonLocationContextFormatsCoordinatesAndWorld() {
        World world = world("spawn");
        TextContext context = TextContexts.location(new Location(world, 12.345, 64, -8.5));

        assertEquals("spawn 12.35 64 -8.5", Texts.plain("<world> <x> <y> <z>", context));
    }

    @Test
    public void messageCatalogAcceptsTextContext() {
        MessageKey key = MessageKey.of("level", "<green><name>: <level>");
        TextContext context = Texts.context()
                .unparsed("name", "Alex")
                .parsed("level", 10)
                .build();

        String plain = PlainTextComponentSerializer.plainText().serialize(
                MessageCatalog.builder().prefix("<gold>[Game]</gold> ").build().render(key, context)
        );

        assertEquals("[Game] Alex: 10", plain);
    }

    @Test
    public void formatterProducesPlainLinesForScoreboards() {
        TextContext context = Texts.context().unparsed("player", "Steve").build();

        assertEquals(
                List.of("Player: Steve", "Coins: 20"),
                Texts.plainLines(List.of("<green>Player: <player>", "<gold>Coins: 20"), context)
        );
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

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0d;
        }
        if (type == float.class) {
            return 0.0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
