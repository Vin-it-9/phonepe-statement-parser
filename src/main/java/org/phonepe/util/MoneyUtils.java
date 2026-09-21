package org.phonepe.util;

import java.io.IOException;

public final class MoneyUtils {

    private static final long[] POW10 = {
            1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
            10_000_000L, 100_000_000L, 1_000_000_000L, 10_000_000_000L,
            100_000_000_000L, 1_000_000_000_000L, 10_000_000_000_000L,
            100_000_000_000_000L, 1_000_000_000_000_000L,
            10_000_000_000_000_000L, 100_000_000_000_000_000L,
            1_000_000_000_000_000_000L
    };

    private MoneyUtils() {
    }

    public static void appendMoney(Appendable out, long paise) throws IOException {
        if (paise < 0) {
            out.append('-');
            paise = Math.negateExact(paise);
        }

        final long rupees = paise / 100;
        final int cents = (int) (paise % 100);

        out.append("Rs ");
        appendGrouped(out, rupees);
        out.append('.');
        out.append((char) ('0' + cents / 10));
        out.append((char) ('0' + cents % 10));
    }

    private static void appendGrouped(Appendable out, long value) throws IOException {
        if (value == 0) {
            out.append('0');
            return;
        }

        final int digitCount = digitCount(value);
        long divisor = POW10[digitCount - 1];
        int posInGroup = digitCount % 3;
        if (posInGroup == 0) {
            posInGroup = 3;
        }

        long remaining = value;
        for (int i = 0; i < digitCount; i++) {
            final int digit = (int) (remaining / divisor);
            out.append((char) ('0' + digit));
            remaining -= (long) digit * divisor;
            divisor /= 10;
            posInGroup--;

            if (posInGroup == 0 && i < digitCount - 1) {
                out.append(',');
                posInGroup = 3;
            }
        }
    }

    private static int digitCount(long value) {
        for (int i = POW10.length - 1; i >= 0; i--) {
            if (value >= POW10[i]) {
                return i + 1;
            }
        }
        return 1;
    }
}