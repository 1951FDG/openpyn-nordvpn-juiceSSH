package com.getsixtyfour.openvpnmgmt.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Apache Software Foundation
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "ClassIndependentOfModule", "ClassUnconnectedToPackage" })
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
    public static @NotNull <T extends CharSequence> T defaultIfBlank(@Nullable T str, @NotNull T defaultStr) {
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
    public static @Nullable CharSequence escapeString(@Nullable CharSequence cs) {
        if ((cs == null) || (cs.length() == 0)) {
            return cs;
        }
        return escapeBashStyleString(cs);
    }

    /**
     * <p>Worker method for the {@link #escapeString(CharSequence)} method.</p>
     *
     * @param cs the CharSequence to escape values in, may be null
     * @return the escaped string
     */
    @SuppressWarnings("MagicCharacter")
    private static String escapeBashStyleString(CharSequence cs) {
        StringBuilder sb = new StringBuilder(cs.length() << 1);
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            char ch = cs.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append('\\');
                    sb.append('\\');
                    break;
                case '"':
                    sb.append('\\');
                    sb.append('"');
                    break;
                default:
                    sb.append(ch);
                    break;
            }
        }
        return sb.toString();
    }
}