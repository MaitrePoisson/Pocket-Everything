package fr.pocketeverything.gametest;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;

final class Gestures {

    private Gestures() {
    }

    static InteractionResult interactOn(ServerPlayer player, Entity target, InteractionHand hand) {
        InteractionResult result = UseEntityCallback.EVENT.invoker().interact(player, player.level(), hand, target, null);
        return result != InteractionResult.PASS ? result : player.interactOn(target, hand);
    }
}
