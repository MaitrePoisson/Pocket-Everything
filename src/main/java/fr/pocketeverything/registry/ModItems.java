package fr.pocketeverything.registry;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.item.CapturedEntityItem;
import fr.pocketeverything.item.CarriedChestItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PocketEverything.MOD_ID);

    public static final DeferredItem<CapturedEntityItem> CAPTURED_ENTITY = ITEMS.registerItem(
            "captured_entity",
            CapturedEntityItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<CarriedChestItem> CARRIED_CHEST = ITEMS.registerItem(
            "carried_chest",
            CarriedChestItem::new,
            new Item.Properties().stacksTo(1).fireResistant());

    private ModItems() {
    }
}
