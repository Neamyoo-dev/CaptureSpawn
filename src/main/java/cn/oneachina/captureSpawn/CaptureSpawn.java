package cn.oneachina.captureSpawn;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import cn.oneachina.captureSpawn.command.CaptureSpawnCommand;
import cn.oneachina.captureSpawn.format.EntityInfoFormatter;
import cn.oneachina.captureSpawn.item.BallItemService;
import cn.oneachina.captureSpawn.item.ItemFactory;
import cn.oneachina.captureSpawn.item.Keys;
import cn.oneachina.captureSpawn.item.RecipeManager;
import cn.oneachina.captureSpawn.listener.BallBlockPlaceListener;
import cn.oneachina.captureSpawn.listener.BallDropReleaseListener;
import cn.oneachina.captureSpawn.listener.CraftPermissionListener;
import cn.oneachina.captureSpawn.listener.DirectInteractListener;
import cn.oneachina.captureSpawn.logging.BallLogService;
import cn.oneachina.captureSpawn.nbt.NbtApiBridge;
import cn.oneachina.captureSpawn.throwing.BallThrower;
import cn.oneachina.captureSpawn.throwing.PacketEventsThrowListener;
import org.bukkit.NamespacedKey;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class CaptureSpawn extends JavaPlugin {
    private Keys keys;
    private ItemFactory itemFactory;
    private NamespacedKey emptyBallRecipeKey;
    private NbtApiBridge nbtApiBridge;
    private PacketListenerCommon packetEventsThrowListener;
    private BallLogService logService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.keys = new Keys(this);
        this.itemFactory = new ItemFactory(this, keys);
        this.nbtApiBridge = new NbtApiBridge();
        this.logService = new BallLogService(this);
        this.logService.start();

        if (getCommand("capturespawn") != null) {
            CaptureSpawnCommand cmd = new CaptureSpawnCommand(this, logService);
            getCommand("capturespawn").setExecutor(cmd);
            getCommand("capturespawn").setTabCompleter(cmd);
        }
        registerRuntimeComponents();

    }

    @Override
    public void onDisable() {
        unregisterPacketEventsListener();
        if (logService != null) {
            logService.stop();
        }
    }

    public boolean reloadPlugin() {
        try {
            reloadConfig();
            unregisterPacketEventsListener();
            HandlerList.unregisterAll(this);
            if (logService != null) {
                logService.stop();
                logService.start();
            }
            registerRuntimeComponents();
            return true;
        } catch (Exception ex) {
            getLogger().severe("Reload failed: " + ex.getMessage());
            return false;
        }
    }

    private void registerRuntimeComponents() {
        RecipeManager recipeManager = new RecipeManager(this, itemFactory);
        this.emptyBallRecipeKey = recipeManager.registerEmptyBallRecipe();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CraftPermissionListener(this, emptyBallRecipeKey), this);
        BallItemService ballItemService = new BallItemService(keys);
        pm.registerEvents(new BallBlockPlaceListener(this, ballItemService), this);
        pm.registerEvents(new BallDropReleaseListener(this, ballItemService, itemFactory, nbtApiBridge, logService), this);
        EntityInfoFormatter formatter = new EntityInfoFormatter();
        String mode = getConfig().getString("interaction-mode", "THROW");
        if (mode != null && mode.equalsIgnoreCase("THROW")) {
            if (PacketEvents.getAPI() == null) {
                getLogger().warning("PacketEvents API is not available. THROW mode will not work.");
                getConfig().set("interaction-mode", "DIRECT");
                pm.registerEvents(new DirectInteractListener(this, ballItemService, itemFactory, nbtApiBridge, formatter, logService), this);
                return;
            }
            BallThrower thrower = new BallThrower(this, ballItemService, itemFactory, nbtApiBridge, formatter, logService);
            this.packetEventsThrowListener = new PacketEventsThrowListener(this, thrower);
            PacketEvents.getAPI().getEventManager().registerListener(packetEventsThrowListener);
        } else {
            pm.registerEvents(new DirectInteractListener(this, ballItemService, itemFactory, nbtApiBridge, formatter, logService), this);
        }
    }

    private void unregisterPacketEventsListener() {
        if (packetEventsThrowListener == null) {
            return;
        }
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetEventsThrowListener);
        }
        packetEventsThrowListener = null;
    }

    public Keys keys() {
        return keys;
    }
}
