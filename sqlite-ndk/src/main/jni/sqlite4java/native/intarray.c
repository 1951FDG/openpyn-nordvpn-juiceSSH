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

#include "intarray.h"
#include <string.h>
#include <assert.h>
#include <ctype.h>
#include <math.h>

#ifdef _MSC_VER
#define stricmp _stricmp
#else
#include <strings.h>
#define stricmp strcasecmp
#endif

#define MODULE_NAME "INTARRAY"

/* Objects used internally by the virtual table implementation */
typedef struct intarray_vtab intarray_vtab;
typedef struct intarray_cursor intarray_cursor;

typedef struct intarray_map_entry intarray_map_entry;
typedef struct intarray_map intarray_map;
struct intarray_map_entry {
  const char *key;
  int hash;   /* hash = -1 means empty cell, but collision checking must continue */
  void *value;
};
struct intarray_map {
  intarray_map_entry *hashtable;
  int size;
  int rehashSize;
  int count;
};


struct sqlite3_intarray_module {
  /* link to the sqlite session */
  sqlite3 *db;
  intarray_map arrayMap;
};

/*
** Definition of the sqlite3_intarray object.
**
** The internal representation of an intarray object is subject
** to change, is not externally visible, and should be used by
** the implementation of intarray only.  This object is opaque
** to users.
*/
struct sqlite3_intarray {
  sqlite3_intarray_module *module;
  char *zName;

  /* data */
  int n;                    /* Number of elements in the array */
  sqlite3_int64 *a;         /* Contents of the array */
  void (*xFree)(void*);     /* Function used to free a[] */
  int ordered;              /* If true, the elements in a[] are guaranteed to be ordered */
  int unique;               /* If true, the elements in a[] are guaranteed to be unique */

  /* lifecycle */
  int useCount;             /* Number of open cursors */
  int connectCount;         /* Number of connected intarray_vtab's -- may be wrong because xDestroy / xDisconnect may not be called by ROLLBACK */
  int commitState;          /* 0 - transaction in progress; 1 - committed; -1 - rolled back */
};

/* A intarray table object */
struct intarray_vtab {
  /* base class */
  sqlite3_vtab base;
  sqlite3_intarray *intarray;
};

/* A intarray cursor object */
struct intarray_cursor {
  sqlite3_vtab_cursor base;    /* Base class */

  /* parameters */
  int mode;                    /* Search mode - see below */
  sqlite3_int64 max;           /* maximum value */
  sqlite3_int64 min;           /* maximum value */
  int hasMax;
  int hasMin;

  /* state */
  int i;                       /* Current cursor position */
  int uniqueLeft;              /* Count of found unique results (cannot be more than max - min + 1) */
};

/* table of intarrays */
static int intarrayMapInit(intarray_map *map) {
  int initialCount = 17;
  int len = initialCount * sizeof(intarray_map_entry);
  map->size = initialCount;
  map->rehashSize = initialCount * 2 / 3;
  map->hashtable = (intarray_map_entry*)sqlite3_malloc(len);
  if (!map->hashtable) return SQLITE_NOMEM;
  memset(map->hashtable, 0, len);
  map->count = 0;
  return SQLITE_OK;
}

static void intarrayMapDestroy(intarray_map *map) {
  sqlite3_free(map->hashtable);
}

/* taken from SQLite */
static unsigned int strHash(const char *s) {
  unsigned int h = 0;
  while (*s) {
    h = (h << 3) ^ h ^ tolower(*s++);
  }
  return h == 0 ? 1 : h & 0x7FFFFFFF;
}

static int mapPut_(intarray_map_entry *t, int size, sqlite3_intarray *a, unsigned int hash) {
  unsigned int i = hash % size;
  int j = size, k = 0;
  while (t[i].key && j > 0) {
    if (hash == (unsigned int)t[i].hash && !stricmp(t[i].key, a->zName)) {
      return INTARRAY_DUPLICATE_NAME;
    }
    i = (i + 1) % size;
    j--;
  }
  if (t[i].key) return INTARRAY_INTERNAL_ERROR;
  if (t[i].hash == -1) {
    // check trail
    k = (i + 1) % size;
    j--;
    while ((t[k].key || t[k].hash == -1) && j > 0) {
      if (hash == (unsigned int)t[k].hash && !stricmp(t[k].key, a->zName)) {
        return INTARRAY_DUPLICATE_NAME;
      }
      k = (k + 1) % size;
      j--;
    }
  }
  t[i].key = a->zName;
  t[i].hash = (int)hash;
  t[i].value = a;
  return SQLITE_OK;
}

