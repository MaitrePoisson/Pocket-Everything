package fr.pocketeverything.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PocketEverythingConfig {

    public record Values(boolean allowHostile, Set<String> extraBlacklist, boolean carryChestsEnabled) {
    }

    public static final String FILE_NAME = "pocket_everything.json";

    public static final Values DEFAULTS = new Values(true, Set.of(), true);

    private static final Logger LOGGER = LoggerFactory.getLogger(PocketEverythingConfig.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static volatile Values current = DEFAULTS;

    private PocketEverythingConfig() {
    }

    public static Values parse(String json) {
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()) {
                return DEFAULTS;
            }
            JsonObject object = root.getAsJsonObject();
            return new Values(
                    readBoolean(object, "allowHostile", DEFAULTS.allowHostile()),
                    readIds(object.get("extraBlacklist")),
                    readBoolean(object, "carryChestsEnabled", DEFAULTS.carryChestsEnabled()));
        } catch (RuntimeException e) {
            return DEFAULTS;
        }
    }

    public static String toJson(Values values) {
        JsonObject object = new JsonObject();
        object.addProperty("allowHostile", values.allowHostile());
        JsonArray list = new JsonArray();
        values.extraBlacklist().forEach(list::add);
        object.add("extraBlacklist", list);
        object.addProperty("carryChestsEnabled", values.carryChestsEnabled());
        return GSON.toJson(object);
    }

    public static Values load(Path file) {
        Values values = DEFAULTS;
        try {
            if (Files.exists(file)) {
                values = parse(Files.readString(file, StandardCharsets.UTF_8));
            } else {
                Files.createDirectories(file.toAbsolutePath().getParent());
                Files.writeString(file, toJson(DEFAULTS), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Cannot read {}, using the default configuration", file, e);
        }
        current = values;
        return values;
    }

    public static void load() {
        load(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    public static void set(Values values) {
        current = values;
    }

    public static boolean allowHostile() {
        return current.allowHostile();
    }

    public static boolean carryChestsEnabled() {
        return current.carryChestsEnabled();
    }

    public static boolean isExtraBlacklisted(ResourceLocation id) {
        return id != null && current.extraBlacklist().contains(id.toString());
    }

    private static boolean readBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return value.getAsBoolean();
        }
        return fallback;
    }

    private static Set<String> readIds(JsonElement value) {
        Set<String> ids = new LinkedHashSet<>();
        if (value != null && value.isJsonArray()) {
            for (JsonElement entry : value.getAsJsonArray()) {
                if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()
                        && ResourceLocation.tryParse(entry.getAsString()) != null) {
                    ids.add(entry.getAsString());
                }
            }
        }
        return Set.copyOf(ids);
    }
}
