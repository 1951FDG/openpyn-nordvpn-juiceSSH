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
 * SQLiteBlob encapsulates <strong><code>sqlite3_blob*</code></strong> handle, which represents an open BLOB
 * (binary large object), stored in a single cell of a table.
 * <p>
 * SQLiteBlob is created by {@link SQLiteConnection#blob} method. After application is done using the instance
 * of SQLiteBlob, it should be disposed with {@link #dispose} method.
 * <p>
 * You can read or write portions of the stored blob using {@link #read} and {@link #write} methods. Note that
 * you cannot change the size of the blob using this interface.
 * <p>
 * Methods of this class are not thread-safe and confined to the thread that opened the SQLite connection. 
 *
 * @author Igor Sereda
 * @see SQLiteConnection#blob
 * @see <a href="http://www.sqlite.org/c3ref/blob_open.html">sqlite3_blob_open</a>
 */
public final class SQLiteBlob {
  /**
   * Debug name
   */
  private final String myName;

  /**
   * Whether blob was opened for writing
   */
  private final boolean myWriteAccess;

  /**
   * Controller, not null
   */
  private SQLiteController myController;

  /**
   * Handle, set to null when disposed
   */
  private SWIGTYPE_p_sqlite3_blob myHandle;

  /**
   * Cached length
   */
  private int myLength = -1;

  SQLiteBlob(SQLiteController controller, SWIGTYPE_p_sqlite3_blob handle, String dbname, String table, String column,
    long rowid, boolean writeAccess)
  {
    assert controller != null;
    assert handle != null;
    myController = controller;
    myHandle = handle;
    myWriteAccess = writeAccess;
    myName = dbname + "." + table + "." + column + ":" + rowid;
  }

  /**
   * Disposes this blob and frees allocated resources.
   * <p>
   * After blob is disposed, it is no longer usable and holds no references to connection
   * or sqlite db.
   */
  public void dispose() {
    if (myHandle == null)
      return;
    try {
      myController.validate();
    } catch (SQLiteException e) {
      Internal.recoverableError(this, "invalid dispose: " + e, true);
      return;
    }
    Internal.logFine(this, "disposing");
    myController.dispose(this);
    // clear may be called from dispose() too
    clear();
  }

  /**
   * Checks if this instance has been disposed
   *
   * @return true if the blob is disposed and cannot be used
   */
  public boolean isDisposed() {
    return myHandle == null;
  }

  /**
   * Returns the size of the open blob. The size cannot be changed via this interface.
   *
   * @return size of the blobs in bytes
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   */
  public int getSize() throws SQLiteException {
    myController.validate();
    if (myLength < 0) {
      myLength = _SQLiteSwigged.sqlite3_blob_bytes(handle());
    }
    return myLength;
  }

  /**
   * Read bytes from the blob into a buffer.
   * <p>
   * <code>blobOffset</code> and <code>length</code> should define a sub-range within blob's content. If attempt is
   * made to read blob beyond its size, an exception is thrown and no data is read.
   *
   * @param blobOffset the position in the blob where to start reading
   * @param buffer target buffer
   * @param offset starting offset in the buffer
   * @param length number of bytes to read
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/blob_read.html">sqlite3_blob_read</a>
   */
  public void read(int blobOffset, byte[] buffer, int offset, int length) throws SQLiteException {
    if (buffer == null)
      throw new NullPointerException();
    if (offset < 0 || offset + length > buffer.length)
      throw new ArrayIndexOutOfBoundsException(buffer.length + " " + offset + " " + length); 
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "read[" + blobOffset + "," + length + "]");
    int rc = _SQLiteManual.sqlite3_blob_read(handle(), blobOffset, buffer, offset, length);
    myController.throwResult(rc, "read", this);
  }

  /**
   * Writes bytes into the blob. Bytes are taken from the specified range in the input byte buffer.
   * <p>
   * Note that you cannot write beyond the current blob's size. The size of the blob
   * cannot be changed via incremental I/O API. To change the size, you need to use {@link SQLiteStatement#bindZeroBlob}
   * method.
   * <p>
   * Bytes are written within the current transaction.
   * <p>
   * If blob was not open for writing, an error is thrown.
   *
   * @param blobOffset the position in the blob where to start writing
   * @param buffer source bytes buffer
   * @param offset starting offset in the buffer
   * @param length number of bytes to write
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/blob_write.html">sqlite3_blob_write</a>
   */
  public void write(int blobOffset, byte[] buffer, int offset, int length) throws SQLiteException {
    if (buffer == null)
      throw new NullPointerException();
    if (offset < 0 || offset + length > buffer.length)
      throw new ArrayIndexOutOfBoundsException(buffer.length + " " + offset + " " + length);
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "write[" + blobOffset + "," + length + "]");
    int rc = _SQLiteManual.sqlite3_blob_write(handle(), blobOffset, buffer, offset, length);
    myController.throwResult(rc, "write", this);
  }

  /**
   * Returns true if this blob instance was opened for writing.
   *
   * @return true if {@link #write} is allowed
   */
  public boolean isWriteAllowed() {
    return myWriteAccess;
  }

  /**
   * Repositions BLOB to another row in the table. It should be quickier that closing the blob and opening another one.
   *
   * @param rowid row id to move to - it must exist and contain data
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/blob_reopen.html">sqlite3_blob_reopen</a>
   */
  public void reopen(long rowid) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "reopen[" + rowid + "]");
    int rc = _SQLiteSwigged.sqlite3_blob_reopen(handle(), rowid);
    myController.throwResult(rc, "reopen", this);
  }

  private SWIGTYPE_p_sqlite3_blob handle() throws SQLiteException {
    SWIGTYPE_p_sqlite3_blob handle = myHandle;
    if (handle == null) {
      throw new SQLiteException(SQLiteConstants.WRAPPER_BLOB_DISPOSED, null);
    }
    return handle;
  }

  SWIGTYPE_p_sqlite3_blob blobHandle() {
    return myHandle;
  }

  /**
   * Clear all data, disposing the blob. May be called by SQLiteConnection on close.
   */
  void clear() {
    myHandle = null;
    myController = SQLiteController.getDisposed(myController);
    Internal.logFine(this, "cleared");
  }

  public String toString() {
    return "[" + myName + "]" + myController;
  }
}
