package com.bs.lotp.desktop.local;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstalledStoreTest {

    @Test
    void roundTrip(@TempDir Path temp) throws Exception {
        InstalledStore store = new InstalledStore(temp);
        assertTrue(store.load().isEmpty());

        store.put(new InstalledStore.InstalledEntry(
                1308, "Broke Legs", "1.0.2", 1786644762L, Instant.parse("2026-08-12T00:00:00Z"),
                List.of("Nolemir")));

        Map<Long, InstalledStore.InstalledEntry> loaded = new InstalledStore(temp).load();
        assertEquals(1, loaded.size());
        InstalledStore.InstalledEntry entry = loaded.get(1308L);
        assertEquals("Broke Legs", entry.name());
        assertEquals("1.0.2", entry.version());
        assertEquals(List.of("Nolemir"), entry.topLevelDirs());

        store.remove(1308L);
        assertTrue(new InstalledStore(temp).load().isEmpty());
    }

    @Test
    void missingFileLoadsEmpty(@TempDir Path temp) throws Exception {
        assertTrue(new InstalledStore(temp.resolve("nope")).load().isEmpty());
    }
}
