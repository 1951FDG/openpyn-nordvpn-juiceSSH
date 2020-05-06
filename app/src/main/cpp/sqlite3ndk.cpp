/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Krystian Bigaj code.
 *
 * The Initial Developer of the Original Code is Krystian Bigaj.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Krystian Bigaj <krystian.bigaj@gmail.com>
 *   1951FDG <1951FDG@users.noreply.github.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

#include "sqlite3ndk.h"
#include "../jni/sqlite/sqlite3.h"

#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <cassert>
#include <dlfcn.h>
#include <jni.h>
#include <cstring>

#ifndef SQLITE_DEFAULT_SECTOR_SIZE
# define SQLITE_DEFAULT_SECTOR_SIZE 4096
#endif

// Uncomment the line below to enable logging
//# define TAG "sqlite3ndk"

#ifdef TAG
#define VERBOSE(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define INFO(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define WARN(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define ERROR(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#else
#define VERBOSE(...) // NOP
#define DEBUG(...) // NOP
#define INFO(...) // NOP
#define WARN(...) // NOP
#define ERROR(...) // NOP
#endif

/**
 * The ndk_vfs structure is subclass of sqlite3_vfs specific
 * to the Android NDK AAssetManager VFS implementations
 */
typedef struct ndk_vfs ndk_vfs;
struct ndk_vfs
{
	sqlite3_vfs vfs; /*** Must be first ***/
	sqlite3_vfs *vfsDefault;
	const struct sqlite3_io_methods *pMethods;

	AAssetManager *mgr;
};

/**
 * The ndk_file structure is subclass of sqlite3_file specific
 * to the Android NDK AAsset VFS implementations
 */
typedef struct ndk_file ndk_file;
struct ndk_file
{
	const sqlite3_io_methods *pMethod; /*** Must be first ***/

	// Pointer to AAsset obtained by AAssetManager_open
	AAsset *asset;

	// Pointer to database content (AAsset_getBuffer)
	const void *buf;

	// Total length of database file (AAsset_getLength)
	off_t len;
};

/*
 * sqlite3_vfs.xOpen - open database file.
 * Implemented using AAssetManager_open
 */
static int ndkOpen(sqlite3_vfs *pVfs,
	const char *zPath,
	sqlite3_file *pFile,
	int flags,
	int *pOutFlags)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);
	auto *file = reinterpret_cast<ndk_file *>(pFile);

	// pMethod must be set to NULL, even if xOpen call fails.
	//
	// http://www.sqlite.org/c3ref/io_methods.html
	// "The only way to prevent a call to xClose following a failed
	// sqlite3_vfs.xOpen is for the sqlite3_vfs.xOpen to set the
	// sqlite3_file.pMethods element to NULL."
	file->pMethod = nullptr;

	// Allow only for opening main database file as read-only.
	// Opening JOURNAL/TEMP/WAL/etc. files will make call fails.
	// We don't need it, as DB opened from 'assets' .apk cannot be modified
	if (!zPath ||
		(flags & SQLITE_OPEN_DELETEONCLOSE) ||
		!(flags & SQLITE_OPEN_READONLY) ||
		(flags & SQLITE_OPEN_READWRITE) ||
		(flags & SQLITE_OPEN_CREATE) ||
		!(flags & SQLITE_OPEN_MAIN_DB)
		)
	{
		return SQLITE_PERM;
	}

	// Try top open database file
	AAsset *asset = AAssetManager_open(ndk->mgr, zPath, AASSET_MODE_RANDOM);
	if (!asset)
	{
		return SQLITE_CANTOPEN;
	}

	// Get pointer to database.
	// This call can fail in case for example out of memory.
	// If file inside .apk is compressed, then whole file must be allocated and
	// read into memory.
	// If file is not compressed (inside .apk/zip), then this functions returns
	// pointer to memory-mapped address in .apk file, so doesn't need to
	// allocate explicit additional memory.
	// As for today there is no simple way to set if specific file must be
	// compressed or not.
	// You can control it only by file extension.
	// Google for: android kNoCompressExt
	const void *buf = AAsset_getBuffer(asset);
	if (!buf)
	{
		AAsset_close(asset);
		return SQLITE_ERROR;
	}

	file->pMethod = ndk->pMethods;
	file->asset = asset;
	file->buf = buf;
	file->len = AAsset_getLength(asset);
	if (pOutFlags)
	{
		*pOutFlags = flags;
	}

	return SQLITE_OK;
}

