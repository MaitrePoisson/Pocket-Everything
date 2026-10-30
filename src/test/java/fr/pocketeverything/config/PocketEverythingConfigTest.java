package fr.pocketeverything.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PocketEverythingConfigTest {

    @AfterEach
    void reset() {
        PocketEverythingConfig.set(PocketEverythingConfig.DEFAULTS);
    }

    @Test
    void validFileIsRead() {
        PocketEverythingConfig.Values values = PocketEverythingConfig.parse(
                "{\"allowHostile\":false,\"extraBlacklist\":[\"minecraft:warden\",\"not valid!\",3],\"carryChestsEnabled\":false}");
        assertFalse(values.allowHostile());
        assertFalse(values.carryChestsEnabled());
        assertEquals(Set.of("minecraft:warden"), values.extraBlacklist());
    }

    @Test
    void invalidContentGivesDefaults() {
        assertEquals(PocketEverythingConfig.DEFAULTS, PocketEverythingConfig.parse("{not json"));
        assertEquals(PocketEverythingConfig.DEFAULTS, PocketEverythingConfig.parse("[1,2]"));
        assertEquals(PocketEverythingConfig.DEFAULTS, PocketEverythingConfig.parse(""));
        PocketEverythingConfig.Values partial = PocketEverythingConfig.parse("{\"allowHostile\":\"yes\",\"carryChestsEnabled\":false}");
        assertTrue(partial.allowHostile());
        assertFalse(partial.carryChestsEnabled());
    }

    @Test
    void missingFileIsCreatedWithDefaults(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("config").resolve(PocketEverythingConfig.FILE_NAME);
        assertEquals(PocketEverythingConfig.DEFAULTS, PocketEverythingConfig.load(file));
        assertTrue(Files.exists(file));
        assertEquals(PocketEverythingConfig.DEFAULTS, PocketEverythingConfig.parse(Files.readString(file)));
    }

    @Test
    void loadedValuesDriveTheAccessors(@TempDir Path dir) throws IOException {
        Path file = dir.resolve(PocketEverythingConfig.FILE_NAME);
        Files.writeString(file, "{\"allowHostile\":false,\"extraBlacklist\":[\"minecraft:warden\"],\"carryChestsEnabled\":false}");
        PocketEverythingConfig.load(file);
        assertFalse(PocketEverythingConfig.allowHostile());
        assertFalse(PocketEverythingConfig.carryChestsEnabled());
        assertTrue(PocketEverythingConfig.isExtraBlacklisted(ResourceLocation.withDefaultNamespace("warden")));
        assertFalse(PocketEverythingConfig.isExtraBlacklisted(ResourceLocation.withDefaultNamespace("cow")));
    }
}