static int rehash(intarray_map *map) {
  int newsize = map->size + (map->size >> 1);
  int newlen = newsize * sizeof(intarray_map_entry);
  intarray_map_entry *newtable = (intarray_map_entry*)sqlite3_malloc(newlen);
  intarray_map_entry *t = map->hashtable;
  int i = 0;

  if (!newtable) return SQLITE_NOMEM;
  memset(newtable, 0, newlen);
  for (i = 0; i < map->size; i++) {
    if (t[i].key) {
      mapPut_(newtable, newsize, (sqlite3_intarray*)t[i].value, t[i].hash);
    }
  }
  map->rehashSize = map->size;
  map->size = newsize;
  map->hashtable = newtable;

  sqlite3_free(t);
  return SQLITE_OK;
}

static int intarrayMapPut(intarray_map *map, sqlite3_intarray *a) {
  unsigned int h1 = strHash(a->zName);
  int rc = mapPut_(map->hashtable, map->size, a, h1);
  if (rc != SQLITE_OK) return rc;

  map->count++;
  if (map->count >= map->rehashSize) {
    rc = rehash(map);
  }
  return rc;
}

static void intarrayMapRemove(intarray_map *map, sqlite3_intarray *a) {
  unsigned int hash = strHash(a->zName);
  unsigned int i = hash % map->size;
  int j = map->size;
  intarray_map_entry *t = map->hashtable;
  while (t[i].key && j > 0) {
    if (hash == (unsigned int)t[i].hash && !stricmp(t[i].key, a->zName)) {
      break;
    }
    i = (i + 1) % map->size;
    j--;
  }
  if (j > 0 && t[i].key) {
    /* mark for further traversal and removal */
    t[i].key = 0;
    t[i].hash = -1;
    t[i].value = 0;
    map->count--;
  }
}

static sqlite3_intarray* intarrayMapFind(intarray_map *map, const char *zName) {
  unsigned int hash = strHash(zName);
  unsigned int i = hash % map->size;
  int j = map->size;
  intarray_map_entry *t = map->hashtable;
  while ((t[i].key || t[i].hash == -1) && j > 0) {
    if (hash == (unsigned int)t[i].hash && !stricmp(t[i].key, zName)) {
      return (sqlite3_intarray*)t[i].value;
    }
    i = (i + 1) % map->size;
    j--;
  }
  return 0;
}

/* ------------------------ */

static int intarrayNextMatch(intarray_cursor *pCur, int startIndex) {
  intarray_vtab *table = (intarray_vtab*)pCur->base.pVtab;
  sqlite3_intarray *arr = table->intarray;
  sqlite3_int64 v = 0;
  if (startIndex >= arr->n || pCur->mode < 0) return arr->n;
  if (pCur->mode == 1) {
    /* search by rowid: check we did not exceed */
    return (pCur->hasMax && startIndex > pCur->max) ? arr->n : startIndex;
  } else if (pCur->mode == 2) {
    /* search by value  */
    if (arr->ordered) {
      return (pCur->hasMax && arr->a[startIndex] > pCur->max) ? arr->n : startIndex;
    }
    if (arr->unique && pCur->uniqueLeft == 0) {
      /* all possible values retrieved */
      return arr->n;
    }
    /* check constraints */
    while (startIndex < arr->n && ((pCur->hasMin && pCur->min > arr->a[startIndex]) || (pCur->hasMax && pCur->max < arr->a[startIndex]))) {
      startIndex++;
    }
    /* manage unique */
    if (arr->unique && pCur->uniqueLeft > 0 && startIndex < arr->n) {
      pCur->uniqueLeft--;
    }
  }
  return startIndex;
}

static int intarray_bsearch(sqlite3_int64 value, const sqlite3_int64 *a, int from, int to, int locateFirst) {
  /* when we need to locate first element, we look not for value, but for "value - 1/2" */
  int i = 0;
  sqlite3_int64 mid = 0;
  to--;
  while (from <= to) {
    i = (from + to) >> 1;
    mid = a[i];
    if (mid < value) from = i + 1;
    else if (mid > value || locateFirst) to = i - 1;
    else return i;
  }
  return from;
}

