package cn.oneachina.captureSpawn.listener;

import cn.oneachina.captureSpawn.CaptureSpawn;
import cn.oneachina.captureSpawn.item.BallItemService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public final class BallBlockPlaceListener implements Listener {
    private final CaptureSpawn plugin;
    private final BallItemService ballItemService;

    public BallBlockPlaceListener(CaptureSpawn plugin, BallItemService ballItemService) {
        this.plugin = plugin;
        this.ballItemService = ballItemService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack inHand = event.getItemInHand();
        if (!ballItemService.isBall(inHand)) {
            return;
        }
        event.setCancelled(true);

        String msg = plugin.getConfig().getString("messages.ball-cannot-place", "&e精灵球不能被放置为方块。");
        if (msg != null && !msg.isBlank()) {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}

