package fr.pocketeverything.registry;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.data.CapturedEntityData;
import fr.pocketeverything.data.CarriedChestData;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class ModDataComponents {

    public static final DataComponentType<CapturedEntityData> CAPTURED_ENTITY = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, "captured_entity"),
            DataComponentType.<CapturedEntityData>builder()
                    .persistent(CapturedEntityData.CODEC)
                    .networkSynchronized(CapturedEntityData.STREAM_CODEC)
                    .build());

    public static final DataComponentType<CarriedChestData> CARRIED_CHEST = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, "carried_chest"),
            DataComponentType.<CarriedChestData>builder()
                    .persistent(CarriedChestData.CODEC)
                    .networkSynchronized(CarriedChestData.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }

    public static void register() {
    }
}
