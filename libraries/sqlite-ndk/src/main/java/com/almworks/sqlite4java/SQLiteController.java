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

/**
 * This interface is used as a strategy for SQLiteStatement lifecycle. Initially it is set by {@link SQLiteConnection#prepare}
 * method, and when statement is disposed the strategy is reset to the dummy implementation.
 *
 * @author Igor Sereda
 */
abstract class SQLiteController {
  /**
   * @throws SQLiteException if connection or statement cannot be used at this moment by the calling thread.
   */
  public abstract void validate() throws SQLiteException;

  /**
   * If result code (from sqlite operation) is not zero (SQLITE_OK), then retrieves additional error info
   * and throws verbose exception.
   */
  public abstract void throwResult(int resultCode, String message, Object additionalMessage) throws SQLiteException;

  /**
   * Performs statement life-keeping on disposal. If the statement is cached, its handle is returned to the
   * connection's cache. If it is not cached, the statement handle is finalized.
   * <p>
   * Implementation may call {@link SQLiteStatement#clear()} during execution.
   *
   * @param statement statement that is about to be disposed
   */
  public abstract void dispose(SQLiteStatement statement);
  
  public abstract void dispose(SQLiteBlob blob);

  public abstract void dispose(SQLiteLongArray array);

  public abstract _SQLiteManual getSQLiteManual();

  public abstract DirectBuffer allocateBuffer(int sizeEstimate) throws IOException, SQLiteException;

  public abstract void freeBuffer(DirectBuffer buffer);

  public abstract ProgressHandler getProgressHandler() throws SQLiteException;

  public static SQLiteController getDisposed(SQLiteController controller) {
    if (controller instanceof Disposed) {
      return controller;
    }
    boolean debug = false;
    assert debug = true;
    if (!debug) {
      return Disposed.INSTANCE;
    } else {
      return new Disposed(controller == null ? "" : controller.toString());
    }
  }


  /**
   * A stub implementation that replaces connection-based implementation when statement is disposed.
   */
  private static class Disposed extends SQLiteController {
    public static final Disposed INSTANCE = new Disposed("");

    private final String myName;

    private Disposed(String namePrefix) {
      myName = namePrefix + "[D]";
    }

    public String toString() {
      return myName;
    }

    public void validate() throws SQLiteException {
      throw new SQLiteException(SQLiteConstants.WRAPPER_MISUSE, "statement is disposed");
    }

    public void throwResult(int resultCode, String message, Object additionalMessage) throws SQLiteException {
    }

    public void dispose(SQLiteStatement statement) {
    }

    public void dispose(SQLiteBlob blob) {
    }

    public void dispose(SQLiteLongArray array) {
    }

    public _SQLiteManual getSQLiteManual() {
      // must not come here anyway
      return new _SQLiteManual();
    }

    public DirectBuffer allocateBuffer(int sizeEstimate) throws IOException, SQLiteException {
      throw new IOException();
    }

    public void freeBuffer(DirectBuffer buffer) {
    }

    public ProgressHandler getProgressHandler() {
      return ProgressHandler.DISPOSED;
    }
  }
}
