package org.phonepe.util;

public final class StringUtils {
    
    private StringUtils() {
    }

    public static boolean containsIgnoreCase(String value, String target) {
        return indexOfIgnoreCase(value, target) >= 0;
    }

    public static int indexOfIgnoreCase(String value, String target) {
        return indexOfIgnoreCase(value, target, 0);
    }

    public static int indexOfIgnoreCase(String value, String target, int fromIndex) {
        final int max = value.length() - target.length();

        for (int i = Math.max(0, fromIndex); i <= max; i++) {
            if (value.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }

        return -1;
    }

    public static int skipSpaces(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    public static String trimLine(String value) {
        int start = 0;
        while (start < value.length() && Character.isWhitespace(value.charAt(start))) {
            start++;
        }

        int end = value.length();
        while (end > start && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }

        if (start == 0 && end == value.length()) {
            return value;
        }

        return value.substring(start, end);
    }
}
