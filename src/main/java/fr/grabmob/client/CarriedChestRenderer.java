package fr.grabmob.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CarriedChestRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CarriedChestRenderer.class);

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
        try {
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chest, poseStack, buffer,
                    packedLight, packedOverlay);
        } catch (RuntimeException e) {
            LOGGER.warn("Rendering of carried chest failed", e);
        }
    }
}
