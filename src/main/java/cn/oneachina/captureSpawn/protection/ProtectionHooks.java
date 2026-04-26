package cn.oneachina.captureSpawn.protection;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;

public final class ProtectionHooks {
    private ProtectionHooks() {
    }

    public static boolean canInteractEntity(Player player, Entity entity) {
        Event event = createInteractEntityEvent(player, entity);
        return event == null || callCancellable(event);
    }

    public static boolean canInteractBlock(Player player, Block block, BlockFace face) {
        Event event = createInteractBlockEvent(player, block, face);
        return event == null || callCancellable(event);
    }

    public static boolean canBuild(Player player, Block block) {
        if (player == null || block == null) {
            return false;
        }
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private static boolean callCancellable(Event event) {
        Bukkit.getPluginManager().callEvent(event);
        if (event instanceof Cancellable cancellable) {
            return !cancellable.isCancelled();
        }
        return true;
    }

    private static Event createInteractEntityEvent(Player player, Entity entity) {
        try {
            return new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND);
        } catch (Throwable ignored) {
        }
        try {
            return new PlayerInteractEntityEvent(player, entity);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Event createInteractBlockEvent(Player player, Block block, BlockFace face) {
        ItemStack item = player.getInventory().getItemInMainHand();
        try {
            return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, block, face, EquipmentSlot.HAND);
        } catch (Throwable ignored) {
        }
        try {
            return new PlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, item, block, face);
        } catch (Throwable ignored) {
        }
        try {
            Class<?> clazz = Class.forName("org.bukkit.event.player.PlayerInteractEvent");
            for (Constructor<?> ctor : clazz.getConstructors()) {
                Class<?>[] p = ctor.getParameterTypes();
                if (p.length < 5) {
                    continue;
                }
                if (p[0] != Player.class || p[1] != Action.class) {
                    continue;
                }
                Object[] args = new Object[p.length];
                args[0] = player;
                args[1] = Action.RIGHT_CLICK_BLOCK;
                args[2] = item;
                args[3] = block;
                args[4] = face;
                if (p.length >= 6 && p[5] == EquipmentSlot.class) {
                    args[5] = EquipmentSlot.HAND;
                }
                return (Event) ctor.newInstance(args);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
