package com.getsixtyfour.openvpnmgmt.utils;

import org.jetbrains.annotations.Nullable;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "StaticMethodOnlyUsedInOneClass", "ClassIndependentOfModule", "ClassUnconnectedToPackage" })
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * <p>Checks if the CharSequence contains any character in the given set of characters.</p>
     *
     * @param cs          the CharSequence to check, may be null
     * @param searchChars the chars to search for, may be null
     * @return the {@code true} if any of the chars are found, {@code false} if no match or null input
     */
    @SuppressWarnings({ "ImplicitNumericConversion", "MethodWithMultipleReturnPoints", "OverlyComplexMethod" })
    public static boolean containsAny(@Nullable CharSequence cs, @Nullable char... searchChars) {
        if ((cs == null) || (cs.length() == 0)) {
            return false;
        }
        if ((searchChars == null) || (searchChars.length == 0)) {
            return false;
        }
        int csLength = cs.length();
        int searchLength = searchChars.length;
        int csLast = csLength - 1;
        int searchLast = searchLength - 1;
        for (int i = 0; i < csLength; i++) {
            char ch = cs.charAt(i);
            for (int j = 0; j < searchLength; j++) {
                if (searchChars[j] == ch) {
                    if (Character.isHighSurrogate(ch)) {
                        if (j == searchLast) {
                            // missing low surrogate, fine, like String.indexOf(String)
                            return true;
                        }
                        if ((i < csLast) && (searchChars[j + 1] == cs.charAt(i + 1))) {
                            return true;
                        }
                    } else {
                        // ch is in the Basic Multilingual Plane
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * <p>Checks if a CharSequence is empty (""), null or whitespace only.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean isBlank(@Nullable CharSequence cs) {
        if ((cs == null) || (cs.length() == 0)) {
            return true;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>Returns either the passed in CharSequence, or if the CharSequence is
     * whitespace, empty ("") or {@code null}, the value of {@code defaultStr}.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * @param <T>        the specific kind of CharSequence
     * @param str        the CharSequence to check, may be null
     * @param defaultStr the default CharSequence to return
     * @return the passed in CharSequence, or the default
     */
    @SuppressWarnings("StandardVariableNames")
    @Nullable
    public static <T extends CharSequence> T defaultIfBlank(@Nullable T str, @Nullable T defaultStr) {
        return isBlank(str) ? defaultStr : str;
    }

    /**
     * <p>Escapes the characters in a {@code String} using backslash-based shell escaping.</p>
     *
     * <p>Deals correctly with double quotes and backslashes.</p>
     *
     * @param cs the CharSequence to escape values in, may be null
     * @return String with escaped values, {@code null} if null string input
     */
    @Nullable
    public static String escapeOpenVPN(@Nullable CharSequence cs) {
        if (cs == null) {
            return null;
        }
        if (cs.length() == 0) {
            return "";
        }
        return escapeOpenVPNStyleString(cs);
    }

    /**
     * <p>Worker method for the {@link #escapeOpenVPN(CharSequence)} method.</p>
     *
     * @param cs the CharSequence to escape values in, may be null
     * @return the escaped string
     */
    private static String escapeOpenVPNStyleString(CharSequence cs) {
        StringBuilder writer = new StringBuilder(cs.length() << 1);
        escapeOpenVPNStyleString(writer, cs);
        return writer.toString();
    }

    /**
     * <p>Worker method for the {@link #escapeOpenVPN(CharSequence)} method.</p>
     *
     * @param out the StringBuilder to receive the escaped string
     * @param cs  the CharSequence to escape values in, may be null
     */
    @SuppressWarnings({ "MagicCharacter", "HardcodedFileSeparator" })
    private static void escapeOpenVPNStyleString(StringBuilder out, CharSequence cs) {
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            char ch = cs.charAt(i);
            switch (ch) {
                case '\\':
                    out.append('\\');
                    out.append('\\');
                    break;
                case '"':
                    out.append('\\');
                    out.append('"');
                    break;
                default:
                    out.append(ch);
                    break;
            }
        }
    }
}
