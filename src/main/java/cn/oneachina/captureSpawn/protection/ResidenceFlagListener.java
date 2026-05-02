package cn.oneachina.captureSpawn.protection;

import cn.oneachina.captureSpawn.CaptureSpawn;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerCommandEvent;

public final class ResidenceFlagListener implements Listener {
    private final CaptureSpawn plugin;

    public ResidenceFlagListener(CaptureSpawn plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin() == null) {
            return;
        }
        if (!event.getPlugin().getName().equalsIgnoreCase("Residence")) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, ProtectionHooks::refreshResidenceCustomFlags);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg == null) {
            return;
        }
        if (!isResidenceReload(msg.startsWith("/") ? msg.substring(1) : msg)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, ProtectionHooks::refreshResidenceCustomFlags);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand() == null) {
            return;
        }
        if (!isResidenceReload(event.getCommand())) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, ProtectionHooks::refreshResidenceCustomFlags);
    }

    private static boolean isResidenceReload(String raw) {
        String s = raw.trim().toLowerCase();
        if (s.startsWith("residence ")) {
            s = "res " + s.substring("residence ".length());
        }
        return s.equals("res reload") || s.equals("res rl") || s.equals("res r");
    }
}
