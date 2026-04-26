package cn.oneachina.captureSpawn.logging;

import org.bukkit.Location;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public record BallLogEntry(
        long timeMillis,
        UUID playerUuid,
        String playerName,
        String action,
        String entityType,
        String world,
        double x,
        double y,
        double z,
        String result,
        String details
) {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    public static BallLogEntry of(PlayerRef player, String action, String entityType, Location loc, String result, String details) {
        String world = loc == null || loc.getWorld() == null ? "" : loc.getWorld().getName();
        double x = loc == null ? 0 : loc.getX();
        double y = loc == null ? 0 : loc.getY();
        double z = loc == null ? 0 : loc.getZ();
        return new BallLogEntry(
                System.currentTimeMillis(),
                player == null ? null : player.uuid(),
                player == null ? "" : player.name(),
                action,
                entityType == null ? "" : entityType,
                world,
                x,
                y,
                z,
                result == null ? "" : result,
                details == null ? "" : details
        );
    }

    public String toLine() {
        return timeMillis + "\t"
                + (playerUuid == null ? "" : playerUuid) + "\t"
                + esc(playerName) + "\t"
                + esc(action) + "\t"
                + esc(entityType) + "\t"
                + esc(world) + "\t"
                + x + "\t"
                + y + "\t"
                + z + "\t"
                + esc(result) + "\t"
                + esc(details);
    }

    public String toPrettyLine() {
        return TS.format(Instant.ofEpochMilli(timeMillis))
                + " " + playerName
                + " " + action
                + " " + entityType
                + " " + world + " " + fmt(x) + " " + fmt(y) + " " + fmt(z)
                + " " + result
                + (details == null || details.isBlank() ? "" : " " + details);
    }

    public static BallLogEntry parse(String line) {
        if (line == null) {
            return null;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 10) {
            return null;
        }
        long t;
        try {
            t = Long.parseLong(parts[0]);
        } catch (Exception ex) {
            return null;
        }
        UUID uuid = null;
        if (!parts[1].isBlank()) {
            try {
                uuid = UUID.fromString(parts[1]);
            } catch (Exception ignored) {
            }
        }
        String name = unesc(parts[2]);
        String action = unesc(parts[3]);
        String entityType = unesc(parts[4]);
        String world = unesc(parts[5]);
        double x = parseDouble(parts, 6);
        double y = parseDouble(parts, 7);
        double z = parseDouble(parts, 8);
        String result = unesc(parts[9]);
        String details = parts.length >= 11 ? unesc(parts[10]) : "";
        return new BallLogEntry(t, uuid, name, action, entityType, world, x, y, z, result, details);
    }

    private static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
    }

    private static String unesc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\\\", "\\");
    }

    private static double parseDouble(String[] parts, int idx) {
        if (idx >= parts.length) {
            return 0;
        }
        try {
            return Double.parseDouble(parts[idx]);
        } catch (Exception ex) {
            return 0;
        }
    }

    private static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    public record PlayerRef(UUID uuid, String name) {
    }
}
