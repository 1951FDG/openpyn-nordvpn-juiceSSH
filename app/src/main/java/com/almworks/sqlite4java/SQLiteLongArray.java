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

import static com.almworks.sqlite4java.SQLiteConstants.WRAPPER_STATEMENT_DISPOSED;

/**
 * SQLiteLongArray wraps a virtual table handle, created with {@link SQLiteConnection#createArray}.
 * Use methods of this class to set the array's contents (maybe several times), and dispose it when done.
 * <p/>
 * Like with other classes, the methods are confined to the thread that opened SQLiteConnection, unless stated otherwise.
 * <p/>
 * The virtual array table has a single column named <code>value</code> with INTEGER affinity.
 * <p/>
 * If you bind ordered and/or unique values to the array, it may greatly improve performance if the underlying
 * code knows about that fact. See {@link #bind(long[], int, int, boolean, boolean)} method for details.
 *
 * @author Igor Sereda
 */
public class SQLiteLongArray {
  /**
   * Controller (whether cached or uncached), used to return the instance.
   */
  private SQLiteController myController;

  /**
   * Handle for native operations.
   */
  private SWIGTYPE_p_intarray myHandle;

  /**
   * Virtual table name.
   */
  private final String myName;

  /**
   * If true, the instance cannot be used.
   */
  private volatile boolean myDisposed;

  SQLiteLongArray(SQLiteController controller, SWIGTYPE_p_intarray handle, String name) {
    assert controller != null;
    assert handle != null;
    assert name != null;
    myController = controller;
    myHandle = handle;
    myName = name;
  }

  /**
   * Returns the name of the virtual table, which should be used in SQL.
   * <p/>
   * This method is thread-safe.
   *
   * @return virtual table's name, not null
   */
  public String getName() {
    return myName;
  }

  /**
   * Returns true if the instance is disposed and cannot be used. If the array was cached, it may still remain
   * and be addressable through SQL, but it may contain no data or any irrelevant data (as defined by the current
   * user of the instance).
   *
   * @return true if the instance is unusable
   */
  public boolean isDisposed() {
    return myDisposed;
  }

  @Override
  public String toString() {
    return myName;
  }

  /**
   * Disposes this instance, making it unusable and freeing the resources. If the array table is cached,
   * then it is unbound from values (emptied) and returned to the cache. If the array table is not cached,
   * it is deleted from the database.
   * <p/>
   * This method is partially thread-safe. When called not from the confining thread, the exception will not be
   * thrown, but the method will do nothing.
   * <p/>
   * Calling <code>dispose()</code> second time has no effect.
   * <p/>
   * Calling {@link #bind} after instance has been disposed would result in exception.
   */
  public void dispose() {
    if (myHandle == null)
      return;
    SQLiteController controller = myController;
    try {
      controller.validate();
    } catch (SQLiteException e) {
      Internal.recoverableError(this, "invalid dispose: " + e, true);
      return;
    }
    Internal.logFine(this, "disposing");
    controller.dispose(this);
    myHandle = null;
    myController = SQLiteController.getDisposed(myController);
    myDisposed = true;
  }

  /**
   * Fills virtual array table with values from the specified portion of a Java array.
   * <p/>
   * Values are copied into a native non-heap memory, so the array can be used for other purposes after
   * calling this method.
   * <p/>
   * If <tt>ordered</tt> and <tt>unique</tt> are false, values need not to be unique or come in any specific order.
   * If <tt>ordered</tt> is true, then the caller guarantees that the values come in ascending order. If <tt>unique</tt>
   * is true, then the values are guaranteed to be unique. Failing those guarantees would result in incorrect search
   * results.
   * <p/>
   * Passing ordered and/or unique values allows SQLite to optimize search and access to the array.
   * <p/>
   * Memory, allocated for the virtual array table, is freed on the next call to bind, when array instance
   * is disposed, or when database is closed.
   *
   * @param values array of the values to bind, may be null if length == 0
   * @param offset the index of an element to be bound as the first row
   * @param length the number of values to bind, if set to 0 then the virtual array table will be empty
   * @param ordered if true, the values within the specified by offset and length region are in non-strict ascending order
   * @param unique if true, the values within the specified by offset and length region are not repeating
   * @return this instance
   * @throws SQLiteException                if this instance has been disposed or problem occurs on the underlying layer
   * @throws ArrayIndexOutOfBoundsException if offset and length do not specify a valid range within values array
   * @throws NullPointerException           if values is null and length is not zero
   */
  public SQLiteLongArray bind(long[] values, int offset, int length, boolean ordered, boolean unique) throws SQLiteException {
    if (offset < 0) throw new ArrayIndexOutOfBoundsException(offset);
    if (length < 0) throw new ArrayIndexOutOfBoundsException(length);
    if (length > 0 && offset + length > values.length) throw new ArrayIndexOutOfBoundsException(offset + length);
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bind[" + length + "]");
    int rc;
    if (length == 0) {
      rc = _SQLiteManual.sqlite3_intarray_unbind(handle());
    } else {
      if (values == null) throw new NullPointerException();
      rc = _SQLiteManual.sqlite3_intarray_bind(handle(), values, offset, length, ordered, unique);
    }
    myController.throwResult(rc, "bind(array)", this);
    return this;
  }

  /**
   * Fills virtual array table with values from a Java array. This is a convenience method for {@link #bind(long[], int, int, boolean, boolean)}.
   *
   * @param values array of the values to bind, may be null if length == 0
   * @param offset the index of an element to be bound as the first row
   * @param length the number of values to bind, if set to 0 then the virtual array table will be empty
   * @return this instance
   * @throws SQLiteException                if this instance has been disposed or problem occurs on the underlying layer
   * @throws ArrayIndexOutOfBoundsException if offset and length do not specify a valid range within values array
   * @throws NullPointerException           if values is null and length is not zero
   */
  public SQLiteLongArray bind(long[] values, int offset, int length) throws SQLiteException {
    return bind(values, offset, length, false, false);
  }

  /**
   * Fills virtual array table with values from a Java array. This is a convenience method for {@link #bind(long[], int, int, boolean, boolean)}.
   *
   * @param values array of the values to bind, if null - bind to an empty array
   * @return this instance
   * @throws SQLiteException                if this instance has been disposed or problem occurs on the underlying layer
   * @throws ArrayIndexOutOfBoundsException if offset and length do not specify a valid range within values array
   */
  public SQLiteLongArray bind(long... values) throws SQLiteException {
    return bind(values, 0, values == null ? 0 : values.length, false, false);
  }

  /**
   * Fills virtual array table with values from a Java array. This is a convenience method for {@link #bind(long[], int, int, boolean, boolean)}.
   *
   * @param values array of the values to bind, if null - bind to an empty array
   * @param ordered if true, the values within the specified by offset and length region are in non-strict ascending order
   * @param unique if true, the values within the specified by offset and length region are not repeating
   * @return this instance
   * @throws SQLiteException                if this instance has been disposed or problem occurs on the underlying layer
   * @throws ArrayIndexOutOfBoundsException if offset and length do not specify a valid range within values array
   */
  public SQLiteLongArray bind(long[] values, boolean ordered, boolean unique) throws SQLiteException {
    return bind(values, 0, values == null ? 0 : values.length, ordered, unique);
  }

  private SWIGTYPE_p_intarray handle() throws SQLiteException {
    SWIGTYPE_p_intarray handle = myHandle;
    if (handle == null) {
      throw new SQLiteException(WRAPPER_STATEMENT_DISPOSED, null);
    }
    return handle;
  }

  SWIGTYPE_p_intarray arrayHandle() {
    return myHandle;
  }
}
