package fr.pocketeverything.gametest;

import fr.pocketeverything.capture.EntityCapturer;
import fr.pocketeverything.registry.ModItems;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class VillagerGameTests {

    private static final String TEMPLATE = "pocket_everything:empty";

    public VillagerGameTests() {
    }

    private record Snapshot(String data, int xp, Tag offers, String name, float health, int age,
                            List<ItemStack> inventory) {
    }

    private static Tag offersTag(GameTestHelper helper, Villager villager) {
        MerchantOffers offers = villager.getOffers();
        return MerchantOffers.CODEC
                .encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), offers)
                .getOrThrow();
    }

    private static String describe(VillagerData data) {
        return data.getType() + "/" + data.getProfession() + "/" + data.getLevel();
    }

    private static Snapshot snapshot(GameTestHelper helper, Villager villager) {
        List<ItemStack> inventory = villager.getInventory().getItems().stream().map(ItemStack::copy).toList();
        String name = villager.getCustomName() == null ? "" : villager.getCustomName().getString();
        return new Snapshot(describe(villager.getVillagerData()), villager.getVillagerXp(), offersTag(helper, villager), name,
                villager.getHealth(), villager.getAge(), inventory);
    }

    private static boolean sameInventory(List<ItemStack> a, List<ItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!ItemStack.matches(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void assertSame(GameTestHelper helper, Snapshot expected, Snapshot actual) {
        helper.assertTrue(expected.data().equals(actual.data()), "VillagerData différent : " + actual.data() + " au lieu de " + expected.data());
        helper.assertTrue(expected.xp() == actual.xp(), "XP différente : " + actual.xp());
        helper.assertTrue(expected.offers().equals(actual.offers()), "les offres devraient être identiques");
        helper.assertTrue(expected.name().equals(actual.name()), "nom différent : " + actual.name());
        helper.assertTrue(expected.health() == actual.health(), "vie différente : " + actual.health());
        helper.assertTrue(expected.age() == actual.age(), "âge différent : " + actual.age());
        helper.assertTrue(sameInventory(expected.inventory(), actual.inventory()), "inventaire différent");
    }

    private static Villager configuredVillager(GameTestHelper helper, VillagerType type, int level, boolean baby) {
        Villager villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, 2, 1, 2);
        villager.setVillagerData(new VillagerData(type, VillagerProfession.LIBRARIAN, level));
        villager.setVillagerXp(37);
        villager.setCustomName(Component.literal("Gaspard"));
        villager.setHealth(7.0F);
        villager.getInventory().addItem(new ItemStack(Items.BREAD, 5));
        villager.getInventory().addItem(new ItemStack(Items.CARROT, 3));
        if (baby) {
            villager.setAge(-12000);
        }
        villager.getOffers();
        return villager;
    }

    private static BlockHitResult groundHit(GameTestHelper helper) {
        BlockPos ground = helper.absolutePos(new BlockPos(3, 0, 3));
        return new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
    }

    private static Villager roundTrip(GameTestHelper helper, ServerPlayer player, Villager villager) {
        player.setShiftKeyDown(true);
        InteractionResult captured = Gestures.interactOn(player, villager, InteractionHand.MAIN_HAND);
        helper.assertTrue(captured.consumesAction(), "la capture devrait être consommée : " + captured + " main=" + player.getMainHandItem() + " vivant=" + villager.isAlive() + " retiré=" + villager.isRemoved());
        helper.assertTrue(villager.isRemoved(), "le villageois aurait dû disparaître");
        ItemStack held = player.getMainHandItem();
        helper.assertTrue(held.is(ModItems.CAPTURED_ENTITY), "l'item capturé devrait être en main");
        player.setShiftKeyDown(false);
        InteractionResult released = player.gameMode.useItemOn(player, helper.getLevel(), held,
                InteractionHand.MAIN_HAND, groundHit(helper));
        helper.assertTrue(released.consumesAction(), "la libération devrait réussir");
        List<Villager> found = helper.getLevel().getEntitiesOfClass(Villager.class,
                helper.getBounds().inflate(2.0), entity -> !entity.isRemoved() && entity != villager);
        helper.assertTrue(found.size() == 1, "un seul villageois relâché était attendu : " + found.size());
        return found.get(0);
    }

    @GameTest(template = TEMPLATE)
    public static void villagerRoundTripKeepsProperties(GameTestHelper helper) {
        Villager villager = configuredVillager(helper, VillagerType.SWAMP, 3, false);
        helper.assertTrue(!villager.getOffers().isEmpty(), "le villageois devrait avoir des offres");
        Snapshot before = snapshot(helper, villager);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Villager back = roundTrip(helper, player, villager);
        assertSame(helper, before, snapshot(helper, back));
        helper.assertTrue(back.getVillagerData().getType() == VillagerType.SWAMP, "la variante de biome doit rester");
        helper.assertTrue(!back.isBaby(), "le villageois adulte doit rester adulte");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void villagerSurvivesTwoSuccessiveRoundTrips(GameTestHelper helper) {
        Villager villager = configuredVillager(helper, VillagerType.DESERT, 4, false);
        Snapshot before = snapshot(helper, villager);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Villager first = roundTrip(helper, player, villager);
        assertSame(helper, before, snapshot(helper, first));
        Villager second = roundTrip(helper, player, first);
        assertSame(helper, before, snapshot(helper, second));
        helper.assertTrue(second != villager && second != first, "le villageois relâché doit être une nouvelle instance");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void babyVillagerKeepsAge(GameTestHelper helper) {
        Villager villager = configuredVillager(helper, VillagerType.TAIGA, 1, true);
        helper.assertTrue(villager.isBaby(), "le villageois de départ devrait être un bébé");
        Snapshot before = snapshot(helper, villager);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Villager back = roundTrip(helper, player, villager);
        assertSame(helper, before, snapshot(helper, back));
        helper.assertTrue(back.isBaby(), "le bébé doit rester un bébé");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void playerCannotBeCaptured(GameTestHelper helper) {
        ServerPlayer capturer = helper.makeMockServerPlayerInLevel();
        ServerPlayer target = helper.makeMockServerPlayerInLevel();
        capturer.setShiftKeyDown(true);
        Gestures.interactOn(capturer, target, InteractionHand.MAIN_HAND);
        helper.assertTrue(!target.isRemoved(), "le joueur ne doit pas être retiré");
        helper.assertTrue(capturer.getMainHandItem().isEmpty(), "aucun item ne doit être donné");
        helper.assertTrue(!EntityCapturer.canCapture(target), "un joueur n'est pas capturable");
        helper.assertTrue(EntityCapturer.capture(capturer, target).isEmpty(), "capture() doit échouer sur un joueur");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeReleaseConsumesItemWithoutDuplicate(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        UUID originalId = cow.getUUID();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        player.setShiftKeyDown(true);
        Gestures.interactOn(player, cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(cow.isRemoved(), "la vache aurait dû être capturée en créatif");
        ItemStack held = player.getMainHandItem();
        helper.assertTrue(held.is(ModItems.CAPTURED_ENTITY), "l'item capturé devrait être en main");
        player.setShiftKeyDown(false);
        player.gameMode.useItemOn(player, helper.getLevel(), held, InteractionHand.MAIN_HAND, groundHit(helper));
        helper.assertTrue(player.getMainHandItem().isEmpty(), "l'item doit être consommé en créatif");
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND,
                groundHit(helper));
        List<Cow> cows = helper.getLevel().getEntitiesOfClass(Cow.class, helper.getBounds().inflate(2.0));
        helper.assertTrue(cows.size() == 1, "une seule vache est attendue : " + cows.size());
        helper.assertTrue(!cows.get(0).getUUID().equals(originalId), "la vache relâchée doit avoir un nouvel UUID");
        helper.succeed();
    }
}
