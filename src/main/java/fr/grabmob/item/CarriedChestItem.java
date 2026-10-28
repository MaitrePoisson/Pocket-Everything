package fr.grabmob.item;

import fr.grabmob.chest.ChestCarrier;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.registry.ModDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class CarriedChestItem extends Item {

    public CarriedChestItem(Properties properties) {
        super(properties);
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