/*
 * sqlite3_vfs.xDelete - not implemented. Assets in .apk are read only
 */
static int ndkDelete(sqlite3_vfs *, const char *, int)
{
	return SQLITE_ERROR;
}

/*
 * sqlite3_vfs.xAccess - tests if file exists and/or can be read.
 * Implemented using AAssetManager_open
 */
static int ndkAccess(sqlite3_vfs *pVfs,
	const char *zPath,
	int flags,
	int *pResOut)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);

	*pResOut = 0;

	switch (flags)
	{
		case SQLITE_ACCESS_EXISTS:
		case SQLITE_ACCESS_READ:
		{
			AAsset *asset =
				AAssetManager_open(ndk->mgr, zPath, AASSET_MODE_RANDOM);
			if (asset)
			{
				AAsset_close(asset);
				*pResOut = 1;
			}
		}
			break;
		default:
			break;
	}

	return SQLITE_OK;
}

/*
 * sqlite3_vfs.xFullPathname - all paths are root paths to 'assets' directory,
 * so just return copy of input path
 */
static int ndkFullPathname(sqlite3_vfs *pVfs,
	const char *zPath,
	int nOut,
	char *zOut)
{
	if (!zPath)
	{
		return SQLITE_ERROR;
	}

	int pos = 0;
	while ((pos < nOut) && zPath[pos])
	{
		zOut[pos] = zPath[pos];
		++pos;
	}
	if (pos >= nOut)
	{
		return SQLITE_ERROR;
	}
	zOut[pos] = '\0';

	return SQLITE_OK;
}

/*
 * sqlite3_vfs.xRandomness - call redirected to default VFS.
 * See: sqlite3_ndk_init(..., ..., ..., osVfs)
 */
static int ndkRandomness(sqlite3_vfs *pVfs, int nBuf, char *zBuf)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);

	return ndk->vfsDefault->xRandomness(ndk->vfsDefault, nBuf, zBuf);
}

/*
 * sqlite3_vfs.xSleep - call redirected to default VFS.
 * See: sqlite3_ndk_init(..., ..., ..., osVfs)
 */
static int ndkSleep(sqlite3_vfs *pVfs, int microseconds)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);

	return ndk->vfsDefault->xSleep(ndk->vfsDefault, microseconds);
}

/*
 * sqlite3_vfs.xCurrentTime - call redirected to default VFS.
 * See: sqlite3_ndk_init(..., ..., ..., osVfs)
 */
static int ndkCurrentTime(sqlite3_vfs *pVfs, double *prNow)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);

	return ndk->vfsDefault->xCurrentTime(ndk->vfsDefault, prNow);
}

/*
 * sqlite3_vfs.xGetLastError - not implemented (no additional information)
 */
static int ndkGetLastError(sqlite3_vfs *, int, char *)
{
	return 0;
}

/*
 * sqlite3_vfs.xCurrentTimeInt64 - call redirected to default VFS.
 * See: sqlite3_ndk_init(..., ..., ..., osVfs)
 */
static int ndkCurrentTimeInt64(sqlite3_vfs *pVfs, sqlite3_int64 *piNow)
{
	const ndk_vfs *ndk = reinterpret_cast<ndk_vfs *>(pVfs);

	return ndk->vfsDefault->xCurrentTimeInt64(ndk->vfsDefault, piNow);
}

/*
 * sqlite3_file.xClose - closing file opened in sqlite3_vfs.xOpen function.
 * Implemented using AAsset_close
 */
static int ndkFileClose(sqlite3_file *pFile)
{
	auto *file = reinterpret_cast<ndk_file *>(pFile);

	if (file->asset)
	{
		AAsset_close(file->asset);
		file->asset = nullptr;
		file->buf = nullptr;
		file->len = 0;
	}
	return SQLITE_OK;
}

/*
 * sqlite3_file.xRead - database read from asset memory.
 * See: AAsset_getBuffer in ndkOpen
 */
