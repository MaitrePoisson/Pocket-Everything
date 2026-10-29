package fr.pocketeverything.client;

public final class RenderScale {

    public static final float TARGET_SIZE = 0.6F;
    public static final float MIN_SCALE = 0.1F;
    public static final float MAX_SCALE = 1.5F;
    public static final float BLOCK_REFERENCE_SIZE = 0.875F;
    public static final float BLOCK_MIN_SCALE = 0.5F;
    public static final float BLOCK_MAX_SCALE = 1.6F;

    private RenderScale() {
    }

    public static float forSize(float width, float height) {
        float largest = Math.max(width, height);
        if (Float.isNaN(largest) || Float.isInfinite(largest) || largest <= 0.0F) {
            return 1.0F;
        }
        float scale = TARGET_SIZE / largest;
        return Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
    }

    public static float forBlock(float largestSide) {
        if (Float.isNaN(largestSide) || Float.isInfinite(largestSide) || largestSide <= 0.0F) {
            return 1.0F;
        }
        return Math.min(BLOCK_MAX_SCALE, Math.max(BLOCK_MIN_SCALE, BLOCK_REFERENCE_SIZE / largestSide));
    }
}
