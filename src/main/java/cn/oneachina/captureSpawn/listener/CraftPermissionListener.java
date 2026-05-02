package cn.oneachina.captureSpawn.listener;

import cn.oneachina.captureSpawn.CaptureSpawn;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

public final class CraftPermissionListener implements Listener {
    private final CaptureSpawn plugin;
    private final NamespacedKey emptyBallRecipeKey;

    public CraftPermissionListener(CaptureSpawn plugin, NamespacedKey emptyBallRecipeKey) {
        this.plugin = plugin;
        this.emptyBallRecipeKey = emptyBallRecipeKey;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (emptyBallRecipeKey == null) {
            return;
        }
        if (!plugin.getConfig().getBoolean("craft.require-permission", false)) {
            return;
        }

        Recipe recipe = event.getRecipe();
        if (!(recipe instanceof org.bukkit.Keyed keyed)) {
            return;
        }
        if (!emptyBallRecipeKey.equals(keyed.getKey())) {
            return;
        }

        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }

        String perm = plugin.getConfig().getString("craft.permission", "capturespawn.craft");
        if (!perm.isBlank() && !player.hasPermission(perm)) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }
}
