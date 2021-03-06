
LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_CFLAGS += -DSQLITE_DEFAULT_AUTOVACUUM=1
LOCAL_CFLAGS += -DSQLITE_DEFAULT_FILE_PERMISSIONS=0600
LOCAL_CFLAGS += -DSQLITE_DEFAULT_MEMSTATUS=0
LOCAL_CFLAGS += -DSQLITE_DEFAULT_SYNCHRONOUS=1
LOCAL_CFLAGS += -DSQLITE_HAVE_ISNAN
LOCAL_CFLAGS += -DSQLITE_LIKE_DOESNT_MATCH_BLOBS
LOCAL_CFLAGS += -DSQLITE_MAX_EXPR_DEPTH=0
LOCAL_CFLAGS += -DSQLITE_OMIT_DEPRECATED
LOCAL_CFLAGS += -DSQLITE_OMIT_GET_TABLE
LOCAL_CFLAGS += -DSQLITE_OMIT_LOAD_EXTENSION
LOCAL_CFLAGS += -DSQLITE_OMIT_SHARED_CACHE
LOCAL_CFLAGS += -DSQLITE_SECURE_DELETE
LOCAL_CFLAGS += -DSQLITE_THREADSAFE=1
LOCAL_CFLAGS += -DSQLITE_UNTESTABLE
LOCAL_CFLAGS += -DSQLITE_USE_ALLOCA
LOCAL_CFLAGS += -DSQLITE_USE_URI=1

# If using SEE, uncomment the following:
# LOCAL_CFLAGS += -DSQLITE_HAS_CODEC

#Define HAVE_USLEEP, otherwise ALL sleep() calls take at least 1000ms
LOCAL_CFLAGS += -DHAVE_USLEEP=1

# Enable SQLite extensions.
#LOCAL_CFLAGS += -DSQLITE_ENABLE_FTS5
#LOCAL_CFLAGS += -DSQLITE_ENABLE_RTREE
#LOCAL_CFLAGS += -DSQLITE_ENABLE_JSON1
LOCAL_CFLAGS += -DSQLITE_ENABLE_FTS4
LOCAL_CFLAGS += -DSQLITE_ENABLE_BATCH_ATOMIC_WRITE
LOCAL_CFLAGS += -DSQLITE_ENABLE_COLUMN_METADATA

# This is important - it causes SQLite to use memory for temp files. Since
# Android has no globally writable temp directory, if this is not defined the
# application throws an exception when it tries to create a temp file.
#
LOCAL_CFLAGS += -DSQLITE_TEMP_STORE=3

LOCAL_CFLAGS += -DHAVE_CONFIG_H -DKHTML_NO_EXCEPTIONS -DGKWQ_NO_JAVA
LOCAL_CFLAGS += -DNO_SUPPORT_JS_BINDING -DQT_NO_WHEELEVENT -DKHTML_NO_XBL
LOCAL_CFLAGS += -U__APPLE__
LOCAL_CFLAGS += -DHAVE_STRCHRNUL=0
LOCAL_CFLAGS += -Wno-unused-parameter -Wno-int-to-pointer-cast
LOCAL_CFLAGS += -Wno-uninitialized -Wno-parentheses
LOCAL_CPPFLAGS += -Wno-conversion-null


ifeq ($(TARGET_ARCH), arm)
	LOCAL_CFLAGS += -DPACKED="__attribute__ ((packed))"
else
	LOCAL_CFLAGS += -DPACKED=""
endif

LOCAL_SRC_FILES:= \
	android_database_SQLiteCommon.cpp \
	android_database_SQLiteConnection.cpp \
	android_database_SQLiteGlobal.cpp \
	android_database_SQLiteDebug.cpp \
	JNIHelp.cpp \
	JniConstants.cpp \

LOCAL_SRC_FILES += sqlite3.c

LOCAL_C_INCLUDES += $(LOCAL_PATH) $(LOCAL_PATH)/nativehelper/

LOCAL_MODULE:= libsqliteX
LOCAL_LDLIBS += -ldl -llog -landroid

include $(BUILD_SHARED_LIBRARY)

