package fr.grabmob.registry;

import fr.grabmob.GrabMob;
import fr.grabmob.data.CapturedEntityData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(GrabMob.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapturedEntityData>> CAPTURED_ENTITY =
            DATA_COMPONENTS.registerComponentType("captured_entity", builder -> builder
                    .persistent(CapturedEntityData.CODEC)
                    .networkSynchronized(CapturedEntityData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
