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

import static com.almworks.sqlite4java.SQLiteConstants.SQLITE_BUSY;
import static com.almworks.sqlite4java.SQLiteConstants.SQLITE_IOERR_BLOCKED;

/**
 * SQLiteBusyException is a special exception that is thrown whenever SQLite returns SQLITE_BUSY or
 * SQLITE_IOERR_BLOCKED error code. These codes mean that the current operation cannot proceed because the
 * required resources are locked.
 * <p>
 * When a timeout is set via {@link SQLiteConnection#setBusyTimeout}, SQLite will attempt to get the lock during
 * the specified timeout before returning this error.
 * <p>
 * It is recommended to rollback the transaction when this exception is received. However, SQLite tries
 * to make sure that only the last statement failed and it's possible to retry that statement within the current
 * transaction.
 *
 * @author Igor Sereda
 * @see <a href="http://www.sqlite.org/c3ref/busy_handler.html">sqlite3_busy_handler</a>
 * @see <a href="http://www.sqlite.org/lang_transaction.html">Response To Errors Within A Transaction</a>
 */
public class SQLiteBusyException extends SQLiteException {
  public SQLiteBusyException(int errorCode, String errorMessage) {
    super(errorCode, errorMessage);
    assert errorCode == SQLITE_BUSY || errorCode == SQLITE_IOERR_BLOCKED : errorCode;
  }
}
