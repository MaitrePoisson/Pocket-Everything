package fr.pocketeverything.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.joml.Quaternionf;

public final class HeldArmRenderer {

    private static final float HAND_CENTER = 0.5F;

    private static final Quaternionf[] YAW_IN = pair(Axis.YP, 45.0F);
    private static final Quaternionf[] ROLL = pair(Axis.ZP, 120.0F);
    private static final Quaternionf[] YAW_OUT = pair(Axis.YP, -135.0F);
    private static final Quaternionf PITCH = Axis.XP.rotationDegrees(200.0F);

    private HeldArmRenderer() {
    }

    private static Quaternionf[] pair(Axis axis, float degrees) {
        return new Quaternionf[] {axis.rotationDegrees(degrees), axis.rotationDegrees(-degrees)};
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean leftHand) {
        render(poseStack, buffer, packedLight, leftHand, 0.0F);
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean leftHand,
            float spread) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(player);
        float side = leftHand ? -1.0F : 1.0F;
        int index = leftHand ? 1 : 0;
        poseStack.pushPose();
        try {
            poseStack.translate(HAND_CENTER, HAND_CENTER, HAND_CENTER);
            poseStack.translate(side * (0.08F + spread), -0.08F, 0.0F);
            poseStack.mulPose(YAW_IN[index]);
            poseStack.translate(side * -1.0F, 3.6F, 3.5F);
            poseStack.mulPose(ROLL[index]);
            poseStack.mulPose(PITCH);
            poseStack.mulPose(YAW_OUT[index]);
            poseStack.translate(side * 5.6F, 0.0F, 0.0F);
            if (leftHand) {
                renderer.renderLeftHand(poseStack, buffer, packedLight, player);
            } else {
                renderer.renderRightHand(poseStack, buffer, packedLight, player);
            }
        } finally {
            poseStack.popPose();
        }
    }
}
