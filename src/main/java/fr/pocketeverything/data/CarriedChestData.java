package fr.pocketeverything.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record CarriedChestData(BlockState state, CompoundTag blockEntity) {

    public static final Codec<CarriedChestData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.fieldOf("state").forGetter(CarriedChestData::state),
            CompoundTag.CODEC.fieldOf("block_entity").forGetter(CarriedChestData::blockEntity)
    ).apply(instance, CarriedChestData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarriedChestData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY), CarriedChestData::state,
            ByteBufCodecs.COMPOUND_TAG, CarriedChestData::blockEntity,
            CarriedChestData::new
    );

    public static final String ITEM_ID = "pocket_everything:carried_chest";

    public int stackCount() {
        return blockEntity.getList("Items", Tag.TAG_COMPOUND).size();
    }

    public static final int MAX_BYTES = 1 << 20;

    public static boolean tooLarge(CompoundTag blockEntity) {
        return blockEntity.sizeInBytes() > MAX_BYTES;
    }

    public static boolean containsCarriedChest(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            if (ITEM_ID.equals(compound.getString("id"))) {
                return true;
            }
            return compound.getAllKeys().stream().anyMatch(key -> containsCarriedChest(compound.get(key)));
        }
        if (tag instanceof ListTag list) {
            return list.stream().anyMatch(CarriedChestData::containsCarriedChest);
        }
        return false;
    }
}
