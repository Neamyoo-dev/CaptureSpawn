package cn.oneachina.captureSpawn.nbt;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.function.Consumer;

public final class NbtApiBridge {
    public NbtApiBridge() {
        if (Bukkit.getPluginManager().getPlugin("NBTAPI") == null) {
            throw new IllegalStateException("NBTAPI plugin is required but not loaded");
        }
    }

    public String saveToSnbt(Entity bukkitEntity) {
        try {
            ReadWriteNBT nbt = NBT.createNBTObject();
            NBT.get(bukkitEntity, nbt::mergeCompound);
            return nbt.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean loadFromSnbt(Entity bukkitEntity, String snbt) {
        if (snbt == null || snbt.isBlank()) {
            return false;
        }
        try {
            ReadWriteNBT tag = NBT.parseNBT(snbt);
            sanitize(tag);
            NBT.modify(bukkitEntity, (Consumer<ReadWriteNBT>) nbt -> nbt.mergeCompound(tag));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void sanitize(ReadWriteNBT compoundTag) {
        List<String> removeKeys = List.of(
                "UUID",
                "UUIDMost",
                "UUIDLeast",
                "Pos",
                "Motion",
                "Rotation",
                "OnGround",
                "PortalCooldown",
                "Passengers",
                "Leash",
                "LeashUUID",
                "WorldUUIDMost",
                "WorldUUIDLeast",
                "Dimension",
                "id"
        );
        for (String key : removeKeys) {
            compoundTag.removeKey(key);
        }
    }
}
