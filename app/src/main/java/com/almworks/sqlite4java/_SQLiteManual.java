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

import java.nio.ByteBuffer;

import com.almworks.sqlite4java.SQLiteColumnMetadata;

final class _SQLiteManual {
  /**
   * These arrays are used for return values. SQLiteConnection facade must ensure the methods are called
   * from the same thread, so these values are confined.
   */
  private final int[] myInt = {0, 0, 0};
  private final long[] myLong = {0};
  private final String[] myString = {null, null};
  private final byte[][] myByteArray = {null};
  private final Object[] myObject = {null, null};

  /**
   * Last return code received for non-static methods.
   */
  private int myLastReturnCode = 0;
  private String myLastOpenError = null;

  public static String wrapper_version() {
    return _SQLiteManualJNI.wrapper_version();
  }

  public static int sqlite3_exec(SWIGTYPE_p_sqlite3 db, String sql, String[] outError) {
    assert outError == null || outError.length == 1 : outError.length;
    return _SQLiteManualJNI.sqlite3_exec(SWIGTYPE_p_sqlite3.getCPtr(db), sql, outError);
  }

  public SQLiteColumnMetadata sqlite3_table_column_metadata(SWIGTYPE_p_sqlite3 db, String dbName, String tableName, String columnName) throws SQLiteException {
    myString[0] = null;
    myString[1] = null;
    myInt[0] = 0;
    myInt[1] = 0;
    myInt[2] = 0;
    int res = _SQLiteManualJNI.sqlite3_table_column_metadata(SWIGTYPE_p_sqlite3.getCPtr(db), dbName, tableName, columnName, myString, myInt);
    SQLiteColumnMetadata columnMetadata = new SQLiteColumnMetadata(myString[0], myString[1], myInt[0] == 1, myInt[1] == 1, myInt[2] == 1);
    myString[0] = null;
    myString[1] = null;
    myInt[0] = 0;
    myInt[1] = 0;
    myInt[2] = 0;
    if (res != SQLiteConstants.SQLITE_OK)
      throw new SQLiteException(res, "failed calling sqlite3_table_column_metadata()");
    return columnMetadata;
  }

