package fr.grabmob.gametest;

import fr.grabmob.capture.EntityCapturer;
import fr.grabmob.data.CapturedEntityData;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@PrefixGameTestTemplate(false)
public final class CaptureGameTests {

    private static final String TEMPLATE = "empty";
    private static final String NAMESPACE = "grabmob";

    private CaptureGameTests() {
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void cowCaptureThenRelease(GameTestHelper helper) {
        Cow cow = helper.spawnWithNoFreeWill(EntityType.COW, 2, 1, 2);
        cow.setCustomName(Component.literal("Marguerite"));
        cow.setHealth(7.0F);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();

        Optional<CapturedEntityData> captured = EntityCapturer.capture(player, cow);
        helper.assertTrue(captured.isPresent(), "la vache aurait dû être capturée");
        helper.assertTrue(cow.isRemoved(), "la vache capturée aurait dû être retirée du monde");
        CompoundTag tag = captured.get().tag();
        for (String key : new String[] {"UUID", "Pos", "Motion", "Passengers", "Leash", "Brain"}) {
            helper.assertFalse(tag.contains(key), "la clé " + key + " aurait dû être retirée");
        }
        helper.assertTrue(captured.get().entityType().equals(ResourceLocation.withDefaultNamespace("cow")),
                "l'identifiant stocké devrait être minecraft:cow");

        Vec3 target = helper.absoluteVec(new Vec3(5.5, 2.0, 5.5));
        Optional<Entity> released = EntityCapturer.release(helper.getLevel(), captured.get(), target, 90.0F);
        helper.assertTrue(released.isPresent(), "la vache aurait dû être relâchée");
        Entity back = released.get();
        helper.assertTrue(back.getType() == EntityType.COW, "le type relâché devrait être la vache");
        helper.assertTrue(back != cow && !back.isRemoved(), "une nouvelle entité vivante était attendue");
        helper.assertTrue(back.getUUID() != null && !back.getUUID().equals(cow.getUUID()),
                "l'entité relâchée doit avoir un nouvel UUID");
        helper.assertTrue(back.hasCustomName() && "Marguerite".equals(back.getCustomName().getString()),
                "le nom personnalisé aurait dû être conservé");
        helper.assertTrue(back instanceof LivingEntity living && living.getHealth() == 7.0F,
                "la vie (7) aurait dû être conservée");
        helper.assertTrue(back.position().distanceToSqr(target) < 0.01, "la position relâchée est incorrecte");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void blacklistedEntitiesAreRefused(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Entity stand = helper.spawn(EntityType.ARMOR_STAND, 3, 1, 3);
        helper.assertFalse(EntityCapturer.capture(player, stand).isPresent(), "le porte-armure doit être refusé");
        helper.assertFalse(stand.isRemoved(), "le porte-armure refusé ne doit pas être retiré");
        helper.assertFalse(EntityCapturer.capture(player, player).isPresent(), "le joueur ne doit pas être capturable");
        helper.assertFalse(player.isRemoved(), "le joueur ne doit pas être retiré");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, templateNamespace = NAMESPACE)
    public static void invalidDataFailsCleanly(GameTestHelper helper) {
        Vec3 target = helper.absoluteVec(new Vec3(4.5, 1.0, 4.5));
        CapturedEntityData unknown = new CapturedEntityData(
                ResourceLocation.fromNamespaceAndPath("grabmob", "does_not_exist"), new CompoundTag());
        helper.assertFalse(EntityCapturer.release(helper.getLevel(), unknown, target, 0.0F).isPresent(),
                "un type inconnu doit échouer proprement");
        CapturedEntityData notCreatable = new CapturedEntityData(
                ResourceLocation.withDefaultNamespace("player"), new CompoundTag());
        helper.assertFalse(EntityCapturer.release(helper.getLevel(), notCreatable, target, 0.0F).isPresent(),
                "un type non créable doit échouer proprement");
        CompoundTag garbage = new CompoundTag();
        garbage.putString("Health", "not a number");
        garbage.putString("CustomName", "{broken json");
        CapturedEntityData damaged = new CapturedEntityData(ResourceLocation.withDefaultNamespace("cow"), garbage);
        Optional<Entity> result = EntityCapturer.release(helper.getLevel(), damaged, target, 0.0F);
        helper.assertTrue(result.isEmpty() || !result.get().isRemoved(), "un NBT abîmé ne doit pas planter");
        result.ifPresent(Entity::discard);
        helper.succeed();
    }
}
