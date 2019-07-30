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

#include "jni_setup.h"
#include "sqlite3_wrap_manual.h"
#include "intarray.h"
#include <sqlite3.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1version(JNIEnv *jenv, jclass jcls) {
  jstring result = (*jenv)->NewStringUTF(jenv, WRAPPER_VERSION);
  return result;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1open_1v2(JNIEnv *jenv, jclass jcls,
  jstring jfilename, jlongArray jresult, jint jflags, jobjectArray joutError)
{
  const char *filename = 0;
  sqlite3* db = 0;
  int rc = 0;
  jlong r = 0;
  const char *errmsg = 0;
  jstring error = 0;

  if (!jfilename) return WRAPPER_INVALID_ARG_1;
  if (!jresult) return WRAPPER_INVALID_ARG_2;
  filename = (*jenv)->GetStringUTFChars(jenv, jfilename, 0);
  if (!filename) return WRAPPER_CANNOT_TRANSFORM_STRING;

  // todo(maybe) call jresult's getBytes("UTF-8") method to get filename in correct UTF-8
  rc = sqlite3_open_v2(filename, &db, (int)jflags, 0);

  if (rc != SQLITE_OK) {
    errmsg = sqlite3_errmsg(db);
    if (errmsg) {
      error = (*jenv)->NewStringUTF(jenv, errmsg);
      if (error) {
        (*jenv)->SetObjectArrayElement(jenv, joutError, 0, error);
      }
    }
    if (db) {
      // on error, open returns db anyway
      sqlite3_close(db);
      db = 0;
    }
  }

  if (db) {
    *((sqlite3**)&r) = db;
    (*jenv)->SetLongArrayRegion(jenv, jresult, 0, 1, &r);
  }
  (*jenv)->ReleaseStringUTFChars(jenv, jfilename, filename);
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1exec(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jsql, jobjectArray joutError)
{
  sqlite3* db = 0;
  const char *sql = 0;
  char* msg = 0;
  char** msgPtr = (joutError) ? &msg : 0;
  jsize sz = 0;
  jstring err = 0;
  int rc = 0;

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!jsql) return WRAPPER_INVALID_ARG_2;
  db = *(sqlite3**)&jdb;

  // todo(maybe) as in open_v2, convert to correct UTF-8
  sql = (*jenv)->GetStringUTFChars(jenv, jsql, 0);
  if (!sql) return WRAPPER_CANNOT_TRANSFORM_STRING;

  rc = sqlite3_exec(db, sql, 0, 0, msgPtr);

  (*jenv)->ReleaseStringUTFChars(jenv, jsql, sql);
  if (msg) {
    if (joutError) {
      // warning! can fail with exception here if bad array is passed
      sz = (*jenv)->GetArrayLength(jenv, joutError);
      if (sz == 1) {
        err = (*jenv)->NewStringUTF(jenv, msg);
        if (err) {
          (*jenv)->SetObjectArrayElement(jenv, joutError, 0, err);
        }
      }
    }
    sqlite3_free(msg);
  }
  
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1table_1column_1metadata(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jDbName, jstring jTableName, jstring jColumnName, jobjectArray jOut12, jintArray jOut345)
{
  sqlite3* db = 0;
  const char* dbName = 0;
  const char* tableName = 0;
  const char* columnName = 0;
  const char* dataType = 0;
  const char* collSeq = 0;

  int flags[3] = {0, 0, 0}; // notNull, primaryKey, autoinc
  jint jflags[3] = {0, 0, 0};

  jstring result = 0;
  int err = 0;
  int rc = 0;

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!jTableName) return WRAPPER_INVALID_ARG_3;
  if (!jColumnName) return WRAPPER_INVALID_ARG_4;
  if (!jOut12) return WRAPPER_INVALID_ARG_5;
  if (!jOut345) return WRAPPER_INVALID_ARG_6;

  db = *(sqlite3**)&jdb;
  dbName = jDbName ? (*jenv)->GetStringUTFChars(jenv, jDbName, 0) : 0;
  tableName = (*jenv)->GetStringUTFChars(jenv, jTableName, 0);
  columnName = (*jenv)->GetStringUTFChars(jenv, jColumnName, 0);

  if (!tableName || !columnName || (!dbName && jDbName)) {
    rc = WRAPPER_CANNOT_TRANSFORM_STRING;
  } else {
    rc = sqlite3_table_column_metadata(db, dbName, tableName, columnName, &dataType, &collSeq, &flags[0], &flags[1], &flags[2]);
  }

  if (dbName) (*jenv)->ReleaseStringUTFChars(jenv, jDbName, dbName);
  if (tableName) (*jenv)->ReleaseStringUTFChars(jenv, jTableName, tableName);
  if (columnName) (*jenv)->ReleaseStringUTFChars(jenv, jColumnName, columnName);

  if (!dataType || !collSeq) {
    if (!db) return WRAPPER_WEIRD;
    err = sqlite3_errcode(db);
    if (err == SQLITE_NOMEM) return err;
  } else {
    result = (*jenv)->NewStringUTF(jenv, dataType);
    if (!result) return WRAPPER_CANNOT_ALLOCATE_STRING;
    (*jenv)->SetObjectArrayElement(jenv, jOut12, 0, result);

    result = (*jenv)->NewStringUTF(jenv, collSeq);
    if (!result) return WRAPPER_CANNOT_ALLOCATE_STRING;
    (*jenv)->SetObjectArrayElement(jenv, jOut12, 1, result);

    jflags[0] = (jint)flags[0];
    jflags[1] = (jint)flags[1];
    jflags[2] = (jint)flags[2];
    (*jenv)->SetIntArrayRegion(jenv, jOut345, 0, 3, jflags);
  }

  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1prepare_1v2(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jsql, jlongArray jresult)
{
  sqlite3* db = 0;
  const jchar *sql = 0;
  sqlite3_stmt* stmt = 0;
  int rc = 0;
  jlong r = 0;
  int length = 0;

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!jsql) return WRAPPER_INVALID_ARG_2;
  if (!jresult) return WRAPPER_INVALID_ARG_3;
  db = *(sqlite3**)&jdb;

  length = (*jenv)->GetStringLength(jenv, jsql) * sizeof(jchar);
  sql = (*jenv)->GetStringCritical(jenv, jsql, 0);

  if (!sql) return WRAPPER_CANNOT_TRANSFORM_STRING;
  stmt = (sqlite3_stmt*)0;
  rc = sqlite3_prepare16_v2(db, sql, length, &stmt, 0);

  (*jenv)->ReleaseStringCritical(jenv, jsql, sql);
  if (stmt) {
    *((sqlite3_stmt**)&r) = stmt;
    (*jenv)->SetLongArrayRegion(jenv, jresult, 0, 1, &r);
  }

  return rc;
}

