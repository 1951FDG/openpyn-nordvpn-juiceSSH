package org.apache.hadoop.hdfs.util;

import androidx.annotation.NonNull;

public class PosixUserNameChecker {

    @SuppressWarnings({ "CharacterComparison", "MagicCharacter", "ImplicitNumericConversion", "MethodWithMultipleReturnPoints" })
    public boolean isValidUserName(@NonNull String username) {
        if (username.isEmpty()) {
            return false;
        }
        int len = username.length();
        char[] carray = new char[len];
        username.getChars(0, len, carray, 0);
        char fc = carray[0];
        if (!(((fc >= 'a') && (fc <= 'z')) || (fc == '_'))) {
            return false;
        }
        for (int i = 1; i < len; i++) {
            char c = carray[i];
            if (!(((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || (c == '-') || (c
                    == '_') || ((c == '$') && (i == (len - 1))))) {
                return false;
            }
        }
        return true;
    }
}
