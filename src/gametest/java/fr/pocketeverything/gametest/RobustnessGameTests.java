package fr.pocketeverything.gametest;

import fr.pocketeverything.capture.EntityCapturer;
import fr.pocketeverything.config.PocketEverythingConfig;
import fr.pocketeverything.data.CapturedEntityData;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class RobustnessGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "pocket_everything";
    private static final int OFFERS = 60;
    private static final int MAX_NETWORK_BYTES = 1_000_000;

    private RobustnessGameTests() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void capturedMountEjectsItsPassenger(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 2);
        Chicken chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, 2, 1, 2);
        helper.assertTrue(chicken.startRiding(pig, true), "le poulet devrait monter sur le cochon");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Optional<CapturedEntityData> data = EntityCapturer.capture(player, pig);
        helper.assertTrue(data.isPresent(), "le cochon devrait être capturé");
        helper.assertTrue(pig.isRemoved(), "le cochon aurait dû disparaître");
        helper.assertTrue(!chicken.isRemoved(), "le poulet ne doit pas être perdu");
        helper.assertTrue(!chicken.isPassenger(), "le poulet doit être démonté");
        helper.assertTrue(!data.get().tag().contains("Passengers"), "aucun passager dans les données");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void capturedRiderLeavesItsMount(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 2);
        Chicken chicken = helper.spawnWithNoFreeWill(EntityType.CHICKEN, 2, 1, 2);
        helper.assertTrue(chicken.startRiding(pig, true), "le poulet devrait monter sur le cochon");
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(EntityCapturer.capture(player, chicken).isPresent(), "le poulet devrait être capturé");
        helper.assertTrue(chicken.isRemoved(), "le poulet aurait dû disparaître");
        helper.assertTrue(!pig.isRemoved(), "le cochon doit rester");
        helper.assertTrue(pig.getPassengers().isEmpty(), "le cochon ne doit plus avoir de passager");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void leashIsReleasedOnCapture(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        cow.setLeashedTo(player, true);
        helper.assertTrue(cow.isLeashed(), "la vache devrait être tenue en laisse");
        Optional<CapturedEntityData> data = EntityCapturer.capture(player, cow);
        helper.assertTrue(data.isPresent(), "la vache devrait être capturée");
        helper.assertTrue(!data.get().tag().contains("leash") && !data.get().tag().contains("Leash"),
                "aucune laisse dans les données");
        List<ItemEntity> leads = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                helper.getBounds().inflate(4.0), item -> item.getItem().is(Items.LEAD));
        helper.assertTrue(leads.size() == 1, "la laisse devrait être lâchée : " + leads.size());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void tamedAnimalKeepsItsOwner(GameTestHelper helper) {
        Wolf wolf = helper.spawnWithNoFreeWill(EntityType.WOLF, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        wolf.tame(player);
        helper.assertTrue(player.getUUID().equals(wolf.getOwnerUUID()), "le loup devrait avoir un propriétaire");
        CapturedEntityData data = EntityCapturer.capture(player, wolf).orElse(null);
        helper.assertTrue(data != null, "le loup devrait être capturé");
        Vec3 where = helper.absoluteVec(new Vec3(3.5, 1.0, 3.5));
        Entity released = EntityCapturer.release(helper.getLevel(), data, where, 0.0F).orElse(null);
        helper.assertTrue(released instanceof Wolf, "un loup devrait être relâché");
        Wolf back = (Wolf) released;
        helper.assertTrue(back.isTame(), "le loup doit rester apprivoisé");
        helper.assertTrue(player.getUUID().equals(back.getOwnerUUID()), "le propriétaire doit être conservé");
        helper.assertTrue(!back.getUUID().equals(wolf.getUUID()), "le loup relâché doit avoir un nouvel UUID");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void villagerPoiIsFreedAndMemoriesAreDropped(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos site = helper.absolutePos(new BlockPos(4, 1, 4));
        level.getPoiManager().add(site, BuiltInRegistries.POINT_OF_INTEREST_TYPE.getHolderOrThrow(PoiTypes.LIBRARIAN));
        helper.assertTrue(level.getPoiManager().take(type -> true, (type, pos) -> true, site, 1).isPresent(),
                "le poste devrait pouvoir être réservé");
        int before = level.getPoiManager().getFreeTickets(site);
        helper.assertTrue(before == 0, "le poste devrait être occupé : " + before);
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 2, 1, 2);
        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE, GlobalPos.of(level.dimension(), site));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, villager).orElse(null);
        helper.assertTrue(data != null, "le villageois devrait être capturé");
        int after = level.getPoiManager().getFreeTickets(site);
        helper.assertTrue(after == 1, "le poste devrait être libéré : " + after);
        helper.assertTrue(!data.tag().contains("Brain"), "aucun souvenir dans les données");
        Entity released = EntityCapturer.release(level, data, helper.absoluteVec(new Vec3(3.5, 1.0, 3.5)), 0.0F)
                .orElse(null);
        helper.assertTrue(released instanceof Villager, "un villageois devrait être relâché");
        helper.assertTrue(((Villager) released).getBrain().getMemory(MemoryModuleType.JOB_SITE).isEmpty(),
                "le villageois relâché ne doit pas garder de poste");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void configCanForbidHostilesAndExtraTypes(GameTestHelper helper) {
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 1, 2);
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 3, 1, 3);
        try {
            helper.assertTrue(EntityCapturer.canCapture(zombie), "hostiles autorisés par défaut");
            PocketEverythingConfig.ALLOW_HOSTILE.set(false);
            helper.assertTrue(!EntityCapturer.canCapture(zombie), "le zombie doit être refusé");
            helper.assertTrue(EntityCapturer.canCapture(cow), "la vache reste capturable");
            PocketEverythingConfig.EXTRA_BLACKLIST.set(List.of("minecraft:cow"));
            helper.assertTrue(!EntityCapturer.canCapture(cow), "la vache doit être refusée par la liste noire");
        } finally {
            PocketEverythingConfig.ALLOW_HOSTILE.set(true);
            PocketEverythingConfig.EXTRA_BLACKLIST.set(List.of());
        }
        helper.assertTrue(EntityCapturer.canCapture(zombie) && EntityCapturer.canCapture(cow),
                "la configuration doit être rétablie");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void manyOffersStayWithinNetworkBudget(GameTestHelper helper) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 2, 1, 2);
        for (int i = 0; i < OFFERS; i++) {
            ItemStack result = new ItemStack(Items.DIAMOND_SWORD);
            result.setDamageValue(i);
            villager.getOffers().add(new MerchantOffer(new ItemCost(Items.EMERALD, 1 + i % 60), result, 12, 5, 0.05F));
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, villager).orElse(null);
        helper.assertTrue(data != null, "le villageois devrait être capturé");
        RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        CapturedEntityData.STREAM_CODEC.encode(buffer, data);
        int size = buffer.readableBytes();
        System.out.println("POCKET_EVERYTHING_NETWORK_SIZE offers=" + OFFERS + " bytes=" + size);
        helper.assertTrue(size < MAX_NETWORK_BYTES, "données trop lourdes pour le réseau : " + size);
        CapturedEntityData decoded = CapturedEntityData.STREAM_CODEC.decode(buffer);
        helper.assertTrue(decoded.equals(data), "le décodage réseau doit redonner les mêmes données");
        helper.succeed();
    }
}
