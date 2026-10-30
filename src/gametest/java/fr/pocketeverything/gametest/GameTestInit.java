package fr.pocketeverything.gametest;

import net.fabricmc.api.ModInitializer;

public final class GameTestInit implements ModInitializer {

    @Override
    public void onInitialize() {
        TestBlocks.register();
    }
}