/* clear data from vtable */
static int drop_array_content(sqlite3_intarray *a) {
  if (!a) return SQLITE_OK;
  if (a->useCount) return INTARRAY_INUSE;
  if (a->xFree) a->xFree(a->a);
  a->xFree = 0;
  a->a = 0;
  a->n = 0;
  a->ordered = 0;
  a->unique = 0;
  return SQLITE_OK;
}

/* create a new vtable */
static int create_vtable(sqlite3_intarray *pIntArray) {
  int rc = SQLITE_OK;
  char *zSql;
  zSql = sqlite3_mprintf("CREATE VIRTUAL TABLE temp.%Q USING INTARRAY", pIntArray->zName);
  rc = sqlite3_exec(pIntArray->module->db, zSql, 0, 0, 0);
  sqlite3_free(zSql);
  return rc;
}

/* drop vtable - clears pIntArray->table through intarrayDestroy method */
static int drop_vtable(sqlite3_intarray *pIntArray) {
  int rc = SQLITE_OK;
  char *zSql;
  zSql = sqlite3_mprintf("DROP TABLE IF EXISTS temp.%Q", pIntArray->zName);
  rc = sqlite3_exec(pIntArray->module->db, zSql, 0, 0, 0);
  sqlite3_free(zSql);
  return rc;
}


/*
** None of this works unless we have virtual tables.
*/
#ifndef SQLITE_OMIT_VIRTUALTABLE

/*
** Table destructor for the intarray module.
*/
static int intarrayDestroy(sqlite3_vtab *p) {
  intarray_vtab *table = *((intarray_vtab**)&p);
  table->intarray->connectCount--;
  sqlite3_free(table);
  return SQLITE_OK;
}

/*
** Table constructor for the intarray module.
*/
static int intarrayCreate(sqlite3 *db, void *pAux, int argc, const char *const*argv, sqlite3_vtab **ppVtab, char **pzErr) {
  int rc = SQLITE_NOMEM;
  sqlite3_intarray_module *module = *((sqlite3_intarray_module**)&pAux);
  intarray_vtab *table;
  sqlite3_intarray *a;

  if (!module || argc < 3) return INTARRAY_INTERNAL_ERROR;
  a = intarrayMapFind(&module->arrayMap, argv[2]);
  if (!a) {
    *pzErr = sqlite3_mprintf("intarray %s is not created", argv[2]);
    return SQLITE_ERROR;
  }

  table = (intarray_vtab*)sqlite3_malloc(sizeof(intarray_vtab));
  if (!table) return rc;

  rc = sqlite3_declare_vtab(db, "CREATE TABLE x(value INTEGER)");
  if (rc != SQLITE_OK) {
    sqlite3_free(table);
    return rc;
  }

  memset(table, 0, sizeof(intarray_vtab));
  table->intarray = a;
  a->connectCount++;

  *ppVtab = (sqlite3_vtab *)table;
  return rc;
}

/*
** Open a new cursor on the intarray table.
*/
static int intarrayOpen(sqlite3_vtab *pVTab, sqlite3_vtab_cursor **ppCursor) {
  int rc = SQLITE_NOMEM;
  intarray_cursor *pCur;
  pCur = (intarray_cursor*)sqlite3_malloc(sizeof(intarray_cursor));
  if (pCur) {
    memset(pCur, 0, sizeof(intarray_cursor));
    *ppCursor = (sqlite3_vtab_cursor *)pCur;
    ((intarray_vtab*)pVTab)->intarray->useCount++;
    rc = SQLITE_OK;
  }
  return rc;
}

/*
** Close a intarray table cursor.
*/
static int intarrayClose(sqlite3_vtab_cursor *cur) {
  intarray_cursor *pCur = (intarray_cursor *)cur;
  ((intarray_vtab*)(cur->pVtab))->intarray->useCount--;
  sqlite3_free(pCur);
  return SQLITE_OK;
}

