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

/**
 * <p>{@code SQLiteColumnMetadata} contains information about a table column:</p>
 * <ul>
 *   <li>Declared datatype;</li>
 *   <li>Collation sequence name;</li>
 *   <li>Flag that is true if NOT NULL constraint exists;</li>
 *   <li>Flag that is true if column is a part of primary key;</li>
 *   <li>Flag that is true if column is auto-increment.</li>
 * </ul>
 *
 * You get instances of SQLiteColumnMetadata via {@link SQLiteConnection#getTableColumnMetadata} method.
 *
 * @author Alexander Smirnov
 * @see <a href="http://www.sqlite.org/c3ref/table_column_metadata.html">sqlite3_table_column_metadata</a>
 */
public final class SQLiteColumnMetadata {
  private final String myDataType;
  private final String myCollSeq;
  private final boolean myNotNull;
  private final boolean myPrimaryKey;
  private final boolean myAutoinc;

  SQLiteColumnMetadata(String dataType, String collSeq, boolean notNull, boolean primaryKey, boolean autoinc) {
    myDataType = dataType;
    myCollSeq = collSeq;
    myNotNull = notNull;
    myPrimaryKey = primaryKey;
    myAutoinc = autoinc;
  }

  /**
   * @return declared data type
   */
  public String getDataType() {
    return myDataType;
  }

  /**
   * @return collation sequence name
   */
  public String getCollSeq() {
    return myCollSeq;
  }

  /**
   * @return True if NOT NULL constraint exists
   */
  public boolean isNotNull() {
    return myNotNull;
  }

  /**
   * @return True if column part of primary key
   */
  public boolean isPrimaryKey() {
    return myPrimaryKey;
  }

  /**
   * @return True if column is auto-increment
   */
  public boolean isAutoinc() {
    return myAutoinc;
  }
}
