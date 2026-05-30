package cn.oneachina.captureSpawn.listener;

import cn.oneachina.captureSpawn.CaptureSpawn;
import cn.oneachina.captureSpawn.format.EntityInfoFormatter;
import cn.oneachina.captureSpawn.item.BallData;
import cn.oneachina.captureSpawn.item.BallItemService;
import cn.oneachina.captureSpawn.item.ItemFactory;
import cn.oneachina.captureSpawn.logging.BallLogEntry;
import cn.oneachina.captureSpawn.logging.BallLogService;
import cn.oneachina.captureSpawn.nbt.NbtApiBridge;
import cn.oneachina.captureSpawn.nbt.NbtPayloadCodec;
import cn.oneachina.captureSpawn.protection.OwnerUtil;
import cn.oneachina.captureSpawn.protection.ProtectionHooks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class DirectInteractListener implements Listener {
    private final CaptureSpawn plugin;
    private final BallItemService ballItemService;
    private final ItemFactory itemFactory;
    private final NbtApiBridge nbtBridge;
    private final EntityInfoFormatter formatter;
    private final BallLogService logService;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public DirectInteractListener(
            CaptureSpawn plugin,
            BallItemService ballItemService,
            ItemFactory itemFactory,
            NbtApiBridge nbtBridge,
            EntityInfoFormatter formatter,
            BallLogService logService
    ) {
        this.plugin = plugin;
        this.ballItemService = ballItemService;
        this.itemFactory = itemFactory;
        this.nbtBridge = nbtBridge;
        this.formatter = formatter;
        this.logService = logService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCapture(PlayerInteractEntityEvent event) {
        if (!isDirectMode()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        BallData data = ballItemService.read(hand);
        if (data == null || data.captured()) {
            return;
        }
        if (!(event.getRightClicked() instanceof LivingEntity living)) {
            return;
        }

        if (!checkWorldAllowed(player.getWorld())) {
            send(player, "messages.capture.invalid-world", "&c当前世界不允许捕捉。");
            return;
        }
        if (!checkCooldown(player)) {
            send(player, "messages.capture.cooldown", "&e冷却中，请稍后再试。");
            return;
        }
        if (plugin.getConfig().getBoolean("capture.require-permission", true)
                && !player.hasPermission(resolvePerm("permissions.capture", "capturespawn.capture"))) {
            send(player, "messages.capture.no-permission", "&c你没有捕捉权限。");
            return;
        }
        if (!canCaptureType(player, living.getType())) {
            send(player, "messages.capture.type-blocked", "&c该生物不可捕捉。");
            return;
        }
        if (plugin.getConfig().getBoolean("protection.enabled", true)) {
            boolean requireBuild = plugin.getConfig().getBoolean("protection.capture-requires-build", false);
            boolean allowed = ProtectionHooks.canCapture(player, living, requireBuild);
            if (!allowed) {
                send(player, "messages.protection.capture", "&c该区域不允许捕捉。");
                logService.log(BallLogEntry.of(playerRef(player), "CAPTURE", living.getType().name(), living.getLocation(), "DENIED", "protection"));
                return;
            }
        }
        if (!checkOwner(player, living)) {
            logService.log(BallLogEntry.of(playerRef(player), "CAPTURE", living.getType().name(), living.getLocation(), "DENIED", "not_owner"));
            return;
        }

        String snbt = nbtBridge.saveToSnbt(living, event.getPlayer());
        if (snbt == null || snbt.isBlank()) {
            send(player, "messages.capture.nbt-failed", "&c捕捉失败：NBT 读取失败。");
            logService.log(BallLogEntry.of(playerRef(player), "CAPTURE", living.getType().name(), living.getLocation(), "FAIL", "save_nbt_failed"));
            return;
        }
        String format = plugin.getConfig().getString("storage.nbt-format", "GZIP_BASE64");
        String payload = NbtPayloadCodec.encode(snbt, format);
        int maxBytes = Math.max(1024, plugin.getConfig().getInt("storage.max-bytes", 131072));
        int currentBytes = payload == null ? 0 : payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (currentBytes > maxBytes) {
            send(player, "messages.capture.too-large", "&c捕捉失败：数据过大。");
            logService.log(BallLogEntry.of(playerRef(player), "CAPTURE", living.getType().name(), living.getLocation(), "FAIL", "too_large"));
            return;
        }

        BallData filledData = new BallData(true, living.getType().name(), payload);
        ItemStack filledBall = itemFactory.createFilledBall(
                formatter.displayName(living),
                formatter.extraLoreLines(living),
                filledData
        );

        living.remove();
        replaceOneMainHandWith(player, filledBall);
        markCooldown(player);
        send(player, "messages.capture.success", "&a捕捉成功。");
        logService.log(BallLogEntry.of(playerRef(player), "CAPTURE", living.getType().name(), living.getLocation(), "SUCCESS", ""));
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRelease(PlayerInteractEvent event) {
        if (!isDirectMode()) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        BallData data = ballItemService.read(hand);
        if (data == null || !data.captured()) {
            return;
        }

        if (!checkWorldAllowed(player.getWorld())) {
            send(player, "messages.release.invalid-world", "&c当前世界不允许放出。");
            return;
        }
        if (!checkCooldown(player)) {
            send(player, "messages.release.cooldown", "&e冷却中，请稍后再试。");
            return;
        }
        if (plugin.getConfig().getBoolean("release.require-permission", true)
                && !player.hasPermission(resolvePerm("permissions.release", "capturespawn.release"))) {
            send(player, "messages.release.no-permission", "&c你没有放出权限。");
            return;
        }
        if (data.entityType() == null || data.entityNbt() == null) {
            send(player, "messages.release.invalid-data", "&c放出失败：球内数据损坏。");
            return;
        }

        Location releaseLoc = resolveReleaseLocation(player, event);
        if (releaseLoc == null) {
            send(player, "messages.release.no-space", "&c放出失败：没有安全空间。");
            return;
        }
        if (plugin.getConfig().getBoolean("protection.enabled", true)) {
            boolean requireBuild = plugin.getConfig().getBoolean("protection.release-requires-build", false);
            boolean allowed = ProtectionHooks.canRelease(player, releaseLoc, requireBuild);
            if (!allowed) {
                send(player, "messages.release.spawn-denied", "&c由于权限原因生成失败（检查领地设置是否开启允许自定义怪物生成）。");
                logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "DENIED", "protection"));
                return;
            }
        }

        EntityType type;
        try {
            type = EntityType.valueOf(data.entityType());
        } catch (Exception ex) {
            send(player, "messages.release.invalid-data", "&c放出失败：球内数据损坏。");
            logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "FAIL", "invalid_entity_type"));
            return;
        }

        Entity spawned;
        try {
            spawned = releaseLoc.getWorld().spawnEntity(releaseLoc, type);
        } catch (Exception ex) {
            send(player, "messages.release.spawn-failed", "&c放出失败：无法生成实体。");
            logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "FAIL", "spawn_failed"));
            return;
        }
        if (spawned == null || !spawned.isValid() || spawned.isDead()) {
            send(player, "messages.release.spawn-denied", "&c由于权限原因生成失败（检查领地设置是否开启允许自定义怪物生成）。");
            logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "DENIED", "spawn_denied"));
            return;
        }

        String snbt = NbtPayloadCodec.decodeToSnbt(data.entityNbt());
        if (nbtBridge.loadFromSnbt(spawned, snbt, player)) {
            spawned.remove();
            send(player, "messages.release.nbt-failed", "&c放出失败：NBT 恢复失败。");
            logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "FAIL", "nbt_failed"));
            return;
        }

        boolean consumeFilled = plugin.getConfig().getBoolean("release.consume-filled", false);
        if (consumeFilled) {
            consumeOneMainHand(player);
        } else {
            replaceOneMainHandWith(player, itemFactory.createEmptyBall());
        }
        markCooldown(player);
        send(player, "messages.release.success", "&a放出成功。");
        logService.log(BallLogEntry.of(playerRef(player), "RELEASE", data.entityType(), releaseLoc, "SUCCESS", ""));
        event.setCancelled(true);
    }

    private static BallLogEntry.PlayerRef playerRef(Player player) {
        if (player == null) {
            return new BallLogEntry.PlayerRef(null, "");
        }
        return new BallLogEntry.PlayerRef(player.getUniqueId(), player.getName());
    }

    private boolean isDirectMode() {
        String mode = plugin.getConfig().getString("interaction-mode", "DIRECT");
        return mode != null && mode.equalsIgnoreCase("DIRECT");
    }

    private boolean checkWorldAllowed(World world) {
        Set<String> enabled = plugin.getConfig().getStringList("capture.enabled-worlds")
                .stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        Set<String> disabled = plugin.getConfig().getStringList("capture.disabled-worlds")
                .stream().map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        String current = world.getName().toLowerCase(Locale.ROOT);

        if (!enabled.isEmpty() && !enabled.contains(current)) {
            return false;
        }
        return !disabled.contains(current);
    }

    private boolean checkCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long until = cooldownUntil.get(player.getUniqueId());
        return until == null || now >= until;
    }

    private void markCooldown(Player player) {
        int ticks = Math.max(0, plugin.getConfig().getInt("capture.cooldown-ticks", 8));
        if (ticks <= 0) {
            cooldownUntil.remove(player.getUniqueId());
            return;
        }
        cooldownUntil.put(player.getUniqueId(), System.currentTimeMillis() + ticks * 50L);
    }

    private boolean canCaptureType(Player player, EntityType type) {
        if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND
                || type == EntityType.ENDER_DRAGON || type == EntityType.WITHER) {
            return false;
        }

        List<String> whitelist = plugin.getConfig().getStringList("capture.whitelist-types");
        if (!whitelist.isEmpty()) {
            Set<String> allow = whitelist.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
            return allow.contains(type.name());
        }

        List<String> blacklist = plugin.getConfig().getStringList("capture.blacklist-types");
        if (blacklist.isEmpty()) {
            return true;
        }
        if (player.hasPermission(resolvePerm("permissions.bypass-blacklist", "capturespawn.bypass.blacklist"))) {
            return true;
        }
        Set<String> deny = blacklist.stream().map(s -> s.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return !deny.contains(type.name());
    }

    private boolean checkOwner(Player player, LivingEntity entity) {
        if (!plugin.getConfig().getBoolean("capture.owner-protection.enabled", true)) {
            return true;
        }
        UUID ownerUuid = OwnerUtil.getEntityOwner(entity);
        if (ownerUuid == null) {
            return true;
        }
        if (ownerUuid.equals(player.getUniqueId())) {
            return true;
        }
        if (player.hasPermission(resolvePerm("permissions.bypass-owner", "capturespawn.bypass.owner"))) {
            return true;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerUuid);
        String ownerName = owner.getName() != null ? owner.getName() : ownerUuid.toString();
        String template = plugin.getConfig().getString("messages.capture.not-owner", "&c你不能捕捉属于 &e{owner}&c 的驯养生物！");
        String msg = template.replace("{owner}", ownerName);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        return false;
    }

    private Location resolveReleaseLocation(Player player, PlayerInteractEvent event) {
        boolean safeSpawn = plugin.getConfig().getBoolean("release.safe-spawn", true);
        Location base;
        if (event.getClickedBlock() != null) {
            BlockFace face = event.getBlockFace() == null ? BlockFace.UP : event.getBlockFace();
            base = event.getClickedBlock().getRelative(face).getLocation().add(0.5, 0.0, 0.5);
        } else {
            Block target = player.getTargetBlockExact(6);
            if (target != null) {
                base = target.getLocation().add(0.5, 1.0, 0.5);
            } else {
                Vector dir = player.getLocation().getDirection().normalize().multiply(2.0);
                base = player.getEyeLocation().add(dir);
            }
        }
        if (!safeSpawn) {
            return base;
        }
        return findSafeLocation(base);
    }

    private Location findSafeLocation(Location base) {
        World world = base.getWorld();
        if (world == null) {
            return null;
        }
        Location start = base.clone();
        for (int dy = 0; dy <= 4; dy++) {
            Location feet = start.clone().add(0, dy, 0);
            Block feetBlock = world.getBlockAt(feet);
            Block headBlock = world.getBlockAt(feet.clone().add(0, 1, 0));
            Block floor = world.getBlockAt(feet.clone().add(0, -1, 0));
            if (feetBlock.isPassable() && headBlock.isPassable() && !floor.isPassable()) {
                return feet.add(0, 0.01, 0);
            }
        }
        return null;
    }

    private void replaceOneMainHandWith(Player player, ItemStack replacement) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return;
        }
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(replacement);
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(hand);
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(replacement);
        for (ItemStack left : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), left);
        }
    }

    private void consumeOneMainHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return;
        }
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        player.getInventory().setItemInMainHand(hand);
    }

    private void send(Player player, String path, String fallback) {
        String msg = plugin.getConfig().getString(path, fallback);
        if (msg == null || msg.isBlank()) {
            return;
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    private String resolvePerm(String path, String fallback) {
        String perm = plugin.getConfig().getString(path, fallback);
        if (perm == null || perm.isBlank()) {
            return fallback;
        }
        return perm;
    }
}

