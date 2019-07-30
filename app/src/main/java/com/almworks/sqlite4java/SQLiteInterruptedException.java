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
 * SQLiteInterruptedException is a special exception that is thrown whenever SQLite returns SQLITE_INTERRUPT
 * following a call to {@link SQLiteConnection#interrupt}.
 * <p/>
 * The transaction is rolled back when interrupted.
 *
 * @author Igor Sereda
 * @see <a href="http://www.sqlite.org/c3ref/interrupt.html">sqlite3_interrupt</a>
 */
public class SQLiteInterruptedException extends SQLiteException {
  public SQLiteInterruptedException() {
    this(SQLiteConstants.SQLITE_INTERRUPT, "");
  }

  public SQLiteInterruptedException(int resultCode, String message) {
    super(resultCode, message);
  }
}
