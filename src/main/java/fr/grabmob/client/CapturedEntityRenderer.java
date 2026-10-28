package fr.grabmob.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.registry.ModDataComponents;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CapturedEntityRenderer extends BlockEntityWithoutLevelRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapturedEntityRenderer.class);
    private static final int CACHE_SIZE = 32;

    private final Map<CapturedEntityData, Optional<Entity>> cache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<CapturedEntityData, Optional<Entity>> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private ClientLevel cachedLevel;

    public CapturedEntityRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        CapturedEntityData data = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        ClientLevel level = Minecraft.getInstance().level;
        if (data == null || level == null) {
            return;
        }
        Entity entity = entityFor(level, data);
        if (entity == null) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        poseStack.pushPose();
        try {
            float scale = RenderScale.forSize(entity.getBbWidth(), entity.getBbHeight());
            poseStack.translate(0.5D, 0.15D, 0.5D);
            poseStack.scale(scale, scale, scale);
            dispatcher.setRenderShadow(false);
            dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, buffer, packedLight);
        } catch (RuntimeException e) {
            LOGGER.warn("Rendering of captured {} failed", data.entityType(), e);
        } finally {
            dispatcher.setRenderShadow(true);
            poseStack.popPose();
        }
    }

    private Entity entityFor(ClientLevel level, CapturedEntityData data) {
        if (level != cachedLevel) {
            cache.clear();
            cachedLevel = level;
        }
        return cache.computeIfAbsent(data, key -> build(level, key)).orElse(null);
    }

    private static Optional<Entity> build(ClientLevel level, CapturedEntityData data) {
        try {
            Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(data.entityType());
            if (type.isEmpty()) {
                return Optional.empty();
            }
            Entity entity = type.get().create(level);
            if (entity == null) {
                return Optional.empty();
            }
            entity.load(data.tag().copy());
            entity.setNoGravity(true);
            return Optional.of(entity);
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot build client entity for {}", data.entityType(), e);
            return Optional.empty();
        }
    }
}
