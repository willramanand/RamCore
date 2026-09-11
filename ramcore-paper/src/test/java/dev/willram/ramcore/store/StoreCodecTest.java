package dev.willram.ramcore.store;

import dev.willram.ramcore.data.DataItem;
import dev.willram.ramcore.store.StoreContractTest.Profile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class StoreCodecTest {

    @Test
    public void gsonEnvelopeRoundTripsValueAndVersion() {
        StoreCodec<Profile> codec = StoreCodec.gson(Profile.class);
        StoredRecord<Profile> record = StoredRecord.of(3, new Profile("will", 12));

        String encoded = codec.encode(record);

        assertTrue(encoded.contains("\"version\": 3"));
        assertEquals(record, codec.decode(encoded));
    }

    @Test
    public void gsonEnvelopeDefaultsMissingVersionToOne() {
        StoreCodec<Profile> codec = StoreCodec.gson(Profile.class);

        StoredRecord<Profile> decoded = codec.decode("{\"data\":{\"name\":\"will\",\"level\":1}}");

        assertEquals(1, decoded.dataVersion());
    }

    @Test
    public void gsonEnvelopeRejectsNonEnvelopeInput() {
        StoreCodec<Profile> codec = StoreCodec.gson(Profile.class);

        assertThrows(StoreException.class, () -> codec.decode("{\"name\":\"will\"}"));
        assertThrows(StoreException.class, () -> codec.decode("not json"));
    }

    @Test
    public void dataItemCodecUsesTheItemVersionFieldAndRawJson() {
        StoreCodec<Legacy> codec = StoreCodec.dataItem(Legacy.class);
        Legacy item = new Legacy("will");
        item.dataVersion(2);

        String encoded = codec.encode(StoredRecord.of(4, item));

        assertTrue(encoded.contains("\"dataVersion\": 4"), "record version wins and is written into the item");
        assertTrue(encoded.contains("\"name\": \"will\""));
        StoredRecord<Legacy> decoded = codec.decode(encoded);
        assertEquals(4, decoded.dataVersion());
        assertEquals("will", decoded.value().name);
    }

    @Test
    public void dataItemCodecReadsFilesWrittenWithoutAVersion() {
        StoreCodec<Legacy> codec = StoreCodec.dataItem(Legacy.class);

        StoredRecord<Legacy> decoded = codec.decode("{\"name\":\"old\",\"dataVersion\":0}");

        assertEquals(1, decoded.dataVersion(), "versions below 1 clamp to the initial version");
    }

    public static final class Legacy extends DataItem {
        String name;

        public Legacy() {
        }

        Legacy(String name) {
            this.name = name;
        }
    }
}
