package fr.grabmob.gametest;

import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.data.CapturedEntityData;
import fr.grabmob.item.CapturedEntityItem;
import fr.grabmob.registry.ModDataComponents;
import fr.grabmob.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class ItemGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";

    private ItemGameTests() {
    }

    private static UseOnContext contextOn(GameTestHelper helper, ServerPlayer player, ItemStack stack) {
        BlockPos ground = helper.absolutePos(new BlockPos(3, 0, 3));
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, hit);
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void capturedItemReleasesAndIsConsumed(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        cow.setCustomName(Component.literal("Marguerite"));
        cow.setHealth(7.0F);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData data = EntityCapturer.capture(player, cow).orElse(null);
        helper.assertTrue(data != null, "la vache aurait dû être capturée");

        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        stack.set(ModDataComponents.CAPTURED_ENTITY.get(), data);
        helper.assertTrue(stack.getMaxStackSize() == 1, "l'item doit être non empilable");

        Component name = stack.getHoverName();
        helper.assertTrue(name.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(CapturedEntityItem.KEY_NAMED)
                        && contents.getArgs().length == 1
                        && contents.getArgs()[0] instanceof Component arg
                        && arg.getString().equals("Marguerite"),
                "le nom de l'item devrait porter le nom de l'entité");

        List<Component> tooltip = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        helper.assertTrue(tooltip.size() == 1, "l'infobulle devrait contenir la vie");
        helper.assertTrue(tooltip.get(0).getContents() instanceof TranslatableContents health
                        && health.getKey().equals(CapturedEntityItem.KEY_HEALTH),
                "la ligne de vie est attendue");

        InteractionResult result = stack.getItem().useOn(contextOn(helper, player, stack));
        helper.assertTrue(result.consumesAction(), "l'utilisation sur un bloc devrait réussir");
        helper.assertTrue(stack.isEmpty(), "l'item aurait dû être consommé");
        List<Cow> cows = helper.getLevel().getEntitiesOfClass(Cow.class,
                helper.getBounds().inflate(2.0), entity -> entity != cow);
        helper.assertTrue(cows.size() == 1, "exactement une vache relâchée était attendue");
        Cow back = cows.get(0);
        helper.assertTrue(back.getHealth() == 7.0F, "la vie (7) aurait dû être conservée");
        helper.assertTrue(back.hasCustomName() && "Marguerite".equals(back.getCustomName().getString()),
                "le nom aurait dû être conservé");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void failedReleaseKeepsTheItem(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        CapturedEntityData unknown = new CapturedEntityData(
                ResourceLocation.fromNamespaceAndPath("grabmob", "does_not_exist"), new CompoundTag());
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        stack.set(ModDataComponents.CAPTURED_ENTITY.get(), unknown);
        InteractionResult result = stack.getItem().useOn(contextOn(helper, player, stack));
        helper.assertTrue(!result.consumesAction(), "un échec ne doit pas consommer l'action");
        helper.assertTrue(stack.getCount() == 1, "l'item doit rester si le spawn échoue");
        helper.assertTrue(stack.getHoverName().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(CapturedEntityItem.KEY_NAME),
                "type inconnu : le nom générique est attendu");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void itemWithoutComponentIsHarmless(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        InteractionResult result = stack.getItem().useOn(contextOn(helper, player, stack));
        helper.assertTrue(result == InteractionResult.PASS, "sans composant, useOn doit passer");
        helper.assertTrue(stack.getCount() == 1, "sans composant, rien ne doit être consommé");
        helper.assertTrue(stack.getHoverName().getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(CapturedEntityItem.KEY_NAME),
                "sans composant, le nom générique est attendu");
        List<Component> tooltip = new ArrayList<>();
        stack.getItem().appendHoverText(stack, Item.TooltipContext.EMPTY, tooltip, TooltipFlag.NORMAL);
        helper.assertTrue(tooltip.isEmpty(), "sans composant, aucune infobulle");
        helper.succeed();
    }
}
