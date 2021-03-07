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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static com.almworks.sqlite4java.SQLiteConstants.*;

/**
 * SQLiteStatement wraps an instance of compiled SQL statement, represented as <strong><code>sqlite3_stmt*</code></strong>
 * handle in SQLite C Interface.
 * <p/>
 * You get instances of SQLiteStatement via {@link SQLiteConnection#prepare} methods. After you've done using
 * the statement, you have to free it using {@link #dispose} method. Statements are usually cached, so until
 * you release the statement, <code>prepare</code> calls for the same SQL will result in needless compilation.
 * <p/>
 * Typical use includes binding parameters, then executing steps and reading columns. Most methods directly
 * correspond to the sqlite3 C interface methods.
 * <pre>
 * SQLiteStatement statement = connection.prepare(".....");
 * try {
 *   statement.bind(....).bind(....);
 *   while (statement.step()) {
 *      statement.columnXXX(...);
 *   }
 * } finally {
 *   statement.dispose();
 * }
 * </pre>
 * <p/>
 * Unless a method is marked as thread-safe, it is confined to the thread that has opened the connection. Calling
 * a confined method from a different thread will result in exception.
 *
 * @author Igor Sereda
 * @see <a href="http://sqlite.org/c3ref/stmt.html">sqlite3_stmt*</a>
 */
public final class SQLiteStatement {
  /**
   * Public instance of initially disposed, dummy statement. To be used as a guardian object.
   */
  public static final SQLiteStatement DISPOSED = new SQLiteStatement();

  /**
   * The SQL of this statement.
   */
  private final SQLParts mySqlParts;

  /**
   * The profiler for this statement, may be null.
   */
  private SQLiteProfiler myProfiler;

  /**
   * The controller that handles connection-level operations. Initially it is set
   */
  private SQLiteController myController;

  /**
   * Statement handle wrapper. Becomes null when disposed.
   */
  private SWIGTYPE_p_sqlite3_stmt myHandle;

  /**
   * When true, the last step() returned SQLITE_ROW, which means data can be read.
   */
  private boolean myHasRow;

  /**
   * When true, values have been bound to the statement. (and they take up memory)
   */
  private boolean myHasBindings;

  /**
   * When true, the statement has performed step() and needs to be reset to be reused.
   */
  private boolean myStepped;

  /**
   * The number of columns in current result set. If negative, the number is unknown and should
   * be requested at first need.
   */
  private int myColumnCount = -1;

  /**
   * All currently active bind streams.
   */
  private List<BindStream> myBindStreams;
  private List<ColumnStream> myColumnStreams;

  /**
   * Contains progress handler instance - only when step() is in progress. Used to cancel the execution.
   * Protected for MT access with this.
   */
  private ProgressHandler myProgressHandler;

  /**
   * True if statement has been cancelled. Cleared at statement reset.
   */
  private boolean myCancelled;

  /**
   * Instances are constructed only by SQLiteConnection.
   *
   * @param controller controller, provided by the connection
   * @param handle     native handle wrapper
   * @param sqlParts   SQL
   * @param profiler   an instance of profiler for the statement, or null
   * @see SQLiteConnection#prepare(String, boolean)
   */
  SQLiteStatement(SQLiteController controller, SWIGTYPE_p_sqlite3_stmt handle, SQLParts sqlParts, SQLiteProfiler profiler) {
    assert handle != null;
    assert sqlParts.isFixed() : sqlParts;
    myController = controller;
    myHandle = handle;
    mySqlParts = sqlParts;
    myProfiler = profiler;
    Internal.logFine(this, "instantiated");
  }

  /**
   * Constructs DISPOSED singleton
   */
  private SQLiteStatement() {
    myController = SQLiteController.getDisposed(null);
    myHandle = null;
    mySqlParts = new SQLParts().fix();
    myProfiler = null;
  }

  /**
   * @return true if the statement is disposed and cannot be used
   */
  public boolean isDisposed() {
    return myHandle == null;
  }

  /**
   * Returns the immutable SQLParts object that was used to create this instance.
   * <p/>
   * This method is <strong>thread-safe</strong>.
   *
   * @return SQL used for this statement
   */
  public SQLParts getSqlParts() {
    return mySqlParts;
  }

  /**
   * Disposes this statement and frees allocated resources. If the statement's handle is cached,
   * it is returned to the connection's cache and can be reused by later calls to <code>prepare</code>
   * <p/>
   * Calling this method on an already disposed instance has no effect.
   * <p/>
   * After SQLiteStatement instance is disposed, it is no longer usable and holds no references to its originating
   * connection or SQLite database.
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
   * Resets the statement if it has been stepped, allowing SQL to be run again. Optionally, clears bindings all binding.
   * <p/>
   * If <code>clearBinding</code> parameter is false, then all preceding bindings remain in place. You can change
   * some or none of them and run statement again.
   *
   * @param clearBindings if true, all parameters will be set to NULL
   * @return this statement
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/reset.html">sqlite3_reset</a>
   * @see <a href="http://www.sqlite.org/c3ref/clear_bindings.html">sqlite3_clear_bindings</a>
   */
  public SQLiteStatement reset(boolean clearBindings) throws SQLiteException {
    myController.validate();
    boolean fineLogging = Internal.isFineLogging();
    if (fineLogging)
      Internal.logFine(this, "reset(" + clearBindings + ")");
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    clearColumnStreams();
    if (myStepped) {
      if (fineLogging)
        Internal.logFine(this, "resetting");
      _SQLiteSwigged.sqlite3_reset(handle);
    }
    myHasRow = false;
    myStepped = false;
    myColumnCount = -1;
    if (clearBindings && myHasBindings) {
      if (fineLogging)
        Internal.logFine(this, "clearing bindings");
      int rc = _SQLiteSwigged.sqlite3_clear_bindings(handle);
      myController.throwResult(rc, "reset.clearBindings()", this);
      clearBindStreams(false);
      myHasBindings = false;
    }
    synchronized (this) {
      myCancelled = false;
    }
    return this;
  }

