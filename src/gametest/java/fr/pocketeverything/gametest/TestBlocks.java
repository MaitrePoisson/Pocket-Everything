package fr.pocketeverything.gametest;

import com.mojang.serialization.MapCodec;
import fr.pocketeverything.PocketEverything;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
public final class TestBlocks {

    public static Block STORAGE;
    public static Block HANDLER;
    public static Block VAULT;
    public static BlockEntityType<ContainerEntity> CONTAINER_TYPE;
    public static BlockEntityType<HandlerEntity> HANDLER_TYPE;

    private TestBlocks() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, path);
    }

    public static void register() {
        STORAGE = Registry.register(BuiltInRegistries.BLOCK, id("test_storage"), new StorageBlock(false));
        HANDLER = Registry.register(BuiltInRegistries.BLOCK, id("test_handler"), new StorageBlock(true));
        VAULT = Registry.register(BuiltInRegistries.BLOCK, id("test_vault"), new StorageBlock(false));
        CONTAINER_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("test_container"),
                BlockEntityType.Builder.of(ContainerEntity::new, STORAGE, VAULT).build(null));
        HANDLER_TYPE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("test_handler"),
                BlockEntityType.Builder.of(HandlerEntity::new, HANDLER).build(null));
    }

    public static final class StorageBlock extends BaseEntityBlock {

        private static final MapCodec<StorageBlock> CODEC = simpleCodec(properties -> new StorageBlock(false));

        private final boolean handler;

        StorageBlock(boolean handler) {
            super(BlockBehaviour.Properties.of().strength(1.0F));
            this.handler = handler;
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Override
        protected RenderShape getRenderShape(BlockState state) {
            return RenderShape.MODEL;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return handler ? new HandlerEntity(pos, state) : new ContainerEntity(pos, state);
        }
    }

    public static final class ContainerEntity extends BaseContainerBlockEntity {

        private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

        public ContainerEntity(BlockPos pos, BlockState state) {
            super(CONTAINER_TYPE, pos, state);
        }

        @Override
        protected Component getDefaultName() {
            return Component.literal("Test storage");
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
            return ChestMenu.threeRows(id, inventory, this);
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, registries);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    public static final class HandlerEntity extends BlockEntity {

        private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

        public HandlerEntity(BlockPos pos, BlockState state) {
            super(HANDLER_TYPE, pos, state);
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            items = NonNullList.withSize(9, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("Inventory"), items, registries);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            CompoundTag inventory = new CompoundTag();
            ContainerHelper.saveAllItems(inventory, items, registries);
            tag.put("Inventory", inventory);
        }
    }
}
