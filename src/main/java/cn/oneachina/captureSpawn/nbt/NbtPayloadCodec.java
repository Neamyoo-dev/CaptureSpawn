package cn.oneachina.captureSpawn.nbt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class NbtPayloadCodec {
    private static final String PREFIX = "GZ:";

    private NbtPayloadCodec() {
    }

    public static String encode(String snbt, String format) {
        if (snbt == null) {
            return null;
        }
        if (format != null && format.equalsIgnoreCase("GZIP_BASE64")) {
            return PREFIX + gzipBase64(snbt);
        }
        return snbt;
    }

    public static String decodeToSnbt(String payload) {
        if (payload == null) {
            return null;
        }
        if (payload.startsWith(PREFIX)) {
            return ungzipBase64(payload.substring(PREFIX.length()));
        }
        if (looksLikeGzipBase64(payload)) {
            String decoded = ungzipBase64(payload);
            if (decoded != null) {
                return decoded;
            }
        }
        return payload;
    }

    private static String gzipBase64(String text) {
        try {
            byte[] input = text.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(input);
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception ex) {
            return text;
        }
    }

    private static String ungzipBase64(String base64) {
        try {
            byte[] gz = Base64.getDecoder().decode(base64);
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean looksLikeGzipBase64(String payload) {
        if (payload.length() < 8) {
            return false;
        }
        if (payload.startsWith("{") || payload.startsWith("[")) {
            return false;
        }
        if (payload.startsWith(PREFIX)) {
            return true;
        }
        return payload.startsWith("H4sI");
    }
}