  /**
   * Convenience method that resets the statement and clears bindings. See {@link #reset(boolean)} for a detailed
   * description.
   *
   * @return this statement
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   */
  public SQLiteStatement reset() throws SQLiteException {
    return reset(true);
  }

  /**
   * Clears parameter bindings, if there are any. All parameters are set to NULL.
   *
   * @return this statement
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/clear_bindings.html">sqlite3_clear_bindings</a>
   */
  public SQLiteStatement clearBindings() throws SQLiteException {
    myController.validate();
    Internal.logFine(this, "clearBindings");
    if (myHasBindings) {
      Internal.logFine(this, "clearing bindings");
      int rc = _SQLiteSwigged.sqlite3_clear_bindings(handle());
      myController.throwResult(rc, "clearBindings()", this);
      clearBindStreams(false);
    }
    myHasBindings = false;
    return this;
  }

  /**
   * Evaluates SQL statement until either there's data to be read, an error occurs, or the statement completes.
   * <p/>
   * An SQL statement is represented as a VM program in SQLite, and a call to <code>step</code> runs that program
   * until there's a "break point".
   * <p/>
   * Note that since SQlite 3.7, {@link #reset} method is called by step() automatically if anything other than
   * SQLITE_ROW is returned.
   * <p/>
   * This method can produce one of the three results:
   * <ul>
   * <li>If the return value is <strong>true</strong>, there's data to be read using <code>columnXYZ</code> methods;
   * <li>If the return value is <strong>false</strong>, the SQL statement is completed and no longer executable until
   * {@link #reset(boolean)} is called;
   * <li>Exception is thrown if any error occurs.
   * </ul>
   *
   * @return true if there is data (SQLITE_ROW) was returned, false if statement has been completed (SQLITE_DONE)
   * @throws SQLiteException if result code from sqlite3_step was neither SQLITE_ROW nor SQLITE_DONE, or if any other problem occurs
   * @see <a href="http://www.sqlite.org/c3ref/step.html">sqlite3_step</a>
   */
  public boolean step() throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "step");
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    int rc;
    ProgressHandler ph = prepareStep();
    try {
      SQLiteProfiler profiler = myProfiler;
      long from = profiler == null ? 0 : System.nanoTime();
      rc = _SQLiteSwigged.sqlite3_step(handle);
      if (profiler != null)
        profiler.reportStep(myStepped, mySqlParts.toString(), from, System.nanoTime(), rc);
    } finally {
      finalizeStep(ph, "step");
    }
    stepResult(rc, "step");
    return myHasRow;
  }

  /**
   * Convenience method that ignores the available data and steps through the SQL statement until evaluation is
   * completed. See {@link #step} for details.
   * <p/>
   * Most often it's used to chain calls.
   *
   * @return this statement
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   */
  public SQLiteStatement stepThrough() throws SQLiteException {
    while (step()) { /* do nothing */ }
    return this;
  }

  /**
   * Cancels the currently running statement. This method has effect only during execution of the step() method,
   * and so it is run from a different thread.
   * <p/>
   * This method works by setting a cancel flag, which is checked by the progress callback. Hence, if the progress
   * callback is disabled, this method will not have effect. Likewise, if <code>stepsPerCallback</code> parameter
   * is set to large values, the reaction to this call may be far from immediate.
   * <p/>
   * If execution is cancelled, the step() method will throw {@link SQLiteInterruptedException}, and transaction
   * will be rolled back.
   * <p/>
   * This method is <strong>thread-safe</strong>.
   * <p/>
   *
   * @see SQLiteConnection#setStepsPerCallback
   * @see <a href="http://www.sqlite.org/c3ref/progress_handler.html">sqlite3_progress_callback</a>
   */
  public void cancel() {
    ProgressHandler handler;
    synchronized (this) {
      myCancelled = true;
      handler = myProgressHandler;
    }
    if (handler != null) {
      handler.cancel();
    }
  }

  /**
   * Checks whether there's data to be read with <code>columnXYZ</code> methods.
   *
   * @return true if last call to {@link #step} has returned true
   */
  public boolean hasRow() {
    return myHasRow;
  }

  /**
   * Checks if some parameters were bound
   *
   * @return true if at least one of the statement parameters has been bound to a value
   */
  public boolean hasBindings() {
    return myHasBindings;
  }

  /**
   * Checks if the statement has been evaluated
   *
   * @return true if the statement has been stepped at least once, and not reset
   */
  public boolean hasStepped() {
    return myStepped;
  }

  /**
   * Loads int values returned from a query into a buffer.
   * <p/>
   * The purpose of this method is to run a query and load a single-column result in bulk. This could save a lot of time
   * by making a single JNI call instead of 2*N calls to <code>step()</code> and <code>columnInt()</code>.
   * <p/>
   * If result set contains NULL value, it's replaced with 0.
   * <p/>
   * This method may be called iteratively with a fixed-size buffer. For example:
   * <pre>
   *   SQLiteStatement st = connection.prepare("SELECT id FROM articles WHERE text LIKE '%whatever%'");
   *   try {
   *     int[] buffer = new int[1000];
   *     while (!st.hasStepped() || st.hasRow()) {
   *       int loaded = st.loadInts(0, buffer, 0, buffer.length);
   *       processResult(buffer, 0, loaded);
   *     }
   *   } finally {
   *     st.dispose();
   *   }
   * </pre>
   * <p/>
   * After method finishes, the number of rows loaded is returned and statement's {@link #hasRow} method indicates
   * whether more rows are available.
   *
   * @param column column index, as used in {@link #columnInt}
   * @param buffer buffer for accepting loaded integers
   * @param offset offset in the buffer to start writing
   * @param length maximum number of integers to load from the database
   * @return actual number of integers loaded
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   */
  public int loadInts(int column, int[] buffer, int offset, int length) throws SQLiteException {
    myController.validate();
    if (buffer == null || length <= 0 || offset < 0 || offset + length > buffer.length) {
      assert false;
      return 0;
    }
    if (Internal.isFineLogging())
      Internal.logFine(this, "loadInts(" + column + "," + offset + "," + length + ")");
    if (myStepped && !myHasRow)
      return 0;
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    int r;
    int rc;
    ProgressHandler ph = prepareStep();
    try {
      _SQLiteManual manual = myController.getSQLiteManual();
      SQLiteProfiler profiler = myProfiler;
      long from = profiler == null ? 0 : System.nanoTime();
      r = manual.wrapper_load_ints(handle, column, buffer, offset, length);
      rc = manual.getLastReturnCode();
      if (profiler != null) profiler.reportLoadInts(myStepped, mySqlParts.toString(), from, System.nanoTime(), rc, r);
    } finally {
      finalizeStep(ph, "loadInts");
    }
    stepResult(rc, "loadInts");
    return r;
  }

  /**
   * Loads long values returned from a query into a buffer.
   * <p/>
   * The purpose of this method is to run a query and load a single-column result in bulk. This could save a lot of time
   * by making a single JNI call instead of 2*N calls to <code>step()</code> and <code>columnLong()</code>.
   * <p/>
   * If result set contains NULL value, it's replaced with 0.
   * <p/>
   * This method may be called iteratively with a fixed-size buffer. For example:
   * <pre>
   *   SQLiteStatement st = connection.prepare("SELECT id FROM articles WHERE text LIKE '%whatever%'");
   *   try {
   *     long[] buffer = new long[1000];
   *     while (!st.hasStepped() || st.hasRow()) {
   *       int loaded = st.loadInts(0, buffer, 0, buffer.length);
   *       processResult(buffer, 0, loaded);
   *     }
   *   } finally {
   *     st.dispose();
   *   }
   * </pre>
   * <p/>
   * After method finishes, the number of rows loaded is returned and statement's {@link #hasRow} method indicates
   * whether more rows are available.
   *
   * @param column column index, as used in {@link #columnInt}
   * @param buffer buffer for accepting loaded longs
   * @param offset offset in the buffer to start writing
   * @param length maximum number of integers to load from the database
   * @return actual number of integers loaded
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   */
  public int loadLongs(int column, long[] buffer, int offset, int length) throws SQLiteException {
    myController.validate();
    if (buffer == null || length <= 0 || offset < 0 || offset + length > buffer.length) {
      assert false;
      return 0;
    }
    if (Internal.isFineLogging())
      Internal.logFine(this, "loadLongs(" + column + "," + offset + "," + length + ")");
    if (myStepped && !myHasRow)
      return 0;
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    int r;
    int rc;
    ProgressHandler ph = prepareStep();
    try {
      _SQLiteManual manual = myController.getSQLiteManual();
      SQLiteProfiler profiler = myProfiler;
      long from = profiler == null ? 0 : System.nanoTime();
      r = manual.wrapper_load_longs(handle, column, buffer, offset, length);
      rc = manual.getLastReturnCode();
      if (profiler != null) profiler.reportLoadLongs(myStepped, mySqlParts.toString(), from, System.nanoTime(), rc, r);
    } finally {
      finalizeStep(ph, "loadLongs");
    }
    stepResult(rc, "loadLongs");
    return r;
  }

  /**
   * Returns the number of parameters that can be bound.
   *
   * @return the number of SQL parameters
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_parameter_count.html">sqlite3_bind_parameter_count</a>
   */
  public int getBindParameterCount() throws SQLiteException {
    myController.validate();
    return _SQLiteSwigged.sqlite3_bind_parameter_count(handle());
  }

  /**
   * Returns the name of a given bind parameter, as defined in the SQL.
   *
   * @param index the index of a bindable parameter, starting with 1
   * @return the name of the parameter, e.g. "?PARAM1", or null if parameter is anonymous (just "?")
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_parameter_name.html">sqlite3_bind_parameter_name</a>
   */
  public String getBindParameterName(int index) throws SQLiteException {
    myController.validate();
    return _SQLiteSwigged.sqlite3_bind_parameter_name(handle(), index);
  }

  /**
   * Returns the index of a bind parameter with a given name, as defined in the SQL.
   *
   * @param name parameter name
   * @return the index of the parameter in the SQL, or 0 if no such parameter found
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_parameter_index.html">sqlite3_bind_parameter_index</a>
   */
  public int getBindParameterIndex(String name) throws SQLiteException {
    myController.validate();
    return _SQLiteSwigged.sqlite3_bind_parameter_index(handle(), name);
  }

  /**
   * Wraps <code>getBindParameterIndex</code> method
   * @throws SQLiteException if parameter with specified name was not found
   */
  private int getValidBindParameterIndex(String name) throws SQLiteException {
    int index = getBindParameterIndex(name);
    if (index == 0) {
      throw new SQLiteException(WRAPPER_INVALID_ARG_1, "failed to find parameter with specified name (" + name + ")");
    }
    return index;
  }

  /**
   * Binds SQL parameter to a value of type double.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @param value non-null double value
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_double</a>
   */
  public SQLiteStatement bind(int index, double value) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bind(" + index + "," + value + ")");
    int rc = _SQLiteSwigged.sqlite3_bind_double(handle(), index, value);
    myController.throwResult(rc, "bind(double)", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a value of type double.
   *
   * @param name parameter name
   * @param value non-null double value
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_double</a>
   */
  public SQLiteStatement bind(String name, double value) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value);
  }

  /**
   * Binds SQL parameter to a value of type int.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @param value non-null int value
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_int</a>
   */
  public SQLiteStatement bind(int index, int value) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bind(" + index + "," + value + ")");
    int rc = _SQLiteSwigged.sqlite3_bind_int(handle(), index, value);
    myController.throwResult(rc, "bind(int)", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a value of type int.
   *
   * @param name parameter name
   * @param value non-null int value
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_int</a>
   */
  public SQLiteStatement bind(String name, int value) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value);
  }

  /**
   * Binds SQL parameter to a value of type long.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @param value non-null long value
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_int64</a>
   */
  public SQLiteStatement bind(int index, long value) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bind(" + index + "," + value + ")");
    int rc = _SQLiteSwigged.sqlite3_bind_int64(handle(), index, value);
    myController.throwResult(rc, "bind(long)", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a value of type long.
   *
   * @param name parameter name
   * @param value non-null long value
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_int64</a>
   */
  public SQLiteStatement bind(String name, long value) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value);
  }

  /**
   * Binds SQL parameter to a value of type String.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @param value String value, if null then {@link #bindNull} will be called
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_text</a>
   */
  public SQLiteStatement bind(int index, String value) throws SQLiteException {
    if (value == null) {
      Internal.logFine(this, "bind(null string)");
      return bindNull(index);
    }
    myController.validate();
    if (Internal.isFineLogging()) {
      if (value.length() <= 20)
        Internal.logFine(this, "bind(" + index + "," + value + ")");
      else
        Internal.logFine(this, "bind(" + index + "," + value.substring(0, 20) + "....)");
    }
    int rc = _SQLiteManual.sqlite3_bind_text(handle(), index, value);
    myController.throwResult(rc, "bind(String)", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a value of type String.
   *
   * @param name parameter name
   * @param value String value, if null then {@link #bindNull} will be called
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_text</a>
   */
  public SQLiteStatement bind(String name, String value) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value);
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a byte array.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @param value an array of bytes to be used as the blob value; if null, {@link #bindNull} is called
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public SQLiteStatement bind(int index, byte[] value) throws SQLiteException {
    return value == null ? bindNull(index) : bind(index, value, 0, value.length);
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a byte array.
   *
   * @param name parameter name
   * @param value an array of bytes to be used as the blob value; if null, {@link #bindNull} is called
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public SQLiteStatement bind(String name, byte[] value) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value);
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a range within byte array.
   *
   * @param index  the index of the boundable parameter, starting with 1
   * @param value  an array of bytes; if null, {@link #bindNull} is called
   * @param offset position in the byte array to start reading value from
   * @param length number of bytes to read from value
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public SQLiteStatement bind(int index, byte[] value, int offset, int length) throws SQLiteException {
    if (value == null) {
      Internal.logFine(this, "bind(null blob)");
      return bindNull(index);
    }
    if (offset < 0 || offset + length > value.length)
      throw new ArrayIndexOutOfBoundsException(value.length + " " + offset + " " + length);
    myController.validate();
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "bind(" + index + ",[" + length + "])");
    }
    int rc = _SQLiteManual.sqlite3_bind_blob(handle(), index, value, offset, length);
    myController.throwResult(rc, "bind(blob)", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a range within byte array.
   *
   * @param name parameter name
   * @param value  an array of bytes; if null, {@link #bindNull} is called
   * @param offset position in the byte array to start reading value from
   * @param length number of bytes to read from value
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public SQLiteStatement bind(String name, byte[] value, int offset, int length) throws SQLiteException {
    return bind(getValidBindParameterIndex(name), value, offset, length);
  }

  /**
   * Binds SQL parameter to a BLOB value, consiting of a given number of zero bytes.
   *
   * @param index  the index of the boundable parameter, starting with 1
   * @param length number of zero bytes to use as a parameter
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_zeroblob</a>
   */
  public SQLiteStatement bindZeroBlob(int index, int length) throws SQLiteException {
    if (length < 0) {
      Internal.logFine(this, "bind(null zeroblob)");
      return bindNull(index);
    }
    myController.validate();
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "bindZeroBlob(" + index + "," + length + ")");
    }
    int rc = _SQLiteSwigged.sqlite3_bind_zeroblob(handle(), index, length);
    myController.throwResult(rc, "bindZeroBlob()", this);
    myHasBindings = true;
    return this;
  }

  /**
   * Binds SQL parameter to a BLOB value, consiting of a given number of zero bytes.
   *
   * @param name parameter name
   * @param length number of zero bytes to use as a parameter
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_zeroblob</a>
   */
  public SQLiteStatement bindZeroBlob(String name, int length) throws SQLiteException {
    return bindZeroBlob(getValidBindParameterIndex(name), length);
  }

  /**
   * Binds SQL parameter to a NULL value.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @return this object
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_null</a>
   */
  public SQLiteStatement bindNull(int index) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bind_null(" + index + ")");
    int rc = _SQLiteSwigged.sqlite3_bind_null(handle(), index);
    myController.throwResult(rc, "bind(null)", this);
    // specifically does not set myHasBindings to true
    return this;
  }

  /**
   * Binds SQL parameter to a NULL value.
   *
   * @param name parameter name
   * @return this object
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_null</a>
   */
  public SQLiteStatement bindNull(String name) throws SQLiteException {
    return bindNull(getValidBindParameterIndex(name));
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a stream. The stream can further be used to write into,
   * before the first call to {@link #step}.
   * <p/>
   * After the application is done writing to the parameter stream, it should be closed.
   * <p/>
   * If statement is executed before the stream is closed, the value will not be set for the parameter.
   *
   * @param index the index of the boundable parameter, starting with 1
   * @return stream to receive data for the BLOB parameter
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public OutputStream bindStream(int index) throws SQLiteException {
    return bindStream(index, 0);
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a stream. The stream can further be used to write into,
   * before the first call to {@link #step}.
   * <p/>
   * After the application is done writing to the parameter stream, it should be closed.
   * <p/>
   * If statement is executed before the stream is closed, the value will not be set for the parameter.
   *
   * @param name parameter name
   * @return stream to receive data for the BLOB parameter
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public OutputStream bindStream(String name) throws SQLiteException {
    return bindStream(getValidBindParameterIndex(name), 0);
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a stream. The stream can further be used to write into,
   * before the first call to {@link #step}.
   * <p/>
   * After the application is done writing to the parameter stream, it should be closed.
   * <p/>
   * If statement is executed before the stream is closed, the value will not be set for the parameter.
   *
   * @param index      the index of the boundable parameter, starting with 1
   * @param bufferSize the number of bytes to be allocated for the buffer (the buffer will grow as needed)
   * @return stream to receive data for the BLOB parameter
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public OutputStream bindStream(int index, int bufferSize) throws SQLiteException {
    myController.validate();
    if (Internal.isFineLogging())
      Internal.logFine(this, "bindStream(" + index + "," + bufferSize + ")");
    try {
      DirectBuffer buffer = myController.allocateBuffer(bufferSize);
      BindStream out = new BindStream(index, buffer);
      List<BindStream> list = myBindStreams;
      if (list == null) {
        myBindStreams = list = new ArrayList<BindStream>(1);
      }
      list.add(out);
      myHasBindings = true;
      return out;
    } catch (IOException e) {
      throw new SQLiteException(WRAPPER_WEIRD, "cannot allocate buffer", e);
    }
  }

  /**
   * Binds SQL parameter to a BLOB value, represented by a stream. The stream can further be used to write into,
   * before the first call to {@link #step}.
   * <p/>
   * After the application is done writing to the parameter stream, it should be closed.
   * <p/>
   * If statement is executed before the stream is closed, the value will not be set for the parameter.
   *
   * @param name parameter name
   * @param bufferSize the number of bytes to be allocated for the buffer (the buffer will grow as needed)
   * @return stream to receive data for the BLOB parameter
   * @throws SQLiteException if SQLite returns an error,
   *         or if parameter with specified name was not found,
   *         or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/bind_blob.html">sqlite3_bind_blob</a>
   */
  public OutputStream bindStream(String name, int bufferSize) throws SQLiteException {
    return bindStream(getValidBindParameterIndex(name), bufferSize);
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type String after {@link #step()} has returned true.
   *
   * @param column the index of the column, starting with 0
   * @return a String value or null if database value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_text16</a>
   */
  public String columnString(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnString(" + column + ")");
    _SQLiteManual sqlite = myController.getSQLiteManual();
    String result = sqlite.sqlite3_column_text(handle, column);
    myController.throwResult(sqlite.getLastReturnCode(), "columnString()", this);
    if (Internal.isFineLogging()) {
      if (result == null) {
        Internal.logFine(this, "columnString(" + column + ") is null");
      } else if (result.length() <= 20) {
        Internal.logFine(this, "columnString(" + column + ")=" + result);
      } else {
        Internal.logFine(this, "columnString(" + column + ")=" + result.substring(0, 20) + "....");
      }
    }
    return result;
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type int after {@link #step()} has returned true.
   *
   * @param column the index of the column, starting with 0
   * @return an int value, or value converted to int, or 0 if value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_int</a>
   */
  public int columnInt(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnInt(" + column + ")");
    int r = _SQLiteSwigged.sqlite3_column_int(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnInt(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type double after {@link #step()} has returned true.
   *
   * @param column the index of the column, starting with 0
   * @return a double value, or value converted to double, or 0.0 if value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_double</a>
   */
  public double columnDouble(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnDouble(" + column + ")");
    double r = _SQLiteSwigged.sqlite3_column_double(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnDouble(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type long after {@link #step()} has returned true.
   *
   * @param column the index of the column, starting with 0
   * @return a long value, or value converted to long, or 0L if the value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_int64</a>
   */
  public long columnLong(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnLong(" + column + ")");
    long r = _SQLiteSwigged.sqlite3_column_int64(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnLong(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type BLOB after {@link #step()} has returned true.
   *
   * @param column the index of the column, starting with 0
   * @return a byte array with the value, or null if the value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_blob</a>
   */
  public byte[] columnBlob(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnBytes(" + column + ")");
    _SQLiteManual sqlite = myController.getSQLiteManual();
    byte[] r = sqlite.sqlite3_column_blob(handle, column);
    myController.throwResult(sqlite.getLastReturnCode(), "columnBytes", this);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnBytes(" + column + ")=[" + (r == null ? "null" : r.length) + "]");
    return r;
  }

  /**
   * Gets an InputStream for reading a BLOB column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of type BLOB after {@link #step()} has returned true.
   * <p/>
   * The stream should be read and closed before next call to step or reset. Otherwise, the stream is automatically
   * closed and disposed, and the following attempts to read from it result in IOException.
   *
   * @param column the index of the column, starting with 0
   * @return a stream to read value from, or null if the value is NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_blob</a>
   */
  public InputStream columnStream(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, true);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnStream(" + column + ")");
    _SQLiteManual sqlite = myController.getSQLiteManual();
    ByteBuffer buffer = sqlite.wrapper_column_buffer(handle, column);
    myController.throwResult(sqlite.getLastReturnCode(), "columnStream", this);
    if (buffer == null)
      return null;
    ColumnStream in = new ColumnStream(buffer);
    List<ColumnStream> table = myColumnStreams;
    if (table == null)
      myColumnStreams = table = new ArrayList<ColumnStream>(1);
    table.add(in);
    return in;
  }

  /**
   * Checks if the value returned in the given column is null.
   *
   * @param column the index of the column, starting with 0
   * @return true if the result for the column was NULL
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_type</a>
   */
  public boolean columnNull(int column) throws SQLiteException {
    myController.validate();
    int valueType = getColumnType(column, handle());
    return valueType == SQLITE_NULL;
  }

  /**
   * Gets the number of columns in the result set.
   * <p/>
   * This method may be called before statement is executed, during execution or after statement has executed (
   * {@link #step} returned false).
   * <p/>
   * However, for some statements where the number of columns may vary - such as
   * "SELECT * FROM ..." - the correct result is guaranteed only if method is called during statement execution,
   * when <code>step()</code> has returned true. (That is so because sqlite3_column_count function does not
   * force statement recompilation if database schema has changed, but sqlite3_step does.)
   *
   * @return the number of columns
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_count.html">sqlite3_column_count</a>
   */
  public int columnCount() throws SQLiteException {
    myController.validate();
    return getColumnCount(handle());
  }

  /**
   * Gets a column value after step has returned a row of the result set.
   * <p/>
   * Call this method to retrieve data of any type after {@link #step()} has returned true.
   * <p/>
   * The class of the object returned depends on the value and the type of value reported by SQLite. It can be:
   * <ul>
   * <li><code>null</code> if the value is NULL
   * <li><code>String</code> if the value has type SQLITE_TEXT
   * <li><code>Integer</code> or <code>Long</code> if the value has type SQLITE_INTEGER (depending on the value)
   * <li><code>Double</code> if the value has type SQLITE_FLOAT
   * <li><code>byte[]</code> if the value has type SQLITE_BLOB
   * </ul>
   *
   * @param column the index of the column, starting with 0
   * @return an object containing the value
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_blob</a>
   */
  public Object columnValue(int column) throws SQLiteException {
    myController.validate();
    int valueType = getColumnType(column, handle());
    switch (valueType) {
      case SQLITE_NULL:
        return null;
      case SQLITE_FLOAT:
        return columnDouble(column);
      case SQLITE_INTEGER:
        long value = columnLong(column);
        if (value == (int)value) {
          return (int)value;
        } else {
          return value;
        }
      case SQLITE_TEXT:
        return columnString(column);
      case SQLITE_BLOB:
        return columnBlob(column);
      default:
        Internal.recoverableError(this, "value type " + valueType + " not yet supported", true);
        return null;
    }
  }

  /**
   * Gets a type of a column after step() has returned a row.
   * <p/>
   * Call this method to retrieve data of any type after {@link #step()} has returned true.
   * <p/>
   * Note that SQLite has dynamic typing, so this method returns the affinity of the specified column.
   * See <a href="http://sqlite.org/datatype3.html">dynamic typing</a> for details.
   * <p/>
   * This method returns an integer constant, defined in {@link SQLiteConstants}: <code>SQLITE_NULL</code>,
   * <code>SQLITE_INTEGER</code>, <code>SQLITE_TEXT</code>, <code>SQLITE_BLOB</code> or <code>SQLITE_FLOAT</code>.
   * <p/>
   * The value returned by this method is only meaningful if
   * no type conversions have occurred as the result of calling columnNNN() methods.
   *
   * @param column the index of the column, starting with 0
   * @return an integer code, indicating the type affinity of the returned column
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_blob.html">sqlite3_column_type</a>
   */
  public int columnType(int column) throws SQLiteException {
    myController.validate();
    return getColumnType(column, handle());
  }

  /**
   * Gets a name of the column in the result set.
   *
   * @param column the index of the column, starting with 0
   * @return column name
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_name.html">sqlite3_column_name</a>
   */
  public String getColumnName(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, false);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnName(" + column + ")");
    String r = _SQLiteSwigged.sqlite3_column_name(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnName(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets a name of the column's table in the result set.
   *
   * @param column the index of the column, starting with 0
   * @return name of the table that the column belongs to
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_database_name.html">sqlite3_column_table_name</a>
   */
  public String getColumnTableName(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, false);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnTableName(" + column + ")");
    String r = _SQLiteSwigged.sqlite3_column_table_name(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnTableName(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets a name of the column's table's database in the result set.
   *
   * @param column the index of the column, starting with 0
   * @return name of the database that contains the table that the column belongs to
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_database_name.html">sqlite3_column_database_name</a>
   */
  public String getColumnDatabaseName(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, false);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnDatabaseName(" + column + ")");
    String r = _SQLiteSwigged.sqlite3_column_database_name(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnDatabaseName(" + column + ")=" + r);
    return r;
  }

  /**
   * Gets the original name of the column that is behind the given column in the result set. The name
   * is not aliased (not defined in the SQL).
   *
   * @param column the index of the column, starting with 0
   * @return name of the table column
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/column_database_name.html">sqlite3_column_database_name</a>
   */
  public String getColumnOriginName(int column) throws SQLiteException {
    myController.validate();
    SWIGTYPE_p_sqlite3_stmt handle = handle();
    checkColumn(column, handle, false);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnOriginName(" + column + ")");
    String r = _SQLiteSwigged.sqlite3_column_origin_name(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnOriginName(" + column + ")=" + r);
    return r;
  }

  /**
   * Check if the underlying statement is a SELECT.
   *
   * @return true if statement is a SELECT; false if it is UPDATE, INSERT or other DML statement. The return value is undefined for some statements - see SQLite docs.
   * @throws SQLiteException if SQLite returns an error, or if the call violates the contract of this class
   * @see <a href="http://www.sqlite.org/c3ref/stmt_readonly.html">sqlite3_stmt_readonly</a>
   */
  public boolean isReadOnly() throws SQLiteException {
    myController.validate();
    return _SQLiteSwigged.sqlite3_stmt_readonly(handle()) != 0;
  }

  /**
   * Clear all data, disposing the statement. May be called by SQLiteConnection on close.
   */
  void clear() {
    clearBindStreams(false);
    clearColumnStreams();
    myHandle = null;
    myHasRow = false;
    myColumnCount = -1;
    myHasBindings = false;
    myStepped = false;
    myController = SQLiteController.getDisposed(myController);
    myProfiler = null;
    Internal.logFine(this, "cleared");
  }

  private void clearColumnStreams() {
    List<ColumnStream> table = myColumnStreams;
    if (table != null) {
      myColumnStreams = null;
      for (ColumnStream stream : table) {
        try {
          stream.close();
        } catch (IOException e) {
          Internal.logFine(this, e.toString());
        }
      }
    }
  }

  private void clearBindStreams(boolean bind) {
    List<BindStream> table = myBindStreams;
    if (table != null) {
      myBindStreams = null;
      for (BindStream stream : table) {
        if (bind && !stream.isDisposed()) {
          try {
            stream.close();
          } catch (IOException e) {
            Internal.logFine(this, e.toString());
          }
        } else {
          stream.dispose();
        }
      }
      table.clear();
    }
  }

  private SWIGTYPE_p_sqlite3_stmt handle() throws SQLiteException {
    SWIGTYPE_p_sqlite3_stmt handle = myHandle;
    if (handle == null) {
      throw new SQLiteException(WRAPPER_STATEMENT_DISPOSED, null);
    }
    return handle;
  }

  private int getColumnType(int column, SWIGTYPE_p_sqlite3_stmt handle) throws SQLiteException {
    checkColumn(column, handle, false);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnType(" + column + ")");
    int valueType = _SQLiteSwigged.sqlite3_column_type(handle, column);
    if (Internal.isFineLogging())
      Internal.logFine(this, "columnType(" + column + ")=" + valueType);
    return valueType;
  }

  private void checkColumn(int column, SWIGTYPE_p_sqlite3_stmt handle, boolean mustHaveRow) throws SQLiteException {
    // assert right thread
    if (mustHaveRow && !myHasRow)
      throw new SQLiteException(WRAPPER_NO_ROW, null);
    if (column < 0)
      throw new SQLiteException(WRAPPER_COLUMN_OUT_OF_RANGE, String.valueOf(column));
    int columnCount = getColumnCount(handle);
    if (column >= columnCount)
      throw new SQLiteException(WRAPPER_COLUMN_OUT_OF_RANGE, column + "(" + columnCount + ")");
  }

  private int getColumnCount(SWIGTYPE_p_sqlite3_stmt handle) {
    int cc = myColumnCount;
    if (cc < 0) {
      // data_count seems more safe than column_count
      Internal.logFine(this, "asking column count");
      myColumnCount = cc = _SQLiteSwigged.sqlite3_column_count(handle);
      if (cc < 0) {
        Internal.recoverableError(this, "columnsCount=" + cc, true);
        cc = 0;
      } else if (Internal.isFineLogging()) {
        Internal.logFine(this, "columnCount=" + cc);
      }
    }
    return cc;
  }

  private ProgressHandler prepareStep() throws SQLiteException {
    clearBindStreams(true);
    clearColumnStreams();
    ProgressHandler ph = myController.getProgressHandler();
    ph.reset();
    synchronized (this) {
      if (myCancelled)
        throw new SQLiteInterruptedException();
      myProgressHandler = ph;
    }
    return ph;
  }

  private void finalizeStep(ProgressHandler ph, String methodName) {
    synchronized (this) {
      myProgressHandler = null;
    }
    if (ph != null) {
      if (Internal.isFineLogging())
        Internal.logFine(this, methodName + " " + ph.getSteps() + " steps");
      ph.reset();
    }
  }

  private void stepResult(int rc, String methodName) throws SQLiteException {
    if (!myStepped) {
      // if this is a first step, the statement may have been recompiled and column count changed
      myColumnCount = -1;
    }
    myStepped = true;
    if (rc == SQLITE_ROW) {
      if (Internal.isFineLogging())
        Internal.logFine(this, methodName + " ROW");
      myHasRow = true;
    } else if (rc == SQLITE_DONE) {
      if (Internal.isFineLogging())
        Internal.logFine(this, methodName + " DONE");
      myHasRow = false;
    } else {
      myController.throwResult(rc, methodName + "()", this);
    }
  }

  public String toString() {
    return "[" + mySqlParts + "]" + myController;
  }

/*
  protected void finalize() throws Throwable {
    super.finalize();
    SWIGTYPE_p_sqlite3_stmt handle = myHandle;
    if (handle != null) {
      Internal.recoverableError(this, "wasn't disposed", true);
    }
  }
*/

  SWIGTYPE_p_sqlite3_stmt statementHandle() {
    return myHandle;
  }

  private final class BindStream extends OutputStream {
    private final int myIndex;
    private DirectBuffer myBuffer;

    public BindStream(int index, DirectBuffer buffer) throws IOException {
      myIndex = index;
      myBuffer = buffer;
      myBuffer.data().clear();
    }

    public void write(int b) throws IOException {
      try {
        myController.validate();
        ByteBuffer data = buffer(1);
        data.put((byte) b);
      } catch (SQLiteException e) {
        dispose();
        throw new IOException("cannot write: " + e);
      }
    }

    public void write(byte b[], int off, int len) throws IOException {
      try {
        myController.validate();
        ByteBuffer data = buffer(len);
        data.put(b, off, len);
      } catch (SQLiteException e) {
        dispose();
        throw new IOException("cannot write: " + e);
      }
    }

    private ByteBuffer buffer(int len) throws IOException, SQLiteException {
      DirectBuffer buffer = getBuffer();
      ByteBuffer data = buffer.data();
      if (data.remaining() < len) {
        DirectBuffer newBuffer = null;
        try {
          newBuffer = myController.allocateBuffer(buffer.getCapacity() + len);
        } catch (IOException e) {
          dispose();
          throw e;
        }
        ByteBuffer newData = newBuffer.data();
        data.flip();
        newData.put(data);
        myController.freeBuffer(buffer);
        data = newData;
        myBuffer = newBuffer;
        assert data.remaining() >= len : data.capacity();
      }
      return data;
    }

    public void close() throws IOException {
      try {
        myController.validate();
        DirectBuffer buffer = myBuffer;
        if (buffer == null)
          return;
        if (Internal.isFineLogging())
          Internal.logFine(SQLiteStatement.this, "BindStream.close:bind([" + buffer.data().capacity() + "])");
        int rc = _SQLiteManual.wrapper_bind_buffer(handle(), myIndex, buffer);
        dispose();
        myController.throwResult(rc, "bind(buffer)", SQLiteStatement.this);
      } catch (SQLiteException e) {
        throw new IOException("cannot write: " + e);
      }
    }

    public boolean isDisposed() {
      return myBuffer == null;
    }

    private DirectBuffer getBuffer() throws IOException {
      DirectBuffer buffer = myBuffer;
      if (buffer == null)
        throw new IOException("stream discarded");
      if (!buffer.isValid())
        throw new IOException("buffer discarded");
      if (!buffer.isUsed())
        throw new IOException("buffer not used");
      return buffer;
    }

    public void dispose() {
      DirectBuffer buffer = myBuffer;
      if (buffer != null) {
        myBuffer = null;
        myController.freeBuffer(buffer);
      }
      List<BindStream> list = myBindStreams;
      if (list != null) {
        list.remove(this);
      }
    }
  }

  private class ColumnStream extends InputStream {
    private ByteBuffer myBuffer;

    public ColumnStream(ByteBuffer buffer) {
      assert buffer != null;
      myBuffer = buffer;
    }

    public int read() throws IOException {
      ByteBuffer buffer = getBuffer();
      if (buffer.remaining() <= 0)
        return -1;
      byte b = 0;
      try {
        b = buffer.get();
      } catch (BufferUnderflowException e) {
        Internal.logWarn(this, "weird: " + e);
        return -1;
      }
      return ((int) b) & 0xFF;
    }

    public int read(byte b[], int off, int len) throws IOException {
      ByteBuffer buffer = getBuffer();
      int rem = buffer.remaining();
      if (rem <= 0)
        return -1;
      try {
        if (rem < len)
          len = rem;
        buffer.get(b, off, len);
        return len;
      } catch (BufferUnderflowException e) {
        Internal.logWarn(this, "weird: " + e);
        return -1;
      }
    }

    public void close() throws IOException {
      myBuffer = null;
      List<ColumnStream> table = myColumnStreams;
      if (table != null)
        table.remove(this);
    }

    public ByteBuffer getBuffer() throws IOException {
      ByteBuffer buffer = myBuffer;
      if (buffer == null)
        throw new IOException("stream closed");
      return buffer;
    }
  }
}
