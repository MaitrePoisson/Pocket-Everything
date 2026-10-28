package fr.grabmob.item;

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
            "item.grabmob.captured_entity",
            "item.grabmob.captured_entity.named",
            "item.grabmob.carried_chest",
            "item.grabmob.carried_chest.named",
            "message.grabmob.cannot_capture",
            "message.grabmob.cannot_carry_hands",
            "message.grabmob.cannot_carry_locked",
            "message.grabmob.cannot_carry_nested",
            "message.grabmob.cannot_carry_protected",
            "message.grabmob.cannot_carry_too_big",
            "message.grabmob.cannot_place_chest",
            "message.grabmob.cannot_release",
            "tooltip.grabmob.chest_contents",
            "tooltip.grabmob.health",
            "tooltip.grabmob.profession");

    private static JsonObject load(String locale) throws IOException {
        try (InputStream in = LangKeysTest.class.getResourceAsStream("/assets/grabmob/lang/" + locale + ".json")) {
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
            assertTrue(lang.get("item.grabmob.captured_entity.named").getAsString().contains("%s"));
            assertEquals(2, lang.get("tooltip.grabmob.health").getAsString().split("%s", -1).length - 1);
            assertTrue(lang.get("tooltip.grabmob.profession").getAsString().contains("%s"));
        }
    }
}