  public static int sqlite3_bind_text(SWIGTYPE_p_sqlite3_stmt stmt, int index, String value) {
    return _SQLiteManualJNI.sqlite3_bind_text(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), index, value);
  }

  public static int sqlite3_bind_blob(SWIGTYPE_p_sqlite3_stmt stmt, int index, byte[] value, int offset, int length) {
    return _SQLiteManualJNI.sqlite3_bind_blob(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), index, value, offset, length);
  }

  public static int sqlite3_blob_read(SWIGTYPE_p_sqlite3_blob blob, int blobOffset, byte[] buffer, int bufferOffset, int length) {
    return _SQLiteManualJNI.sqlite3_blob_read(SWIGTYPE_p_sqlite3_blob.getCPtr(blob), blobOffset, buffer, bufferOffset, length);
  }

  public static int sqlite3_blob_write(SWIGTYPE_p_sqlite3_blob blob, int blobOffset, byte[] buffer, int bufferOffset, int length) {
    return _SQLiteManualJNI.sqlite3_blob_write(SWIGTYPE_p_sqlite3_blob.getCPtr(blob), blobOffset, buffer, bufferOffset, length);
  }

  public int getLastReturnCode() {
    return myLastReturnCode;
  }

  public String drainLastOpenError() {
    String r = myLastOpenError;
    myLastOpenError = null;
    return r;
  }

  public SWIGTYPE_p_sqlite3 sqlite3_open_v2(String filename, int flags) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myString[0] = null;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_open_v2(filename, myLong, flags, myString);
    long ptr = myLong[0];
    myLong[0] = 0;
    myLastOpenError = myString[0];
    myString[0] = null;
    return ptr == 0 ? null : new SWIGTYPE_p_sqlite3(ptr, true);
  }

  public SWIGTYPE_p_sqlite3_stmt sqlite3_prepare_v2(SWIGTYPE_p_sqlite3 db, String sql) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_prepare_v2(SWIGTYPE_p_sqlite3.getCPtr(db), sql, myLong);
    long ptr = myLong[0];
    myLong[0] = 0;
    return ptr == 0 ? null : new SWIGTYPE_p_sqlite3_stmt(ptr, true);
  }

  public SWIGTYPE_p_sqlite3_stmt sqlite3_prepare_v3(SWIGTYPE_p_sqlite3 db, String sql, int prepFlags) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_prepare_v3(SWIGTYPE_p_sqlite3.getCPtr(db), sql, prepFlags, myLong);
    long ptr = myLong[0];
    myLong[0] = 0;
    return ptr == 0 ? null : new SWIGTYPE_p_sqlite3_stmt(ptr, true);
  }

  public String sqlite3_column_text(SWIGTYPE_p_sqlite3_stmt stmt, int column) {
    myLastReturnCode = 0;
    myString[0] = null;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_column_text(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), column, myString);
    String r = myString[0];
    myString[0] = null;
    return r;
  }

  public byte[] sqlite3_column_blob(SWIGTYPE_p_sqlite3_stmt stmt, int column) {
    myLastReturnCode = 0;
    myByteArray[0] = null;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_column_blob(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), column, myByteArray);
    byte[] r = myByteArray[0];
    myByteArray[0] = null;
    return r;
  }

  public SWIGTYPE_p_sqlite3_blob sqlite3_blob_open(SWIGTYPE_p_sqlite3 db, String database, String table, String column, long rowid, boolean writeAccess) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_blob_open(SWIGTYPE_p_sqlite3.getCPtr(db), database, table, column, rowid, writeAccess, myLong);
    long ptr = myLong[0];
    myLong[0] = 0;
    return ptr == 0 ? null : new SWIGTYPE_p_sqlite3_blob(ptr, true);
  }

  public DirectBuffer wrapper_alloc(int size) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myObject[0] = null;
    myObject[1] = null;
    myLastReturnCode = _SQLiteManualJNI.wrapper_alloc(size, myLong, myObject);
    ByteBuffer controlBuffer = myObject[0] instanceof ByteBuffer ? (ByteBuffer) myObject[0] : null;
    ByteBuffer dataBuffer = myObject[1] instanceof ByteBuffer ? (ByteBuffer) myObject[1] : null;
    long ptr = myLong[0];
    if (controlBuffer == null || dataBuffer == null || ptr == 0) {
      return null;
    }
    return new DirectBuffer(new SWIGTYPE_p_direct_buffer(ptr, true), controlBuffer, dataBuffer, size);
  }

  public static int wrapper_free(DirectBuffer buffer) {
    SWIGTYPE_p_direct_buffer handle = buffer.getHandle();
    buffer.invalidate();
    if (handle == null)
      return 0;
    int rc = _SQLiteManualJNI.wrapper_free(SWIGTYPE_p_direct_buffer.getCPtr(handle));
    return rc;
  }

  public static int wrapper_bind_buffer(SWIGTYPE_p_sqlite3_stmt stmt, int index, DirectBuffer buffer) {
    SWIGTYPE_p_direct_buffer handle = buffer.getHandle();
    if (handle == null)
      return SQLiteConstants.WRAPPER_WEIRD;
    int size = buffer.getPosition();
    return _SQLiteManualJNI.wrapper_bind_buffer(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), index, SWIGTYPE_p_direct_buffer.getCPtr(handle), size);
  }

  public ByteBuffer wrapper_column_buffer(SWIGTYPE_p_sqlite3_stmt stmt, int column) {
    myLastReturnCode = 0;
    myObject[0] = null;
    myLastReturnCode = _SQLiteManualJNI.wrapper_column_buffer(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), column, myObject);
    ByteBuffer r = myObject[0] instanceof ByteBuffer ? (ByteBuffer) myObject[0] : null;
    myObject[0] = null;
    return r;
  }

  public ProgressHandler install_progress_handler(SWIGTYPE_p_sqlite3 db, int stepsPerCallback) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myObject[0] = null;
    myLastReturnCode = _SQLiteManualJNI.install_progress_handler(SWIGTYPE_p_sqlite3.getCPtr(db), stepsPerCallback, myLong, myObject);
    ByteBuffer r = myObject[0] instanceof ByteBuffer ? (ByteBuffer) myObject[0] : null;
    myObject[0] = null;
    long ptr = myLong[0];
    myLong[0] = 0;
    if (ptr == 0 || r == null)
      return null;
    return new ProgressHandler(new SWIGTYPE_p_direct_buffer(ptr, true), r, stepsPerCallback);
  }

  public static int uninstall_progress_handler(SWIGTYPE_p_sqlite3 db, ProgressHandler handler) {
    SWIGTYPE_p_direct_buffer pointer = handler.dispose();
    if (pointer == null)
      return 0;
    return _SQLiteManualJNI.uninstall_progress_handler(SWIGTYPE_p_sqlite3.getCPtr(db), SWIGTYPE_p_direct_buffer.getCPtr(pointer));
  }

  public int wrapper_load_ints(SWIGTYPE_p_sqlite3_stmt stmt, int column, int[] buffer, int offset, int count) {
    myLastReturnCode = 0;
    myInt[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.wrapper_load_ints(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), column, buffer, offset, count, myInt);
    int r = myInt[0];
    myInt[0] = 0;
    return r;
  }

  public int wrapper_load_longs(SWIGTYPE_p_sqlite3_stmt stmt, int column, long[] buffer, int offset, int count) {
    myLastReturnCode = 0;
    myInt[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.wrapper_load_longs(SWIGTYPE_p_sqlite3_stmt.getCPtr(stmt), column, buffer, offset, count, myInt);
    int r = myInt[0];
    myInt[0] = 0;
    return r;
  }

  public SWIGTYPE_p_intarray_module sqlite3_intarray_register(SWIGTYPE_p_sqlite3 db) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_intarray_register(SWIGTYPE_p_sqlite3.getCPtr(db), myLong);
    return myLong[0] == 0 ? null : new SWIGTYPE_p_intarray_module(myLong[0], true);
  }

  public SWIGTYPE_p_intarray sqlite3_intarray_create(SWIGTYPE_p_intarray_module module, String name) {
    myLastReturnCode = 0;
    myLong[0] = 0;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_intarray_create(SWIGTYPE_p_intarray_module.getCPtr(module), name, myLong);
    return myLong[0] == 0 ? null : new SWIGTYPE_p_intarray(myLong[0], true);
  }

  public static int sqlite3_intarray_bind(SWIGTYPE_p_intarray array, long[] values, int offset, int length, boolean ordered, boolean unique) {
    return _SQLiteManualJNI.sqlite3_intarray_bind(SWIGTYPE_p_intarray.getCPtr(array), values, offset, length, ordered, unique);
  }

  public static int sqlite3_intarray_unbind(SWIGTYPE_p_intarray array) {
    return _SQLiteManualJNI.sqlite3_intarray_unbind(SWIGTYPE_p_intarray.getCPtr(array));
  }

  public static int sqlite3_intarray_destroy(SWIGTYPE_p_intarray array) {
    return _SQLiteManualJNI.sqlite3_intarray_destroy(SWIGTYPE_p_intarray.getCPtr(array));
  }

  public String sqlite3_load_extension(SWIGTYPE_p_sqlite3 db, String file, String proc) {
    myLastReturnCode = 0;
    myString[0] = null;
    myLastReturnCode = _SQLiteManualJNI.sqlite3_load_extension(SWIGTYPE_p_sqlite3.getCPtr(db), file, proc, myString);
    String r = myString[0];
    myString[0] = null;
    return r;
  }
}
