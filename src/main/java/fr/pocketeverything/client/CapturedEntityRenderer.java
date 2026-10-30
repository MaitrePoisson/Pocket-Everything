package fr.pocketeverything.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.pocketeverything.data.CapturedEntityData;
import fr.pocketeverything.registry.ModDataComponents;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CapturedEntityRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CapturedEntityRenderer.class);
    private static final int CACHE_SIZE = 32;
    private static final int FIXED_TICK = 0;

    private record IdentityKey(CapturedEntityData data) {

        @Override
        public boolean equals(Object other) {
            return other instanceof IdentityKey key && key.data == data;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(data);
        }
    }

    private final Map<IdentityKey, Optional<Entity>> cache = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<IdentityKey, Optional<Entity>> eldest) {
            return size() > CACHE_SIZE;
        }
    };
    private ClientLevel cachedLevel;

    @Override
    public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        CapturedEntityData data = stack.get(ModDataComponents.CAPTURED_ENTITY);
        ClientLevel level = Minecraft.getInstance().level;
        if (data == null || level == null) {
            return;
        }
        Entity entity = entityFor(level, data);
        if (entity == null) {
            return;
        }
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        DisplayPlacement placement = placementFor(context);
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            HeldArmRenderer.render(poseStack, buffer, packedLight, context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        }
        boolean gui = context == ItemDisplayContext.GUI;
        Quaternionf previousOrientation = gui ? new Quaternionf(dispatcher.cameraOrientation()) : null;
        poseStack.pushPose();
        try {
            entity.tickCount = FIXED_TICK;
            float scale = RenderScale.forSize(entity.getBbWidth(), entity.getBbHeight()) * placement.scaleFactor();
            poseStack.translate(placement.x(), placement.y(), placement.z());
            if (placement.rollDegrees() != 0.0F) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(placement.rollDegrees()));
            }
            if (placement.pitchDegrees() != 0.0F) {
                poseStack.mulPose(Axis.XP.rotationDegrees(placement.pitchDegrees()));
            }
            if (placement.yawDegrees() != 0.0F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(placement.yawDegrees()));
            }
            poseStack.scale(scale, scale, scale * placement.depthFactor());
            if (placement.centerVertically()) {
                poseStack.translate(0.0D, -entity.getBbHeight() / 2.0D, 0.0D);
            }
            if (gui) {
                Lighting.setupForEntityInInventory();
                dispatcher.overrideCameraOrientation(new Quaternionf());
            }
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(() -> dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, poseStack, buffer,
                    packedLight));
            if (gui && buffer instanceof MultiBufferSource.BufferSource source) {
                source.endBatch();
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Rendering of captured {} failed", data.entityType(), e);
        } finally {
            dispatcher.setRenderShadow(true);
            if (gui) {
                dispatcher.overrideCameraOrientation(previousOrientation);
                Lighting.setupForFlatItems();
            }
            poseStack.popPose();
        }
    }

    private static DisplayPlacement placementFor(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> DisplayPlacement.gui(Util.getMillis());
            case FIRST_PERSON_LEFT_HAND -> DisplayPlacement.firstPerson(true);
            case FIRST_PERSON_RIGHT_HAND -> DisplayPlacement.firstPerson(false);
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> DisplayPlacement.thirdPerson();
            case GROUND -> DisplayPlacement.ground();
            case FIXED -> DisplayPlacement.frame();
            default -> DisplayPlacement.neutral();
        };
    }

    private Entity entityFor(ClientLevel level, CapturedEntityData data) {
        if (level != cachedLevel) {
            cache.clear();
            cachedLevel = level;
        }
        return cache.computeIfAbsent(new IdentityKey(data), key -> build(level, key.data())).orElse(null);
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
            entity.setCustomNameVisible(false);
            entity.clearFire();
            entity.setYRot(0.0F);
            entity.yRotO = 0.0F;
            entity.setXRot(0.0F);
            entity.xRotO = 0.0F;
            if (entity instanceof LivingEntity living) {
                living.yBodyRot = 0.0F;
                living.yBodyRotO = 0.0F;
                living.yHeadRot = 0.0F;
                living.yHeadRotO = 0.0F;
            }
            entity.tickCount = FIXED_TICK;
            entity.refreshDimensions();
            return Optional.of(entity);
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot build client entity for {}", data.entityType(), e);
            return Optional.empty();
        }
    }
}
