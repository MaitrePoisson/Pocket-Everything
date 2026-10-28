package fr.grabmob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public final class HeldArmRenderer {

    private static final float HAND_CENTER = 0.5F;

    private HeldArmRenderer() {
    }

    public static void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, boolean leftHand) {
        AbstractClientPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) {
            return;
        }
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(player);
        float side = leftHand ? -1.0F : 1.0F;
        poseStack.pushPose();
        try {
            poseStack.translate(HAND_CENTER, HAND_CENTER, HAND_CENTER);
            poseStack.translate(side * 0.08F, -0.08F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(side * 45.0F));
            poseStack.translate(side * -1.0F, 3.6F, 3.5F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(side * 120.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(200.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(side * -135.0F));
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
