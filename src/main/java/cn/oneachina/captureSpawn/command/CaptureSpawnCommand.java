package cn.oneachina.captureSpawn.command;

import cn.oneachina.captureSpawn.CaptureSpawn;
import cn.oneachina.captureSpawn.logging.BallLogEntry;
import cn.oneachina.captureSpawn.logging.BallLogService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class CaptureSpawnCommand implements TabExecutor {
    private final CaptureSpawn plugin;
    private final BallLogService logService;

    public CaptureSpawnCommand(CaptureSpawn plugin, BallLogService logService) {
        this.plugin = plugin;
        this.logService = logService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("capturespawn")) {
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "用法: /capturespawn reload | /capturespawn log <玩家|me> <开始时间> [结束时间] [release|capture|all] [数量]");
            return true;
        }
        if (args[0].equalsIgnoreCase("debug")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令。");
                return true;
            }
            if (!sender.hasPermission("capturespawn.debug")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行该命令。");
                return true;
            }
            Player player = (Player) sender;
            boolean nowOn = plugin.toggleDebug(player);
            player.sendMessage(ChatColor.GREEN + "Debug 模式已" + (nowOn ? "开启" : "关闭") + "，将会在聊天栏显示 NBT 信息。");
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("capturespawn.reload")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行该命令。");
                return true;
            }
            boolean ok = plugin.reloadPlugin();
            sender.sendMessage(ok ? ChatColor.GREEN + "CaptureSpawn 配置已重载。" : ChatColor.RED + "重载失败，请查看控制台日志。");
            return true;
        }
        if (args[0].equalsIgnoreCase("log")) {
            if (!sender.hasPermission("capturespawn.log")) {
                sender.sendMessage(ChatColor.RED + "你没有权限执行该命令。");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.YELLOW + "用法: /capturespawn log <玩家|me> <开始时间> [结束时间] [release|capture|all] [数量]");
                sender.sendMessage(ChatColor.GRAY + "时间示例: 10m 1h 2d 1w 或 1h30m");
                return true;
            }
            String playerArg = args[1];
            if (playerArg.equalsIgnoreCase("me") && sender instanceof Player p) {
                playerArg = p.getName();
            }
            long now = System.currentTimeMillis();
            long sinceOffset = parseDurationToMillis(args[2]);
            if (sinceOffset <= 0) {
                sender.sendMessage(ChatColor.RED + "时间格式不正确。示例: 10m 1h 2d 1w 或 1h30m");
                return true;
            }
            long sinceMillis = now - sinceOffset;
            long untilMillis = now;

            int idx = 3;
            if (args.length > idx) {
                long untilOffset = parseDurationToMillis(args[idx]);
                if (untilOffset > 0) {
                    untilMillis = now - untilOffset;
                    idx++;
                }
            }
            if (sinceMillis > untilMillis) {
                long tmp = sinceMillis;
                sinceMillis = untilMillis;
                untilMillis = tmp;
            }

            String filter = args.length > idx ? args[idx] : "all";
            idx++;
            int limit = 20;
            if (args.length > idx) {
                try {
                    limit = Integer.parseInt(args[idx]);
                } catch (Exception ignored) {
                }
            }
            limit = Math.max(1, Math.min(200, limit));

            String resolvedName = resolvePlayerName(playerArg);
            List<BallLogEntry> entries = logService.query(resolvedName, sinceMillis, untilMillis, filter, limit);
            sender.sendMessage(ChatColor.GOLD + "CaptureSpawn 日志 - " + resolvedName + "（" + filter + "）");
            if (entries.isEmpty()) {
                sender.sendMessage(ChatColor.GRAY + "没有记录。");
                return true;
            }
            for (BallLogEntry e : entries) {
                sender.sendMessage(ChatColor.GRAY + e.toPrettyLine());
            }
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "用法: /capturespawn reload | /capturespawn log <玩家|me> <开始时间> [结束时间] [release|capture|all] [数量]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("capturespawn")) {
            return List.of();
        }
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("reload", "log", "debug"), args[0]);
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("log")) {
            if (args.length == 2) {
                List<String> list = new ArrayList<>();
                list.add("me");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
                return filterPrefix(list, args[1]);
            }
            if (args.length == 3) {
                return filterPrefix(Arrays.asList("10m", "30m", "1h", "6h", "12h", "1d", "3d", "7d"), args[2]);
            }
            if (args.length == 4) {
                if (args[3] != null && !args[3].isBlank() && Character.isDigit(args[3].trim().charAt(0))) {
                    return filterPrefix(Arrays.asList("10m", "30m", "1h", "6h", "12h", "1d", "3d", "7d"), args[3]);
                }
                return filterPrefix(Arrays.asList("release", "capture", "all"), args[3]);
            }
            if (args.length == 5) {
                return filterPrefix(Arrays.asList("release", "capture", "all", "10", "20", "50", "100", "200"), args[4]);
            }
            if (args.length == 6) {
                return filterPrefix(Arrays.asList("10", "20", "50", "100", "200"), args[5]);
            }
        }
        return List.of();
    }

    private static List<String> filterPrefix(List<String> candidates, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return candidates;
        }
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String c : candidates) {
            if (c.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(c);
            }
        }
        return out;
    }

    private static long parseDurationToMillis(String input) {
        if (input == null) {
            return -1;
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isBlank()) {
            return -1;
        }
        long totalSeconds = 0;
        int i = 0;
        while (i < s.length()) {
            int j = i;
            while (j < s.length() && Character.isDigit(s.charAt(j))) {
                j++;
            }
            if (j == i || j >= s.length()) {
                return -1;
            }
            long num;
            try {
                num = Long.parseLong(s.substring(i, j));
            } catch (Exception ex) {
                return -1;
            }
            char unit = s.charAt(j);
            long mul;
            switch (unit) {
                case 's':
                    mul = 1;
                    break;
                case 'm':
                    mul = 60;
                    break;
                case 'h':
                    mul = 3600;
                    break;
                case 'd':
                    mul = 86400;
                    break;
                case 'w':
                    mul = 604800;
                    break;
                default:
                    return -1;
            }
            totalSeconds += num * mul;
            i = j + 1;
        }
        return totalSeconds * 1000L;
    }

    private static String resolvePlayerName(String input) {
        if (input == null) {
            return "";
        }
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(input);
        if (off.getName() != null && !off.getName().isBlank()) {
            return off.getName();
        }
        return input;
    }
}
