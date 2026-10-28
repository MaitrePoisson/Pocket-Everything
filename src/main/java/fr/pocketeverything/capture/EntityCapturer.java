package fr.pocketeverything.capture;

import fr.pocketeverything.PocketEverything;
import fr.pocketeverything.config.PocketEverythingConfig;
import fr.pocketeverything.data.CapturedEntityData;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityCapturer {

    public static final TagKey<EntityType<?>> BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(PocketEverything.MOD_ID, "blacklist"));

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCapturer.class);

    private static final String[] TRANSIENT_KEYS = {"UUID", "Pos", "Motion", "Passengers", "Leash", "leash", "Brain",
            "FallDistance", "Fire", "Fuse", "ignited", "HurtTime", "HurtByTimestamp", "DeathTime"};

    private static final List<MemoryModuleType<GlobalPos>> POI_MEMORIES = List.of(MemoryModuleType.JOB_SITE,
            MemoryModuleType.POTENTIAL_JOB_SITE, MemoryModuleType.HOME, MemoryModuleType.MEETING_POINT);

    private static final double MAX_LIFT = 1.0D;
    private static final double LIFT_STEP = 0.25D;

    private EntityCapturer() {
    }

    public static boolean canCapture(Entity entity) {
        if (entity == null || entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        return entity instanceof LivingEntity && isAllowedType(entity.getType()) && isHostileAllowed(entity);
    }

    private static boolean isAllowedType(EntityType<?> type) {
        if (!type.canSerialize() || type.is(BLACKLIST) || !BuiltInRegistries.ENTITY_TYPE.containsValue(type)) {
            return false;
        }
        return !PocketEverythingConfig.isExtraBlacklisted(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    private static boolean isHostileAllowed(Entity entity) {
        return PocketEverythingConfig.allowHostile() || !(entity instanceof Enemy);
    }

    private static boolean isOwnedByAnother(ServerPlayer player, Entity entity) {
        return entity instanceof OwnableEntity pet && pet.getOwnerUUID() != null
                && !pet.getOwnerUUID().equals(player.getUUID());
    }

    public static Optional<CapturedEntityData> capture(ServerPlayer player, Entity entity) {
        if (player == null || !canCapture(entity) || entity.level() != player.level()
                || isOwnedByAnother(player, entity)) {
            return Optional.empty();
        }
        try {
            entity.ejectPassengers();
            entity.stopRiding();
            if (entity instanceof Mob mob && mob.isLeashed()) {
                mob.dropLeash(true, true);
            }
            if (entity instanceof Villager villager && entity.level() instanceof ServerLevel serverLevel) {
                releasePois(serverLevel, villager);
            }
            CompoundTag tag = entity.saveWithoutId(new CompoundTag());
            for (String key : TRANSIENT_KEYS) {
                tag.remove(key);
            }
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            entity.discard();
            return Optional.of(new CapturedEntityData(id, tag));
        } catch (RuntimeException e) {
            LOGGER.warn("Capture failed for {}", entity.getType(), e);
            return Optional.empty();
        }
    }

    private static void releasePois(ServerLevel level, Villager villager) {
        for (MemoryModuleType<GlobalPos> memory : POI_MEMORIES) {
            villager.getBrain().getMemory(memory)
                    .filter(site -> site.dimension() == level.dimension())
                    .ifPresent(site -> level.getPoiManager().release(site.pos()));
        }
    }

    private static boolean placeWithoutCollision(ServerLevel level, Entity entity, Vec3 pos, float yaw) {
        for (double lift = 0.0D; lift <= MAX_LIFT; lift += LIFT_STEP) {
            entity.moveTo(pos.x, pos.y + lift, pos.z, yaw, 0.0F);
            if (level.noCollision(entity)) {
                return true;
            }
        }
        return false;
    }

    public static Optional<Entity> release(ServerLevel level, CapturedEntityData data, Vec3 pos, float yaw) {
        if (level == null || data == null || pos == null) {
            return Optional.empty();
        }
        Optional<EntityType<?>> type = BuiltInRegistries.ENTITY_TYPE.getOptional(data.entityType());
        if (type.isEmpty() || !isAllowedType(type.get())) {
            return Optional.empty();
        }
        try {
            Entity entity = type.get().create(level);
            if (entity == null || !isHostileAllowed(entity)
                    || entity instanceof Enemy && level.getDifficulty() == Difficulty.PEACEFUL) {
                return Optional.empty();
            }
            entity.load(data.tag().copy());
            entity.setYHeadRot(yaw);
            if (entity instanceof LivingEntity living) {
                living.setYBodyRot(yaw);
            }
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            if (!placeWithoutCollision(level, entity, pos, yaw) || !level.addFreshEntity(entity)) {
                return Optional.empty();
            }
            return Optional.of(entity);
        } catch (RuntimeException e) {
            LOGGER.warn("Release failed for {}", data.entityType(), e);
            return Optional.empty();
        }
    }
}
