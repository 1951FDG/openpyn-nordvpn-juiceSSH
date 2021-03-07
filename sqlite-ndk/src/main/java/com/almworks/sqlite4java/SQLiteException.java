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
 * SQLiteException is thrown whenever SQLite cannot execute an operation and returns an error code.
 * <p>
 * Error codes can be compared against {@link SQLiteConstants}.
 * <p>
 * It's safe to rollback the transaction when SQLiteException is caught.
 *
 * @author Igor Sereda
 */
public class SQLiteException extends Exception {
  private final int myErrorCode;

  /**
   * Creates an instance of SQLiteException.
   *
   * @param errorCode codes are defined in {@link SQLiteConstants}
   * @param errorMessage optional error message
   */
  public SQLiteException(int errorCode, String errorMessage) {
    this(errorCode, errorMessage, null);
  }

  /**
   * Creates an instance of SQLiteException.
   *
   * @param errorCode codes are defined in {@link SQLiteConstants}
   * @param errorMessage optional error message
   * @param cause error cause
   */
  public SQLiteException(int errorCode, String errorMessage, Throwable cause) {
    super("[" + errorCode + "] " + (errorMessage == null ? "sqlite error" : errorMessage), cause);
    myErrorCode = errorCode;
    if (Internal.isFineLogging()) {
      Internal.logFine(getClass(), getMessage());
    }
  }

  /**
   * Gets the error code returned by SQLite.
   *
   * @return error code
   */
  public int getErrorCode() {
    return myErrorCode;
  }

  /**
   * Gets base error code returned by SQLite. Base error code is the lowest 8 bit from the extended error code,
   * like SQLITE_IOERR_BLOCKED.
   *
   * @return error code
   */
  public int getBaseErrorCode() {
    return myErrorCode >= 0 ? myErrorCode & 0xFF : myErrorCode;
  }
}
