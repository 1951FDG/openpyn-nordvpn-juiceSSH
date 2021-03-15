/*
 * Copyright 2010 ALM Works Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almworks.sqlite4java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQLParts is a means to avoid excessive garbage production during String concatenation when SQL
 * is constructed.
 * <p>
 * Often, same SQL statements get constructed over and over again and passed to <code>SQLiteConnecton.prepare</code>.
 * To avoid producing garbage and to facilitate quick look-up in the cache of prepared statements, better use SQLParts
 * than String concatenation.
 * <p>
 * SQLParts object may be <strong>fixed</strong>, which means it cannot be changed anymore.
 * <p>
 * This class is <strong>not thread-safe</strong> and not intended to be used from different threads.
 *
 * @author Igor Sereda
 */
public final class SQLParts {
  private static final String[] PARAMS_STRINGS = new String[101];

  private final List<String> myParts;

  private int myHash;
  private String mySql;
  private boolean myFixed;

  /**
   * Create empty SQLParts object.
   */
  public SQLParts() {
    myParts = new ArrayList<String>(5);
  }

  /**
   * Create a copy of another SQLParts object. SQL pieces are copied, but the new object is not fixed even if the
   * original object is fixed.
   *
   * @param copyFrom the original object
   */
  public SQLParts(SQLParts copyFrom) {
    myParts = new ArrayList<String>(copyFrom == null ? 5 : copyFrom.myParts.size());
    if (copyFrom != null) {
      myParts.addAll(copyFrom.myParts);
    }
  }

  /**
   * Create an instance of SQLParts containing only single piece of SQL.
   *
   * @param sql SQL piece
   */
  public SQLParts(String sql) {
    myParts = new ArrayList<String>(1);
    append(sql);
  }

  /**
   * Makes instance immutable. Further calls to {@link #append} will throw an exception.
   *
   * @return this object, fixed
   */
  public SQLParts fix() {
    myFixed = true;
    return this;
  }

  /**
   * If this object is fixed, returns itself, otherwise
   * returns a fixed copy of this object.
   *
   * @return fixed SQLParts, representing the same SQL
   */
  public SQLParts getFixedParts() {
    return myFixed ? this : new SQLParts(this).fix();
  }

  public int hashCode() {
    if (myHash == 0)
      myHash = calcHash();
    return myHash;
  }

  private int calcHash() {
    int r = 0;
    for (String myPart : myParts)
      r = 31 * r + myPart.hashCode();
    return r;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    List<String> other = ((SQLParts) o).myParts;
    if (myParts.size() != other.size())
      return false;
    for (int i = 0; i < myParts.size(); i++)
      if (!myParts.get(i).equals(other.get(i)))
        return false;
    return true;
  }

  /**
   * Empties this SQLParts instance.
   *
   * @throws IllegalStateException if instance is fixed
   */
  public void clear() {
    if (myFixed) {
      throw new IllegalStateException(String.valueOf(this));
    }
    myParts.clear();
    dropCachedValues();
  }

  /**
   * Adds a part to the SQL.
   *
   * @param part a piece of SQL added
   * @return this instance
   * @throws IllegalStateException if instance is fixed
   */
  public SQLParts append(String part) {
    if (myFixed) {
      throw new IllegalStateException(String.valueOf(this));
    }
    if (part != null && part.length() > 0) {
      myParts.add(part);
      dropCachedValues();
    }
    return this;
  }

  /**
   * Adds all parts from a different SQLParts to the SQL.
   *
   * @param parts source object to copy parts from, may be null 
   * @return this instance
   * @throws IllegalStateException if instance is fixed
   */
  public SQLParts append(SQLParts parts) {
    if (myFixed) {
      throw new IllegalStateException(String.valueOf(this));
    }
    if (parts != null && !parts.myParts.isEmpty()) {
      myParts.addAll(parts.myParts);
      dropCachedValues();
    }
    return this;
  }

  /**
   * Appends an SQL part consisting of a list of bind parameters.
   * <p>
   * That is, <code>appendParams(1)</code> appends <code><strong>?</strong></code>, <code>appendParams(2)</code>
   * appends <code><strong>?,?</strong></code> and so on.
   *
   * @param count the number of parameters ("?" symbols) to be added
   * @return this instance
   * @throws IllegalStateException if instance is fixed
   */
  public SQLParts appendParams(int count) {
    return append(getParamsString(count));
  }

  private String getParamsString(int count) {
    if (count < 1)
      return null;
    if (count >= PARAMS_STRINGS.length)
      return createParamsString(count);
    String s = PARAMS_STRINGS[count];
    if (s == null)
      s = PARAMS_STRINGS[count] = createParamsString(count);
    return s;
  }

  private String createParamsString(int count) {
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < count; i++) {
      if (i > 0)
        b.append(',');
      b.append('?');
    }
    return b.toString();
  }

  private void dropCachedValues() {
    myHash = 0;
    mySql = null;
  }

  /**
   * Returns the SQL representation of this params
   *
   * @return SQL
   */
  public String toString() {
    if (mySql == null) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < myParts.size(); i++) {
        builder.append(myParts.get(i));
      }
      mySql = builder.toString();
    }
    return mySql;
  }

  /**
   * Gets the list of SQL parts.
   *
   * @return unmodifiable list of SQL pieces.
   */
  public List<String> getParts() {
    return Collections.unmodifiableList(myParts);
  }

  /**
   * Checks if this instance is fixed.
   *
   * @return true if the instance is fixed, that is, read-only
   * @see #fix
   */
  public boolean isFixed() {
    return myFixed;
  }
}
