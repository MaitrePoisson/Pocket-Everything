package fr.grabmob.client;

import fr.grabmob.GrabMob;
import fr.grabmob.registry.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid = GrabMob.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new CapturedEntityClientExtensions(), ModItems.CAPTURED_ENTITY.get());
        event.registerItem(new CarriedChestClientExtensions(), ModItems.CARRIED_CHEST.get());
    }
}