/*
//
//JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1prepare_1v2_1optimized(JNIEnv *jenv, jclass jcls,
//  jlong jdb, jbyteArray jsql, jint jsqlLength, jlongArray jresult)
//{
//  sqlite3* db = 0;
//  char *sql = 0;
//  sqlite3_stmt* stmt = 0;
//  const char *tail = 0;
//  int rc = 0;
//  jlong r = 0;
//
//  if (!jdb) return WRAPPER_INVALID_ARG_1;
//  if (!jsql) return WRAPPER_INVALID_ARG_2;
//  if (!jresult) return WRAPPER_INVALID_ARG_3;
//  if ((*jenv)->GetArrayLength(jenv, jsql) <= jsqlLength) return WRAPPER_INVALID_ARG_4;
//  if (jsqlLength < 0) return WRAPPER_INVALID_ARG_5;
//  sql = (char*)(*jenv)->GetPrimitiveArrayCritical(jenv, jsql, 0);
//  if (!sql) return WRAPPER_CANNOT_TRANSFORM_STRING;
//  sql[jsqlLength] = 0;
//
//  db = *(sqlite3**)&jdb;
//  stmt = (sqlite3_stmt*)0;
//  tail = 0;
//
//  rc = sqlite3_prepare_v2(db, sql, -1, &stmt, &tail);
//
//  if (stmt) {
//    *((sqlite3_stmt**)&r) = stmt;
//    (*jenv)->SetLongArrayRegion(jenv, jresult, 0, 1, &r);
//  }
//  (*jenv)->ReleasePrimitiveArrayCritical(jenv, jsql, sql, 0);
//
//  return rc;
//}
//
*/

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1prepare_1v3(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jsql, jint jprepFlags, jlongArray jresult)
{
    sqlite3* db = 0;
    const jchar *sql = 0;
    sqlite3_stmt* stmt = 0;
    int rc = 0;
    jlong r = 0;
    int length = 0;
    unsigned int prepFlags = (unsigned)jprepFlags;

    if (!jdb) return WRAPPER_INVALID_ARG_1;
    if (!jsql) return WRAPPER_INVALID_ARG_2;
    if (!jresult) return WRAPPER_INVALID_ARG_3;
    db = *(sqlite3**)&jdb;

    length = (*jenv)->GetStringLength(jenv, jsql) * sizeof(jchar);
    sql = (*jenv)->GetStringCritical(jenv, jsql, 0);

    if (!sql) return WRAPPER_CANNOT_TRANSFORM_STRING;
    stmt = (sqlite3_stmt*)0;
    rc = sqlite3_prepare16_v3(db, sql, length, prepFlags, &stmt, 0);

    (*jenv)->ReleaseStringCritical(jenv, jsql, sql);
    if (stmt) {
      *((sqlite3_stmt**)&r) = stmt;
      (*jenv)->SetLongArrayRegion(jenv, jresult, 0, 1, &r);
    }

    return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1bind_1text(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jindex, jstring jvalue)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  int length = 0;
  const jchar *value = 0;
  void (*destructor)(void*) = 0;
  int rc = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!jvalue) return WRAPPER_INVALID_ARG_3;
  length = (*jenv)->GetStringLength(jenv, jvalue) * sizeof(jchar);
  if (length > 0) {
    value = (*jenv)->GetStringCritical(jenv, jvalue, 0);
    destructor = SQLITE_TRANSIENT;
  } else {
    value = (const jchar*)"";
    destructor = SQLITE_STATIC;
  }
  if (!value) return WRAPPER_CANNOT_TRANSFORM_STRING;

  rc = sqlite3_bind_text16(stmt, jindex, value, length, destructor);

  if (length > 0) {
    (*jenv)->ReleaseStringCritical(jenv, jvalue, value);
  }
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1bind_1blob(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jindex, jbyteArray jvalue, jint joffset, jint jlength)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  int length = 0;
  void *value = 0;
  int rc = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!jvalue) return WRAPPER_INVALID_ARG_2;
  if (joffset < 0) return WRAPPER_INVALID_ARG_3;
  if (jlength < 0) return WRAPPER_INVALID_ARG_4;
  length = (int)(*jenv)->GetArrayLength(jenv, jvalue);
  if (joffset > length) return WRAPPER_INVALID_ARG_5;
  if (joffset + jlength > length) return WRAPPER_INVALID_ARG_6;

  if (jlength == 0) {
    rc = sqlite3_bind_zeroblob(stmt, jindex, 0);
  } else {
    value = (void*)(*jenv)->GetPrimitiveArrayCritical(jenv, jvalue, 0);
    if (!value) return WRAPPER_CANNOT_TRANSFORM_STRING;
    rc = sqlite3_bind_blob(stmt, jindex, (void*)(((unsigned char*)value) + joffset), jlength, SQLITE_TRANSIENT);
    (*jenv)->ReleasePrimitiveArrayCritical(jenv, jvalue, value, 0);
  }
  return rc;
}


JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1column_1text(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jcolumn, jobjectArray joutValue)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  const jchar *text = 0;
  jstring result = 0;
  sqlite3* db = 0;
  int err = 0;
  int length = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!joutValue) return WRAPPER_INVALID_ARG_3;
  text = sqlite3_column_text16(stmt, jcolumn);
  if (!text) {
    // maybe we're out of memory
    db = sqlite3_db_handle(stmt);
    if (!db) return WRAPPER_WEIRD;
    err = sqlite3_errcode(db);
    if (err == SQLITE_NOMEM) return err;
  } else {
    length = sqlite3_column_bytes16(stmt, jcolumn);
    if (length < 0) return WRAPPER_WEIRD_2;
    result = (*jenv)->NewString(jenv, text, length / sizeof (jchar));
    if (!result) return WRAPPER_CANNOT_ALLOCATE_STRING;
  }
  (*jenv)->SetObjectArrayElement(jenv, joutValue, 0, result);
  return SQLITE_OK;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1column_1blob(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jcolumn, jobjectArray joutValue)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  const void *value = 0;
  sqlite3* db = 0;
  int err = 0;
  int length = 0;
  jbyteArray result = 0;
  void* resultPtr = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!joutValue) return WRAPPER_INVALID_ARG_3;

  value = sqlite3_column_blob(stmt, jcolumn);
  if (!value) {
    // maybe we're out of memory
    db = sqlite3_db_handle(stmt);
    if (!db) return WRAPPER_WEIRD;
    err = sqlite3_errcode(db);
    if (err == SQLITE_NOMEM) return err;
  } else {
    length = sqlite3_column_bytes(stmt, jcolumn);
    if (length < 0) return WRAPPER_WEIRD_2;
    result = (*jenv)->NewByteArray(jenv, length);
    if (!result) return WRAPPER_CANNOT_ALLOCATE_STRING;
    resultPtr = (void*)(*jenv)->GetPrimitiveArrayCritical(jenv, result, 0);
    if (!resultPtr) return WRAPPER_CANNOT_ALLOCATE_STRING;
    memcpy(resultPtr, value, length);
    (*jenv)->ReleasePrimitiveArrayCritical(jenv, result, resultPtr, 0);
  }
  (*jenv)->SetObjectArrayElement(jenv, joutValue, 0, result);
  return SQLITE_OK;
}


JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1blob_1open(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jdbname, jstring jtable, jstring jcolumn, jlong jrowid, jboolean jwriteAccess, jlongArray jresult)
{
  sqlite3* db = 0;
  const char *dbname = 0;
  const char *table = 0;
  const char *column = 0;
  int rc = 0;
  sqlite3_blob *blob = 0;
  jlong r = 0;

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!jtable) return WRAPPER_INVALID_ARG_3;
  if (!jcolumn) return WRAPPER_INVALID_ARG_4;
  if (!jresult) return WRAPPER_INVALID_ARG_5;

  db = *(sqlite3**)&jdb;
  dbname = jdbname ? (*jenv)->GetStringUTFChars(jenv, jdbname, 0) : 0;
  table = (*jenv)->GetStringUTFChars(jenv, jtable, 0);
  column = (*jenv)->GetStringUTFChars(jenv, jcolumn, 0);
  if (!table || !column || (!dbname && jdbname)) {
    rc = WRAPPER_CANNOT_TRANSFORM_STRING;
  } else {
    rc = sqlite3_blob_open(db, dbname, table, column, jrowid, jwriteAccess ? 1 : 0, &blob);
    if (blob) {
      *((sqlite3_blob**)&r) = blob;
      (*jenv)->SetLongArrayRegion(jenv, jresult, 0, 1, &r);
    }
  }
  if (dbname) (*jenv)->ReleaseStringUTFChars(jenv, jdbname, dbname);
  if (table) (*jenv)->ReleaseStringUTFChars(jenv, jtable, table);
  if (column) (*jenv)->ReleaseStringUTFChars(jenv, jcolumn, column);
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1blob_1read(JNIEnv *jenv, jclass jcls,
  jlong jblob, jint jblobOffset, jbyteArray jbuffer, jint jbufferOffset, jint jlength)
{
  int length = 0;
  sqlite3_blob* blob = 0;
  void *buffer = 0;
  int rc = 0;

  if (!jblob) return WRAPPER_INVALID_ARG_1;
  if (!jbuffer) return WRAPPER_INVALID_ARG_2;
  if (jbufferOffset < 0) return WRAPPER_INVALID_ARG_3;
  if (jlength < 0) return WRAPPER_INVALID_ARG_4;
  if (jlength == 0) return SQLITE_OK;

  length = (int)(*jenv)->GetArrayLength(jenv, jbuffer);
  if (jbufferOffset > length) return WRAPPER_INVALID_ARG_5;
  if (jbufferOffset + jlength > length) return WRAPPER_INVALID_ARG_6;

  blob = *(sqlite3_blob**)&jblob;
  buffer = (void*)(*jenv)->GetPrimitiveArrayCritical(jenv, jbuffer, 0);
  if (!buffer) return WRAPPER_CANNOT_TRANSFORM_STRING;

  rc = sqlite3_blob_read(blob, (void*)(((unsigned char*)buffer) + jbufferOffset), jlength, jblobOffset);
  (*jenv)->ReleasePrimitiveArrayCritical(jenv, jbuffer, buffer, 0);
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1blob_1write(JNIEnv *jenv, jclass jcls,
  jlong jblob, jint jblobOffset, jbyteArray jbuffer, jint jbufferOffset, jint jlength)
{
  int length = 0;
  sqlite3_blob* blob = 0;
  void *buffer = 0;
  int rc = 0;

  if (!jblob) return WRAPPER_INVALID_ARG_1;
  if (!jbuffer) return WRAPPER_INVALID_ARG_2;
  if (jbufferOffset < 0) return WRAPPER_INVALID_ARG_3;
  if (jlength < 0) return WRAPPER_INVALID_ARG_4;
  if (jlength == 0) return SQLITE_OK;

  length = (int)(*jenv)->GetArrayLength(jenv, jbuffer);
  if (jbufferOffset > length) return WRAPPER_INVALID_ARG_5;
  if (jbufferOffset + jlength > length) return WRAPPER_INVALID_ARG_6;

  blob = *(sqlite3_blob**)&jblob;
  buffer = (void*)(*jenv)->GetPrimitiveArrayCritical(jenv, jbuffer, 0);
  if (!buffer) return WRAPPER_CANNOT_TRANSFORM_STRING;

  rc = sqlite3_blob_write(blob, (void*)(((unsigned char*)buffer) + jbufferOffset), jlength, jblobOffset);
  (*jenv)->ReleasePrimitiveArrayCritical(jenv, jbuffer, buffer, 0);
  return rc;
}

//typedef struct {
//  char busy;  // if set, the buffer is used
//  char dispose;  // if set, buffer must be freed when it is no longer busy
//  char data[];
//} wrapper_buffer;

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1alloc(JNIEnv *jenv, jclass jcls,
  jint size, jlongArray ppBuf, jobjectArray ppByteBuffer)
{
  void *ptr = 0;
  jlong lptr = 0;
  jobject controlBuffer = 0;
  jobject dataBuffer = 0;

  if (size < 3) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_2;
  if (!ppByteBuffer) return WRAPPER_INVALID_ARG_3;

  ptr = sqlite3_malloc(size);
  if (!ptr) return WRAPPER_OUT_OF_MEMORY;

  *((void**)&lptr) = ptr;
  controlBuffer = (*jenv)->NewDirectByteBuffer(jenv, ptr, 2);
  if (!controlBuffer) {
    sqlite3_free(ptr);
    return WRAPPER_OUT_OF_MEMORY;
  }
  dataBuffer = (*jenv)->NewDirectByteBuffer(jenv, (void*)(((unsigned char*)ptr) + 2), size - 2);
  if (!dataBuffer) {
    sqlite3_free(ptr);
    return WRAPPER_OUT_OF_MEMORY;
  }

  memset(ptr, 0, size);

  (*jenv)->SetLongArrayRegion(jenv, ppBuf, 0, 1, &lptr);
  (*jenv)->SetObjectArrayElement(jenv, ppByteBuffer, 0, controlBuffer);
  (*jenv)->SetObjectArrayElement(jenv, ppByteBuffer, 1, dataBuffer);

  return SQLITE_OK;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1free(JNIEnv *jenv, jclass jcls,
  jlong jbuffer)
{
  unsigned char *ptr = *(unsigned char**)&jbuffer;
  if (!ptr) return SQLITE_OK;

  // actually free if not in use
  if (!ptr[0]) {
    ptr[1] = -1;
    sqlite3_free((void*)ptr);
  } else {
    ptr[1] = 1;
  }

  return SQLITE_OK;
}

void bind_release(void *ptr);

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1bind_1buffer(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jindex, jlong jbuffer, jint jlength)
{
  sqlite3_stmt *stmt = *(sqlite3_stmt**)&jstmt;
  unsigned char *buffer = *(unsigned char**)&jbuffer;
  int rc = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!buffer) return WRAPPER_INVALID_ARG_2;

  // if should be freed...
  if (buffer[1]) return WRAPPER_INVALID_ARG_3;
  // mark as used
  buffer[0]++;

  rc = sqlite3_bind_blob(stmt, jindex, (const void*)(buffer + 2), jlength, &bind_release);
  if (rc != SQLITE_OK) {
    buffer[0]--;
  }

  return rc;
}

void bind_release(void *ptr) {
  unsigned char *buffer;
  if (!ptr) return;
  buffer = ((unsigned char *)ptr) - 2;
  if (buffer[0] > 0) {
    buffer[0]--;
  }
  if (buffer[1] == 1) {
    sqlite3_free((void*)buffer);
  }
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1column_1buffer(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint jcolumn, jobjectArray joutBuffer)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  const void *value = 0;
  sqlite3* db = 0;
  int err = 0;
  int length = 0;
  jobject result = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!joutBuffer) return WRAPPER_INVALID_ARG_3;

  value = sqlite3_column_blob(stmt, jcolumn);
  if (!value) {
    // maybe we're out of memory
    db = sqlite3_db_handle(stmt);
    if (!db) return WRAPPER_WEIRD;
    err = sqlite3_errcode(db);
    if (err == SQLITE_NOMEM) return err;
  } else {
    length = sqlite3_column_bytes(stmt, jcolumn);
    if (length < 0) return WRAPPER_WEIRD_2;
    result = (*jenv)->NewDirectByteBuffer(jenv, (void*)value, length);
    if (!result) return WRAPPER_CANNOT_ALLOCATE_STRING;
  }
  (*jenv)->SetObjectArrayElement(jenv, joutBuffer, 0, result);
  return SQLITE_OK;
}


