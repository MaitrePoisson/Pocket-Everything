package fr.pocketeverything.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.registry.ModDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
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

    private static final int MAX_CACHED_STATES = 64;

    private final Map<BlockState, Prepared> prepared = new HashMap<>();

    private static final class Prepared {

        final BlockState state;
        final BlockEntity blockEntity;
        final float scale;
        boolean modelFailed;
        boolean entityFailed;

        Prepared(BlockState state, BlockEntity blockEntity, float scale) {
            this.state = state;
            this.blockEntity = blockEntity;
            this.scale = scale;
        }
    }

    public CarriedChestRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        CarriedChestData data = stack.get(ModDataComponents.CARRIED_CHEST.get());
        BlockState carried = data == null ? Blocks.CHEST.defaultBlockState() : data.state();
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
            if (!draw(prepare(carried), poseStack, buffer, packedLight, packedOverlay)) {
                draw(prepare(Blocks.CHEST.defaultBlockState()), poseStack, buffer, packedLight, packedOverlay);
            }
        } finally {
            poseStack.popPose();
        }
    }

    private Prepared prepare(BlockState carried) {
        Prepared cached = prepared.get(carried);
        if (cached != null) {
            return cached;
        }
        if (prepared.size() >= MAX_CACHED_STATES) {
            prepared.clear();
        }
        BlockState state = carried.getBlock() instanceof ChestBlock ? carried.getBlock().defaultBlockState() : carried;
        BlockEntity blockEntity = null;
        float scale = 1.0F;
        try {
            if (state.getBlock() instanceof EntityBlock entityBlock) {
                blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
            }
            VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (!shape.isEmpty()) {
                AABB bounds = shape.bounds();
                scale = RenderScale.forBlock((float) Math.max(bounds.getXsize(), Math.max(bounds.getYsize(),
                        bounds.getZsize())));
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Carried block {} could not be prepared for rendering", state, e);
        }
        Prepared result = new Prepared(state, blockEntity, scale);
        prepared.put(carried, result);
        return result;
    }

    private boolean draw(Prepared target, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
            int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean hasEntityRenderer = target.blockEntity != null
                && minecraft.getBlockEntityRenderDispatcher().getRenderer(target.blockEntity) != null;
        boolean drawn = false;
        poseStack.pushPose();
        try {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(target.scale, target.scale, target.scale);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            RenderShape shape = target.state.getRenderShape();
            if (shape != RenderShape.INVISIBLE && !target.modelFailed && (shape == RenderShape.MODEL
                    || !hasEntityRenderer)) {
                try {
                    minecraft.getBlockRenderer().renderSingleBlock(target.state, poseStack, buffer, packedLight,
                            packedOverlay);
                    drawn = true;
                } catch (RuntimeException e) {
                    target.modelFailed = true;
                    LOGGER.warn("Model of carried block {} failed to render", target.state, e);
                }
            }
            if (hasEntityRenderer && !target.entityFailed) {
                try {
                    drawn |= minecraft.getBlockEntityRenderDispatcher().renderItem(target.blockEntity, poseStack,
                            buffer, packedLight, packedOverlay);
                } catch (RuntimeException e) {
                    target.entityFailed = true;
                    LOGGER.warn("Block entity of carried block {} failed to render", target.state, e);
                }
            }
        } finally {
            poseStack.popPose();
        }
        return drawn;
    }
}
