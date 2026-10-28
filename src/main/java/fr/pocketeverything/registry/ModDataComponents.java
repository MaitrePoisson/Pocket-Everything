package fr.pocketeverything.registry;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.data.CapturedEntityData;
import fr.pocketeverything.data.CarriedChestData;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(PocketEverything.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CapturedEntityData>> CAPTURED_ENTITY =
            DATA_COMPONENTS.registerComponentType("captured_entity", builder -> builder
                    .persistent(CapturedEntityData.CODEC)
                    .networkSynchronized(CapturedEntityData.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CarriedChestData>> CARRIED_CHEST =
            DATA_COMPONENTS.registerComponentType("carried_chest", builder -> builder
                    .persistent(CarriedChestData.CODEC)
                    .networkSynchronized(CarriedChestData.STREAM_CODEC));

    private ModDataComponents() {
    }
}
