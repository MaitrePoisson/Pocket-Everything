package fr.grabmob.event;

import fr.grabmob.GrabMob;
import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = GrabMob.MOD_ID)
public final class ModEvents {

    public static final String KEY_CANNOT_CAPTURE = "message.grabmob.cannot_capture";

    private ModEvents() {
    }

    public static boolean isCaptureGesture(Player player, InteractionHand hand, Entity target) {
        return hand == InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && !player.isSpectator()
                && player.getAbilities().mayBuild
                && player.getMainHandItem().isEmpty()
                && !(target instanceof Player);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (!isCaptureGesture(player, event.getHand(), target)) {
            return;
        }
        if (!EntityCapturer.canCapture(target)) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide()) {
            if (!tryCapture(serverPlayer, target)) {
                serverPlayer.displayClientMessage(Component.translatable(KEY_CANNOT_CAPTURE), true);
            }
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    public static boolean tryCapture(ServerPlayer player, Entity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return false;
        }
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() / 2.0;
        double z = target.getZ();
        double spreadX = target.getBbWidth() / 2.0;
        double spreadY = target.getBbHeight() / 3.0;
        if (level.getServer().isUnderSpawnProtection(level, target.blockPosition(), player)) {
            return false;
        }
        Optional<CapturedEntityData> data = EntityCapturer.capture(player, target);
        if (data.isEmpty()) {
            return false;
        }
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        stack.set(ModDataComponents.CAPTURED_ENTITY.get(), data.get());
        giveToPlayer(player, stack);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 12, spreadX, spreadY, spreadX, 0.02);
        level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.8F);
        return true;
    }

    private static void giveToPlayer(ServerPlayer player, ItemStack stack) {
        if (player.getMainHandItem().isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            return;
        }
        if (player.getInventory().getFreeSlot() < 0 || !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
