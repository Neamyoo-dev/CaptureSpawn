package cn.oneachina.captureSpawn.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public final NamespacedKey ball;
    public final NamespacedKey captured;
    public final NamespacedKey entityType;
    public final NamespacedKey entityNbt;
    public final NamespacedKey formatVersion;
    public final NamespacedKey uid;
    public final NamespacedKey shooter;
    public final NamespacedKey display;

    public Keys(Plugin plugin) {
        this.ball = new NamespacedKey(plugin, "ball");
        this.captured = new NamespacedKey(plugin, "captured");
        this.entityType = new NamespacedKey(plugin, "entity_type");
        this.entityNbt = new NamespacedKey(plugin, "entity_nbt");
        this.formatVersion = new NamespacedKey(plugin, "format_version");
        this.uid = new NamespacedKey(plugin, "uid");
        this.shooter = new NamespacedKey(plugin, "shooter");
        this.display = new NamespacedKey(plugin, "display");
    }
}
