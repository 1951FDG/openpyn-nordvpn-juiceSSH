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

final class _SQLiteManualJNI {
  public final static native String wrapper_version();

  /**
   * @param filename database file name, not null
   * @param ppDb long[1] container for the db handle
   * @param flags see SQLITE_OPEN_* constants
   * @return return code SQLITE_OK or other
   */
  public final static native int sqlite3_open_v2(String filename, long[] ppDb, int flags, String[] ppOpenError);

  /**
   * @param db handle
   * @param sql sql statements
   * @param ppParseError (nullable) container for parsing errors
   * @return
   */
  public final static native int sqlite3_exec(long db, String sql, String[] ppParseError);

  /**
   * @param db handle
   * @param dbName database name
   * @param tableName table name
   * @param columnName column name
   * @param out12 array of:
   *              dataType (data type) and 
   *              collSeq (collation sequence)
   * @param out345 array of:
   *               notNull (True if NOT NULL constraint exists), 
   *               primaryKey (True if column part of PK),
   *               autoinc (True if column is auto-increment)
   * @return result code
   */
  public final static native int sqlite3_table_column_metadata(long db, String dbName, String tableName, String columnName, String[] out12, int[] out345);

  /**
   * @param db handle
   * @param sql sql statement
   * @param ppStmt long[1] container for statement handle
   * @return result code
   */
  public final static native int sqlite3_prepare_v2(long db, String sql, long[] ppStmt);

  /**
   * @param db handle
   * @param sql sql statement
   * @param ppStmt long[1] container for statement handle
   * @param prepFlags
   * @return result code
   */
  public final static native int sqlite3_prepare_v3(long db, String sql, int prepFlags, long[] ppStmt);

  /**
   * @param stmt prepared statement
   * @param index index of param, 1-based
   * @param value string value, UTF-safe
   * @return result code
   */
  public final static native int sqlite3_bind_text(long stmt, int index, String value);

  public final static native int sqlite3_bind_blob(long stmt, int index, byte[] value, int offset, int length);

  /**
   * @param stmt executed statement
   * @param column index of column, 0-based
   * @param ppValue String[1] container for the result
   * @return result code
   */
  public final static native int sqlite3_column_text(long stmt, int column, String[] ppValue);

  public final static native int sqlite3_column_blob(long stmt, int column, byte[][] ppValue);

  /**
   * @param db database
   * @param database db name
   * @param table table name
   * @param column column name
   * @param rowid rowid of the blob to open
   * @param writeAccess if true, can read/write, if false, can only read
   * @param ppBlob output
   * @return result code
   */
  public final static native int sqlite3_blob_open(long db, String database, String table, String column, long rowid, boolean writeAccess, long[] ppBlob);

  public final static native int sqlite3_blob_read(long blob, int blobOffset, byte[] buffer, int bufferOffset, int length);

  public final static native int sqlite3_blob_write(long blob, int blobOffset, byte[] buffer, int bufferOffset, int length);

  public final static native int wrapper_alloc(int size, long[] ppBuf, Object[] ppByteBuffer);

  public final static native int wrapper_free(long buffer);

  public final static native int wrapper_bind_buffer(long stmt, int index, long data, int size);

  public final static native int wrapper_column_buffer(long cPtr, int column, Object[] ppByteBuffer);

  public final static native int install_progress_handler(long db, int steps, long[] ppBuf, Object[] ppByteBuffer);

  public final static native int uninstall_progress_handler(long db, long ptr);

  public final static native int wrapper_load_ints(long stmt, int column, int[] buffer, int offset, int count, int[] ppCount);

  public final static native int wrapper_load_longs(long stmt, int column, long[] buffer, int offset, int count, int[] ppCount);

  public final static native int sqlite3_intarray_register(long db, long[] ppIntarrayModule);

  public final static native int sqlite3_intarray_create(long module, String name, long[] ppIntarray);

  public final static native int sqlite3_intarray_destroy(long intarray);

  public final static native int sqlite3_intarray_bind(long intarray, long[] buffer, int offset, int length, boolean ordered, boolean unique);

  public final static native int sqlite3_intarray_unbind(long intarray);

  public final static native int sqlite3_load_extension(long db, String file, String proc, String[] ppError);

  public final static native int sqlite3_win32_set_directory(int type, String zValue);

}
