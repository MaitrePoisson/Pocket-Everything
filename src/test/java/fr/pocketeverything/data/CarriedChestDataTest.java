package fr.pocketeverything.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CarriedChestDataTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    private static CarriedChestData sample() {
        BlockState state = Blocks.TRAPPED_CHEST.defaultBlockState()
                .setValue(ChestBlock.FACING, Direction.EAST)
                .setValue(ChestBlock.TYPE, ChestType.SINGLE)
                .setValue(ChestBlock.WATERLOGGED, true);
        CompoundTag tag = new CompoundTag();
        tag.putString("CustomName", "{\"text\":\"Stock\"}");
        tag.putString("Lock", "clef");
        ListTag items = new ListTag();
        for (int slot = 0; slot < 27; slot++) {
            CompoundTag item = new CompoundTag();
            item.putByte("Slot", (byte) slot);
            item.putString("id", "minecraft:cobblestone");
            item.putInt("count", slot + 1);
            items.add(item);
        }
        tag.put("Items", items);
        return new CarriedChestData(state, tag);
    }

    @Test
    void codecRoundTripKeepsStateAndFullContents() {
        CarriedChestData original = sample();
        Tag encoded = CarriedChestData.CODEC.encodeStart(NbtOps.INSTANCE, original).getOrThrow();
        CarriedChestData decoded = CarriedChestData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        assertEquals(original, decoded);
        assertEquals(27, decoded.blockEntity().getList("Items", Tag.TAG_COMPOUND).size());
    }

    @Test
    void streamCodecRoundTripKeepsStateAndFullContents() {
        CarriedChestData original = sample();
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        CarriedChestData.STREAM_CODEC.encode(buffer, original);
        assertEquals(original, CarriedChestData.STREAM_CODEC.decode(buffer));
    }

    @Test
    void stackCountCountsOccupiedSlots() {
        assertEquals(27, sample().stackCount());
        assertEquals(0, new CarriedChestData(Blocks.CHEST.defaultBlockState(), new CompoundTag()).stackCount());
    }

    @Test
    void nestedCarriedChestIsDetected() {
        CompoundTag inner = new CompoundTag();
        inner.putString("id", CarriedChestData.ITEM_ID);
        ListTag items = new ListTag();
        items.add(inner);
        CompoundTag tag = new CompoundTag();
        tag.put("Items", items);
        assertTrue(CarriedChestData.containsCarriedChest(tag));
        assertFalse(CarriedChestData.containsCarriedChest(sample().blockEntity()));
        assertFalse(CarriedChestData.containsCarriedChest(new CompoundTag()));
    }

    @Test
    void stackCountAlsoCountsNestedInventories() {
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:stone");
        ListTag items = new ListTag();
        items.add(item);
        items.add(item.copy());
        CompoundTag inventory = new CompoundTag();
        inventory.put("Items", items);
        CompoundTag tag = new CompoundTag();
        tag.put("Inventory", inventory);
        assertEquals(2, new CarriedChestData(Blocks.BARREL.defaultBlockState(), tag).stackCount());
    }

    @Test
    void dataWithoutKnownBlockIsRejectedWithoutException() {
        CompoundTag encoded = new CompoundTag();
        encoded.putString("Name", "unknown_mod:storage");
        CompoundTag data = new CompoundTag();
        data.put("state", encoded);
        data.put("block_entity", new CompoundTag());
        assertTrue(CarriedChestData.CODEC.parse(NbtOps.INSTANCE, data).result().isEmpty());
    }

    @Test
    void anyBlockStateRoundTripsThroughTheCodec() {
        CarriedChestData barrel = new CarriedChestData(Blocks.BARREL.defaultBlockState(), new CompoundTag());
        Tag encoded = CarriedChestData.CODEC.encodeStart(NbtOps.INSTANCE, barrel).getOrThrow();
        assertEquals(barrel, CarriedChestData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow());
    }
}
