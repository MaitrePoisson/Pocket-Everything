package fr.pocketeverything.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

class LangKeysTest {

    private static final Set<String> EXPECTED = Set.of(
            "item.pocket_everything.captured_entity",
            "item.pocket_everything.captured_entity.named",
            "item.pocket_everything.carried_chest",
            "item.pocket_everything.carried_chest.block",
            "item.pocket_everything.carried_chest.named",
            "message.pocket_everything.cannot_capture",
            "message.pocket_everything.cannot_carry_hands",
            "message.pocket_everything.cannot_carry_locked",
            "message.pocket_everything.cannot_carry_nested",
            "message.pocket_everything.cannot_carry_protected",
            "message.pocket_everything.cannot_carry_too_big",
            "message.pocket_everything.cannot_place_chest",
            "message.pocket_everything.cannot_release",
            "tooltip.pocket_everything.chest_contents",
            "tooltip.pocket_everything.health",
            "tooltip.pocket_everything.profession");

    private static JsonObject load(String locale) throws IOException {
        try (InputStream in = LangKeysTest.class.getResourceAsStream("/assets/pocket_everything/lang/" + locale + ".json")) {
            assertNotNull(in, "fichier de langue absent : " + locale);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void englishAndFrenchDefineExactlyTheDesignedKeys() throws IOException {
        assertEquals(EXPECTED, new TreeSet<>(load("en_us").keySet()));
        assertEquals(EXPECTED, new TreeSet<>(load("fr_fr").keySet()));
    }

    @Test
    void valuesAreNotBlankAndKeepTheirPlaceholders() throws IOException {
        for (String locale : new String[] {"en_us", "fr_fr"}) {
            JsonObject lang = load(locale);
            for (String key : EXPECTED) {
                assertTrue(!lang.get(key).getAsString().isBlank(), key + " vide en " + locale);
            }
            assertTrue(lang.get("item.pocket_everything.captured_entity.named").getAsString().contains("%s"));
            assertEquals(2, lang.get("tooltip.pocket_everything.health").getAsString().split("%s", -1).length - 1);
            assertTrue(lang.get("tooltip.pocket_everything.profession").getAsString().contains("%s"));
        }
    }
}
