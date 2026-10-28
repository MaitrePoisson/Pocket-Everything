package fr.grabmob.item;

import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.registry.ModDataComponents;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

public class CapturedEntityItem extends Item {

    public static final String KEY_NAME = "item.grabmob.captured_entity";
    public static final String KEY_NAMED = "item.grabmob.captured_entity.named";
    public static final String KEY_HEALTH = "tooltip.grabmob.health";
    public static final String KEY_PROFESSION = "tooltip.grabmob.profession";

    public CapturedEntityItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CapturedEntityData data = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos target = context.getClickedPos().relative(context.getClickedFace());
        Vec3 position = Vec3.atBottomCenterOf(target);
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() + 180.0F : context.getRotation();
        Optional<Entity> released = EntityCapturer.release(level, data, position, yaw);
        if (released.isEmpty()) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        if (player != null && player.hasInfiniteMaterials() && stack.isEmpty()) {
            player.setItemInHand(context.getHand(), ItemStack.EMPTY);
        }
        level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.2F);
        return InteractionResult.CONSUME;
    }

    @Override
    public Component getName(ItemStack stack) {
        CapturedEntityData data = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        if (data == null) {
            return Component.translatable(KEY_NAME);
        }
        Component entityName = CapturedEntityInfo.customNameJson(data.tag())
                .map(CapturedEntityItem::parseName)
                .orElseGet(() -> BuiltInRegistries.ENTITY_TYPE.getOptional(data.entityType())
                        .map(EntityType::getDescription)
                        .orElse(null));
        if (entityName == null) {
            return Component.translatable(KEY_NAME);
        }
        return Component.translatable(KEY_NAMED, entityName);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CapturedEntityData data = stack.get(ModDataComponents.CAPTURED_ENTITY.get());
        if (data == null) {
            return;
        }
        CapturedEntityInfo.health(data.tag()).ifPresent(health -> {
            String max = CapturedEntityInfo.maxHealth(data.tag())
                    .map(CapturedEntityInfo::formatNumber)
                    .orElse("?");
            tooltip.add(Component.translatable(KEY_HEALTH, CapturedEntityInfo.formatNumber(health), max)
                    .withStyle(ChatFormatting.GRAY));
        });
        CapturedEntityInfo.professionTranslationKey(data.tag()).ifPresent(key ->
                tooltip.add(Component.translatable(KEY_PROFESSION, Component.translatable(key))
                        .withStyle(ChatFormatting.GRAY)));
    }

    private static Component parseName(String json) {
        try {
            return Component.Serializer.fromJson(json, RegistryAccess.EMPTY);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
