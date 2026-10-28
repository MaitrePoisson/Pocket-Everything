package fr.grabmob.gametest;

import fr.grabmob.chest.ChestCarrier;
import fr.grabmob.config.GrabMobConfig;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.event.ChestEvents;
import fr.grabmob.item.CarriedChestItem;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ChestGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);

    private ChestGameTests() {
    }

    private static void fill(ChestBlockEntity chest) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = new ItemStack(slot % 3 == 0 ? Items.DIAMOND_SWORD : Items.COBBLESTONE, slot % 3 == 0 ? 1 : 64 - slot);
            if (slot % 3 == 0) {
                stack.setDamageValue(slot);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal("Lame " + slot));
            }
            chest.setItem(slot, stack);
        }
        CompoundTag named = chest.saveWithoutMetadata(chest.getLevel().registryAccess());
        named.putString("CustomName", "{\"text\":\"Réserve\"}");
        chest.loadWithComponents(named, chest.getLevel().registryAccess());
    }

    private static ChestBlockEntity filledChest(GameTestHelper helper, BlockPos at, BlockState state) {
        helper.setBlock(at, state);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(at);
        fill(chest);
        return chest;
    }

    private static CompoundTag contentsOf(GameTestHelper helper, BlockPos at) {
        return helper.getBlockEntity(at).saveWithoutMetadata(helper.getLevel().registryAccess());
    }

    private static void assertNothingDropped(GameTestHelper helper) {
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4.0));
        helper.assertTrue(drops.isEmpty(), "aucun objet ne devait être lâché au sol (" + drops.size() + ")");
    }

    private static CarriedChestData take(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        return ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(at)).orElse(null);
    }

    private static boolean put(GameTestHelper helper, ServerPlayer player, BlockPos clicked, CarriedChestData data) {
        return ChestCarrier.place(player, helper.getLevel(), helper.absolutePos(clicked), Direction.UP, data);
    }

    private static void roundTrip(GameTestHelper helper, BlockState state) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, state);
        CompoundTag before = contentsOf(helper, SPOT);
        CarriedChestData data = take(helper, player, SPOT);
        helper.assertTrue(data != null, "le coffre aurait dû être pris");
        helper.assertTrue(helper.getBlockState(SPOT).isAir(), "le coffre aurait dû disparaître");
        assertNothingDropped(helper);
        helper.assertTrue(data.blockEntity().equals(before), "le contenu de l'objet doit être celui du coffre");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose aurait dû réussir");
        helper.assertTrue(helper.getBlockState(TARGET).is(state.getBlock()), "le bon bloc aurait dû être posé");
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu doit être identique après la pose");
        helper.assertTrue(helper.getBlockState(TARGET).getValue(ChestBlock.FACING) == player.getDirection().getOpposite(),
                "le coffre doit être orienté vers le joueur");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void simpleChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.CHEST.defaultBlockState());
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void trappedChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.TRAPPED_CHEST.defaultBlockState());
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void orientedChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void doubleChestHalfBecomesSingle(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos left = SPOT;
        BlockPos right = SPOT.east();
        BlockState base = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        filledChest(helper, left, base.setValue(ChestBlock.TYPE, ChestType.LEFT));
        filledChest(helper, right, base.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        CompoundTag leftBefore = contentsOf(helper, left);
        CompoundTag rightBefore = contentsOf(helper, right);
        CarriedChestData data = take(helper, player, left);
        helper.assertTrue(data != null, "la moitié aurait dû être prise");
        helper.assertTrue(data.state().getValue(ChestBlock.TYPE) == ChestType.SINGLE, "l'objet doit porter un coffre simple");
        helper.assertTrue(helper.getBlockState(left).isAir(), "la moitié prise doit disparaître");
        helper.assertTrue(helper.getBlockState(right).getValue(ChestBlock.TYPE) == ChestType.SINGLE,
                "l'autre moitié doit redevenir simple");
        helper.assertTrue(contentsOf(helper, right).equals(rightBefore), "l'autre moitié doit garder son contenu");
        assertNothingDropped(helper);
        helper.assertTrue(data.blockEntity().equals(leftBefore), "l'objet doit porter le contenu de la moitié prise");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose aurait dû réussir");
        helper.assertTrue(contentsOf(helper, TARGET).equals(leftBefore), "le contenu posé doit être identique");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void underwaterChestKeepsWater(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.setBlock(TARGET, Blocks.WATER.defaultBlockState());
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, true));
        CompoundTag before = contentsOf(helper, SPOT);
        CarriedChestData data = take(helper, player, SPOT);
        helper.assertTrue(data != null, "le coffre immergé aurait dû être pris");
        helper.assertTrue(helper.getBlockState(SPOT).getFluidState().isSource() && helper.getBlockState(SPOT).is(Blocks.WATER),
                "l'eau doit remplacer le coffre");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose dans l'eau aurait dû réussir");
        BlockState placed = helper.getBlockState(TARGET);
        helper.assertTrue(placed.getValue(ChestBlock.WATERLOGGED) && placed.getFluidState().getType() == Fluids.WATER,
                "le coffre posé dans l'eau doit être immergé");
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu doit être identique");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void lootTableAndLockSurviveTheTrip(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.setBlock(SPOT, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(SPOT);
        chest.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                ResourceLocation.withDefaultNamespace("chests/simple_dungeon")), 1234L);
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(before.contains("LootTable") && before.getLong("LootTableSeed") == 1234L, "table de butin attendue");
        CarriedChestData data = take(helper, player, SPOT);
        helper.assertTrue(data != null, "le coffre à butin aurait dû être pris");
        helper.assertTrue(data.blockEntity().equals(before), "la table de butin doit rester non résolue");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose aurait dû réussir");
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "la table de butin doit être conservée");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void lockedChestIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(SPOT);
        CompoundTag locked = chest.saveWithoutMetadata(helper.getLevel().registryAccess());
        locked.putString("Lock", "clef");
        chest.loadWithComponents(locked, helper.getLevel().registryAccess());
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un coffre verrouillé ne doit pas être pris");
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST) && contentsOf(helper, SPOT).equals(before),
                "le coffre verrouillé doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void occupiedPlaceKeepsTheChestData(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag before = contentsOf(helper, SPOT);
        CarriedChestData data = take(helper, player, SPOT);
        helper.setBlock(TARGET, Blocks.STONE.defaultBlockState());
        helper.assertTrue(!put(helper, player, TARGET.below(), data), "la pose sur un bloc plein doit être refusée");
        helper.assertTrue(helper.getBlockState(TARGET).is(Blocks.STONE), "le bloc existant doit rester");
        helper.assertTrue(data.blockEntity().equals(before), "les données doivent rester intactes");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void viewersAreKickedOutOnPickup(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer viewer = helper.makeMockServerPlayerInLevel();
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        viewer.openMenu(chest);
        helper.assertTrue(viewer.containerMenu instanceof ChestMenu, "le menu aurait dû s'ouvrir");
        helper.assertTrue(take(helper, player, SPOT) != null, "la prise aurait dû réussir");
        helper.assertTrue(viewer.containerMenu == viewer.inventoryMenu, "le menu de l'autre joueur doit être fermé");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void itemPlacesTheChestAndIsConsumed(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag before = contentsOf(helper, SPOT);
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST.get());
        stack.set(ModDataComponents.CARRIED_CHEST.get(), take(helper, player, SPOT));
        helper.assertTrue(stack.getMaxStackSize() == 1, "l'objet doit être non empilable");
        BlockPos ground = helper.absolutePos(TARGET.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
        InteractionResult result = stack.getItem()
                .useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result.consumesAction(), "l'utilisation sur un bloc devrait réussir");
        helper.assertTrue(stack.isEmpty(), "l'objet aurait dû être consommé");
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu doit être identique");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void itemWithoutComponentIsHarmless(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST.get());
        BlockPos ground = helper.absolutePos(TARGET.below());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
        InteractionResult result = stack.getItem()
                .useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
        helper.assertTrue(result == InteractionResult.PASS, "sans composant, useOn doit passer");
        helper.assertTrue(stack.getCount() == 1 && helper.getBlockState(TARGET).isAir(), "rien ne doit être posé ni consommé");
        helper.succeed();
    }

    private static BlockHitResult hitOn(GameTestHelper helper, BlockPos at) {
        BlockPos abs = helper.absolutePos(at);
        return new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
    }

    private static InteractionResult click(GameTestHelper helper, ServerPlayer player, InteractionHand hand, BlockPos at) {
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hitOn(helper, at));
    }

    private static ItemStack carried(CarriedChestData data) {
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST.get());
        stack.set(ModDataComponents.CARRIED_CHEST.get(), data);
        return stack;
    }

    private static void assertCarrying(GameTestHelper helper, ServerPlayer player, boolean expected) {
        helper.assertTrue(player.getMainHandItem().is(ModItems.CARRIED_CHEST.get()) == expected,
                expected ? "le coffre aurait dû être en main" : "aucun coffre ne devait être en main");
    }

    private static void takeAndPlaceEndToEnd(GameTestHelper helper, GameType mode) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(mode);
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag before = contentsOf(helper, SPOT);
        player.setShiftKeyDown(true);
        helper.assertTrue(click(helper, player, InteractionHand.MAIN_HAND, SPOT).consumesAction(), "le geste aurait dû être consommé");
        helper.assertTrue(helper.getBlockState(SPOT).isAir(), "le coffre aurait dû disparaître");
        helper.assertTrue(player.containerMenu == player.inventoryMenu, "le coffre ne doit pas s'ouvrir");
        assertCarrying(helper, player, true);
        helper.assertTrue(player.getMainHandItem().get(ModDataComponents.CARRIED_CHEST.get()).blockEntity().equals(before),
                "le contenu porté doit être identique");
        assertNothingDropped(helper);
        click(helper, player, InteractionHand.MAIN_HAND, TARGET.below());
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu posé doit être identique");
        assertCarrying(helper, player, false);
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void gestureTakesAndPlacesInCreative(GameTestHelper helper) {
        takeAndPlaceEndToEnd(helper, GameType.CREATIVE);
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void gestureTakesAndPlacesInSurvival(GameTestHelper helper) {
        takeAndPlaceEndToEnd(helper, GameType.SURVIVAL);
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void adventureModeCannotTake(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.ADVENTURE);
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        player.setShiftKeyDown(true);
        click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST), "le coffre doit rester en mode aventure");
        assertCarrying(helper, player, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void gestureNeedsSneakAndFreeHands(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST), "sans sneak le coffre doit rester");
        assertCarrying(helper, player, false);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST), "main pleine : le coffre doit rester");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STICK));
        CompoundTag before = contentsOf(helper, SPOT);
        click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST) && contentsOf(helper, SPOT).equals(before),
                "main secondaire pleine : le coffre doit rester intact");
        assertCarrying(helper, player, false);
        helper.assertTrue(player.getOffhandItem().is(Items.STICK), "l'objet de la main secondaire doit rester");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void configDisablesTheGesture(GameTestHelper helper) {
        if (!GrabMobConfig.SPEC.isLoaded()) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        player.setShiftKeyDown(true);
        GrabMobConfig.CARRY_CHESTS_ENABLED.set(false);
        try {
            click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        } finally {
            GrabMobConfig.CARRY_CHESTS_ENABLED.set(true);
        }
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST), "config désactivée : le coffre doit rester");
        assertCarrying(helper, player, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void offHandIsBlockedWhileCarrying(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(NeoForge.EVENT_BUS.post(new LivingSwapItemsEvent.Hands(player)).isCanceled(),
                "l'échange des mains doit être annulé");
        helper.assertTrue(!click(helper, player, InteractionHand.OFF_HAND, TARGET.below()).consumesAction()
                && helper.getBlockState(TARGET).isAir(), "la pose depuis la main secondaire doit être refusée");
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertTrue(player.getOffhandItem().isEmpty(), "la main secondaire doit être vidée");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.COBBLESTONE)), "l'objet doit retourner à l'inventaire");
        assertCarrying(helper, player, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void nestedCarriedChestIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT.east(2), Blocks.CHEST.defaultBlockState());
        CarriedChestData inner = take(helper, player, SPOT.east(2));
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        chest.setItem(0, carried(inner));
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un coffre contenant un coffre porté ne doit pas être pris");
        helper.assertTrue(contentsOf(helper, SPOT).equals(before), "le coffre doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void droppedChestIsIndestructible(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 3, 3.5));
        ItemEntity drop = new ItemEntity(helper.getLevel(), at.x, at.y, at.z, carried(data));
        helper.getLevel().addFreshEntity(drop);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(drop.isInvulnerable() && !drop.hurt(helper.getLevel().damageSources().explosion(null), 100.0F),
                    "l'objet lâché doit être invulnérable");
            helper.assertTrue(drop.getAge() < 0, "l'objet lâché ne doit pas disparaître avec le temps");
            helper.assertTrue(!drop.isRemoved() && data.equals(drop.getItem().get(ModDataComponents.CARRIED_CHEST.get())),
                    "le contenu doit rester intact");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void nameAndTooltipDescribeTheChest(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        ItemStack named = carried(take(helper, player, SPOT));
        helper.assertTrue(named.getHoverName().getContents() instanceof TranslatableContents key
                && CarriedChestItem.KEY_NAMED.equals(key.getKey()), "le nom personnalisé doit apparaître");
        List<Component> tooltip = new ArrayList<>();
        named.getItem().appendHoverText(named, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        helper.assertTrue(tooltip.size() == 1 && tooltip.get(0).getContents() instanceof TranslatableContents line
                && CarriedChestItem.KEY_CONTENTS.equals(line.getKey()) && line.getArgs()[0].equals(27),
                "l'infobulle doit compter les 27 piles");
        helper.setBlock(SPOT, Blocks.CHEST.defaultBlockState());
        ItemStack plain = carried(take(helper, player, SPOT));
        helper.assertTrue(plain.getHoverName().getContents() instanceof TranslatableContents key
                && CarriedChestItem.KEY_NAME.equals(key.getKey()), "un coffre ordinaire garde le nom par défaut");
        helper.assertTrue(!ChestEvents.isCarrying(player), "rien en main");
        helper.succeed();
    }
}
