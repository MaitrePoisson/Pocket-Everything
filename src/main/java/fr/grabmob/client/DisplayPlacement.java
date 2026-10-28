package fr.grabmob.client;

public record DisplayPlacement(float x, float y, float z, float rollDegrees, float pitchDegrees, float yawDegrees,
        float scaleFactor, float depthFactor, boolean centerVertically) {

    public static final float GUI_PERIOD_MILLIS = 9000.0F;
    public static final float GUI_BASE_YAW = 25.0F;
    public static final float GUI_SWAY = 35.0F;

    public static DisplayPlacement gui(long millis) {
        double phase = (millis % (long) GUI_PERIOD_MILLIS) / (double) GUI_PERIOD_MILLIS;
        float yaw = GUI_BASE_YAW + GUI_SWAY * (float) Math.sin(phase * Math.PI * 2.0D);
        return new DisplayPlacement(0.5F, 0.5F, 0.5F, 180.0F, 0.0F, yaw, 1.4F, 1.0F, true);
    }

    public static DisplayPlacement firstPerson(boolean leftHand) {
        float yaw = leftHand ? 30.0F : -30.0F;
        return new DisplayPlacement(0.5F, 0.4F, 0.5F, 0.0F, 0.0F, yaw, 0.8F, 1.0F, false);
    }

    public static DisplayPlacement thirdPerson() {
        return new DisplayPlacement(0.5F, 0.5F, 0.5F, 0.0F, 90.0F, 0.0F, 0.7F, 1.0F, false);
    }

    public static DisplayPlacement ground() {
        return new DisplayPlacement(0.5F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.8F, 1.0F, true);
    }

    public static DisplayPlacement frame() {
        return new DisplayPlacement(0.5F, 0.5F, 0.5F, 0.0F, 0.0F, 0.0F, 0.85F, 0.4F, true);
    }

    public static DisplayPlacement neutral() {
        return new DisplayPlacement(0.5F, 0.15F, 0.5F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, false);
    }

    public boolean flipsVertically() {
        return rollDegrees != 0.0F;
    }
}
