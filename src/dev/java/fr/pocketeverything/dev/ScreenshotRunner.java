package fr.pocketeverything.dev;

import com.mojang.blaze3d.platform.NativeImage;
import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.data.CapturedEntityData;
import fr.pocketeverything.data.CarriedChestData;
import fr.pocketeverything.registry.ModDataComponents;
import fr.pocketeverything.registry.ModItems;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = PocketEverything.MOD_ID, value = Dist.CLIENT)
public final class ScreenshotRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotRunner.class);
    private static final String DIR_PROPERTY = "pocket_everything.screenshot.dir";
    private static final int WORLD_SETTLE_TICKS = 100;
    private static final int SHOT_SETTLE_TICKS = 12;

    private record Shot(String name, Runnable setup) {
    }

    private static final List<ItemStack> STACKS = new ArrayList<>();
    private static final List<String> NAMES = new ArrayList<>();
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final List<ItemEntity> GROUND = new ArrayList<>();
    private static boolean worldRequested;
    private static boolean sequenceBuilt;
    private static boolean finished;
    private static boolean captureNext;
    private static boolean guiGrid;
    private static int ticksInWorld;
    private static int settle;
    private static int next;
    private static Shot current;

    private ScreenshotRunner() {
    }

    private static boolean enabled() {
        return System.getProperty(DIR_PROPERTY) != null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!enabled() || finished) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        try {
            if (mc.level == null || mc.player == null) {
                openWorld(mc);
                return;
            }
            ticksInWorld++;
            if (ticksInWorld < WORLD_SETTLE_TICKS) {
                return;
            }
            if (!sequenceBuilt) {
                buildSequence(mc);
                sequenceBuilt = true;
            }
            if (captureNext) {
                return;
            }
            if (current != null && settle > 0) {
                settle--;
                if (settle == 0) {
                    captureNext = true;
                }
                return;
            }
            if (next >= SHOTS.size()) {
                finish(mc);
                return;
            }
            current = SHOTS.get(next++);
            current.setup().run();
            settle = SHOT_SETTLE_TICKS;
        } catch (RuntimeException e) {
            LOGGER.error("Screenshot run failed", e);
            finish(mc);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!enabled() || !captureNext || current == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();
        if (guiGrid) {
            drawGrid(mc, graphics);
        }
        graphics.flush();
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            Path dir = Path.of(System.getProperty(DIR_PROPERTY));
            Files.createDirectories(dir);
            image.writeToFile(dir.resolve("m9_" + current.name() + ".png"));
            LOGGER.info("Screenshot written: {}", current.name());
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Screenshot {} failed", current.name(), e);
        }
        captureNext = false;
        current = null;
    }

    private static void openWorld(Minecraft mc) {
        if (mc.screen instanceof AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.setScreen(new TitleScreen());
            return;
        }
        if (mc.screen instanceof TitleScreen && !worldRequested) {
            worldRequested = true;
            mc.options.pauseOnLostFocus = false;
            mc.options.tutorialStep = TutorialSteps.NONE;
            LevelSettings settings = new LevelSettings("pocket_everything_shot", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("pocket_everything_shot", settings, new WorldOptions(12345L, false, false),
                    access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                            .value().createWorldDimensions(), mc.screen);
        }
    }

    private static void buildSequence(Minecraft mc) {
        ClientLevel level = mc.level;
        addStack(level, "bee", EntityType.BEE, null);
        addStack(level, "cow", EntityType.COW, null);
        addStack(level, "golem", EntityType.IRON_GOLEM, null);
        addStack(level, "villager_baby", EntityType.VILLAGER, entity -> {
            Villager villager = (Villager) entity;
            villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.FARMER, 2));
            villager.setAge(-24000);
        });
        addChest("chest", Blocks.CHEST.defaultBlockState());
        addChest("trapped_chest", Blocks.TRAPPED_CHEST.defaultBlockState());
        SHOTS.add(new Shot("gui", () -> {
            clearHand(mc);
            guiGrid = true;
        }));
        for (int i = 0; i < STACKS.size(); i++) {
            int index = i;
            SHOTS.add(new Shot("hand_" + NAMES.get(i), () -> {
                guiGrid = false;
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                holdInHand(mc, STACKS.get(index));
            }));
        }
        SHOTS.add(new Shot("third_cow", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(1));
        }));
        SHOTS.add(new Shot("third_golem", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(2));
        }));
        SHOTS.add(new Shot("third_chest", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(4));
        }));
        SHOTS.add(new Shot("third_left_chest", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(4));
            mc.options.mainHand().set(HumanoidArm.LEFT);
        }));
        SHOTS.add(new Shot("hand_left_chest", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            holdInHand(mc, STACKS.get(4));
            mc.options.mainHand().set(HumanoidArm.LEFT);
        }));
        SHOTS.add(new Shot("hand_left_chest_late", () -> {
        }));
        SHOTS.add(new Shot("hand_off_chest", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            clearHand(mc);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.player.getInventory().offhand.set(0, STACKS.get(4).copy());
        }));
        SHOTS.add(new Shot("third_back_chest", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            holdInHand(mc, STACKS.get(5));
        }));
        SHOTS.add(new Shot("third_back_cow", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            holdInHand(mc, STACKS.get(1));
        }));
        SHOTS.add(new Shot("third_villager_baby", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(3));
        }));
        SHOTS.add(new Shot("third_left_cow", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            holdInHand(mc, STACKS.get(1));
            mc.options.mainHand().set(HumanoidArm.LEFT);
        }));
        SHOTS.add(new Shot("hand_left_cow", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            holdInHand(mc, STACKS.get(1));
            mc.options.mainHand().set(HumanoidArm.LEFT);
        }));
        SHOTS.add(new Shot("hand_off_golem", () -> {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            clearHand(mc);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.player.getInventory().offhand.set(0, STACKS.get(2).copy());
        }));
        SHOTS.add(new Shot("third_off_golem", () -> {
            mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            clearHand(mc);
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.player.getInventory().offhand.set(0, STACKS.get(2).copy());
        }));
        SHOTS.add(new Shot("ground", () -> {
            mc.options.mainHand().set(HumanoidArm.RIGHT);
            mc.player.getInventory().offhand.set(0, ItemStack.EMPTY);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            clearHand(mc);
            LocalPlayer player = mc.player;
            player.setYRot(0.0F);
            player.setXRot(35.0F);
            for (int i = 0; i < STACKS.size(); i++) {
                ItemEntity item = new ItemEntity(mc.level, player.getX() - 1.5D + i, player.getY(),
                        player.getZ() + 2.5D, STACKS.get(i).copy());
                item.setNoGravity(true);
                item.setDeltaMovement(0.0D, 0.0D, 0.0D);
                mc.level.addEntity(item);
                GROUND.add(item);
            }
        }));
    }

    private static void addStack(ClientLevel level, String name, EntityType<?> type, Consumer<Entity> tweak) {
        Entity entity = type.create(level);
        if (tweak != null) {
            tweak.accept(entity);
        }
        CompoundTag tag = new CompoundTag();
        entity.saveWithoutId(tag);
        ItemStack stack = new ItemStack(ModItems.CAPTURED_ENTITY.get());
        stack.set(ModDataComponents.CAPTURED_ENTITY.get(),
                new CapturedEntityData(BuiltInRegistries.ENTITY_TYPE.getKey(type), tag));
        STACKS.add(stack);
        NAMES.add(name);
    }

    private static void addChest(String name, BlockState state) {
        ItemStack stack = new ItemStack(ModItems.CARRIED_CHEST.get());
        stack.set(ModDataComponents.CARRIED_CHEST.get(), new CarriedChestData(state, new CompoundTag()));
        STACKS.add(stack);
        NAMES.add(name);
    }

    private static void holdInHand(Minecraft mc, ItemStack stack) {
        LocalPlayer player = mc.player;
        player.getInventory().items.set(player.getInventory().selected, stack.copy());
        player.getInventory().offhand.set(0, ItemStack.EMPTY);
        mc.options.mainHand().set(HumanoidArm.RIGHT);
        player.setXRot(0.0F);
    }

    private static void clearHand(Minecraft mc) {
        LocalPlayer player = mc.player;
        player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
    }

    private static void drawGrid(Minecraft mc, GuiGraphics graphics) {
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, width, height, 0xFF707070);
        int cell = 64;
        int startX = (width - cell * STACKS.size()) / 2;
        int y = (height - cell) / 2;
        for (int i = 0; i < STACKS.size(); i++) {
            graphics.pose().pushPose();
            graphics.pose().translate(startX + i * cell, y, 0.0F);
            graphics.pose().scale(4.0F, 4.0F, 1.0F);
            graphics.renderItem(STACKS.get(i), 0, 0);
            graphics.pose().popPose();
            graphics.renderItem(STACKS.get(i), 8 + i * 20, 8);
        }
    }

    private static void finish(Minecraft mc) {
        finished = true;
        LOGGER.info("Screenshot run finished");
        mc.stop();
    }
}
