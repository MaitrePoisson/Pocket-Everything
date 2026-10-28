package fr.grabmob.gametest;

import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class EventGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";

    private EventGameTests() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void sneakInteractCapturesTheCow(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        cow.setHealth(6.0F);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        InteractionResult result = player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction(), "l'interaction doit être consommée");
        helper.assertTrue(cow.isRemoved(), "la vache aurait dû disparaître");
        ItemStack held = player.getMainHandItem();
        helper.assertTrue(held.is(ModItems.CAPTURED_ENTITY.get()), "l'item capturé devrait être en main");
        CapturedEntityData data = held.get(ModDataComponents.CAPTURED_ENTITY.get());
        helper.assertTrue(data != null && data.entityType().getPath().equals("cow"),
                "le composant devrait décrire la vache");
        helper.assertTrue(data.tag().getFloat("Health") == 6.0F, "la vie devrait être conservée");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void withoutSneakNothingHappens(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(false);
        player.interactOn(cow, InteractionHand.MAIN_HAND);
        helper.assertTrue(!cow.isRemoved(), "sans sneak la vache doit rester");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "sans sneak aucun item ne doit être donné");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void nonEmptyHandNothingHappens(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        player.interactOn(pig, InteractionHand.MAIN_HAND);
        helper.assertTrue(!pig.isRemoved(), "main non vide : le cochon doit rester");
        helper.assertTrue(player.getMainHandItem().is(Items.STICK), "la main ne doit pas changer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void offHandNothingHappens(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.interactOn(cow, InteractionHand.OFF_HAND);
        helper.assertTrue(!cow.isRemoved(), "la seconde main ne doit pas capturer");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void blacklistedTargetKeepsEntityAndIsNotIntercepted(GameTestHelper helper) {
        ArmorStand stand =
                helper.spawn(EntityType.ARMOR_STAND, 2, 1, 2);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setShiftKeyDown(true);
        player.interactOn(stand, InteractionHand.MAIN_HAND);
        helper.assertTrue(!stand.isRemoved(), "l'entité en liste noire doit rester");
        helper.assertTrue(player.getMainHandItem().isEmpty(), "aucun item ne doit être donné");
        helper.succeed();
    }
}
