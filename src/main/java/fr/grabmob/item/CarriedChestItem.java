package fr.grabmob.item;

import fr.grabmob.chest.ChestCarrier;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.UseOnContext;

public class CarriedChestItem extends Item {

    public static final String KEY_NAME = "item.grabmob.carried_chest";
    public static final String KEY_NAMED = "item.grabmob.carried_chest.named";
    public static final String KEY_CONTENTS = "tooltip.grabmob.chest_contents";

    public CarriedChestItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        entity.setInvulnerable(true);
        entity.setUnlimitedLifetime();
        return false;
    }

    @Override
    public Component getName(ItemStack stack) {
        CarriedChestData data = stack.get(ModDataComponents.CARRIED_CHEST.get());
        if (data == null) {
            return Component.translatable(KEY_NAME);
        }
        Component custom = customName(data.blockEntity());
        if (custom == null && data.state().is(Blocks.CHEST)) {
            return Component.translatable(KEY_NAME);
        }
        return Component.translatable(KEY_NAMED, custom != null ? custom : data.state().getBlock().getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CarriedChestData data = stack.get(ModDataComponents.CARRIED_CHEST.get());
        if (data != null) {
            tooltip.add(Component.translatable(KEY_CONTENTS, data.stackCount()).withStyle(ChatFormatting.GRAY));
        }
    }

    private static Component customName(CompoundTag tag) {
        if (!tag.contains("CustomName", Tag.TAG_STRING)) {
            return null;
        }
        try {
            return Component.Serializer.fromJson(tag.getString("CustomName"), RegistryAccess.EMPTY);
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        CarriedChestData data = stack.get(ModDataComponents.CARRIED_CHEST.get());
        if (data == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)
                || !ChestCarrier.place(player, level, context.getClickedPos(), context.getClickedFace(), data)) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        if (player.hasInfiniteMaterials() && stack.isEmpty()) {
            player.setItemInHand(context.getHand(), ItemStack.EMPTY);
        }
        return InteractionResult.CONSUME;
    }
}
