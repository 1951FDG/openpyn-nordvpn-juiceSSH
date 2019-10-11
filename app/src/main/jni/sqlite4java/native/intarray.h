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

/**
 This file defines virtual table module for tables that represent a memory array.
 The module is loosely based on test_intarray.[ch] from SQLite distribution.
*/
#include "sqlite3.h"

/* Specific error codes */
#define INTARRAY_INUSE    210     /* Attempting to re-bind array while a cursor is traversing old values */
#define INTARRAY_INTERNAL_ERROR  212     /* Some other problem with initialization */
#define INTARRAY_DUPLICATE_NAME  213     /* Creating intarray with the same name as already existing intarray */

/* Represents Integer Array with some name. A virtual table may or may not exist - it is created as needed. */
typedef struct sqlite3_intarray sqlite3_intarray;

/* Represents virtual table module. Its sole purpose is to link sqlite3_intarray with intarray_vtab. */
typedef struct sqlite3_intarray_module sqlite3_intarray_module;

/* Register and return an instance of the module */
int sqlite3_intarray_register(sqlite3 *db, sqlite3_intarray_module **ppReturn);

/*
** Invoke this routine to create a specific instance of an intarray object.
** The new intarray is returned by the 3rd parameter.
**
** Each intarray object corresponds to a virtual table in the TEMP table
** with a name of zName.
**
** zName should be allocated with sqlite3_malloc or similar methods and
** will be freed automatically by sqlite3_intarray_destroy or on creation error.
**
** The virtual table is created initially when this method is called.
** However, if dropped afterwards, it will be recreated on the next call to sqlite3_intarray_bind.
*/
int sqlite3_intarray_create(sqlite3_intarray_module *module, char *zName, sqlite3_intarray **ppReturn);

/*
** Destroy intarray and drop table. The pointer (array) is no longer usable after calling this method.
*/
int sqlite3_intarray_destroy(sqlite3_intarray *array);

/*
** Bind a new array array of integers to a specific intarray object.
**
** The array of integers bound must be unchanged for the duration of
** any query against the corresponding virtual table.  If the integer
** array does change or is deallocated undefined behavior will result.
*/
int sqlite3_intarray_bind(
  sqlite3_intarray *pIntArray,   /* The intarray object to bind to */
  int nElements,                 /* Number of elements in the intarray */
  sqlite3_int64 *aElements,      /* Content of the intarray */
  void (*xFree)(void*),          /* How to dispose of the intarray when done */
  int bOrdered,                  /* If non-zero, the values are guaranteed to be in ascending order */
  int bUnique,                   /* If non-zero, the values are guaranteed to be unique */
  int ensureTableExists
);

