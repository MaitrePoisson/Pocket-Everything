package fr.pocketeverything.client;

public final class RenderScale {

    public static final float TARGET_SIZE = 0.6F;
    public static final float MIN_SCALE = 0.1F;
    public static final float MAX_SCALE = 1.5F;

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
}