/*
** Retrieve a column of data.
*/
static int intarrayColumn(sqlite3_vtab_cursor *cur, sqlite3_context *ctx, int column) {
  intarray_cursor *pCur = (intarray_cursor*)cur;
  intarray_vtab *table = (intarray_vtab*)cur->pVtab;
  sqlite3_intarray *arr = table->intarray;
  if (pCur->i >= 0 && pCur->i < arr->n) {
    sqlite3_result_int64(ctx, arr->a[pCur->i]);
  }
  return SQLITE_OK;
}

/*
** Retrieve the current rowid.
*/
static int intarrayRowid(sqlite3_vtab_cursor *cur, sqlite_int64 *pRowid) {
  intarray_cursor *pCur = (intarray_cursor *)cur;
  *pRowid = pCur->i;
  return SQLITE_OK;
}

static int intarrayEof(sqlite3_vtab_cursor *cur) {
  intarray_cursor *pCur = (intarray_cursor *)cur;
  intarray_vtab *table = (intarray_vtab *)cur->pVtab;
  sqlite3_intarray *arr = table->intarray;
  return pCur->i >= arr->n;
}

/*
** Advance the cursor to the next row.
*/
static int intarrayNext(sqlite3_vtab_cursor *cur) {
  intarray_cursor *pCur = (intarray_cursor *)cur;
  pCur->i = intarrayNextMatch(pCur, pCur->i + 1);
  return SQLITE_OK;
}

/* search params */

/*
** We're forced to encode comparison types in the int:
** bit  meaning
** ---  -------
**  0-1 0: full-scan search (ignore arguments)
**      1: search by rowid (1 or 2 arguments)
**      2: search by value (1 or 2 arguments)
**    2 argv[0] is higher bound
**    3 argv[0] may be equal
**    4 argv[0] is lower bound
**    5 argv[1] is higher bound
**    6 argv[1] may be equal
**    7 argv[1] is lower bound
*/

#define INTARRAY_SCAN 0
#define INTARRAY_ROWID 1
#define INTARRAY_VALUE 2
#define INTARRAY_ZEROSET -1

#define INTARRAY_UPPER 1
#define INTARRAY_EQ 2
#define INTARRAY_LOWER 4

#define SMALLEST_INT64 (((sqlite3_int64)1) << 63)
#define LARGEST_INT64  (~(SMALLEST_INT64))

#define INTARRAY_MAX(cur, val, exclusive) \
  if ((exclusive) && val > SMALLEST_INT64) val--; \
  if (!(cur)->hasMax || (cur)->max > val) { (cur)->max = val; (cur)->hasMax = 1; }

#define INTARRAY_MIN(cur, val, exclusive) \
  if ((exclusive) && val < LARGEST_INT64) val++; \
  if (!(cur)->hasMin || (cur)->min < val) { (cur)->min = val; (cur)->hasMin = 1; }

static void intarrayBoundary(int op, intarray_cursor *pCur, sqlite3_int64 intValue, int exactInt) {
  if ((op & INTARRAY_LOWER)) {
    INTARRAY_MIN(pCur, intValue, exactInt && !(op & INTARRAY_EQ))
  } else if ((op & INTARRAY_UPPER)) {
    INTARRAY_MAX(pCur, intValue, exactInt && !(op & INTARRAY_EQ))
  } else if ((op & INTARRAY_EQ)) {
    if (!exactInt) {
      pCur->mode = INTARRAY_ZEROSET;
    } else {
      INTARRAY_MIN(pCur, intValue, 0)
      INTARRAY_MAX(pCur, intValue, 0)
    }
  }
}

static void intarrayConstrainCursor(int op, sqlite3_value *value, intarray_cursor *pCur) {
  int vtype = sqlite3_value_type(value);
  sqlite3_int64 intValue = 0;
  double doubleValue = 0.0;
  int exactInt = 0;

  switch (vtype) {
  case SQLITE_TEXT:
    // peculiar SQLite behavior makes all numbers less than a string.
    // setting negative mode will make zero result set
    if (!(op & INTARRAY_UPPER)) pCur->mode = INTARRAY_ZEROSET;
    break;
  case SQLITE_FLOAT:
    doubleValue = sqlite3_value_double(value);
    if (doubleValue > (double)LARGEST_INT64) {
      if (!(op & INTARRAY_UPPER) || (op & INTARRAY_LOWER)) {
        pCur->mode = INTARRAY_ZEROSET;
      }
      break;
    } else if (doubleValue < (double)SMALLEST_INT64) {
      if (!(op & INTARRAY_LOWER) || (op & INTARRAY_UPPER)) {
        pCur->mode = INTARRAY_ZEROSET;
      }
      break;
    }

    intValue =
      (op & INTARRAY_LOWER) ? (sqlite3_int64)ceil(doubleValue) :
      (op & INTARRAY_UPPER) ? (sqlite3_int64)floor(doubleValue) :
      (sqlite3_int64)doubleValue;
    exactInt = ((double)intValue) == doubleValue;
    intarrayBoundary(op, pCur, intValue, exactInt);
    break;
  case SQLITE_INTEGER :
    intValue = sqlite3_value_int64(value);
    intarrayBoundary(op, pCur, intValue, 1);
    break;
  default:
    pCur->mode = INTARRAY_ZEROSET;
    break;
  }
}

