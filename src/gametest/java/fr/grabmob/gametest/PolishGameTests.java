package fr.grabmob.gametest;

import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.config.GrabMobConfig;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.event.ModEvents;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class PolishGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";

    private PolishGameTests() {
    }

    private static ItemStack stackOf(CapturedEntityData data) {
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        stack.set(ModDataComponents.CAPTURED_ENTITY.get(), data);
        return stack;
    }

    private static InteractionResult useOnUnderside(GameTestHelper helper, ServerPlayer player, ItemStack stack,
            BlockPos relative) {
        BlockPos block = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(block), Direction.DOWN, block, false);
        return stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void creativeFullInventoryDropsTheCapturedItem(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 1));
        }
        player.getInventory().items.set(0, new ItemStack(Items.STICK, 1));
        player.getInventory().selected = 0;
        helper.assertTrue(ModEvents.tryCapture(player, cow), "la capture doit réussir");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(3.0), item -> item.getItem().is(ModItems.CAPTURED_ENTITY.get())
                        && item.position().distanceToSqr(player.getEyePosition()) < 1.0);
        helper.assertTrue(drops.size() == 1, "l'item doit être lâché au sol, jamais détruit");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void releaseUnderBlockNeverSuffocates(GameTestHelper helper) {
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, golem).orElse(null);
        helper.assertTrue(data != null, "le golem doit être capturé");
        int floor = 0;
        while (!helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(3, floor, 3))).isAir()) {
            floor++;
        }
        BlockPos low = new BlockPos(3, floor + 2, 3);
        BlockPos high = new BlockPos(3, floor + 5, 3);
        helper.setBlock(low, Blocks.STONE);
        ItemStack tight = stackOf(data);
        InteractionResult refused = useOnUnderside(helper, player, tight, low);
        helper.assertTrue(refused == InteractionResult.FAIL && !tight.isEmpty(),
                "sous un plafond trop bas, l'échec doit garder l'item");
        helper.setBlock(low, Blocks.AIR);
        helper.setBlock(high, Blocks.STONE);
        ItemStack roomy = stackOf(data);
        InteractionResult accepted = useOnUnderside(helper, player, roomy, high);
        helper.assertTrue(accepted.consumesAction(), "assez de place : le relâchement doit réussir");
        List<IronGolem> golems = helper.getLevel().getEntitiesOfClass(IronGolem.class,
                helper.getBounds().inflate(2.0));
        helper.assertTrue(golems.size() == 1 && helper.getLevel().noCollision(golems.get(0)),
                "le golem doit être hors des blocs");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void transientCombatStateIsNotCarried(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        cow.fallDistance = 40.0F;
        cow.setRemainingFireTicks(200);
        cow.hurtTime = 10;
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, cow).orElse(null);
        helper.assertTrue(data != null, "la vache doit être capturée");
        Entity back = EntityCapturer.release(helper.getLevel(), data,
                helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), 0.0F).orElse(null);
        helper.assertTrue(back != null && back.fallDistance == 0.0F && !back.isOnFire(),
                "chute et feu ne doivent pas être conservés");
        CompoundTag tag = data.tag();
        helper.assertTrue(!tag.contains("FallDistance") && !tag.contains("HurtTime"), "clés transitoires retirées");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void othersTamedPetCannotBeCaptured(GameTestHelper helper) {
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, 2, 1, 2);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        wolf.tame(owner);
        ServerPlayer thief = helper.makeMockServerPlayerInLevel();
        helper.assertFalse(EntityCapturer.capture(thief, wolf).isPresent(), "le loup d'un autre est protégé");
        helper.assertFalse(wolf.isRemoved(), "le loup doit rester");
        helper.assertTrue(EntityCapturer.capture(owner, wolf).isPresent(), "le propriétaire peut le capturer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void nonLivingEntitiesAreRefused(GameTestHelper helper) {
        for (EntityType<?> type : List.of(EntityType.ITEM, EntityType.TNT, EntityType.EXPERIENCE_ORB,
                EntityType.END_CRYSTAL, EntityType.MARKER, EntityType.TEXT_DISPLAY)) {
            Entity entity = type.create(helper.getLevel());
            helper.assertTrue(entity != null && !EntityCapturer.canCapture(entity), type + " doit être refusé");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void adventureModePlayerCannotCapture(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.ADVENTURE);
        player.setShiftKeyDown(true);
        helper.assertFalse(ModEvents.isCaptureGesture(player, InteractionHand.MAIN_HAND, cow),
                "le mode Aventure ne doit pas capturer");
        helper.succeed();
    }

    private static InteractionResult useOnGround(GameTestHelper helper, ServerPlayer player, ItemStack stack,
            BlockPos relative) {
        BlockPos block = helper.absolutePos(relative);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(block), Direction.UP, block, false);
        return stack.getItem().useOn(new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit));
    }

    private static <T extends Entity> List<T> releasedAround(GameTestHelper helper, Class<T> type, Entity original) {
        Vec3 center = helper.absoluteVec(new Vec3(3.5, 2.0, 3.5));
        return helper.getLevel().getEntitiesOfClass(type, new AABB(center, center).inflate(0.6, 1.0, 0.6),
                entity -> entity != original);
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void villagerBabyAndGolemReleaseOnPlainGround(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Villager adult = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 2, 1, 2);
        Villager baby = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 5, 1, 5);
        baby.setAge(-24000);
        IronGolem golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, 5, 1, 2);
        Entity[] originals = {adult, baby, golem};
        for (Entity original : originals) {
            ItemStack stack = stackOf(EntityCapturer.capture(player, original).orElse(null));
            InteractionResult result = useOnGround(helper, player, stack, new BlockPos(3, 1, 3));
            helper.assertTrue(result.consumesAction() && stack.isEmpty(), original.getType() + " : relâche refusée");
            List<? extends Entity> back = releasedAround(helper, original.getClass(), original);
            helper.assertTrue(back.size() == 1 && helper.getLevel().noCollision(back.get(0)),
                    original.getType() + " : une seule entité libre attendue");
            boolean isBaby = back.get(0) instanceof Villager villager && villager.isBaby();
            helper.assertTrue(isBaby == (original == baby), original.getType() + " : âge incorrect");
            back.get(0).discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void releaseInLowCeilingFailsAndKeepsItem(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, cow).orElse(null);
        helper.assertTrue(data != null, "la vache doit être capturée");
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.STONE);
        ItemStack tight = stackOf(data);
        InteractionResult refused = useOnGround(helper, player, tight, new BlockPos(3, 1, 3));
        helper.assertTrue(refused == InteractionResult.FAIL && tight.getCount() == 1,
                "plafond à 1 bloc : échec et item gardé");
        helper.assertTrue(releasedAround(helper, Cow.class, cow).isEmpty(), "aucune vache ne doit apparaître");
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.AIR);
        helper.setBlock(new BlockPos(3, 4, 3), Blocks.STONE);
        ItemStack roomy = stackOf(data);
        helper.assertTrue(useOnGround(helper, player, roomy, new BlockPos(3, 1, 3)).consumesAction()
                && roomy.isEmpty(), "plafond à 2 blocs : la vache passe");
        List<Cow> back = releasedAround(helper, Cow.class, cow);
        helper.assertTrue(back.size() == 1 && helper.getLevel().noCollision(back.get(0)), "vache libre attendue");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void ownerCapturesTamedHorseAndKeepsOwnership(GameTestHelper helper) {
        Horse horse = helper.spawnWithNoFreeWill(EntityType.HORSE, 2, 1, 2);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer thief = helper.makeMockServerPlayerInLevel();
        horse.setTamed(true);
        horse.setOwnerUUID(owner.getUUID());
        horse.getInventory().setItem(0, new ItemStack(Items.SADDLE));
        helper.assertFalse(EntityCapturer.capture(thief, horse).isPresent(), "le cheval d'un autre est protégé");
        helper.assertFalse(horse.isRemoved(), "le cheval doit rester");
        CapturedEntityData data = EntityCapturer.capture(owner, horse).orElse(null);
        helper.assertTrue(data != null, "le propriétaire doit pouvoir capturer son cheval");
        Entity back = EntityCapturer.release(helper.getLevel(), data,
                helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), 0.0F).orElse(null);
        helper.assertTrue(back instanceof Horse restored && restored.isTamed() && restored.isSaddled()
                && owner.getUUID().equals(restored.getOwnerUUID()), "apprivoisement, selle et propriétaire conservés");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void sneakGestureOnNonLivingEntityIsNotIntercepted(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        ItemEntity item = new ItemEntity(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)).x,
                helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)).y, helper.absoluteVec(new Vec3(2.5, 1.0, 2.5)).z,
                new ItemStack(Items.DIRT));
        helper.getLevel().addFreshEntity(item);
        PlayerInteractEvent.EntityInteract event =
                new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, item);
        ModEvents.onEntityInteract(event);
        helper.assertFalse(event.isCanceled(), "un objet au sol ne doit pas être intercepté");
        helper.assertFalse(item.isRemoved(), "l'objet doit rester");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void hostileIsNotReleasedInPeaceful(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, zombie).orElse(null);
        helper.assertTrue(data != null && GrabMobConfig.allowHostile(), "le zombie doit être capturé");
        Difficulty previous = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        boolean released;
        try {
            released = EntityCapturer.release(helper.getLevel(), data,
                    helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), 0.0F).isPresent();
        } finally {
            helper.getLevel().getServer().setDifficulty(previous, true);
        }
        helper.assertFalse(released, "un hostile n'est pas relâché en Peaceful (item gardé)");
        helper.succeed();
    }
}
