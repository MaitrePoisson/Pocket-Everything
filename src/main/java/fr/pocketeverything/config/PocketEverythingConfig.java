package fr.pocketeverything.config;

import net.minecraft.resources.ResourceLocation;

public final class PocketEverythingConfig {

    private PocketEverythingConfig() {
    }

    public static boolean allowHostile() {
        return true;
    }

    public static boolean carryChestsEnabled() {
        return true;
    }

    public static boolean isExtraBlacklisted(ResourceLocation id) {
        return false;
    }
}
