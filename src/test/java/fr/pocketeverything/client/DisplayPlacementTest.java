package fr.pocketeverything.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisplayPlacementTest {

    @Test
    void contextsHaveDistinctPlacements() {
        DisplayPlacement[] all = {
            DisplayPlacement.gui(0L),
            DisplayPlacement.firstPerson(false),
            DisplayPlacement.thirdPerson(),
            DisplayPlacement.ground(),
            DisplayPlacement.frame(),
            DisplayPlacement.neutral()
        };
        for (int i = 0; i < all.length; i++) {
            for (int j = i + 1; j < all.length; j++) {
                assertNotEquals(all[i], all[j], i + " et " + j);
            }
        }
    }

    @Test
    void guiRotationIsSmoothAndPeriodic() {
        float previous = DisplayPlacement.gui(0L).yawDegrees();
        for (long t = 50L; t <= 9000L; t += 50L) {
            float yaw = DisplayPlacement.gui(t).yawDegrees();
            assertTrue(Math.abs(yaw - previous) < 3.0F, "saut a t=" + t);
            previous = yaw;
        }
        assertEquals(DisplayPlacement.gui(0L).yawDegrees(), DisplayPlacement.gui(9000L).yawDegrees(), 0.001F);
    }

    @Test
    void guiYawStaysInSwayBounds() {
        for (long t = 0L; t < 9000L; t += 100L) {
            float yaw = DisplayPlacement.gui(t).yawDegrees();
            assertTrue(yaw >= DisplayPlacement.GUI_BASE_YAW - DisplayPlacement.GUI_SWAY - 0.01F);
            assertTrue(yaw <= DisplayPlacement.GUI_BASE_YAW + DisplayPlacement.GUI_SWAY + 0.01F);
        }
    }

    @Test
    void handsMirrorAndFrameIsFlattened() {
        assertEquals(-DisplayPlacement.firstPerson(false).yawDegrees(), DisplayPlacement.firstPerson(true).yawDegrees());
        assertTrue(DisplayPlacement.frame().depthFactor() < 1.0F);
    }
}
