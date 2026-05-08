package dev.willram.ramcore.content;

import dev.willram.ramcore.exception.ApiMisuseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class ContentRegistryTest {

    @Test
    public void registerAndLookupByNamespacedId() {
        ContentRegistry<String> registry = ContentRegistry.create(String.class);
        ContentKey<String> key = ContentKey.of("ramcore", "example", String.class);

        ContentEntry<String> entry = registry.register("RamCore", key, "value");

        assertEquals("RamCore", entry.owner());
        assertSame("value", registry.require(ContentId.of("ramcore", "example")));
        assertTrue(registry.contains(ContentId.parse("ramcore:example")));
    }

    @Test
    public void duplicateIdFailsFast() {
        ContentRegistry<String> registry = ContentRegistry.create(String.class);
        ContentKey<String> key = ContentKey.of("ramcore", "example", String.class);
        registry.register("RamCore", key, "first");

        try {
            registry.register("Other", key, "second");
        } catch (ApiMisuseException e) {
            assertTrue(e.problem().contains("already registered"));
            return;
        }

        throw new AssertionError("expected ApiMisuseException");
    }

    @Test
    public void unregisterOwnerRemovesOwnedEntries() {
        ContentRegistry<String> registry = ContentRegistry.create(String.class);
        ContentId first = ContentId.of("ramcore", "first");
        ContentId second = ContentId.of("ramcore", "second");
        registry.register("A", ContentKey.of(first, String.class), "first");
        registry.register("B", ContentKey.of(second, String.class), "second");

        assertEquals(1, registry.unregisterOwner("A"));

        assertFalse(registry.contains(first));
        assertTrue(registry.contains(second));
    }
}
