package cn.oneachina.captureSpawn.item;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class BallItemService {
    private final Keys keys;

    public BallItemService(Keys keys) {
        this.keys = keys;
    }

    public boolean isBall(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte marker = pdc.get(keys.ball, PersistentDataType.BYTE);
        Integer version = pdc.get(keys.formatVersion, PersistentDataType.INTEGER);
        return marker != null && marker == (byte) 1 && version != null && version == 1;
    }

    public BallData read(ItemStack item) {
        if (!isBall(item)) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte captured = pdc.get(keys.captured, PersistentDataType.BYTE);
        boolean isCaptured = captured != null && captured == (byte) 1;
        String type = pdc.get(keys.entityType, PersistentDataType.STRING);
        String nbt = pdc.get(keys.entityNbt, PersistentDataType.STRING);
        return new BallData(isCaptured, type, nbt);
    }
}
