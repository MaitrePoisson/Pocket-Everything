package fr.pocketeverything.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class CapturedEntityClientExtensions implements IClientItemExtensions {

    private CapturedEntityRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            renderer = new CapturedEntityRenderer();
        }
        return renderer;
    }
}