int progress_handler(void *ptr);

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_install_1progress_1handler(JNIEnv *jenv, jclass jcls,
  jlong jdb, jint steps, jlongArray ppBuf, jobjectArray ppByteBuffer)
{
  sqlite3* db = 0;
  int rc = 0;
  void *ptr = 0;
  jlong lptr = 0;
  jobject buffer = 0;
  int len = 2 * sizeof(jlong);

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_2;
  if (!ppByteBuffer) return WRAPPER_INVALID_ARG_3;
  if (steps < 1) return WRAPPER_INVALID_ARG_4;
  db = *(sqlite3**)&jdb;

  ptr = (jlong*)sqlite3_malloc(len);
  if (!ptr) return WRAPPER_OUT_OF_MEMORY;

  *((void**)&lptr) = ptr;
  buffer = (*jenv)->NewDirectByteBuffer(jenv, ptr, len);
  if (!buffer) {
    sqlite3_free(ptr);
    return WRAPPER_OUT_OF_MEMORY;
  }

  memset(ptr, 0, len);

  (*jenv)->SetLongArrayRegion(jenv, ppBuf, 0, 1, &lptr);
  (*jenv)->SetObjectArrayElement(jenv, ppByteBuffer, 0, buffer);

  sqlite3_progress_handler(db, steps, &progress_handler, ptr);

  return SQLITE_OK;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_uninstall_1progress_1handler(JNIEnv *jenv, jclass jcls,
  jlong jdb, jlong jptr)
{
  sqlite3* db = 0;
  void *ptr = 0;

  if (!jdb) return WRAPPER_INVALID_ARG_1;
  if (!jptr) return WRAPPER_INVALID_ARG_2;
  db = *(sqlite3**)&jdb;
  ptr = *(void**)&jptr;

  sqlite3_progress_handler(db, 1, 0, 0);
  sqlite3_free(ptr);

  return SQLITE_OK;
}

int progress_handler(void *ptr) {
  jlong* lptr = 0;

  if (!ptr) return 1;
  lptr = *(jlong**)&ptr;

  lptr[1]++;
  if (lptr[0] != 0) return -1;

  return 0;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1load_1ints(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint column, jintArray ppBuf, jint offset, jint count, jintArray ppCount)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  jint loaded = 0;
  int p = offset;
  int rc = 0;
  jint *buf = 0;
  jint len = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_2;
  if (!ppCount) return WRAPPER_INVALID_ARG_3;
  if (count <= 0) return WRAPPER_INVALID_ARG_4;

  len = (*jenv)->GetArrayLength(jenv, ppBuf);
  if (offset < 0 || offset + count > len) return WRAPPER_INVALID_ARG_4;

  buf = (*jenv)->GetIntArrayElements(jenv, ppBuf, 0);
  if (!buf) return WRAPPER_CANNOT_ALLOCATE_STRING;

  while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
    buf[p++] = sqlite3_column_int(stmt, column);
    if (++loaded >= count) {
      break;
    }
  }

  (*jenv)->ReleaseIntArrayElements(jenv, ppBuf, buf, 0);
  (*jenv)->SetIntArrayRegion(jenv, ppCount, 0, 1, &loaded);

  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_wrapper_1load_1longs(JNIEnv *jenv, jclass jcls,
  jlong jstmt, jint column, jlongArray ppBuf, jint offset, jint count, jintArray ppCount)
{
  sqlite3_stmt* stmt = *(sqlite3_stmt**)&jstmt;
  jint loaded = 0;
  int p = offset;
  int rc = 0;
  jlong *buf = 0;
  jint len = 0;

  if (!stmt) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_2;
  if (!ppCount) return WRAPPER_INVALID_ARG_3;
  if (count <= 0) return WRAPPER_INVALID_ARG_4;

  len = (*jenv)->GetArrayLength(jenv, ppBuf);
  if (offset < 0 || offset + count > len) return WRAPPER_INVALID_ARG_4;

  buf = (*jenv)->GetLongArrayElements(jenv, ppBuf, 0);
  if (!buf) return WRAPPER_CANNOT_ALLOCATE_STRING;

  while ((rc = sqlite3_step(stmt)) == SQLITE_ROW) {
    buf[p++] = sqlite3_column_int64(stmt, column);
    if (++loaded >= count) {
      break;
    }
  }

  (*jenv)->ReleaseLongArrayElements(jenv, ppBuf, buf, 0);
  (*jenv)->SetIntArrayRegion(jenv, ppCount, 0, 1, &loaded);

  return rc;
}


JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1intarray_1register(JNIEnv *jenv, jclass jcls,
  jlong jdb, jlongArray ppBuf)
{
  sqlite3* db = *(sqlite3**)&jdb;
  sqlite3_intarray_module *module = 0;
  jlong r = 0;
  int rc = 0;

  if (!db) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_3;

  rc = sqlite3_intarray_register(db, &module);
  if (module) {
    *((sqlite3_intarray_module**)&r) = module;
    (*jenv)->SetLongArrayRegion(jenv, ppBuf, 0, 1, &r);
  }

  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1intarray_1create(JNIEnv *jenv, jclass jcls,
  jlong jmodule, jstring jname, jlongArray ppBuf)
{
  sqlite3_intarray_module* module = *(sqlite3_intarray_module**)&jmodule;
  sqlite3_intarray *arr = 0;
  jlong r = 0;
  const char *name = 0;
  char *namec = 0;
  int rc = 0;
 
  if (!module) return WRAPPER_INVALID_ARG_1;
  if (!ppBuf) return WRAPPER_INVALID_ARG_3;

  name = (*jenv)->GetStringUTFChars(jenv, jname, 0);
  if (!name) return WRAPPER_CANNOT_TRANSFORM_STRING;
  namec = (char*)sqlite3_malloc((int)strlen(name) + 1);
  if (namec) {
    strcpy(namec, name);
  }
  (*jenv)->ReleaseStringUTFChars(jenv, jname, name);
  if (!namec) return SQLITE_NOMEM;

  rc = sqlite3_intarray_create(module, namec, &arr);
  if (arr) {
    *((sqlite3_intarray**)&r) = arr;
    (*jenv)->SetLongArrayRegion(jenv, ppBuf, 0, 1, &r);
  }
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1intarray_1bind(JNIEnv *jenv, jclass jcls,
	jlong jarray, jlongArray jbuffer, jint joffset, jint jlength, jboolean ordered, jboolean unique)
{
  sqlite3_intarray* array = *(sqlite3_intarray**)&jarray;
  jlong *buf = 0;
  sqlite3_int64 *copy = 0;
  int len = 0;
  int rc = 0;

  if (!array) return WRAPPER_INVALID_ARG_1;
  if (!jbuffer) return WRAPPER_INVALID_ARG_2;

  len = (*jenv)->GetArrayLength(jenv, jbuffer);
  if (len < 0) return WRAPPER_INVALID_ARG_3;
  if (joffset < 0 || joffset > len) return WRAPPER_INVALID_ARG_4;
  if (jlength < 0 || joffset + jlength > len) return WRAPPER_INVALID_ARG_5;

  if (jlength > 0) {
    copy = (sqlite3_int64*) sqlite3_malloc(jlength * sizeof(sqlite3_int64));
    if (!copy) return WRAPPER_CANNOT_ALLOCATE_STRING;
    buf = (jlong*)(*jenv)->GetPrimitiveArrayCritical(jenv, jbuffer, 0);
    if (!buf) return WRAPPER_CANNOT_ALLOCATE_STRING;
    memcpy(copy, buf + joffset, jlength * sizeof(sqlite3_int64));
    (*jenv)->ReleasePrimitiveArrayCritical(jenv, jbuffer, (void*)buf, JNI_ABORT);
    rc = sqlite3_intarray_bind(array, jlength, copy, sqlite3_free, (int)ordered, (int)unique, 1);
  } else {
    rc = sqlite3_intarray_bind(array, 0, 0, 0, 0, 0, 1);
  }

  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1intarray_1unbind(JNIEnv *jenv, jclass jcls,
  jlong jarray)
{
  sqlite3_intarray* array = *(sqlite3_intarray**)&jarray;
  int rc = 0;

  if (!array) return WRAPPER_INVALID_ARG_1;
  rc = sqlite3_intarray_bind(array, 0, 0, 0, 0, 0, 0);
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1intarray_1destroy(JNIEnv *jenv, jclass jcls,
  jlong jarray)
{
  sqlite3_intarray* array = *(sqlite3_intarray**)&jarray;
  int rc = 0;

  if (!array) return WRAPPER_INVALID_ARG_1;
  rc = sqlite3_intarray_destroy(array);
  return rc;
}

JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1load_1extension(JNIEnv *jenv, jclass jcls,
  jlong jdb, jstring jfile, jstring jproc, jobjectArray ppError)
{
  sqlite3* db = *(sqlite3**)&jdb;
  int rc = 0;
  char* file = 0;
  char* proc = 0;
  char* error = 0;
  jstring errorString = 0;

  if (jfile) {
    file = (char *)(*jenv)->GetStringUTFChars(jenv, jfile, 0);
    if (!file) return WRAPPER_CANNOT_TRANSFORM_STRING;
  }
  if (jproc) {
    proc = (char *)(*jenv)->GetStringUTFChars(jenv, jproc, 0);
    if (!proc) {
      if (file) (*jenv)->ReleaseStringUTFChars(jenv, jfile, (const char *)file);
      return WRAPPER_CANNOT_TRANSFORM_STRING;
    }
  }

  rc = sqlite3_load_extension(db, (char const *)file, (char const *)proc, &error);

  if (error) {
    errorString = (*jenv)->NewStringUTF(jenv, error);
    if (errorString) {
      (*jenv)->SetObjectArrayElement(jenv, ppError, 0, errorString);
    }
    sqlite3_free(error);
  }

  if (proc) (*jenv)->ReleaseStringUTFChars(jenv, jproc, (const char *)proc);
  if (file) (*jenv)->ReleaseStringUTFChars(jenv, jfile, (const char *)file);

  return rc;
}


JNIEXPORT jint JNICALL Java_com_almworks_sqlite4java__1SQLiteManualJNI_sqlite3_1win32_1set_1directory(JNIEnv *jenv, jclass jcls,
  jint jtype, jstring zValue)
{
#ifdef SQLITE_OS_WIN
  int rc;
  const jchar *name = 0;
  unsigned long type = (unsigned long)jtype;

  if (zValue) {
    name = (*jenv)->GetStringCritical(jenv, zValue, 0);
    if (!name) return WRAPPER_CANNOT_TRANSFORM_STRING;
    rc = sqlite3_win32_set_directory16(type, name);
    (*jenv)->ReleaseStringCritical(jenv, zValue, name);
  } else {
    rc = sqlite3_win32_set_directory16(type, 0);
  }

  return rc;
#else
  return SQLITE_ERROR;
#endif
}

#ifdef __cplusplus
}
#endif
