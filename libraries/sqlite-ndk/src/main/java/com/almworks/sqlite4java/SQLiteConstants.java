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
 * This interface lists SQLite constants that may be used with sqlite4java. Normally you don't need to use them.
 * <p/>
 * Some error messages are produced by SQLite wrapper, these have names WRAPPER_*. For
 * documentation on SQLite constants, see SQLite docs.
 *
 * @author Igor Sereda
 * @see <a href="http://sqlite.org/c3ref/constlist.html">list of constants in SQLite docs</a>
 */
public interface SQLiteConstants {
  // SQLite return codes
  int SQLITE_OK = 0;   /* Successful result */
  int SQLITE_ERROR = 1;   /* SQL error or missing database */
  int SQLITE_INTERNAL = 2;   /* Internal logic error in SQLite */
  int SQLITE_PERM = 3;   /* Access permission denied */
  int SQLITE_ABORT = 4;   /* Callback routine requested an abort */
  int SQLITE_BUSY = 5;   /* The database file is locked */
  int SQLITE_LOCKED = 6;   /* A table in the database is locked */
  int SQLITE_NOMEM = 7;   /* A malloc() failed */
  int SQLITE_READONLY = 8;   /* Attempt to write a readonly database */
  int SQLITE_INTERRUPT = 9;   /* Operation terminated by sqlite3_interrupt()*/
  int SQLITE_IOERR = 10;   /* Some kind of disk I/O error occurred */
  int SQLITE_CORRUPT = 11;   /* The database disk image is malformed */
  int SQLITE_NOTFOUND = 12;   /* NOT USED. Table or record not found */
  int SQLITE_FULL = 13;   /* Insertion failed because database is full */
  int SQLITE_CANTOPEN = 14;   /* Unable to open the database file */
  int SQLITE_PROTOCOL = 15;   /* NOT USED. Database lock protocol error */
  int SQLITE_EMPTY = 16;   /* Database is empty */
  int SQLITE_SCHEMA = 17;   /* The database schema changed */
  int SQLITE_TOOBIG = 18;   /* String or BLOB exceeds size limit */
  int SQLITE_CONSTRAINT = 19;   /* Abort due to constraint violation */
  int SQLITE_MISMATCH = 20;   /* Data type mismatch */
  int SQLITE_MISUSE = 21;   /* Library used incorrectly */
  int SQLITE_NOLFS = 22;   /* Uses OS features not supported on host */
  int SQLITE_AUTH = 23;   /* Authorization denied */
  int SQLITE_FORMAT = 24;   /* Auxiliary database format error */
  int SQLITE_RANGE = 25;   /* 2'nd parameter to sqlite3_bind out of range */
  int SQLITE_NOTADB = 26;   /* File opened that is not a database file */
  int SQLITE_NOTICE = 27;   /* Notifications from sqlite3_log() */
  int SQLITE_WARNING = 28;   /* Warnings from sqlite3_log() */
  int SQLITE_ROW = 100;  /* sqlite_step() has another row ready */
  int SQLITE_DONE = 101;  /* sqlite_step() has finished executing */

  int INTARRAY_INUSE = 210;   /* Attempting to re-bind array while a cursor is traversing old values */
  int INTARRAY_INTERNAL_ERROR = 212;  /* Some other problem */
  int INTARRAY_DUPLICATE_NAME = 213;  /* Trying to create an array with a duplicate name */

