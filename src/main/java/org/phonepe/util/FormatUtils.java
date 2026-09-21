package org.phonepe.util;

public final class FormatUtils {

    private FormatUtils() {
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            final long tenths = (bytes * 10L + 512L) / 1024L;
            return (tenths / 10) + "." + (tenths % 10) + " KB";
        }
        if (bytes < 1024L * 1024 * 1024) {
            final long tenths = (bytes * 10L + 524288L) / (1024L * 1024L);
            return (tenths / 10) + "." + (tenths % 10) + " MB";
        }

        final long hundredths = (bytes * 100L + 536870912L) / (1024L * 1024L * 1024L);
        final long fraction = hundredths % 100;

        return (hundredths / 100) + "." + (fraction < 10 ? "0" : "") + fraction + " GB";
    }
}