package fr.pocketeverything.client;

import fr.pocketeverything.registry.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

public final class PocketEverythingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.CAPTURED_ENTITY, new CapturedEntityRenderer());
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.CARRIED_CHEST, new CarriedChestRenderer());
    }
}