  int SQLITE_ERROR_MISSING_COLLSEQ = (SQLITE_ERROR | (1 << 8));
  int SQLITE_ERROR_RETRY = (SQLITE_ERROR | (2 << 8));
  int SQLITE_IOERR_READ = (SQLITE_IOERR | (1 << 8));
  int SQLITE_IOERR_SHORT_READ = (SQLITE_IOERR | (2 << 8));
  int SQLITE_IOERR_WRITE = (SQLITE_IOERR | (3 << 8));
  int SQLITE_IOERR_FSYNC = (SQLITE_IOERR | (4 << 8));
  int SQLITE_IOERR_DIR_FSYNC = (SQLITE_IOERR | (5 << 8));
  int SQLITE_IOERR_TRUNCATE = (SQLITE_IOERR | (6 << 8));
  int SQLITE_IOERR_FSTAT = (SQLITE_IOERR | (7 << 8));
  int SQLITE_IOERR_UNLOCK = (SQLITE_IOERR | (8 << 8));
  int SQLITE_IOERR_RDLOCK = (SQLITE_IOERR | (9 << 8));
  int SQLITE_IOERR_DELETE = (SQLITE_IOERR | (10 << 8));
  int SQLITE_IOERR_BLOCKED = (SQLITE_IOERR | (11 << 8));
  int SQLITE_IOERR_NOMEM = (SQLITE_IOERR | (12 << 8));
  int SQLITE_IOERR_ACCESS = (SQLITE_IOERR | (13 << 8));
  int SQLITE_IOERR_CHECKRESERVEDLOCK = (SQLITE_IOERR | (14 << 8));
  int SQLITE_IOERR_LOCK = (SQLITE_IOERR | (15 << 8));
  int SQLITE_IOERR_CLOSE = (SQLITE_IOERR | (16 << 8));
  int SQLITE_IOERR_DIR_CLOSE = (SQLITE_IOERR | (17 << 8));
  int SQLITE_IOERR_SHMOPEN = (SQLITE_IOERR | (18 << 8));
  int SQLITE_IOERR_SHMSIZE = (SQLITE_IOERR | (19 << 8));
  int SQLITE_IOERR_SHMLOCK = (SQLITE_IOERR | (20 << 8));
  int SQLITE_IOERR_SHMMAP = (SQLITE_IOERR | (21 << 8));
  int SQLITE_IOERR_SEEK = (SQLITE_IOERR | (22 << 8));
  int SQLITE_IOERR_DELETE_NOENT = (SQLITE_IOERR | (23 << 8));
  int SQLITE_IOERR_MMAP = (SQLITE_IOERR | (24 << 8));
  int SQLITE_IOERR_GETTEMPPATH = (SQLITE_IOERR | (25 << 8));
  int SQLITE_IOERR_CONVPATH = (SQLITE_IOERR | (26 << 8));
  int SQLITE_IOERR_VNODE = (SQLITE_IOERR | (27 << 8));
  int SQLITE_IOERR_AUTH = (SQLITE_IOERR | (28 << 8));
  int SQLITE_IOERR_BEGIN_ATOMIC = (SQLITE_IOERR | (29 << 8));
  int SQLITE_IOERR_COMMIT_ATOMIC = (SQLITE_IOERR | (30 << 8));
  int SQLITE_IOERR_ROLLBACK_ATOMIC = (SQLITE_IOERR | (31 << 8));
  int SQLITE_LOCKED_SHAREDCACHE = (SQLITE_LOCKED | (1 << 8));
  int SQLITE_LOCKED_VTAB = (SQLITE_LOCKED | (2 << 8));
  int SQLITE_BUSY_RECOVERY = (SQLITE_BUSY   |  (1 << 8));
  int SQLITE_BUSY_SNAPSHOT = (SQLITE_BUSY   |  (2 << 8));
  int SQLITE_CANTOPEN_NOTEMPDIR = (SQLITE_CANTOPEN | (1 << 8));
  int SQLITE_CANTOPEN_ISDIR = (SQLITE_CANTOPEN | (2 << 8));
  int SQLITE_CANTOPEN_FULLPATH = (SQLITE_CANTOPEN | (3 << 8));
  int SQLITE_CANTOPEN_CONVPATH = (SQLITE_CANTOPEN | (4 << 8));
  int SQLITE_CORRUPT_VTAB = (SQLITE_CORRUPT | (1 << 8));
  int SQLITE_CORRUPT_SEQUENCE = (SQLITE_CORRUPT | (2 << 8));
  int SQLITE_READONLY_RECOVERY = (SQLITE_READONLY | (1 << 8));
  int SQLITE_READONLY_CANTLOCK = (SQLITE_READONLY | (2 << 8));
  int SQLITE_READONLY_ROLLBACK = (SQLITE_READONLY | (3 << 8));
  int SQLITE_READONLY_DBMOVED = (SQLITE_READONLY | (4 << 8));
  int SQLITE_READONLY_CANTINIT = (SQLITE_READONLY | (5 << 8));
  int SQLITE_READONLY_DIRECTORY = (SQLITE_READONLY | (6 << 8));
  int SQLITE_ABORT_ROLLBACK = (SQLITE_ABORT | (2 << 8));
  int SQLITE_CONSTRAINT_CHECK = (SQLITE_CONSTRAINT | (1 << 8));
  int SQLITE_CONSTRAINT_COMMITHOOK = (SQLITE_CONSTRAINT | (2 << 8));
  int SQLITE_CONSTRAINT_FOREIGNKEY = (SQLITE_CONSTRAINT | (3 << 8));
  int SQLITE_CONSTRAINT_FUNCTION = (SQLITE_CONSTRAINT | (4 << 8));
  int SQLITE_CONSTRAINT_NOTNULL = (SQLITE_CONSTRAINT | (5 << 8));
  int SQLITE_CONSTRAINT_PRIMARYKEY = (SQLITE_CONSTRAINT | (6 << 8));
  int SQLITE_CONSTRAINT_TRIGGER = (SQLITE_CONSTRAINT | (7 << 8));
  int SQLITE_CONSTRAINT_UNIQUE = (SQLITE_CONSTRAINT | (8 << 8));
  int SQLITE_CONSTRAINT_VTAB = (SQLITE_CONSTRAINT | (9 << 8));
  int SQLITE_CONSTRAINT_ROWID = (SQLITE_CONSTRAINT |(10 << 8));
  int SQLITE_NOTICE_RECOVER_WAL = (SQLITE_NOTICE | (1 << 8));
  int SQLITE_NOTICE_RECOVER_ROLLBACK = (SQLITE_NOTICE | (2 << 8));
  int SQLITE_WARNING_AUTOINDEX = (SQLITE_WARNING | (1 << 8));
  int SQLITE_AUTH_USER = (SQLITE_AUTH | (1 << 8));
  int SQLITE_OK_LOAD_PERMANENTLY = (SQLITE_OK | (1 << 8));



