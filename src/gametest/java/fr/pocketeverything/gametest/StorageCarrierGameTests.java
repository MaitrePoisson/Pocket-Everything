package fr.pocketeverything.gametest;

import static fr.pocketeverything.gametest.ChestGameTests.assertCarrying;
import static fr.pocketeverything.gametest.ChestGameTests.assertNothingDropped;
import static fr.pocketeverything.gametest.ChestGameTests.carried;
import static fr.pocketeverything.gametest.ChestGameTests.click;
import static fr.pocketeverything.gametest.ChestGameTests.put;
import static fr.pocketeverything.gametest.ChestGameTests.take;

import fr.pocketeverything.chest.ChestCarrier;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import java.util.List;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class StorageCarrierGameTests {

    private static final String TEMPLATE = "pocket_everything:empty";
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);

    public StorageCarrierGameTests() {
    }

    private static ItemStack sword(int slot) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.setDamageValue(slot + 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Lame " + slot));
        return stack;
    }

    private static void fillAt(GameTestHelper helper, BlockPos at, Block block) {
        helper.setBlock(at, block.defaultBlockState());
        if (helper.getBlockEntity(at) instanceof TestBlocks.ContainerEntity container) {
            for (int slot = 0; slot < 9; slot++) {
                container.setItem(slot, slot % 2 == 0 ? sword(slot) : new ItemStack(Items.COBBLESTONE, 64 - slot));
            }
            return;
        }
        ListTag items = new ListTag();
        for (int slot = 0; slot < 4; slot++) {
            CompoundTag entry = (CompoundTag) (slot % 2 == 0 ? sword(slot) : new ItemStack(Items.COBBLESTONE, 10 + slot))
                    .save(helper.getLevel().registryAccess());
            entry.putByte("Slot", (byte) slot);
            items.add(entry);
        }
        CompoundTag tag = new CompoundTag();
        CompoundTag inventory = new CompoundTag();
        inventory.put("Items", items);
        tag.put("Inventory", inventory);
        helper.getBlockEntity(at).loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static CompoundTag contentsOf(GameTestHelper helper, BlockPos at) {
        return helper.getBlockEntity(at).saveWithoutMetadata(helper.getLevel().registryAccess());
    }

    private static int countCarriedStacks(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            total += stack.is(ModItems.CARRIED_CHEST) ? stack.getCount() : 0;
        }
        return total;
    }

    private static void roundTrip(GameTestHelper helper, Block block) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, block);
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(before.contains("Items") || before.contains("Inventory"), "le bloc de test aurait dû être rempli");
        CarriedChestData data = take(helper, player, SPOT);
        helper.assertTrue(data != null, "le bloc de test aurait dû être pris");
        helper.assertTrue(helper.getBlockState(SPOT).isAir(), "le bloc aurait dû disparaître");
        assertNothingDropped(helper);
        helper.assertTrue(data.blockEntity().equals(before), "le contenu porté doit être celui du bloc");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose aurait dû réussir");
        helper.assertTrue(helper.getBlockState(TARGET).is(block), "le bon bloc aurait dû être posé");
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu et le NBT doivent être identiques après la pose");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void containerBlockRoundTrip(GameTestHelper helper) {
        roundTrip(helper, TestBlocks.STORAGE);
    }

    @GameTest(template = TEMPLATE)
    public static void nbtOnlyBlockRoundTrip(GameTestHelper helper) {
        roundTrip(helper, TestBlocks.HANDLER);
    }

    @GameTest(template = TEMPLATE)
    public static void gestureNeverDuplicates(GameTestHelper helper) {
        for (Block block : List.of(TestBlocks.STORAGE, TestBlocks.HANDLER)) {
            ServerPlayer player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            fillAt(helper, SPOT, block);
            CompoundTag before = contentsOf(helper, SPOT);
            player.setShiftKeyDown(true);
            helper.assertTrue(click(helper, player, InteractionHand.MAIN_HAND, SPOT).consumesAction(), "le geste aurait dû être consommé");
            helper.assertTrue(helper.getBlockState(SPOT).isAir(), "le bloc doit être retiré à la prise");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "le bloc ne doit pas s'ouvrir");
            assertCarrying(helper, player, true);
            helper.assertTrue(countCarriedStacks(player) == 1, "un seul objet porté attendu (" + countCarriedStacks(player) + ")");
            assertNothingDropped(helper);
            click(helper, player, InteractionHand.MAIN_HAND, TARGET.below());
            helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu posé doit être identique");
            helper.assertTrue(countCarriedStacks(player) == 0, "plus aucun objet porté après la pose");
            assertNothingDropped(helper);
            helper.setBlock(TARGET, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lockedBlockIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.STORAGE);
        CompoundTag locked = contentsOf(helper, SPOT);
        locked.putString("Lock", "clef");
        helper.getBlockEntity(SPOT).loadWithComponents(locked, helper.getLevel().registryAccess());
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un bloc verrouillé ne doit pas être pris");
        helper.assertTrue(helper.getBlockState(SPOT).is(TestBlocks.STORAGE) && contentsOf(helper, SPOT).equals(before),
                "le bloc verrouillé doit rester intact");
        helper.succeed();
    }

    private static volatile boolean vetoBreaks;

    static {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> !vetoBreaks);
    }

    @GameTest(template = TEMPLATE)
    public static void cancelledBreakEventIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.STORAGE);
        CompoundTag before = contentsOf(helper, SPOT);
        vetoBreaks = true;
        try {
            helper.assertTrue(take(helper, player, SPOT) == null, "un BreakEvent annulé doit refuser la prise");
        } finally {
            vetoBreaks = false;
        }
        helper.assertTrue(helper.getBlockState(SPOT).is(TestBlocks.STORAGE) && contentsOf(helper, SPOT).equals(before),
                "le bloc protégé doit rester intact");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void adventureModeIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.ADVENTURE);
        fillAt(helper, SPOT, TestBlocks.HANDLER);
        helper.assertTrue(take(helper, player, SPOT) == null, "le mode Aventure ne doit pas pouvoir prendre le bloc");
        helper.assertTrue(helper.getBlockState(SPOT).is(TestBlocks.HANDLER), "le bloc doit rester");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void excludedBlockIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.VAULT);
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un bloc de never_carryable ne doit pas être pris");
        helper.assertTrue(helper.getBlockState(SPOT).is(TestBlocks.VAULT) && contentsOf(helper, SPOT).equals(before),
                "le bloc exclu doit rester intact");
        CarriedChestData forged = new CarriedChestData(TestBlocks.VAULT.defaultBlockState(), before);
        helper.assertTrue(!put(helper, player, TARGET.below(), forged), "un bloc exclu ne doit pas être posé");
        helper.assertTrue(helper.getBlockState(TARGET).isAir(), "rien ne doit être posé");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void blockOutsideTheTagIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.setBlock(SPOT, Blocks.FURNACE);
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un bloc hors étiquette ne doit pas être pris");
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.FURNACE) && contentsOf(helper, SPOT).equals(before),
                "le bloc hors étiquette doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nestedCarriedBlockIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT.east(2), TestBlocks.HANDLER);
        CarriedChestData inner = take(helper, player, SPOT.east(2));
        fillAt(helper, SPOT, TestBlocks.STORAGE);
        ((TestBlocks.ContainerEntity) helper.getBlockEntity(SPOT)).setItem(0, carried(inner));
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un bloc contenant un objet porté ne doit pas être pris");
        ((TestBlocks.ContainerEntity) helper.getBlockEntity(SPOT)).setItem(0, shulkerWith(inner));
        before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un objet porté caché dans un shulker doit être refusé");
        helper.assertTrue(contentsOf(helper, SPOT).equals(before), "le bloc doit rester intact");
        helper.succeed();
    }

    private static ItemStack shulkerWith(CarriedChestData inner) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(carried(inner))));
        return box;
    }

    @GameTest(template = TEMPLATE)
    public static void viewersAreClosedOnPickup(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.STORAGE);
        viewer.openMenu((TestBlocks.ContainerEntity) helper.getBlockEntity(SPOT));
        helper.assertTrue(viewer.containerMenu instanceof ChestMenu, "le menu du bloc à Container aurait dû s'ouvrir");
        helper.assertTrue(take(helper, player, SPOT) != null, "la prise aurait dû réussir");
        helper.assertTrue(viewer.containerMenu == viewer.inventoryMenu, "le menu du bloc à Container doit être fermé");

        ServerPlayer other = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.HANDLER);
        other.moveTo(helper.absoluteVec(SPOT.getCenter()));
        other.openMenu(new SimpleMenuProvider((id, inventory, p) -> ChestMenu.threeRows(id, inventory, new SimpleContainer(27)),
                Component.literal("Menu du bloc")));
        helper.assertTrue(other.containerMenu instanceof ChestMenu, "le menu du bloc sans Container aurait dû s'ouvrir");
        helper.assertTrue(take(helper, player, SPOT) != null, "la prise du bloc à inventaire NBT aurait dû réussir");
        helper.assertTrue(other.containerMenu == other.inventoryMenu, "le menu proche doit être fermé");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathDropIsRecoverable(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer rescuer = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.HANDLER);
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        player.die(helper.getLevel().damageSources().generic());
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0),
                drop -> drop.tickCount < 20 && drop.getItem().is(ModItems.CARRIED_CHEST));
        helper.assertTrue(drops.size() == 1, "le bloc porté doit être lâché à la mort (" + drops.size() + ")");
        ItemEntity drop = drops.get(0);
        helper.runAfterDelay(3, () -> {
            drop.setNoPickUpDelay();
            drop.playerTouch(rescuer);
            helper.assertTrue(drop.isRemoved(), "l'objet doit être ramassé");
            boolean found = false;
            for (ItemStack stack : rescuer.getInventory().items) {
                found |= data.equals(stack.get(ModDataComponents.CARRIED_CHEST));
            }
            helper.assertTrue(found, "le sauveteur doit récupérer le contenu intact");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void blockSurvivesDimensionChange(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.HANDLER);
        CompoundTag before = contentsOf(helper, SPOT);
        CarriedChestData data = take(helper, player, SPOT);
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "le Nether doit être chargé");
        BlockPos ground = new BlockPos(8, 70, 8);
        nether.getChunk(ground);
        BlockState oldGround = nether.getBlockState(ground);
        BlockState oldAbove = nether.getBlockState(ground.above());
        nether.setBlock(ground, Blocks.STONE.defaultBlockState(), 3);
        nether.setBlock(ground.above(), Blocks.AIR.defaultBlockState(), 3);
        try {
            helper.assertTrue(ChestCarrier.place(player, nether, ground, Direction.UP, data), "la pose dans le Nether aurait dû réussir");
            helper.assertTrue(nether.getBlockEntity(ground.above()).saveWithoutMetadata(nether.registryAccess()).equals(before),
                    "le contenu doit être identique dans l'autre dimension");
        } finally {
            nether.removeBlockEntity(ground.above());
            nether.setBlock(ground.above(), oldAbove, 3);
            nether.setBlock(ground, oldGround, 3);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void mixedChainNeitherLosesNorDuplicates(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        fillAt(helper, SPOT, TestBlocks.STORAGE);
        fillAt(helper, SPOT.south(), TestBlocks.HANDLER);
        CompoundTag storage = contentsOf(helper, SPOT);
        CompoundTag handler = contentsOf(helper, SPOT.south());
        BlockPos a = SPOT;
        BlockPos b = SPOT.south();
        for (int i = 0; i < 10; i++) {
            ServerPlayer actor = i % 2 == 0 ? first : second;
            CarriedChestData dataA = take(helper, actor, a);
            CarriedChestData dataB = take(helper, actor, b);
            helper.assertTrue(dataA != null && dataB != null, "prise " + i + " impossible");
            a = new BlockPos(1 + (i * 3) % 5, 2, 1 + (i * 5) % 5);
            b = a.east(2);
            helper.assertTrue(put(helper, actor, a.below(), dataA) && put(helper, actor, b.below(), dataB), "pose " + i + " impossible");
        }
        int storages = 0;
        int handlers = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 9, 2, 9)) {
            storages += helper.getBlockState(pos).is(TestBlocks.STORAGE) ? 1 : 0;
            handlers += helper.getBlockState(pos).is(TestBlocks.HANDLER) ? 1 : 0;
        }
        helper.assertTrue(storages == 1 && handlers == 1, "un seul bloc de chaque type attendu (" + storages + "/" + handlers + ")");
        helper.assertTrue(contentsOf(helper, a).equals(storage) && contentsOf(helper, b).equals(handler),
                "les contenus doivent rester identiques après 10 allers-retours");
        assertNothingDropped(helper);
        helper.succeed();
    }
}
