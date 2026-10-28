package fr.grabmob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void modIdIsStable() {
        assertEquals("grabmob", GrabMob.MOD_ID);
    }
}