static int ndkFileRead(sqlite3_file *pFile,
	void *pBuf,
	int amt,
	sqlite3_int64 offset)
{
	const ndk_file *file = reinterpret_cast<ndk_file *>(pFile);
	int got;
	int off;
	int rc;

	off = (int) offset;

	// Sanity check
	if (file->asset == nullptr)
	{
		return SQLITE_IOERR_READ;
	}

	if (off + amt <= file->len)
	{
		got = amt;
		rc = SQLITE_OK;
	}
	else
	{
		got = file->len - off;
		if (got < 0)
		{
			rc = SQLITE_IOERR_READ;
		}
		else
		{
			// http://www.sqlite.org/c3ref/io_methods.html
			// "If xRead() returns SQLITE_IOERR_SHORT_READ it must also fill in
			// the unread portions of the buffer with zeros.
			// A VFS that fails to zero-fill short reads might seem to work.
			// However, failure to zero-fill short reads will eventually lead to
			// database corruption."
			//
			// It might be not a problem in read-only databases, but do it as
			// documentation says
			rc = SQLITE_IOERR_SHORT_READ;
			memset(&((char *) pBuf)[got], 0, amt - got);
		}
	}

	if (got > 0)
	{
		memcpy(pBuf, (char *) file->buf + off, got);
	}

	return rc;
}

/*
 * sqlite3_file.xWrite - not implemented (.apk is read-only)
 */
static int ndkFileWrite(sqlite3_file *, const void *, int, sqlite3_int64)
{
	return SQLITE_IOERR_WRITE;
}

/*
 * sqlite3_file.xTruncate - not implemented (.apk is read-only)
 */
static int ndkFileTruncate(sqlite3_file *, sqlite3_int64)
{
	return SQLITE_IOERR_TRUNCATE;
}

/*
 * sqlite3_file.xSync - not implemented (.apk is read-only)
 */
static int ndkFileSync(sqlite3_file *, int)
{
	return SQLITE_IOERR_FSYNC;
}

/*
 * sqlite3_file.xFileSize - get database file size.
 * See: AAsset_getLength in ndkOpen
 */
static int ndkFileSize(sqlite3_file *pFile, sqlite3_int64 *pSize)
{
	auto *file = reinterpret_cast<ndk_file *>(pFile);
	*pSize = file->len;

	return SQLITE_OK;
}

/*
 * sqlite3_file.xLock - not implemented (.apk is read-only)
 */
static int ndkFileLock(sqlite3_file *, int)
{
	return SQLITE_OK;
}

/*
 * sqlite3_file.xUnlock - not implemented (.apk is read-only)
 */
static int ndkFileUnlock(sqlite3_file *, int)
{
	return SQLITE_OK;
}

/*
 * sqlite3_file.xCheckReservedLock - not implemented (.apk is read-only)
 */
static int ndkFileCheckReservedLock(sqlite3_file *, int *pResOut)
{
	*pResOut = 0;

	return SQLITE_OK;
}

/*
 * sqlite3_file.xFileControl - not implemented (no special codes needed for now)
 */
static int ndkFileControl(sqlite3_file *, int, void *)
{
	return SQLITE_NOTFOUND;
}

/*
 * sqlite3_file.xSectorSize - use same value as in sqlite3.c
 */
static int ndkFileSectorSize(sqlite3_file *)
{
	return SQLITE_DEFAULT_SECTOR_SIZE;
}

/*
 * sqlite3_file.xDeviceCharacteristics - not implemented (.apk is read-only)
 */
static int ndkFileDeviceCharacteristics(sqlite3_file *)
{
	return 0;
}

/*
 * Register into SQLite. For more information see sqlite3ndk.h
 */
