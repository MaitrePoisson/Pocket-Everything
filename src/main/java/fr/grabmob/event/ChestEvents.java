package fr.grabmob.event;

import fr.grabmob.GrabMob;
import fr.grabmob.chest.ChestCarrier;
import fr.grabmob.config.GrabMobConfig;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = GrabMob.MOD_ID)
public final class ChestEvents {

    public static final String KEY_HANDS = "message.grabmob.cannot_carry_hands";

    private ChestEvents() {
    }

    public static boolean isCarrying(Player player) {
        return player.getMainHandItem().is(ModItems.CARRIED_CHEST.get());
    }

    public static boolean isPickupGesture(Player player, InteractionHand hand, BlockState state) {
        return hand == InteractionHand.MAIN_HAND
                && GrabMobConfig.carryChestsEnabled()
                && player.isShiftKeyDown()
                && !player.isSpectator()
                && player.getAbilities().mayBuild
                && player.getMainHandItem().isEmpty()
                && state.is(ChestCarrier.CARRYABLE);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() == InteractionHand.OFF_HAND && isCarrying(player)) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }
        if (!isPickupGesture(player, event.getHand(), player.level().getBlockState(event.getPos()))) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && player.level() instanceof ServerLevel level
                && !pickUp(serverPlayer, level, event)) {
            serverPlayer.displayClientMessage(Component.translatable(KEY_HANDS), true);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean pickUp(ServerPlayer player, ServerLevel level, PlayerInteractEvent.RightClickBlock event) {
        if (!player.getOffhandItem().isEmpty()) {
            return false;
        }
        Optional<CarriedChestData> data = ChestCarrier.pickUp(player, level, event.getPos());
        data.ifPresent(carried -> {
            ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST.get());
            stack.set(ModDataComponents.CARRIED_CHEST.get(), carried);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        });
        return true;
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() == InteractionHand.OFF_HAND && isCarrying(event.getEntity())) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSwapHands(LivingSwapItemsEvent.Hands event) {
        if (event.getEntity() instanceof Player player && isCarrying(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getOffhandItem().isEmpty()
                || !isCarrying(player) && !player.getOffhandItem().is(ModItems.CARRIED_CHEST.get())) {
            return;
        }
        ItemStack other = player.getOffhandItem();
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        if (!player.getInventory().add(other)) {
            player.drop(other, false);
        }
    }
}
