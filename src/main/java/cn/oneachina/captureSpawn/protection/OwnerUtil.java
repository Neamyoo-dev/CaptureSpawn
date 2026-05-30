package cn.oneachina.captureSpawn.protection;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;

import java.util.UUID;
import java.util.function.Function;

public final class OwnerUtil {

    private OwnerUtil() {
    }

    public static UUID getEntityOwner(LivingEntity entity) {
        if (entity instanceof Tameable tameable && tameable.isTamed()) {
            AnimalTamer owner = tameable.getOwner();
            if (owner != null) return owner.getUniqueId();
            return readOwnerFromNbt(entity);
        }
        return null;
    }

    private static UUID readOwnerFromNbt(LivingEntity entity) {
        try {
            return NBT.get(entity, (Function<ReadableNBT, UUID>) nbt -> {
                if (!nbt.hasTag("Owner")) return null;
                int[] arr = nbt.getIntArray("Owner");
                if (arr == null || arr.length != 4) return null;
                long most = ((long) arr[0] << 32) | (arr[1] & 0xFFFFFFFFL);
                long least = ((long) arr[2] << 32) | (arr[3] & 0xFFFFFFFFL);
                return new UUID(most, least);
            });
        } catch (Exception e) {
            return null;
        }
    }
}
