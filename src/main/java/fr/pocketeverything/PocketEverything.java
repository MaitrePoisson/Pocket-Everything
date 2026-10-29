package fr.pocketeverything;

import fr.pocketeverything.config.PocketEverythingConfig;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PocketEverything.MOD_ID)
public final class PocketEverything {

    public static final String MOD_ID = "pocket_everything";

    private static final Logger LOGGER = LoggerFactory.getLogger(PocketEverything.class);

    public PocketEverything(IEventBus modEventBus, ModContainer container) {
        LOGGER.info("Pocket-Everything {}", container.getModInfo().getVersion());
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, PocketEverythingConfig.SPEC);
    }
}