#define INTARRAY_BSEARCH_THRESHOLD 7

static int intarrayFilter(sqlite3_vtab_cursor *pVtabCursor, int idxNum, const char *idxStr, int argc, sqlite3_value **argv) {
  intarray_cursor *pCur = (intarray_cursor *)pVtabCursor;
  intarray_vtab *table = (intarray_vtab*)pCur->base.pVtab;
  sqlite3_intarray *arr = table->intarray;

  int op1 = (idxNum >> 2) & 7, op2 = (idxNum >> 5) & 7;
  sqlite3_int64 v = 0;
  int startIndex = 0;

  pCur->mode = (idxNum & 3);
  pCur->hasMin = 0, pCur->hasMax = 0;
  pCur->min = 0;
  pCur->max = 0;
  pCur->uniqueLeft = -1;
  if (pCur->mode > 0 && argc > 0 && op1) {
    intarrayConstrainCursor(op1, argv[0], pCur);
  }
  if (pCur->mode > 0 && argc > 1 && op2) {
    intarrayConstrainCursor(op2, argv[1], pCur);
  }

  if (pCur->mode < 0 || (pCur->hasMin && pCur->hasMax && pCur->min > pCur->max)) {
    /* constraint is never true */
    pCur->i = arr->n;
    return SQLITE_OK;
  }

  if (pCur->hasMin && pCur->mode == 1) {
    startIndex = (int)pCur->min;
    if (startIndex < 0) startIndex = 0;
  } else if (pCur->hasMin && pCur->mode == 2 && arr->ordered && arr->n > INTARRAY_BSEARCH_THRESHOLD) {
    startIndex = intarray_bsearch(pCur->min, arr->a, 0, arr->n, !arr->unique);
  }

  if (arr->unique && pCur->mode == 2 && pCur->hasMin && pCur->hasMax) {
    v = pCur->max - pCur->min + 1;
    if (v > 0 && v < 0x7FFFFFFF) {
      pCur->uniqueLeft = (int)v;
    }
  }

  pCur->i = intarrayNextMatch(pCur, startIndex);
  return SQLITE_OK;
}


#define INTARRAY_ACCEPTED_OPS (SQLITE_INDEX_CONSTRAINT_EQ | SQLITE_INDEX_CONSTRAINT_GE | SQLITE_INDEX_CONSTRAINT_GT | SQLITE_INDEX_CONSTRAINT_LE | SQLITE_INDEX_CONSTRAINT_LT)

static int intarrayOpbit(int op) {
  switch (op) {
  case SQLITE_INDEX_CONSTRAINT_EQ:
    return INTARRAY_EQ;
  case SQLITE_INDEX_CONSTRAINT_GE:
    return INTARRAY_EQ | INTARRAY_LOWER;
  case SQLITE_INDEX_CONSTRAINT_GT:
    return INTARRAY_LOWER;
  case SQLITE_INDEX_CONSTRAINT_LE:
    return INTARRAY_EQ | INTARRAY_UPPER;
  case SQLITE_INDEX_CONSTRAINT_LT:
    return INTARRAY_UPPER;
  default:
    return 0;
  }
}

