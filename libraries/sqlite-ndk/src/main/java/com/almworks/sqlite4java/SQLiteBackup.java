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

import static com.almworks.sqlite4java.SQLiteConstants.SQLITE_DONE;
import static com.almworks.sqlite4java.SQLiteConstants.WRAPPER_BACKUP_DISPOSED;

/**
 * SQLiteBackup wraps an instance of SQLite database backup, represented as <strong><code>sqlite3_backup*</strong></code>
 * in SQLite C API.
 * <p>
 * Usage example:
 * <pre>
 * SQLiteBackup backup = connection.initializeBackup(new File("filename"));
 * try {
 *   while (!backup.isFinished()) {
 *     backup.backupStep(32);
 *   }
 * } finally {
 *   backup.dispose();
 * }
 * </pre>
 * </p>
 * <p>
 * Unless a method is marked as thread-safe, it is confined to the thread that has opened the connection to the source
 * database. Calling a confined method from a different thread will result in exception.
 * </p>
 *
 * @author Igor Korsunov
 * @see <a href="http://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupinit"> SQLite Online Backup API</a>
 * @see <a href="http://www.sqlite.org/backup.html">Using the SQLite Online Backup API</a>
 * @see SQLiteConnection#initializeBackup
 */
public class SQLiteBackup {

  /**
   * Source database connection
   */
  private final SQLiteConnection mySource;

  /**
   * Destination database connection
   */
  private final SQLiteConnection myDestination;

  /**
   * Handle for native operations
   */
  private SWIGTYPE_p_sqlite3_backup myHandle;

  private SQLiteController myDestinationController;
  private SQLiteController mySourceController;

  /**
   * If true, last call to sqlite3_backup_step() returned SQLITE_DONE
   */
  private boolean myFinished;

  SQLiteBackup(SQLiteController sourceController, SQLiteController destinationController,
    SWIGTYPE_p_sqlite3_backup handle, SQLiteConnection source, SQLiteConnection destination)
  {
    mySourceController = sourceController;
    myDestinationController = destinationController;
    myHandle = handle;
    myDestination = destination;
    mySource = source;
    Internal.logFine(this, "instantiated");
  }

  /**
   * Copy up to pagesToBackup pages from source database to destination. If pagesToBackup is negative, all remaining
   * pages are copied.
   * <p>
   * If source database is modified during backup by any connection other than the source connection,
   * then the backup will be restarted by the next call to backupStep.
   * If the source database is modified by the source connection itself, then destination
   * database is be updated without backup restart.
   * </p>
   *
   * @param pagesToBackup the maximum number of pages to back up during this step, or negative number to back up all pages
   * @return true if the backup was finished, false if there are still pages to back up
   * @throws SQLiteException     if SQLite returns an error or if the call violates the contract of this class
   * @throws SQLiteBusyException if SQLite cannot establish SHARED_LOCK on the source database or RESERVED_LOCK on
   *                             the destination database or source connection is currently used to write to the database.
   *                             In these cases call to backupStep can be retried later.
   * @see <a href="http://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupstep">sqlite3_backup_step</a>
   */
  public boolean backupStep(int pagesToBackup) throws SQLiteException, SQLiteBusyException {
    mySource.checkThread();
    myDestination.checkThread();
    if (myFinished) {
      Internal.logWarn(this, "already finished");
      return true;
    }
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "backupStep(" + pagesToBackup + ")");
    }
    SWIGTYPE_p_sqlite3_backup handle = handle();
    int rc = _SQLiteSwigged.sqlite3_backup_step(handle, pagesToBackup);
    throwResult(rc, "backupStep failed");
    if (rc == SQLITE_DONE) {
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "finished");
      }
      myFinished = true;
    }
    return myFinished;
  }

  /**
   * Checks whether the backup was successfully finished.
   *
   * @return true if last call to {@link #backupStep} has returned true.
   */
  public boolean isFinished() {
    return myFinished;
  }

  /**
   * Returns connection to the destination database, that was opened by {@link com.almworks.sqlite4java.SQLiteConnection#initializeBackup}.
   * <p>
   * <strong>Important!</strong> If you call this method, you should be careful about disposing the connection you got.
   * You should only dispose it <strong>after</strong> disposing SQLiteBackup instance, otherwise the JVM might crash.
   * </p>
   *
   * @return destination database connection
   */
  public SQLiteConnection getDestinationConnection() {
    return myDestination;
  }

  /**
   * Dispose this backup instance and, if <code>disposeDestination</code> is true, dispose the connection to
   * the destination database as well.
   * <p/>
   * You might want to pass <code>false</code> to this method to subsequently call {@link #getDestinationConnection()}
   * and perform any actions on the fresh backup of the database, then dispose it yourself.
   *
   * @param disposeDestination if true, connection to the destination database will be disposed
   */
  public void dispose(boolean disposeDestination) {
    try {
      mySourceController.validate();
      myDestinationController.validate();
    } catch (SQLiteException e) {
      Internal.recoverableError(this, "invalid dispose: " + e, true);
      return;
    }
    Internal.logFine(this, "disposing");
    SWIGTYPE_p_sqlite3_backup handle = myHandle;
    if (handle != null) {
      _SQLiteSwigged.sqlite3_backup_finish(handle);
      myHandle = null;
      mySourceController = SQLiteController.getDisposed(mySourceController);
      myDestinationController = SQLiteController.getDisposed(myDestinationController);
    }
    if (disposeDestination) {
      myDestination.dispose();
    }
  }

  /**
   * Disposes this backup instance and connection to the destination database.
   * <p>
   * This is a convenience method, equivalent to <code>dispose(true)</code>.
   * </p>
   *
   * @see #dispose(boolean)
   */
  public void dispose() {
    dispose(true);
  }

  /**
   * Returns the total number of pages in the source database.
   *
   * @return total number of pages to back up
   * @throws SQLiteException if called from a different thread or if source or destination connection are disposed
   * @see <a href="http://www.sqlite.org/c3ref/backup_finish.html#sqlite3backupfinish">SQLite Online Backup API</a>
   */
  public int getPageCount() throws SQLiteException {
    mySourceController.validate();
    myDestinationController.validate();
    SWIGTYPE_p_sqlite3_backup handle = handle();
    return _SQLiteSwigged.sqlite3_backup_pagecount(handle);
  }

  /**
   * Returns the number of pages still to be backed up.
   *
   * @return number of remaining pages
   * @throws SQLiteException if called from a different thread or if source or destination connection are disposed
   */
  public int getRemaining() throws SQLiteException {
    mySourceController.validate();
    myDestinationController.validate();
    SWIGTYPE_p_sqlite3_backup handle = handle();
    return _SQLiteSwigged.sqlite3_backup_remaining(handle);
  }

  @Override
  public String toString() {
    return "Backup [" + mySource + " -> " + myDestination + "]";
  }

  private SWIGTYPE_p_sqlite3_backup handle() throws SQLiteException {
    SWIGTYPE_p_sqlite3_backup handle = myHandle;
    if (handle == null) {
      throw new SQLiteException(WRAPPER_BACKUP_DISPOSED, null);
    }
    return handle;
  }

  private void throwResult(int rc, String operation) throws SQLiteException {
    if (rc == SQLITE_DONE) return;
    myDestination.throwResult(rc, operation);
  }
}
