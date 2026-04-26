package cn.oneachina.captureSpawn.logging;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class ReverseLineReader {
    private ReverseLineReader() {
    }

    public static List<String> readLastLines(File file, int maxLines) {
        int limit = Math.max(1, maxLines);
        List<String> lines = new ArrayList<>(Math.min(200, limit));
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long pointer = raf.length() - 1;
            if (pointer < 0) {
                return lines;
            }
            byte[] buf = new byte[8192];
            int bufPos = 0;
            while (pointer >= 0 && lines.size() < limit) {
                raf.seek(pointer);
                int b = raf.read();
                if (b == '\n') {
                    if (bufPos > 0) {
                        lines.add(decodeReverse(buf, bufPos));
                        bufPos = 0;
                    } else {
                        lines.add("");
                    }
                } else if (b != '\r') {
                    if (bufPos >= buf.length) {
                        lines.add(decodeReverse(buf, bufPos));
                        bufPos = 0;
                    }
                    buf[bufPos++] = (byte) b;
                }
                pointer--;
            }
            if (bufPos > 0 && lines.size() < limit) {
                lines.add(decodeReverse(buf, bufPos));
            }
        } catch (Exception ignored) {
        }
        return lines;
    }

    private static String decodeReverse(byte[] buf, int len) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            out[i] = buf[len - i - 1];
        }
        return new String(out, StandardCharsets.UTF_8);
    }
}
