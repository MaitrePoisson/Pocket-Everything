package fr.pocketeverything.gametest;

import fr.pocketeverything.chest.ChestCarrier;
import fr.pocketeverything.config.PocketEverythingConfig;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.event.ChestEvents;
import fr.pocketeverything.item.CarriedChestItem;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class ChestGameTests {

    private static final String TEMPLATE = "pocket_everything:empty";
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);
    private static final BlockPos TARGET = new BlockPos(5, 2, 5);

    public ChestGameTests() {
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

    static void assertNothingDropped(GameTestHelper helper) {
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds().inflate(4.0));
        helper.assertTrue(drops.isEmpty(), "aucun objet ne devait être lâché au sol (" + drops.size() + ")");
    }

    static CarriedChestData take(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        return ChestCarrier.pickUp(player, helper.getLevel(), helper.absolutePos(at)).orElse(null);
    }

    static boolean put(GameTestHelper helper, ServerPlayer player, BlockPos clicked, CarriedChestData data) {
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

    @GameTest(template = TEMPLATE)
    public static void simpleChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.CHEST.defaultBlockState());
    }

    @GameTest(template = TEMPLATE)
    public static void trappedChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.TRAPPED_CHEST.defaultBlockState());
    }

    @GameTest(template = TEMPLATE)
    public static void orientedChestRoundTrip(GameTestHelper helper) {
        roundTrip(helper, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST));
    }

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
    public static void itemPlacesTheChestAndIsConsumed(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag before = contentsOf(helper, SPOT);
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST);
        stack.set(ModDataComponents.CARRIED_CHEST, take(helper, player, SPOT));
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

    @GameTest(template = TEMPLATE)
    public static void itemWithoutComponentIsHarmless(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST);
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

    static InteractionResult click(GameTestHelper helper, ServerPlayer player, InteractionHand hand, BlockPos at) {
        return player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hitOn(helper, at));
    }

    static ItemStack carried(CarriedChestData data) {
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST);
        stack.set(ModDataComponents.CARRIED_CHEST, data);
        return stack;
    }

    static void assertCarrying(GameTestHelper helper, ServerPlayer player, boolean expected) {
        helper.assertTrue(player.getMainHandItem().is(ModItems.CARRIED_CHEST) == expected,
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
        helper.assertTrue(player.getMainHandItem().get(ModDataComponents.CARRIED_CHEST).blockEntity().equals(before),
                "le contenu porté doit être identique");
        assertNothingDropped(helper);
        click(helper, player, InteractionHand.MAIN_HAND, TARGET.below());
        helper.assertTrue(contentsOf(helper, TARGET).equals(before), "le contenu posé doit être identique");
        assertCarrying(helper, player, false);
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void gestureTakesAndPlacesInCreative(GameTestHelper helper) {
        takeAndPlaceEndToEnd(helper, GameType.CREATIVE);
    }

    @GameTest(template = TEMPLATE)
    public static void gestureTakesAndPlacesInSurvival(GameTestHelper helper) {
        takeAndPlaceEndToEnd(helper, GameType.SURVIVAL);
    }

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
    public static void configDisablesTheGesture(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        player.setShiftKeyDown(true);
        PocketEverythingConfig.set(new PocketEverythingConfig.Values(true, Set.of(), false));
        try {
            click(helper, player, InteractionHand.MAIN_HAND, SPOT);
        } finally {
            PocketEverythingConfig.set(PocketEverythingConfig.DEFAULTS);
        }
        helper.assertTrue(helper.getBlockState(SPOT).is(Blocks.CHEST), "config désactivée : le coffre doit rester");
        assertCarrying(helper, player, false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void offHandIsBlockedWhileCarrying(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 5));
        helper.assertTrue(ChestEvents.cancelsSwapHands(player),
                "l'échange des mains doit être annulé");
        helper.assertTrue(!click(helper, player, InteractionHand.OFF_HAND, TARGET.below()).consumesAction()
                && helper.getBlockState(TARGET).isAir(), "la pose depuis la main secondaire doit être refusée");
        ChestEvents.onPlayerTick(player);
        helper.assertTrue(player.getOffhandItem().isEmpty(), "la main secondaire doit être vidée");
        helper.assertTrue(player.getInventory().contains(new ItemStack(Items.COBBLESTONE)), "l'objet doit retourner à l'inventaire");
        assertCarrying(helper, player, true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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
            helper.assertTrue(!drop.isRemoved() && data.equals(drop.getItem().get(ModDataComponents.CARRIED_CHEST)),
                    "le contenu doit rester intact");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
    public static void playerInventoryNbtRoundTrip(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        ListTag saved = player.getInventory().save(new ListTag());
        Inventory reloaded = new Inventory(player);
        reloaded.load(saved);
        ItemStack back = reloaded.getItem(player.getInventory().selected);
        helper.assertTrue(back.is(ModItems.CARRIED_CHEST), "l'objet doit revenir à la même case après rechargement");
        helper.assertTrue(data.equals(back.get(ModDataComponents.CARRIED_CHEST)),
                "le contenu doit survivre à la sauvegarde NBT du joueur");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void deathDropIsRecoverableByAnotherPlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ServerPlayer rescuer = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        player.die(helper.getLevel().damageSources().generic());
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(8.0),
                drop -> drop.tickCount < 20 && drop.getItem().is(ModItems.CARRIED_CHEST));
        helper.assertTrue(drops.size() == 1, "le coffre porté doit être lâché à la mort (" + drops.size() + ")");
        ItemEntity drop = drops.get(0);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(drop.isInvulnerable() && drop.getAge() < 0 && !drop.isRemoved(), "l'objet lâché doit être protégé");
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
    public static void chestSurvivesDimensionChange(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
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
    public static void runsOnDedicatedServerWithoutClientClasses(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER, "ce test doit tourner sur un serveur dédié");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        ItemStack stack = carried(take(helper, player, SPOT));
        helper.assertTrue(stack.getHoverName() != null, "le nom doit fonctionner sans classe client");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void vanillaSavedChestIsTakenWithoutMigration(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.setBlock(SPOT, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = (ChestBlockEntity) helper.getBlockEntity(SPOT);
        CompoundTag saved = new CompoundTag();
        ListTag items = new ListTag();
        CompoundTag diamond = new CompoundTag();
        diamond.putByte("Slot", (byte) 4);
        diamond.putString("id", "minecraft:diamond");
        diamond.putInt("count", 7);
        items.add(diamond);
        saved.put("Items", items);
        chest.loadWithComponents(saved, helper.getLevel().registryAccess());
        CarriedChestData data = take(helper, player, SPOT);
        helper.assertTrue(data != null, "un coffre issu d'un monde existant doit pouvoir être pris");
        helper.assertTrue(put(helper, player, TARGET.below(), data), "la pose aurait dû réussir");
        ChestBlockEntity placed = (ChestBlockEntity) helper.getBlockEntity(TARGET);
        helper.assertTrue(placed.getItem(4).is(Items.DIAMOND) && placed.getItem(4).getCount() == 7, "le contenu doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repeatedChainsNeitherLoseNorDuplicate(GameTestHelper helper) {
        ServerPlayer first = helper.makeMockServerPlayerInLevel();
        ServerPlayer second = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag before = contentsOf(helper, SPOT);
        BlockPos current = SPOT;
        for (int i = 0; i < 12; i++) {
            ServerPlayer actor = i % 2 == 0 ? first : second;
            CarriedChestData data = take(helper, actor, current);
            helper.assertTrue(data != null, "prise " + i + " impossible");
            helper.assertTrue(helper.getBlockState(current).isAir(), "le bloc doit disparaître à la prise " + i);
            BlockPos next = new BlockPos(1 + (i * 3) % 7, 2, 1 + (i * 5) % 7);
            if (next.equals(current)) {
                next = next.east();
            }
            helper.assertTrue(put(helper, actor, next.below(), data), "pose " + i + " impossible");
            current = next;
        }
        int chests = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 2, 0, 9, 2, 9)) {
            chests += helper.getBlockState(pos).is(Blocks.CHEST) ? 1 : 0;
        }
        helper.assertTrue(chests == 1, "un seul coffre doit exister (" + chests + ")");
        helper.assertTrue(contentsOf(helper, current).equals(before), "le contenu doit rester identique après 12 allers-retours");
        assertNothingDropped(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bulkyChestFitsTheNetworkBudget(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            List<ItemStack> inside = new ArrayList<>();
            for (int i = 0; i < 27; i++) {
                ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
                sword.set(DataComponents.CUSTOM_NAME, Component.literal("Lame " + slot + "-" + i));
                sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Forgée à la main"), Component.literal("Numéro " + i))));
                inside.add(sword);
            }
            ItemStack box = new ItemStack(Items.SHULKER_BOX);
            box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(inside));
            chest.setItem(slot, box);
        }
        ItemStack stack = carried(take(helper, player, SPOT));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, stack);
        int size = buffer.readableBytes();
        System.out.println("POCKET_EVERYTHING_NETWORK_SIZE carriedChest bytes=" + size);
        helper.assertTrue(size < 2_000_000, "coffre trop lourd pour le réseau : " + size);
        ItemStack decoded = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        helper.assertTrue(stack.get(ModDataComponents.CARRIED_CHEST).equals(decoded.get(ModDataComponents.CARRIED_CHEST)),
                "le décodage réseau doit redonner le même contenu");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void nestedChestInsideShulkerIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT.east(2), Blocks.CHEST.defaultBlockState());
        CarriedChestData inner = take(helper, player, SPOT.east(2));
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(carried(inner))));
        chest.setItem(0, box);
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un coffre porté caché dans un shulker doit être refusé");
        helper.assertTrue(contentsOf(helper, SPOT).equals(before), "le coffre doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void oversizedChestIsRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ChestBlockEntity chest = filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CompoundTag big = new CompoundTag();
        big.putString("x", "a".repeat(50_000));
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = new ItemStack(Items.STICK);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(big.copy()));
            chest.setItem(slot, stack);
        }
        CompoundTag before = contentsOf(helper, SPOT);
        helper.assertTrue(take(helper, player, SPOT) == null, "un coffre trop volumineux doit être refusé");
        helper.assertTrue(contentsOf(helper, SPOT).equals(before), "le coffre doit rester intact");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void swapHandsPacketIsCancelledWhileCarrying(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        CarriedChestData data = take(helper, player, SPOT);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried(data));
        helper.assertTrue(player.connection != null, "le joueur de test doit avoir une connexion");
        player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        helper.assertTrue(player.getOffhandItem().isEmpty(), "le coffre ne doit pas passer en main secondaire");
        assertCarrying(helper, player, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE));
        player.connection.handlePlayerAction(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
        helper.assertTrue(player.getOffhandItem().is(Items.COBBLESTONE), "l'échange normal doit rester possible");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void carriedChestInOffHandGoesBackToInventory(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        filledChest(helper, SPOT, Blocks.CHEST.defaultBlockState());
        player.setItemInHand(InteractionHand.OFF_HAND, carried(take(helper, player, SPOT)));
        ChestEvents.onPlayerTick(player);
        helper.assertTrue(player.getOffhandItem().isEmpty(), "la main secondaire doit être vidée");
        helper.assertTrue(player.getInventory().hasAnyMatching(s -> s.is(ModItems.CARRIED_CHEST)), "le coffre doit rester dans l'inventaire");
        helper.succeed();
    }
}
