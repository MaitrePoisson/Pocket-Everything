package fr.grabmob;

import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(GrabMob.MOD_ID)
public final class GrabMob {

    public static final String MOD_ID = "grabmob";

    public GrabMob(IEventBus modEventBus) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
    }
}
