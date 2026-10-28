package fr.grabmob.item;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class CapturedEntityInfo {

    private static final String MAX_HEALTH_ID = "minecraft:generic.max_health";

    private CapturedEntityInfo() {
    }

    public static Optional<Float> health(CompoundTag tag) {
        if (tag == null || !tag.contains("Health", Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        return Optional.of(tag.getFloat("Health"));
    }

    public static Optional<Double> maxHealth(CompoundTag tag) {
        if (tag == null) {
            return Optional.empty();
        }
        ListTag attributes = tag.getList("attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag entry = attributes.getCompound(i);
            if (MAX_HEALTH_ID.equals(entry.getString("id")) && entry.contains("base", Tag.TAG_ANY_NUMERIC)) {
                return Optional.of(entry.getDouble("base"));
            }
        }
        return Optional.empty();
    }

    public static Optional<String> professionTranslationKey(CompoundTag tag) {
        if (tag == null || !tag.contains("VillagerData", Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        String profession = tag.getCompound("VillagerData").getString("profession");
        int colon = profession.indexOf(':');
        String namespace = colon < 0 ? "minecraft" : profession.substring(0, colon);
        String path = colon < 0 ? profession : profession.substring(colon + 1);
        if (path.isEmpty() || path.equals("none")) {
            return Optional.empty();
        }
        String suffix = namespace.equals("minecraft") ? path : namespace + "." + path;
        return Optional.of("entity.minecraft.villager." + suffix);
    }

    public static Optional<String> customNameJson(CompoundTag tag) {
        if (tag == null || !tag.contains("CustomName", Tag.TAG_STRING)) {
            return Optional.empty();
        }
        String json = tag.getString("CustomName");
        return json.isEmpty() ? Optional.empty() : Optional.of(json);
    }

    public static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.05) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
