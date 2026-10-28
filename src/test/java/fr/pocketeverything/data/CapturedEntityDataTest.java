package fr.pocketeverything.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CapturedEntityDataTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static CompoundTag sampleTag() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", 7.0F);
        tag.putInt("Age", -24000);
        tag.putString("CustomName", "{\"text\":\"Bessie\"}");
        ListTag offers = new ListTag();
        offers.add(StringTag.valueOf("offer"));
        tag.put("Offers", offers);
        CompoundTag nested = new CompoundTag();
        nested.putByte("OnGround", (byte) 1);
        tag.put("Nested", nested);
        return tag;
    }

    private static CapturedEntityData sample() {
        return new CapturedEntityData(ResourceLocation.withDefaultNamespace("cow"), sampleTag());
    }

    @Test
    void codecRoundTripThroughNbt() {
        CapturedEntityData original = sample();
        Tag encoded = CapturedEntityData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        CapturedEntityData decoded = CapturedEntityData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded);
        assertEquals(original.entityType(), decoded.entityType());
        assertEquals(original.tag(), decoded.tag());
    }

    @Test
    void streamCodecRoundTrip() {
        CapturedEntityData original = sample();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        CapturedEntityData.STREAM_CODEC.encode(buf, original);
        CapturedEntityData decoded = CapturedEntityData.STREAM_CODEC.decode(buf);
        assertEquals(original, decoded);
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void equalsAndHashCodeFollowContent() {
        CapturedEntityData a = sample();
        CapturedEntityData b = sample();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentContentIsNotEqual() {
        CapturedEntityData a = sample();
        CompoundTag changed = sampleTag();
        changed.putFloat("Health", 8.0F);
        assertNotEquals(a, new CapturedEntityData(a.entityType(), changed));
        assertNotEquals(a, new CapturedEntityData(ResourceLocation.withDefaultNamespace("pig"), sampleTag()));
    }
}
