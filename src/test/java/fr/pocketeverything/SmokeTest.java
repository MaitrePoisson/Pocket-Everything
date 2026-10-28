package fr.pocketeverything;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void modIdIsStable() {
        assertEquals("pocket_everything", PocketEverything.MOD_ID);
    }
}
