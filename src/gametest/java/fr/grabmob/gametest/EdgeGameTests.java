package fr.grabmob.gametest;

import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.event.ModEvents;
import fr.grabmob.registry.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class EdgeGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";

    private EdgeGameTests() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void secondCaptureOfSameEntityGivesNothing(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(EntityCapturer.capture(player, cow).isPresent(), "la première capture doit réussir");
        helper.assertFalse(EntityCapturer.capture(player, cow).isPresent(), "une entité déjà capturée ne doit plus l'être");
        helper.assertFalse(ModEvents.tryCapture(player, cow), "tryCapture sur une entité retirée doit échouer");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "aucun item ne doit apparaître");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void bossesAndVehiclesAreBlacklisted(GameTestHelper helper) {
        for (EntityType<?> type : List.of(EntityType.ENDER_DRAGON, EntityType.WITHER, EntityType.BOAT,
                EntityType.MINECART, EntityType.ITEM_FRAME, EntityType.PAINTING, EntityType.LEASH_KNOT)) {
            Entity entity = type.create(helper.getLevel());
            helper.assertTrue(entity != null && !EntityCapturer.canCapture(entity), type + " doit être refusé");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void babyAnimalStaysBaby(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        cow.setAge(-6000);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, cow).orElse(null);
        helper.assertTrue(data != null, "le veau devrait être capturé");
        Optional<Entity> back = EntityCapturer.release(helper.getLevel(), data,
                helper.absoluteVec(new Vec3(4.5, 1.0, 4.5)), 0.0F);
        helper.assertTrue(back.isPresent() && back.get() instanceof Cow calf && calf.isBaby() && calf.getAge() == -6000,
                "le veau doit rester un veau du même âge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void fullInventoryDropsTheCapturedItem(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            player.getInventory().items.set(slot, new ItemStack(Items.DIRT, 1));
        }
        player.getInventory().items.set(0, new ItemStack(Items.STICK, 1));
        player.getInventory().selected = 0;
        helper.assertTrue(ModEvents.tryCapture(player, cow), "la capture doit réussir même inventaire plein");
        helper.assertTrue(cow.isRemoved(), "la vache doit disparaître");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(3.0), item -> item.getItem().is(ModItems.CAPTURED_ENTITY.get())
                        && item.position().distanceToSqr(player.getEyePosition()) < 1.0);
        boolean inInventory = player.getInventory().items.stream().anyMatch(s -> s.is(ModItems.CAPTURED_ENTITY.get()));
        helper.assertTrue(drops.size() == 1 || inInventory, "l'item capturé ne doit jamais être perdu");
        helper.succeed();
    }
}
