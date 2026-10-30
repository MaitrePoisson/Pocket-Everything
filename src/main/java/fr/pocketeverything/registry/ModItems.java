package fr.pocketeverything.registry;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.item.CapturedEntityItem;
import fr.pocketeverything.item.CarriedChestItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {

    public static final CapturedEntityItem CAPTURED_ENTITY = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, "captured_entity"),
            new CapturedEntityItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static final CarriedChestItem CARRIED_CHEST = Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, "carried_chest"),
            new CarriedChestItem(new Item.Properties().stacksTo(1).fireResistant()));

    private ModItems() {
    }

    public static void register() {
    }
}
