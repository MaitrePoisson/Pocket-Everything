package fr.grabmob.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class CarriedChestClientExtensions implements IClientItemExtensions {

    private CarriedChestRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new CarriedChestRenderer();
        }
        return renderer;
    }
}
