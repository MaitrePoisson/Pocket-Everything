package fr.grabmob.config;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class GrabMobConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ALLOW_HOSTILE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_BLACKLIST;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ALLOW_HOSTILE = builder
                .comment("Allow capturing hostile mobs (zombies, creepers...).")
                .define("allowHostile", true);
        EXTRA_BLACKLIST = builder
                .comment("Additional entity type ids that cannot be captured, for example \"minecraft:warden\".")
                .defineListAllowEmpty("extraBlacklist", List.of(), () -> "modid:entity", GrabMobConfig::isValidId);
        SPEC = builder.build();
    }

    private GrabMobConfig() {
    }

    private static boolean isValidId(Object value) {
        return value instanceof String text && ResourceLocation.tryParse(text) != null;
    }

    public static boolean allowHostile() {
        return !SPEC.isLoaded() || ALLOW_HOSTILE.get();
    }

    public static boolean isExtraBlacklisted(ResourceLocation id) {
        if (!SPEC.isLoaded() || id == null) {
            return false;
        }
        String text = id.toString();
        return EXTRA_BLACKLIST.get().stream().anyMatch(text::equals);
    }
}
