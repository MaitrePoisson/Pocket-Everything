package fr.grabmob.chest;

import fr.grabmob.GrabMob;
import fr.grabmob.data.CarriedChestData;
import fr.grabmob.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.LockCode;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ChestCarrier {

    public static final TagKey<Block> CARRYABLE =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(GrabMob.MOD_ID, "carryable_chests"));

    public static final String KEY_LOCKED = "message.grabmob.cannot_carry_locked";
    public static final String KEY_PROTECTED = "message.grabmob.cannot_carry_protected";
    public static final String KEY_NESTED = "message.grabmob.cannot_carry_nested";
    public static final String KEY_TOO_BIG = "message.grabmob.cannot_carry_too_big";
    public static final String KEY_CANNOT_PLACE = "message.grabmob.cannot_place_chest";

    private static final Logger LOGGER = LoggerFactory.getLogger(ChestCarrier.class);

    private ChestCarrier() {
    }

    public static boolean isCarryable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(CARRYABLE) && state.hasProperty(ChestBlock.TYPE)
                && level.getBlockEntity(pos) instanceof ChestBlockEntity;
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    private static void closeMenusOn(ServerLevel level, ChestBlockEntity chest) {
        List<ServerPlayer> viewers = new ArrayList<>();
        for (ServerPlayer viewer : level.players()) {
            if (viewer.containerMenu instanceof ChestMenu menu && isMenuOn(menu.getContainer(), chest)) {
                viewers.add(viewer);
            }
        }
        viewers.forEach(ServerPlayer::closeContainer);
    }

    private static boolean isMenuOn(Container container, ChestBlockEntity chest) {
        return container == chest || container instanceof CompoundContainer pair && pair.contains(chest);
    }

    public static Optional<CarriedChestData> pickUp(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (player == null || level == null || pos == null || !isCarryable(level, pos)
                || !(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            return Optional.empty();
        }
        BlockState state = level.getBlockState(pos);
        CompoundTag before = chest.saveWithoutMetadata(level.registryAccess());
        LockCode lock = LockCode.fromTag(before);
        if (lock != LockCode.NO_LOCK && !lock.unlocksWith(player.getMainHandItem())) {
            refuse(player, KEY_LOCKED);
            return Optional.empty();
        }
        if (CarriedChestData.containsCarriedChest(before)) {
            refuse(player, KEY_NESTED);
            return Optional.empty();
        }
        if (CarriedChestData.tooLarge(before)) {
            refuse(player, KEY_TOO_BIG);
            return Optional.empty();
        }
        if (!level.mayInteract(player, pos) || player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
                || NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) {
            refuse(player, KEY_PROTECTED);
            return Optional.empty();
        }
        closeMenusOn(level, chest);
        CarriedChestData data = new CarriedChestData(state.setValue(ChestBlock.TYPE, ChestType.SINGLE),
                chest.saveWithoutMetadata(level.registryAccess()));
        try {
            level.removeBlockEntity(pos);
            level.removeBlock(pos, false);
            level.playSound(null, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
        } catch (RuntimeException e) {
            LOGGER.warn("Chest pickup failed at {}", pos, e);
            if (level.getBlockState(pos).is(CARRYABLE)) {
                return Optional.empty();
            }
        }
        return Optional.of(data);
    }

    public static boolean place(ServerPlayer player, ServerLevel level, BlockPos clicked, Direction face,
            CarriedChestData data) {
        if (player == null || level == null || clicked == null || face == null || data == null
                || !(data.state().getBlock() instanceof ChestBlock block)) {
            return false;
        }
        ItemStack stand = new ItemStack(ModItems.CARRIED_CHEST.get());
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(clicked), face, clicked, false);
        BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stand, hit);
        BlockPos pos = context.getClickedPos();
        BlockState placed = context.canPlace() ? block.getStateForPlacement(context) : null;
        if (placed == null || !placed.canSurvive(level, pos)
                || !level.isUnobstructed(placed, pos, CollisionContext.of(player))) {
            refuse(player, KEY_CANNOT_PLACE);
            return false;
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stand)) {
            refuse(player, KEY_PROTECTED);
            return false;
        }
        try {
            if (!level.setBlock(pos, placed, Block.UPDATE_ALL)) {
                refuse(player, KEY_CANNOT_PLACE);
                return false;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof ChestBlockEntity) {
                entity.loadWithComponents(data.blockEntity().copy(), level.registryAccess());
                entity.setChanged();
            }
            level.playSound(null, pos, placed.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
            return true;
        } catch (RuntimeException e) {
            LOGGER.warn("Chest placement failed at {}", pos, e);
            level.removeBlock(pos, false);
            return false;
        }
    }
}
