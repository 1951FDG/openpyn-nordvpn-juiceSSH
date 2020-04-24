/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfs.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class PosixPathNameChecker {

  /** The directory separator, a slash. */
  public static final String SEPARATOR = "/";
  public static final char SEPARATOR_CHAR = '/';

  public static final int MAX_PATH_LENGTH = 255;
  private static final int MAX_PATH_DEPTH = 51;

  /**
   * Test whether the character c belongs to the accepted list of posix
   * filename characters A-Za-z0-9._-
   * @param c
   * @return
   */
  @SuppressWarnings({ "CharacterComparison", "MagicCharacter", "ImplicitNumericConversion" })
  protected boolean isValidPosixFileChar(char c) {
    return ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || (
            (c >= '0') && (c <= '9')) || (c == '.') || (c == '_') || (c
            == '-');
  }

  /**
   * Test whether filename is a valid posix filename
   * A posix filename must contain characters A-Za-z0-9._- and - must not be
   * the first character
   *
   */
  public boolean isValidPosixFileName(@NonNull CharSequence name) {
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (i == 0) {
        if (c == '-') {
          return false;
        }
      }
      if (!isValidPosixFileChar(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Test whether path is a valid posix path
   * A posix filename must contain characters A-Za-z0-9._- and - must not be
   * the first character
   * A valid path will have posix filenames separated by '/'
   * A valid path should not have relative elements ".", ".."
   *
   * @param path
   * @return
   */
  public boolean isValidPath(@NonNull String path) {
    if ((path.length() > MAX_PATH_LENGTH)
         || !path.startsWith(SEPARATOR)) {
      return false;
    }
    String[] names = split(path, SEPARATOR_CHAR);
    if (names.length > MAX_PATH_DEPTH) {
      return false;
    }
    for(String element : names) {
      if ("..".equals(element) ||
          ".".equals(element)  ||
          !isValidPosixFileName(element)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Split a string using the given separator, with no escaping performed.
   * @param str a string to be split. Note that this may not be null.
   * @param separator a separator char
   * @return an array of strings
   */
  private String[] split(String str, char separator) {
    // String.split returns a single empty result for splitting the empty
    // string.
    int len = str.length();
    if (len == 0) {
      return new String[]{""};
    }
    ArrayList<String> strList = new ArrayList<>();
    int startIndex = 0;
    int nextIndex = 0;
    while ((nextIndex = str.indexOf(separator, startIndex)) != -1) {
      strList.add(str.substring(startIndex, nextIndex));
      startIndex = nextIndex + 1;
    }
    if(startIndex < len) {
      // only add the last component if "/" is not the last character
      strList.add(str.substring(startIndex));
    }
    // remove trailing empty split(s)
    int last = strList.size(); // last split
    while ((--last >= 0) && (strList.get(last) != null) && strList.get(last)
            .isEmpty()) {
      strList.remove(last);
    }
    return strList.toArray(new String[0]);
  }
}
