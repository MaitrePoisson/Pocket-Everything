package fr.grabmob.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record CapturedEntityData(ResourceLocation entityType, CompoundTag tag) {

    public static final Codec<CapturedEntityData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity_type").forGetter(CapturedEntityData::entityType),
            CompoundTag.CODEC.fieldOf("tag").forGetter(CapturedEntityData::tag)
    ).apply(instance, CapturedEntityData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CapturedEntityData> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, CapturedEntityData::entityType,
            ByteBufCodecs.COMPOUND_TAG, CapturedEntityData::tag,
            CapturedEntityData::new
    );
}
