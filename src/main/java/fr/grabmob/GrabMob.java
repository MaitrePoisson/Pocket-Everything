package fr.grabmob;

import fr.grabmob.config.GrabMobConfig;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(GrabMob.MOD_ID)
public final class GrabMob {

    public static final String MOD_ID = "grabmob";

    public GrabMob(IEventBus modEventBus, ModContainer container) {
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, GrabMobConfig.SPEC);
    }
}