static int intarrayC2opbits(sqlite3_index_info *pIdxInfo, int *ix) {
  int r = 0;
  int op1bits = 0, op2bits = 0;
  pIdxInfo->aConstraintUsage[ix[0]].argvIndex = 1;
  pIdxInfo->aConstraintUsage[ix[0]].omit = 1;
  op1bits = intarrayOpbit(pIdxInfo->aConstraint[ix[0]].op);
  r |= op1bits << 2;
  if (ix[1] >= 0) {
    op2bits = intarrayOpbit(pIdxInfo->aConstraint[ix[1]].op);
    if (op2bits & op1bits & 5) {
      /* two constraints with both lower or both higher bound: strange */
      /* do something? */
    } else {
      pIdxInfo->aConstraintUsage[ix[1]].argvIndex = 2;
      pIdxInfo->aConstraintUsage[ix[1]].omit = 1;
      r |= op2bits << 5;
    }
  }
  return r;
}

/*
** Analyse the WHERE condition.
*/
#define FULLSCAN_COST(a) ((a) < 64 ? 64 : (a))
static int llog2(int x) {
  int r = 0;
  while (x > 1) {
    r++;
    x >>= 1;
  }
  return r;
}

static int intarrayBestIndex(sqlite3_vtab *tab, sqlite3_index_info *pIdxInfo) {
  sqlite3_intarray *arr = ((intarray_vtab*)tab)->intarray;
  int arraySize = arr ? arr->n : 0;
  int mode = 0; /*full scan*/
  int i = 0;
  /* support only 2 constraints at maximum - search optimal */
  int rcount = 0, vcount = 0;
  int ix[4] = {-1, -1, -1, -1};

  for (i = 0; i < pIdxInfo->nConstraint; i++) {
    if (pIdxInfo->aConstraint[i].usable) {
      if (pIdxInfo->aConstraint[i].op & (~INTARRAY_ACCEPTED_OPS)) {
        /* strange operation */
        pIdxInfo->estimatedCost = FULLSCAN_COST(arraySize);
        pIdxInfo->idxNum = 0;
        return SQLITE_OK;
      }
      if (pIdxInfo->aConstraint[i].iColumn < 0) {
        if (rcount < 2) ix[rcount] = i;
        rcount++;
      } else {
        if (vcount < 2) ix[2 + vcount] = i;
        vcount++;
      }
    }
  }

  if (rcount > 0) {
    /* search by rowid */
    mode = 1;
    mode |= intarrayC2opbits(pIdxInfo, ix);
    pIdxInfo->estimatedCost = 1.0;
  } else if (vcount > 0) {
    mode = 2;
    mode |= intarrayC2opbits(pIdxInfo, ix + 2);
    pIdxInfo->estimatedCost = 1.0 + llog2(arraySize);
  } else {
    // full scan
    pIdxInfo->estimatedCost = FULLSCAN_COST(arraySize);
  }


  /* todo - to consume orderBy we need to make sure the data is always ordered:
  ** right now an intarray may be rebound with no-ordered data, but bestIndex and vdbe
  ** program will not be recompiled. Another solution would be to create a virtual table
  ** every time bind() is called and tick schema.
  */

  pIdxInfo->idxNum = mode;
  return SQLITE_OK;
}

static int intarrayBegin(sqlite3_vtab *pVTab) {
  return SQLITE_OK;
}

static int intarrayRollback(sqlite3_vtab *pVTab) {
  sqlite3_intarray *a = ((intarray_vtab*)pVTab)->intarray;
  if (a->commitState == 0) a->commitState = -1;
  return SQLITE_OK;
}

static int intarrayCommit(sqlite3_vtab *pVTab) {
  sqlite3_intarray *a = ((intarray_vtab*)pVTab)->intarray;
  if (a->commitState == 0) a->commitState = 1;
  return SQLITE_OK;
}


/*
** A virtual table module that merely echos method calls into TCL
** variables.
*/
static sqlite3_module intarrayModule = {
  2,                           /* iVersion */
  intarrayCreate,              /* xCreate - create a new virtual table */
  intarrayCreate,              /* xConnect - connect to an existing vtab */
  intarrayBestIndex,           /* xBestIndex - find the best query index */
  intarrayDestroy,             /* xDisconnect - disconnect a vtab */
  intarrayDestroy,             /* xDestroy - destroy a vtab */
  intarrayOpen,                /* xOpen - open a cursor */
  intarrayClose,               /* xClose - close a cursor */
  intarrayFilter,              /* xFilter - configure scan constraints */
  intarrayNext,                /* xNext - advance a cursor */
  intarrayEof,                 /* xEof */
  intarrayColumn,              /* xColumn - read data */
  intarrayRowid,               /* xRowid - read data */
  0,                           /* xUpdate */
  intarrayBegin,               /* xBegin */
  0,                           /* xSync */
  intarrayCommit,              /* xCommit */
  intarrayRollback,            /* xRollback */
  0,                           /* xFindMethod */
  0,                           /* xRename */
  0, 0, 0, 0
};

