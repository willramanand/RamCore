package dev.willram.ramcore.pdc;

import io.papermc.paper.persistence.PersistentDataContainerView;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PdcTypedApiTest {

    @Test
    public void typedKeysReadWriteAndRemoveValues() {
        MemoryPdc container = new MemoryPdc();
        PdcKey<Integer, Integer> coins = PdcKey.of("example", "coins", PersistentDataType.INTEGER);

        PDCs.set(container, coins, 10);

        assertTrue(PDCs.has(container, coins));
        assertEquals(Optional.of(10), PDCs.get(container, coins));

        PDCs.edit(container).remove(coins);
        assertFalse(PDCs.has(container, coins));
    }

    @Test
    public void defaultValuesAreSupported() {
        MemoryPdc container = new MemoryPdc();
        PdcKey<String, String> rank = PdcKey.of("example", "rank", PersistentDataType.STRING).defaultValue("default");

        assertEquals("default", PDCs.view(container).getOrDefault(rank));
        assertEquals("fallback", PDCs.view(container).getOrDefault(PdcKey.of("example", "missing", PersistentDataType.STRING), "fallback"));
    }

    @Test
    public void jsonObjectDataTypeRoundTripsObjects() {
        PdcJsonDataType<Profile> type = new PdcJsonDataType<>(Profile.class);
        PdcKey<String, Profile> profileKey = PdcKey.of("example", "profile", type);
        MemoryPdc container = new MemoryPdc();

        PDCs.set(container, profileKey, new Profile("Will", 7));

        assertEquals(new Profile("Will", 7), PDCs.get(container, profileKey).orElseThrow());
    }

    @Test
    public void objectCodecDataTypeRoundTripsObjects() {
        PdcObjectDataType<Profile> type = new PdcObjectDataType<>(Profile.class, new PdcObjectCodec<>() {
            @Override
            public String encode(Profile value) {
                return value.name() + ":" + value.level();
            }

            @Override
            public Profile decode(String value) {
                String[] parts = value.split(":");
                return new Profile(parts[0], Integer.parseInt(parts[1]));
            }
        });
        PdcKey<String, Profile> profileKey = PdcKey.of("example", "profile", type);
        MemoryPdc container = new MemoryPdc();

        PDCs.edit(container).set(profileKey, new Profile("Ram", 12));

        assertEquals(new Profile("Ram", 12), PDCs.view(container).require(profileKey));
    }

    private record Profile(String name, int level) {
    }

    private static final class MemoryPdc implements PersistentDataContainer {
        private final Map<NamespacedKey, Entry<?, ?>> values = new HashMap<>();
        private final PersistentDataAdapterContext context = new MemoryContext();

        @Override
        public <P, C> void set(NamespacedKey key, PersistentDataType<P, C> type, C value) {
            this.values.put(key, new Entry<>(type, type.toPrimitive(value, this.context)));
        }

        @Override
        public <P, C> boolean has(NamespacedKey key, PersistentDataType<P, C> type) {
            Entry<?, ?> entry = this.values.get(key);
            return entry != null && entry.type().equals(type);
        }

        @Override
        public boolean has(NamespacedKey key) {
            return this.values.containsKey(key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <P, C> C get(NamespacedKey key, PersistentDataType<P, C> type) {
            Entry<?, ?> entry = this.values.get(key);
            if (entry == null || !entry.type().equals(type)) {
                return null;
            }
            return type.fromPrimitive((P) entry.primitive(), this.context);
        }

        @Override
        public <P, C> C getOrDefault(NamespacedKey key, PersistentDataType<P, C> type, C defaultValue) {
            C value = get(key, type);
            return value == null ? defaultValue : value;
        }

        @Override
        public Set<NamespacedKey> getKeys() {
            return Set.copyOf(this.values.keySet());
        }

        @Override
        public boolean isEmpty() {
            return this.values.isEmpty();
        }

        @Override
        public void copyTo(PersistentDataContainer other, boolean replace) {
            for (Map.Entry<NamespacedKey, Entry<?, ?>> entry : this.values.entrySet()) {
                if (replace || !other.has(entry.getKey())) {
                    entry.getValue().copyTo(entry.getKey(), other);
                }
            }
        }

        @Override
        public PersistentDataAdapterContext getAdapterContext() {
            return this.context;
        }

        @Override
        public byte[] serializeToBytes() {
            return new byte[0];
        }

        @Override
        public int getSize() {
            return this.values.size();
        }

        @Override
        public void remove(NamespacedKey key) {
            this.values.remove(key);
        }

        @Override
        public void readFromBytes(byte[] bytes, boolean clear) throws IOException {
            if (clear) {
                this.values.clear();
            }
        }
    }

    private record Entry<P, C>(PersistentDataType<P, C> type, P primitive) {
        private void copyTo(NamespacedKey key, PersistentDataContainer other) {
            other.set(key, this.type, this.type.fromPrimitive(this.primitive, other.getAdapterContext()));
        }
    }

    private static final class MemoryContext implements PersistentDataAdapterContext {
        @Override
        public PersistentDataContainer newPersistentDataContainer() {
            return new MemoryPdc();
        }
    }
}
