package fr.pocketeverything.client;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = PocketEverything.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new CapturedEntityClientExtensions(), ModItems.CAPTURED_ENTITY);
        event.registerItem(new CarriedChestClientExtensions(), ModItems.CARRIED_CHEST);
    }
}
