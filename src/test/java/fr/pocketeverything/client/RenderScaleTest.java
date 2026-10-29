package fr.pocketeverything.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RenderScaleTest {

    @Test
    void beeIsScaledUpSlightly() {
        assertEquals(0.6F / 0.7F, RenderScale.forSize(0.7F, 0.6F), 1.0E-5F);
    }

    @Test
    void cowIsScaledByItsHeight() {
        assertEquals(0.6F / 1.4F, RenderScale.forSize(0.9F, 1.4F), 1.0E-5F);
    }

    @Test
    void ironGolemIsScaledDown() {
        assertEquals(0.6F / 2.7F, RenderScale.forSize(1.4F, 2.7F), 1.0E-5F);
    }

    @Test
    void tinyEntityIsClampedToMaximum() {
        assertEquals(RenderScale.MAX_SCALE, RenderScale.forSize(0.1F, 0.1F));
    }

    @Test
    void hugeEntityIsClampedToMinimum() {
        assertEquals(RenderScale.MIN_SCALE, RenderScale.forSize(10.0F, 12.0F));
    }

    @Test
    void invalidSizeGivesNeutralScale() {
        assertEquals(1.0F, RenderScale.forSize(0.0F, 0.0F));
        assertEquals(1.0F, RenderScale.forSize(Float.NaN, Float.NaN));
        assertEquals(1.0F, RenderScale.forSize(-1.0F, -2.0F));
    }

    @Test
    void chestSizedBlockKeepsItsScale() {
        assertEquals(1.0F, RenderScale.forBlock(0.875F), 1.0E-5F);
    }

    @Test
    void fullBlockIsSlightlyReduced() {
        assertEquals(0.875F, RenderScale.forBlock(1.0F), 1.0E-5F);
    }

    @Test
    void smallBlockIsEnlargedWithinLimits() {
        assertEquals(0.875F / 0.7F, RenderScale.forBlock(0.7F), 1.0E-5F);
        assertEquals(RenderScale.BLOCK_MAX_SCALE, RenderScale.forBlock(0.1F));
    }

    @Test
    void hugeBlockIsClampedToMinimum() {
        assertEquals(RenderScale.BLOCK_MIN_SCALE, RenderScale.forBlock(5.0F));
    }

    @Test
    void invalidBlockSizeGivesNeutralScale() {
        assertEquals(1.0F, RenderScale.forBlock(0.0F));
        assertEquals(1.0F, RenderScale.forBlock(Float.NaN));
    }
}
