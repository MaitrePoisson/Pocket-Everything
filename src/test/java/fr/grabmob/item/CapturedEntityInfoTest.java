package fr.grabmob.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

class CapturedEntityInfoTest {

    @Test
    void healthAndMaxHealthAreRead() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Health", 7.0F);
        CompoundTag attribute = new CompoundTag();
        attribute.putString("id", "minecraft:generic.max_health");
        attribute.putDouble("base", 20.0);
        ListTag attributes = new ListTag();
        attributes.add(attribute);
        tag.put("attributes", attributes);
        assertEquals(7.0F, CapturedEntityInfo.health(tag).orElseThrow());
        assertEquals(20.0, CapturedEntityInfo.maxHealth(tag).orElseThrow());
    }

    @Test
    void missingValuesGiveEmpty() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Health", "oops");
        assertTrue(CapturedEntityInfo.health(tag).isEmpty());
        assertTrue(CapturedEntityInfo.maxHealth(tag).isEmpty());
        assertTrue(CapturedEntityInfo.professionTranslationKey(tag).isEmpty());
        assertTrue(CapturedEntityInfo.customNameJson(tag).isEmpty());
        assertTrue(CapturedEntityInfo.health(null).isEmpty());
    }

    @Test
    void professionKeyHandlesVanillaModdedAndNone() {
        assertEquals("entity.minecraft.villager.farmer", CapturedEntityInfo.professionTranslationKey(villager("minecraft:farmer")).orElseThrow());
        assertEquals("entity.minecraft.villager.mymod.baker", CapturedEntityInfo.professionTranslationKey(villager("mymod:baker")).orElseThrow());
        assertTrue(CapturedEntityInfo.professionTranslationKey(villager("minecraft:none")).isEmpty());
    }

    @Test
    void customNameJsonIsReturnedOnlyWhenPresent() {
        CompoundTag tag = new CompoundTag();
        tag.put("CustomName", StringTag.valueOf("{\"text\":\"Marguerite\"}"));
        assertEquals("{\"text\":\"Marguerite\"}", CapturedEntityInfo.customNameJson(tag).orElseThrow());
    }

    @Test
    void numbersAreFormattedWithoutUselessDecimals() {
        assertEquals("7", CapturedEntityInfo.formatNumber(7.0));
        assertEquals("6.5", CapturedEntityInfo.formatNumber(6.5));
        assertEquals("20", CapturedEntityInfo.formatNumber(20.0));
    }

    private static CompoundTag villager(String profession) {
        CompoundTag data = new CompoundTag();
        data.putString("profession", profession);
        CompoundTag tag = new CompoundTag();
        tag.put("VillagerData", data);
        return tag;
    }
}
