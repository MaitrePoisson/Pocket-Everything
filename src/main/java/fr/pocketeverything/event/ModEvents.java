package fr.pocketeverything.event;

import fr.pocketeverything.capture.EntityCapturer;
import fr.pocketeverything.data.CapturedEntityData;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import java.util.Optional;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

public final class ModEvents {

    public static final String KEY_CANNOT_CAPTURE = "message.pocket_everything.cannot_capture";

    private ModEvents() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register(ModEvents::onEntityInteract);
    }

    public static boolean isCaptureGesture(Player player, InteractionHand hand, Entity target) {
        return hand == InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && !player.isSpectator()
                && player.getAbilities().mayBuild
                && player.getMainHandItem().isEmpty()
                && !(target instanceof Player);
    }

    public static InteractionResult onEntityInteract(Player player, Level level, InteractionHand hand, Entity target,
            EntityHitResult hit) {
        if (!isCaptureGesture(player, hand, target) || !EntityCapturer.canCapture(target)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer && !level.isClientSide() && !tryCapture(serverPlayer, target)) {
            serverPlayer.displayClientMessage(Component.translatable(KEY_CANNOT_CAPTURE), true);
        }
        return InteractionResult.SUCCESS;
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
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY);
        stack.set(ModDataComponents.CAPTURED_ENTITY, data.get());
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
