package fr.pocketeverything.event;

import fr.pocketeverything.chest.ChestCarrier;
import fr.pocketeverything.config.PocketEverythingConfig;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class ChestEvents {

    public static final String KEY_HANDS = "message.pocket_everything.cannot_carry_hands";

    private ChestEvents() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register(ChestEvents::onUseBlock);
        UseItemCallback.EVENT.register(ChestEvents::onUseItem);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(ChestEvents::onPlayerTick));
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> protectDroppedChest(entity));
    }

    public static boolean isCarrying(Player player) {
        return player.getMainHandItem().is(ModItems.CARRIED_CHEST);
    }

    public static boolean isPickupGesture(Player player, InteractionHand hand, BlockState state) {
        return hand == InteractionHand.MAIN_HAND
                && PocketEverythingConfig.carryChestsEnabled()
                && player.isShiftKeyDown()
                && !player.isSpectator()
                && player.getAbilities().mayBuild
                && player.getMainHandItem().isEmpty()
                && state.is(ChestCarrier.CARRYABLE);
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.OFF_HAND && isCarrying(player)) {
            return InteractionResult.FAIL;
        }
        if (!isPickupGesture(player, hand, level.getBlockState(hit.getBlockPos()))) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel
                && !pickUp(serverPlayer, serverLevel, hit.getBlockPos())) {
            serverPlayer.displayClientMessage(Component.translatable(KEY_HANDS), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean pickUp(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!player.getOffhandItem().isEmpty()) {
            return false;
        }
        Optional<CarriedChestData> data = ChestCarrier.pickUp(player, level, pos);
        data.ifPresent(carried -> {
            ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST);
            stack.set(ModDataComponents.CARRIED_CHEST, carried);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        });
        return true;
    }

    public static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
        if (hand == InteractionHand.OFF_HAND && isCarrying(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }

    public static boolean cancelsSwapHands(Player player) {
        return isCarrying(player);
    }

    public static void onPlayerTick(ServerPlayer player) {
        if (player.getOffhandItem().isEmpty()
                || !isCarrying(player) && !player.getOffhandItem().is(ModItems.CARRIED_CHEST)) {
            return;
        }
        ItemStack other = player.getOffhandItem();
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        if (!player.getInventory().add(other)) {
            player.drop(other, false);
        }
    }

    public static void protectDroppedChest(Entity entity) {
        if (entity instanceof ItemEntity item && item.getItem().is(ModItems.CARRIED_CHEST)) {
            item.setInvulnerable(true);
            item.setUnlimitedLifetime();
        }
    }
}