#endif /* !defined(SQLITE_OMIT_VIRTUALTABLE) */

static void sqlite3_module_free(void *v) {
  sqlite3_intarray_module *module = (sqlite3_intarray_module *)v;
  intarrayMapDestroy(&module->arrayMap);
  sqlite3_free(v);
}

int sqlite3_intarray_register(sqlite3 *db, sqlite3_intarray_module **ppReturn) {
  int rc = SQLITE_OK;
#ifndef SQLITE_OMIT_VIRTUALTABLE
  sqlite3_intarray_module *p;
  p = (sqlite3_intarray_module*)sqlite3_malloc(sizeof(*p));
  if (!p) return SQLITE_NOMEM;
  p->db = db;
  rc = intarrayMapInit(&p->arrayMap);
  if (rc != SQLITE_OK) return rc;
  rc = sqlite3_create_module_v2(db, MODULE_NAME, &intarrayModule, p, sqlite3_module_free);
  if (rc == SQLITE_OK) {
    *ppReturn = p;
  }
#endif
  return rc;
}



int sqlite3_intarray_create(sqlite3_intarray_module *module, char *zName, sqlite3_intarray **ppReturn) {
  int rc = SQLITE_OK;
#ifndef SQLITE_OMIT_VIRTUALTABLE
  sqlite3_intarray *p;
  p = (sqlite3_intarray*)sqlite3_malloc(sizeof(*p));
  if (!p) {
    sqlite3_free(zName);
    return SQLITE_NOMEM;
  }
  memset(p, 0, sizeof(*p));
  p->module = module;
  p->zName = zName;
  rc = intarrayMapPut(&module->arrayMap, p);
  if (rc != SQLITE_OK) {
    sqlite3_free(zName);
    sqlite3_free(p);
    return rc;
  }
  p->commitState = sqlite3_get_autocommit(module->db) ? 1 : 0;
  rc = create_vtable(p);
  if (rc != SQLITE_OK) {
    intarrayMapRemove(&module->arrayMap, p);
    sqlite3_free(zName);
    sqlite3_free(p);
  } else {
    *ppReturn = p;
  }
#endif
  return rc;
}

int sqlite3_intarray_bind(sqlite3_intarray *pIntArray, int nElements, sqlite3_int64 *aElements, void (*xFree)(void*), int bOrdered, int bUnique, int ensureTableExists) {
  int rc = SQLITE_OK;
#ifndef SQLITE_OMIT_VIRTUALTABLE
  rc = drop_array_content(pIntArray);
  if (rc != SQLITE_OK) return rc;
  if (ensureTableExists && (pIntArray->commitState < 0 || pIntArray->connectCount <= 0)) {
    rc = create_vtable(pIntArray);
    if (rc == SQLITE_OK) {
      pIntArray->connectCount = 1;
      pIntArray->commitState = sqlite3_get_autocommit(pIntArray->module->db) ? 1 : 0;
    }
    /* ignore rc (duplicate table exists); todo: discern other errors */
    /*if (rc != SQLITE_OK) return rc;*/
    rc = SQLITE_OK;
  }
  pIntArray->n = nElements;
  pIntArray->a = aElements;
  pIntArray->xFree = xFree;
  pIntArray->ordered = bOrdered;
  pIntArray->unique = bUnique;
#endif
  return rc;
}

int sqlite3_intarray_destroy(sqlite3_intarray *a) {
  int rc = SQLITE_OK;
#ifndef SQLITE_OMIT_VIRTUALTABLE
  rc = drop_array_content(a);
  if (rc != SQLITE_OK) return rc;
  rc = drop_vtable(a);
  if (rc != SQLITE_OK) return rc;
  /* if (a->connectCount) return INTARRAY_INUSE;  *//* not reliable */
  intarrayMapRemove(&a->module->arrayMap, a);
  sqlite3_free(a->zName);
  sqlite3_free(a);
#endif
  return rc;
}