  // SQLite Value Types
  int SQLITE_INTEGER = 1;
  int SQLITE_FLOAT = 2;
  int SQLITE_TEXT = 3;
  int SQLITE_BLOB = 4;
  int SQLITE_NULL = 5;

  // SQLITE_OPEN_* are modifiers for opening the database
  int SQLITE_OPEN_READONLY = 0x00000001;
  int SQLITE_OPEN_READWRITE = 0x00000002;
  int SQLITE_OPEN_CREATE = 0x00000004;
  int SQLITE_OPEN_DELETEONCLOSE = 0x00000008;
  int SQLITE_OPEN_EXCLUSIVE = 0x00000010;
  int SQLITE_OPEN_AUTOPROXY = 0x00000020;
  int SQLITE_OPEN_URI = 0x00000040;
  int SQLITE_OPEN_MEMORY = 0x00000080;
  int SQLITE_OPEN_MAIN_DB = 0x00000100;
  int SQLITE_OPEN_TEMP_DB = 0x00000200;
  int SQLITE_OPEN_TRANSIENT_DB = 0x00000400;
  int SQLITE_OPEN_MAIN_JOURNAL = 0x00000800;
  int SQLITE_OPEN_TEMP_JOURNAL = 0x00001000;
  int SQLITE_OPEN_SUBJOURNAL = 0x00002000;
  int SQLITE_OPEN_MASTER_JOURNAL = 0x00004000;
  int SQLITE_OPEN_NOMUTEX = 0x00008000;
  int SQLITE_OPEN_FULLMUTEX = 0x00010000;
  int SQLITE_OPEN_SHAREDCACHE = 0x00020000;
  int SQLITE_OPEN_PRIVATECACHE = 0x00040000;
  int SQLITE_OPEN_WAL = 0x00080000;

  int SQLITE_PREPARE_PERSISTENT = 0x01;

  int SQLITE_WIN32_DATA_DIRECTORY_TYPE = 1;
  int SQLITE_WIN32_TEMP_DIRECTORY_TYPE = 2;

  // SQLITE_LIMIT_* identify class of constructs to be size limited
  int SQLITE_LIMIT_LENGTH = 0;
  int SQLITE_LIMIT_SQL_LENGTH = 1;
  int SQLITE_LIMIT_COLUMN = 2;
  int SQLITE_LIMIT_EXPR_DEPTH = 3;
  int SQLITE_LIMIT_COMPOUND_SELECT = 4;
  int SQLITE_LIMIT_VDBE_OP = 5;
  int SQLITE_LIMIT_FUNCTION_ARG = 6;
  int SQLITE_LIMIT_ATTACHED = 7;
  int SQLITE_LIMIT_LIKE_PATTERN_LENGTH = 8;
  int SQLITE_LIMIT_VARIABLE_NUMBER = 9;
  int SQLITE_LIMIT_TRIGGER_DEPTH = 10;
  int SQLITE_LIMIT_WORKER_THREADS = 11;

  /**
   * Something strange happened.
   */
  int WRAPPER_WEIRD = -99;

  /**
   * Method called in thread that wasn't allowed.
   */
  int WRAPPER_CONFINEMENT_VIOLATED = -98;

  /**
   * Wasn't opened
   */
  int WRAPPER_NOT_OPENED = -97;

  /**
   * Statement disposed
   */
  int WRAPPER_STATEMENT_DISPOSED = -96;

  /**
   * column() requested when no row returned
   */
  int WRAPPER_NO_ROW = -95;

  int WRAPPER_COLUMN_OUT_OF_RANGE = -94;

  /**
   * Blob disposed
   */
  int WRAPPER_BLOB_DISPOSED = -93;

  /**
   * Backup disposed
   */
  int WRAPPER_BACKUP_DISPOSED = -113;

  int WRAPPER_INVALID_ARG_1 = -11;
  int WRAPPER_INVALID_ARG_2 = -12;
  int WRAPPER_INVALID_ARG_3 = -13;
  int WRAPPER_INVALID_ARG_4 = -14;
  int WRAPPER_INVALID_ARG_5 = -15;
  int WRAPPER_INVALID_ARG_6 = -16;
  int WRAPPER_INVALID_ARG_7 = -17;
  int WRAPPER_INVALID_ARG_8 = -18;
  int WRAPPER_INVALID_ARG_9 = -19;

  int WRAPPER_CANNOT_TRANSFORM_STRING = -20;
  int WRAPPER_CANNOT_ALLOCATE_STRING = -21;
  int WRAPPER_OUT_OF_MEMORY = -22;

  int WRAPPER_WEIRD_2 = -199;

  int WRAPPER_CANNOT_LOAD_LIBRARY = -91;
  int WRAPPER_MISUSE = -92;

  int WRAPPER_USER_ERROR = -999;
}
