package org.phonepe.parser;

import org.phonepe.config.domain.TransactionHeader;
import org.phonepe.util.StringUtils;

public final class TransactionParser {

    private static final int JAN = ('j' << 16) | ('a' << 8) | 'n';
    private static final int FEB = ('f' << 16) | ('e' << 8) | 'b';
    private static final int MAR = ('m' << 16) | ('a' << 8) | 'r';
    private static final int APR = ('a' << 16) | ('p' << 8) | 'r';
    private static final int MAY = ('m' << 16) | ('a' << 8) | 'y';
    private static final int JUN = ('j' << 16) | ('u' << 8) | 'n';
    private static final int JUL = ('j' << 16) | ('u' << 8) | 'l';
    private static final int AUG = ('a' << 16) | ('u' << 8) | 'g';
    private static final int SEP = ('s' << 16) | ('e' << 8) | 'p';
    private static final int OCT = ('o' << 16) | ('c' << 8) | 't';
    private static final int NOV = ('n' << 16) | ('o' << 8) | 'v';
    private static final int DEC = ('d' << 16) | ('e' << 8) | 'c';

    private TransactionParser() {
    }

    public static TransactionHeader parseTransactionHeader(String line) {
        final int len = line.length();
        int p = StringUtils.skipSpaces(line, 0);
        final int dateStart = p; // captured once, reused later — old code recomputed this

        if (p + 10 >= len) {
            return null;
        }

        if (!isMonthAt(line, p)) {
            return null;
        }

        final int monthEnd = p + 3;
        if (monthEnd >= len || !isSpaceFast(line.charAt(monthEnd))) {
            return null;
        }

        p = StringUtils.skipSpaces(line, monthEnd);
        final int dayStart = p;

        if (p >= len || !isDigitFast(line.charAt(p))) {
            return null;
        }

        p++;
        if (p < len && isDigitFast(line.charAt(p))) {
            p++;
        }

        if (p >= len || line.charAt(p) != ',') {
            return null;
        }

        final int dayEnd = p;
        p++;
        p = StringUtils.skipSpaces(line, p);

        if (p + 4 > len) {
            return null;
        }

        for (int i = 0; i < 4; i++) {
            if (!isDigitFast(line.charAt(p + i))) {
                return null;
            }
        }

        final int yearEnd = p + 4;
        if (yearEnd < len && isDigitFast(line.charAt(yearEnd))) {
            return null;
        }

        if (dayEnd <= dayStart) {
            return null;
        }

        final String transactionPart = line.substring(yearEnd).strip();
        if (transactionPart.isEmpty()) {
            return null;
        }

        final String date = line.substring(dateStart, yearEnd);
        return new TransactionHeader(date, transactionPart);
    }

    private static boolean isMonthAt(String value, int index) {
        if (index + 3 > value.length()) {
            return false;
        }

        final int key = pack(
                Character.toLowerCase(value.charAt(index)),
                Character.toLowerCase(value.charAt(index + 1)),
                Character.toLowerCase(value.charAt(index + 2)));

        return switch (key) {
            case JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC -> true;
            default -> false;
        };
    }

    private static int pack(char a, char b, char c) {
        return ((a & 0xFF) << 16) | ((b & 0xFF) << 8) | (c & 0xFF);
    }

    private static boolean isDigitFast(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isSpaceFast(char c) {
        return c == ' ' || c == '\t' || c == '\r' || c == '\n' || c == '\f';
    }

    public static long extractAmountAfterINR(String value) {
        final int inr = indexOfInr(value);
        if (inr < 0) {
            return -1;
        }

        final int len = value.length();
        int p = StringUtils.skipSpaces(value, inr + 3);
        final int start = p;

        while (p < len) {
            final char c = value.charAt(p);
            if (!(isDigitFast(c) || c == ',' || c == '.')) {
                break;
            }
            p++;
        }

        if (p == start) {
            return -1;
        }

        return parseMoneyToken(value, start, p);
    }

    // manual word-boundary scan for "INR" — no toLowerCase() copy of the whole string
    private static int indexOfInr(String value) {
        final int len = value.length();
        for (int i = 0; i + 3 <= len; i++) {
            final char c0 = value.charAt(i);
            if (c0 != 'I' && c0 != 'i') {
                continue;
            }
            final char c1 = value.charAt(i + 1);
            final char c2 = value.charAt(i + 2);
            if ((c1 == 'N' || c1 == 'n') && (c2 == 'R' || c2 == 'r')) {
                final boolean leftOk = i == 0 || !Character.isLetterOrDigit(value.charAt(i - 1));
                final boolean rightOk = i + 3 >= len || !Character.isLetterOrDigit(value.charAt(i + 3));
                if (leftOk && rightOk) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static long extractTrailingAmount(String line) {
        int end = line.length();
        while (end > 0 && isSpaceFast(line.charAt(end - 1))) {
            end--;
        }

        if (end < 4) {
            return -1;
        }

        if (!isDigitFast(line.charAt(end - 1))) {
            return -1;
        }
        if (!isDigitFast(line.charAt(end - 2))) {
            return -1;
        }
        if (line.charAt(end - 3) != '.') {
            return -1;
        }

        int start = end - 4;
        while (start > 0) {
            final char c = line.charAt(start - 1);
            if (isDigitFast(c) || c == ',') {
                start--;
                continue;
            }
            break;
        }

        return parseMoneyToken(line, start, end);
    }

    public static long parseFullMoneyLine(String line) {
        final int len = line.length();
        int start = 0;
        while (start < len && isSpaceFast(line.charAt(start))) {
            start++;
        }

        int end = len;
        while (end > start && isSpaceFast(line.charAt(end - 1))) {
            end--;
        }

        if (start == end) {
            return -1;
        }

        for (int i = start; i < end; i++) {
            final char c = line.charAt(i);
            if (!(isDigitFast(c) || c == ',' || c == '.')) {
                return -1;
            }
        }

        return parseMoneyToken(line, start, end);
    }

    private static long parseMoneyToken(CharSequence value, int start, int end) {
        long rupees = 0;
        long fraction = 0;
        boolean decimalSeen = false;
        int decimalDigits = 0;
        boolean hasDigit = false;

        for (int i = start; i < end; i++) {
            final char c = value.charAt(i);
            if (c == ',') {
                continue;
            }
            if (c == '.') {
                if (decimalSeen) {
                    return -1;
                }
                decimalSeen = true;
                continue;
            }
            if (!isDigitFast(c)) {
                return -1;
            }

            hasDigit = true;
            final int digit = c - '0';

            if (!decimalSeen) {
                rupees = Math.addExact(Math.multiplyExact(rupees, 10), digit);
            } else {
                if (decimalDigits >= 2) {
                    return -1;
                }
                fraction = fraction * 10 + digit;
                decimalDigits++;
            }
        }

        if (!hasDigit) {
            return -1;
        }

        if (decimalDigits == 1) {
            fraction *= 10;
        }

        return Math.addExact(Math.multiplyExact(rupees, 100), fraction);
    }
}