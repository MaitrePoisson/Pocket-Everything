package fr.grabmob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarriedChestRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarriedChestRenderer.class);

    private static final float FIRST_PERSON_HAND_SHIFT = 0.56F;
    private static final float FIRST_PERSON_LIFT = 0.12F;
    private static final float FIRST_PERSON_DEPTH = 0.0F;
    private static final float FIRST_PERSON_SCALE = 0.55F;
    private static final float FIRST_PERSON_SPREAD = 0.3F;
    private static final float THIRD_PERSON_SHIFT = -0.05F;
    private static final float THIRD_PERSON_LIFT = -0.05F;
    private static final float THIRD_PERSON_DEPTH = -0.35F;
    private static final float THIRD_PERSON_SCALE = 0.5F;
    private static final Quaternionf HALF_TURN = Axis.YP.rotationDegrees(180.0F);

    private final ChestBlockEntity plain = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
    private final ChestBlockEntity trapped = new TrappedChestBlockEntity(BlockPos.ZERO,
            Blocks.TRAPPED_CHEST.defaultBlockState());

    public CarriedChestRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        CarriedChestData data = stack.get(ModDataComponents.CARRIED_CHEST.get());
        BlockEntity chest = data != null && data.state().is(Blocks.TRAPPED_CHEST) ? trapped : plain;
        boolean left = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean first = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        poseStack.pushPose();
        try {
            if (first) {
                poseStack.translate(0.5F + (left ? FIRST_PERSON_HAND_SHIFT : -FIRST_PERSON_HAND_SHIFT),
                        0.5F + FIRST_PERSON_LIFT, 0.5F + FIRST_PERSON_DEPTH);
                poseStack.pushPose();
                poseStack.translate(-0.5F, -0.5F, -0.5F);
                HeldArmRenderer.render(poseStack, buffer, packedLight, false, FIRST_PERSON_SPREAD);
                HeldArmRenderer.render(poseStack, buffer, packedLight, true, FIRST_PERSON_SPREAD);
                poseStack.popPose();
                poseStack.mulPose(HALF_TURN);
                poseStack.scale(FIRST_PERSON_SCALE, FIRST_PERSON_SCALE, FIRST_PERSON_SCALE);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            boolean third = context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                    || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            if (third) {
                poseStack.translate(0.5F + (left ? -THIRD_PERSON_SHIFT : THIRD_PERSON_SHIFT),
                        0.5F + THIRD_PERSON_LIFT, 0.5F + THIRD_PERSON_DEPTH);
                poseStack.scale(THIRD_PERSON_SCALE, THIRD_PERSON_SCALE, THIRD_PERSON_SCALE);
                poseStack.translate(-0.5F, -0.5F, -0.5F);
            }
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chest, poseStack, buffer,
                    packedLight, packedOverlay);
        } catch (RuntimeException e) {
            LOGGER.warn("Rendering of carried chest failed", e);
        } finally {
            poseStack.popPose();
        }
    }
}