int sqlite3_ndk_init(AAssetManager *assetMgr,
	const char *vfsName,
	int makeDflt,
	const char *osVfs)
{
	static ndk_vfs ndkVfs;
	int rc;

	// assetMgr is required parameter
	if (!assetMgr)
	{
		return SQLITE_ERROR;
	}

	// Check if there was successful call to sqlite3_ndk_init before
	if (ndkVfs.mgr)
	{
		if (ndkVfs.mgr == assetMgr)
		{
			return SQLITE_OK;
		}
		else
		{
			// Second call to sqlite3_ndk_init cannot change assetMgr
			return SQLITE_ERROR;
		}
	}

	// Find os VFS.
	// Used to redirect xRandomness, xSleep, xCurrentTime, ndkCurrentTimeInt64
	// calls
	ndkVfs.vfsDefault = sqlite3_vfs_find(osVfs);
	if (ndkVfs.vfsDefault == nullptr)
	{
		return SQLITE_ERROR;
	}

	// vfsFile
	static const sqlite3_io_methods ndkFileMethods =
		{
			1,
			ndkFileClose,
			ndkFileRead,
			ndkFileWrite,
			ndkFileTruncate,
			ndkFileSync,
			ndkFileSize,
			ndkFileLock,
			ndkFileUnlock,
			ndkFileCheckReservedLock,
			ndkFileControl,
			ndkFileSectorSize,
			ndkFileDeviceCharacteristics
		};

	// pMethods will be used in ndkOpen
	ndkVfs.pMethods = &ndkFileMethods;

	// vfs
	ndkVfs.vfs.iVersion = 3;
	ndkVfs.vfs.szOsFile = sizeof(ndk_file);
	ndkVfs.vfs.mxPathname = SQLITE_NDK_VFS_MAX_PATH;
	ndkVfs.vfs.pNext = nullptr;
	if (vfsName)
	{
		ndkVfs.vfs.zName = vfsName;
	}
	else
	{
		ndkVfs.vfs.zName = SQLITE_NDK_VFS_NAME;
	}
	ndkVfs.vfs.pAppData = nullptr;
	ndkVfs.vfs.xOpen = ndkOpen;
	ndkVfs.vfs.xDelete = ndkDelete;
	ndkVfs.vfs.xAccess = ndkAccess;
	ndkVfs.vfs.xFullPathname = ndkFullPathname;
	ndkVfs.vfs.xDlOpen = nullptr;
	ndkVfs.vfs.xDlError = nullptr;
	ndkVfs.vfs.xDlSym = nullptr;
	ndkVfs.vfs.xDlClose = nullptr;
	ndkVfs.vfs.xRandomness = ndkRandomness;
	ndkVfs.vfs.xSleep = ndkSleep;
	ndkVfs.vfs.xCurrentTime = ndkCurrentTime;
	ndkVfs.vfs.xGetLastError = ndkGetLastError;
	ndkVfs.vfs.xCurrentTimeInt64 = ndkCurrentTimeInt64;
	ndkVfs.vfs.xSetSystemCall = nullptr;
	ndkVfs.vfs.xGetSystemCall = nullptr;
	ndkVfs.vfs.xNextSystemCall = nullptr;

	// Asset manager
	ndkVfs.mgr = assetMgr;

	// Last part, try to register VFS
	rc = sqlite3_vfs_register(&ndkVfs.vfs, makeDflt);

	if (rc != SQLITE_OK)
	{
		// sqlite3_vfs_register could fail in case of sqlite3_initialize failure
		ndkVfs.mgr = nullptr;
	}

	return rc;
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;
	if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK)
	{
		return JNI_ERR;
	}

	/*int rc;
	if ((rc = sqlite3_config(SQLITE_CONFIG_SINGLETHREAD)) != SQLITE_OK)
	{
		ERROR("Can not config sqlite3 threads: sqlite3_config returned %d.",
			  rc);
	}
	else
	{
		VERBOSE("SUCCESS: sqlite3_config returned %d.", rc);
	}*/

	VERBOSE("found sqlite library version %s thread safe %d",
		sqlite3_libversion(), sqlite3_threadsafe());

	jclass cActivityThread = env->FindClass("android/app/ActivityThread");
	if (cActivityThread == JNI_FALSE)
	{
		return JNI_ERR;
	}

	jmethodID mCurrentApplication = env->GetStaticMethodID(
		cActivityThread,
		"currentApplication",
		"()Landroid/app/Application;");
	if (mCurrentApplication == JNI_FALSE)
	{
		return JNI_ERR;
	}

	jobject gApplication = env->CallStaticObjectMethod(
		cActivityThread,
		mCurrentApplication);

	jmethodID mGetAssets = env->GetMethodID(
		env->GetObjectClass(gApplication),
		"getAssets",
		"()Landroid/content/res/AssetManager;");
	if (mGetAssets == JNI_FALSE)
	{
		return JNI_ERR;
	}

	jobject gAssetManager = env->CallObjectMethod(
		gApplication,
		mGetAssets);

	jobject assetManager = env->NewGlobalRef(gAssetManager);

	AAssetManager *assetMgr = AAssetManager_fromJava(env, assetManager);
	assert(assetMgr != nullptr);

	VERBOSE("enabling sqlite3ndk");
	if (sqlite3_ndk_init(
		assetMgr,
		SQLITE_NDK_VFS_NAME,
		SQLITE_NDK_VFS_MAKE_DEFAULT,
		SQLITE_NDK_VFS_PARENT_VFS) != SQLITE_OK)
	{
		ERROR("sqlite3_ndk_init failed!");
		return JNI_ERR;
	}

	VERBOSE("sqlite3_ndk_init OK");

	// release
	env->DeleteLocalRef(cActivityThread);
	env->DeleteLocalRef(gApplication);
	env->DeleteLocalRef(gAssetManager);

	return JNI_VERSION_1_6;
}
