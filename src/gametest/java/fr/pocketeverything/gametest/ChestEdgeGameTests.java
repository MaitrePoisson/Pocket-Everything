package fr.pocketeverything.gametest;

import fr.pocketeverything.chest.ChestCarrier;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ChestEdgeGameTests {

    private static final String TEMPLATE = "pocket_everything:empty";
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);

    public ChestEdgeGameTests() {
    }

    private static ChestBlockEntity filledChest(GameTestHelper helper, BlockPos at, BlockState state) {
        helper.setBlock(at, state);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(at);
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(slot % 2 == 0 ? Items.COBBLESTONE : Items.DIAMOND, 1 + slot));
        }
        return chest;
    }

    private static CompoundTag contentsOf(GameTestHelper helper, BlockPos at) {
        return helper.getBlockEntity(at).saveWithoutMetadata(helper.getLevel().registryAccess());
    }

    private static ItemStack carried(CarriedChestData data) {
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST);
        stack.set(ModDataComponents.CARRIED_CHEST, data);
        return stack;
    }

    private static int carriedCount(ServerPlayer player) {
        int count = 0;
        for (List<ItemStack> list : List.of(player.getInventory().items, player.getInventory().offhand, player.getInventory().armor)) {
            for (ItemStack stack : list) {
                count += stack.is(ModItems.CARRIED_CHEST) ? 1 : 0;
            }
        }
        return count + (player.containerMenu.getCarried().is(ModItems.CARRIED_CHEST) ? 1 : 0);
    }

    private static int total(CompoundTag chest, net.minecraft.world.item.Item item) {
        int sum = 0;
        for (net.minecraft.nbt.Tag tag : chest.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) tag;
            if (entry.getString("id").equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString())) {
                sum += entry.getInt("count");
            }
        }
        return sum;
    }

    private static void assertNothingDropped(GameTestHelper helper) {
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4.0));
        helper.assertTrue(drops.isEmpty(), "aucun objet ne devait être lâché au sol (" + drops.size() + ")");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void trappedChestWithComparatorAndHopperKeepsEverything(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.TRAPPED_CHEST.defaultBlockState());
        helper.setBlock(SPOT.east(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));
        helper.setBlock(SPOT.west(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getBlockEntity(SPOT.west());
        hopper.setItem(0, new ItemStack(Items.COBBLESTONE, 30));
        viewer.openMenu(chest);
        helper.runAfterDelay(12, () -> {
            ComparatorBlockEntity comparator = (ComparatorBlockEntity) helper.getBlockEntity(SPOT.east());
            helper.assertTrue(comparator.getOutputSignal() > 0, "le comparateur doit lire le coffre piégé (" + comparator.getOutputSignal() + ")");
            CompoundTag before = contentsOf(helper, SPOT);
            int hopperBefore = hopper.getItem(0).getCount();
            int cobbleBefore = total(before, Items.COBBLESTONE);
            CarriedChestData data = ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(SPOT)).orElse(null);
            helper.assertTrue(data != null, "la prise du coffre piégé aurait dû réussir");
            helper.assertTrue(data.blockEntity().equals(before), "le contenu pris doit être celui du coffre");
            helper.assertTrue(viewer.containerMenu == viewer.inventoryMenu, "le spectateur doit être expulsé");
            assertNothingDropped(helper);
            helper.runAfterDelay(20, () -> {
                helper.assertTrue(comparator.getOutputSignal() == 0, "le comparateur doit retomber à 0 (" + comparator.getOutputSignal() + ")");
                helper.assertTrue(hopper.getItem(0).getCount() == hopperBefore, "l'entonnoir ne doit rien perdre ni voler");
                helper.assertTrue(!(helper.getLevel().getBlockEntity(helper.absolutePos(SPOT)) instanceof ChestBlockEntity),
                        "plus aucun coffre à l'ancien emplacement");
                helper.assertTrue(ChestCarrier.place(player, helper.getLevel(), helper.absolutePos(TARGET.below()), Direction.UP, data),
                        "la pose aurait dû réussir");
                helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu posé doit être identique");
                helper.assertTrue(total(contentsOf(helper, TARGET), Items.COBBLESTONE) == cobbleBefore, "total identique");
                assertNothingDropped(helper);
                helper.succeed();
            });
        });
    }

    @GameTest(template = TEMPLATE)
    public static void doubleChestViewerIsClosedAndCursorIsKept(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        BlockState base = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        filledChest(helper, SPOT, base.setValue(ChestBlock.TYPE, ChestType.LEFT));
        filledChest(helper, SPOT.east(), base.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        CompoundTag rightBefore = contentsOf(helper, SPOT.east());
        viewer.openMenu(helper.getBlockState(SPOT).getMenuProvider(helper.getLevel(), helper.absolutePos(SPOT)));
        helper.assertTrue(viewer.containerMenu instanceof ChestMenu menu && menu.getRowCount() == 6, "le menu double aurait dû s'ouvrir");
        viewer.containerMenu.setCarried(new ItemStack(Items.EMERALD, 5));
        helper.assertTrue(ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(SPOT)).isPresent(), "la prise aurait dû réussir");
        helper.assertTrue(viewer.containerMenu == viewer.inventoryMenu, "le menu double doit être fermé");
        int emeralds = 0;
        for (ItemStack stack : viewer.getInventory().items) {
            emeralds += stack.is(Items.EMERALD) ? stack.getCount() : 0;
        }
        helper.assertTrue(emeralds == 5, "l'objet du curseur doit revenir une seule fois (" + emeralds + ")");
        helper.assertTrue(contentsOf(helper, SPOT.east()).equals(rightBefore), "l'autre moitié doit rester intacte");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clickingTheCarriedChestNeverDuplicatesIt(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        ChestBlockEntity target = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity other = filledChest(helper, SPOT.east(3), Blocks.CHEST.defaultBlockState());
        for (int slot = 0; slot < other.getContainerSize(); slot++) {
            other.setItem(slot, ItemStack.EMPTY);
        }
        CarriedChestData data = ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(SPOT)).orElse(null);
        helper.assertTrue(data != null, "prise");
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        player.openMenu(other);
        ChestMenu menu = (ChestMenu) player.containerMenu;
        int hand = -1;
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() == player.getInventory().selected) {
                hand = slot.index;
            }
        }
        helper.assertTrue(hand >= 0, "case de la main introuvable");
        menu.clicked(hand, 0, ClickType.PICKUP, player);
        helper.assertTrue(carriedCount(player) == 1, "après ramassage au curseur : un seul coffre (" + carriedCount(player) + ")");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertTrue(carriedCount(player) == 0, "déposé dans le coffre : plus rien chez le joueur (" + carriedCount(player) + ")");
        helper.assertTrue(other.getItem(0).is(ModItems.CARRIED_CHEST), "le coffre porté doit être dans le coffre");
        menu.clicked(0, 0, ClickType.QUICK_MOVE, player);
        helper.assertTrue(carriedCount(player) == 1 && other.getItem(0).isEmpty(), "le shift-clic ne doit pas dupliquer");
        player.closeContainer();
        helper.assertTrue(carriedCount(player) == 1, "un seul coffre au total (" + carriedCount(player) + ")");
        helper.assertTrue(target.getLevel() != null && helper.getBlockState(SPOT).isAir(), "l'ancien emplacement reste vide");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void placingNextToAChestMergesWithoutLosingContents(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag taken = contentsOf(helper, SPOT);
        BlockState facing = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, player.getDirection().getOpposite());
        filledChest(helper, TARGET, facing);
        ((ChestBlockEntity) helper.getBlockEntity(TARGET)).setItem(0, new ItemStack(Items.EMERALD, 3));
        CompoundTag existing = contentsOf(helper, TARGET);
        CarriedChestData data = ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(SPOT)).orElse(null);
        helper.assertTrue(data != null, "prise");
        Direction side = player.getDirection().getClockWise();
        BlockPos beside = TARGET.relative(side);
        helper.assertTrue(ChestCarrier.place(player, helper.getLevel(), helper.absolutePos(beside.below()), Direction.UP, data),
                "la pose à côté aurait dû réussir");
        System.out.println("POCKET_EVERYTHING_DOUBLE types=" + helper.getBlockState(TARGET).getValue(ChestBlock.TYPE) + "/"
                + helper.getBlockState(beside).getValue(ChestBlock.TYPE));
        helper.assertTrue(contentsOf(helper, beside).equals(taken), "le coffre posé doit garder son contenu");
        helper.assertTrue(contentsOf(helper, TARGET).equals(existing), "le coffre voisin doit garder son contenu");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void placingBesideAnExistingChestKeepsBothContents(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag taken = contentsOf(helper, SPOT);
        filledChest(helper, TARGET, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) helper.getBlockEntity(TARGET)).setItem(0, new ItemStack(Items.EMERALD, 3));
        CompoundTag existing = contentsOf(helper, TARGET);
        CarriedChestData data = ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(SPOT)).orElse(null);
        helper.assertTrue(data != null, "prise");
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        BlockPos abs = helper.absolutePos(TARGET);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0.5, 0, 0), Direction.EAST, abs, false);
        player.setShiftKeyDown(true);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        BlockPos side = TARGET.east();
        helper.assertTrue(helper.getBlockState(side).is(Blocks.CHEST), "le coffre aurait dû être posé à côté");
        helper.assertTrue(contentsOf(helper, side).equals(taken), "le coffre posé doit garder son contenu");
        helper.assertTrue(contentsOf(helper, TARGET).equals(existing), "le coffre voisin doit garder son contenu");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "le coffre porté doit être consommé");
        assertNothingDropped(helper);
        helper.succeed();
    }
}
