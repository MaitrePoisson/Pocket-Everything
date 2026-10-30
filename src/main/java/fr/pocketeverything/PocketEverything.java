package fr.pocketeverything;

import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PocketEverything implements ModInitializer {

    public static final String MOD_ID = "pocket_everything";

    private static final Logger LOGGER = LoggerFactory.getLogger(PocketEverything.class);

    @Override
    public void onInitialize() {
        LOGGER.info("Pocket-Everything {}",
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion());
        ModDataComponents.register();
        ModItems.register();
    }
}
