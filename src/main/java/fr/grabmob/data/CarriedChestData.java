package fr.grabmob.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
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
}
