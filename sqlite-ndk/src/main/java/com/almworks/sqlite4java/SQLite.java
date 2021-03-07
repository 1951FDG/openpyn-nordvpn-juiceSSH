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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.*;

import static com.almworks.sqlite4java.SQLiteConstants.WRAPPER_CANNOT_LOAD_LIBRARY;

/**
 * SQLite class has several utility methods that are applicable to the whole instance of
 * SQLite library within the current process. It is not needed for basic operations.
 * <p/>
 * All methods in this class are <strong>thread-safe</strong>.
 *
 * @author Igor Sereda
 */
public final class SQLite {
  /**
   * System property that, if set, defines where to look for native sqlite4java libraries.
   */
  public static final String LIBRARY_PATH_PROPERTY = "sqlite4java.library.path";

  private static boolean debugBinaryPreferred = "true".equalsIgnoreCase(System.getProperty("sqlite4java.debug.binary.preferred"));
  private static boolean libraryLoaded = false;
  private static String jarVersion = null;
  private static Boolean threadSafe = null;

  /**
   * Native sqlite4java code, including SQLite itself, is compiled in <code>DEBUG</code> and <code>RELEASE</code>
   * configurations. Binaries compiled with debug configurations have <code>-d</code> suffix, and can be placed in
   * the same directory with release binaries.
   * <p/>
   * sqlite4java will load any available binary that suits the platform, but in case both RELEASE and DEBUG
   * binaries are available, it will load RELEASE binary by default.
   * <p/>
   * You can use this method to change the preference and load DEBUG binary by default.
   * <p/>
   * This method must be called before the first call to {@link SQLiteConnection#open} or any other methods that
   * require library loading.
   * <p/>
   * You can also change the default by setting <strong>sqlite4java.debug.binary.preferred</strong> system property to
   * <strong>true</strong>.
   *
   * @param debug if true, choose DEBUG binary in case both DEBUG and RELEASE binaries are available
   */
  public static synchronized void setDebugBinaryPreferred(boolean debug) {
    if (libraryLoaded) {
      Internal.logWarn(SQLite.class, "cannot set library preference, library already loaded");
      return;
    }
    debugBinaryPreferred = debug;
  }

  /**
   * Used to check whether DEBUG binary is preferred over RELEASE binary when SQLite native code is loaded.
   *
   * @return true if DEBUG library is preferred
   * @see #setDebugBinaryPreferred(boolean)
   */
  public static synchronized boolean isDebugBinaryPreferred() {
    return debugBinaryPreferred;
  }

  /**
   * Tries to load the native library. If unsuccessful, throws an exception, otherwise just exits.
   * <p/>
   * If native library is already loaded, just exits.
   *
   * @throws SQLiteException if library loading fails
   */
  public static synchronized void loadLibrary() throws SQLiteException {
    if (!libraryLoaded) {
      Throwable t = Internal.loadLibraryX();
      if (t != null)
        throw new SQLiteException(WRAPPER_CANNOT_LOAD_LIBRARY, "cannot load library: " + t, t);
      libraryLoaded = true;
      int threadSafe = _SQLiteSwigged.sqlite3_threadsafe();
      if (threadSafe == 0) {
        Internal.logWarn(SQLite.class, "library is not thread-safe");
      }
    }
  }

  /**
   * Gets the version of SQLite database.
   *
   * @return SQLite version
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/libversion.html">sqlite3_libversion</a>
   */
  public static String getSQLiteVersion() throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_libversion();
  }

  /**
   * Gets the compile-time options used to compile the used library.
   *
   * @return a string with all compile-time options delimited by a space
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/compileoption_get.html">sqlite3_compileoption_get</a>
   */
  public static String getSQLiteCompileOptions() throws SQLiteException {
    loadLibrary();
    StringBuilder b = new StringBuilder();
    int i = 0;
    while (true) {
      String option = _SQLiteSwigged.sqlite3_compileoption_get(i++);
      if (option == null || option.length() == 0) break;
      if (b.length() > 0) b.append(' ');
      b.append(option);
    }
    return b.toString();
  }

  /**
   * Gets the numeric representation of the SQLite version.
   *
   * @return a number representing the version; example: version "3.6.23.1" is represented as 3006023.
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/libversion.html">sqlite3_version</a>
   */
  public static int getSQLiteVersionNumber() throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_libversion_number();
  }

  /**
   * Checks if SQLite has been compiled with the THREADSAFE option.
   * <p/>
   *
   * @return true if SQLite has been compiled with THREADSAFE option
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/threadsafe.html">sqlite3_threadsafe</a>
   */
  public static boolean isThreadSafe() throws SQLiteException {
    Boolean cachedResult = threadSafe;
    if (cachedResult != null) return cachedResult;
    loadLibrary();
    boolean r = _SQLiteSwigged.sqlite3_threadsafe() != 0;
    threadSafe = r;
    return r;
  }

  /**
   * Checks if the given SQL is complete.
   *
   * @param sql the SQL
   * @return true if sql is a complete statement
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/complete.html">sqlite3_complete</a>
   */
  public static boolean isComplete(String sql) throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_complete(sql) != 0;
  }

  /**
   * Gets the amount of memory currently used by SQLite library. The returned value shows the amount of non-heap
   * "native" memory taken up by SQLite caches and anything else allocated with sqlite3_malloc.
   * <p/>
   * This value does not include any heap or other JVM-allocated memory taken up by sqlite4java objects and classes.
   *
   * @return the number of bytes used by SQLite library in this process (for all connections)
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/memory_highwater.html">sqlite3_memory_used</a>
   */
  public static long getMemoryUsed() throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_memory_used();
  }

  /**
   * Returns the maximum amount of memory that was used by SQLite since the last time the highwater
   * was reset.
   *
   * @param reset if true, the highwatermark is reset after this call
   * @return the maximum number of bytes ever used by SQLite library since the start of the application
   *         or the last reset of the highwatermark.
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/memory_highwater.html">sqlite3_memory_highwater</a>
   */
  public static long getMemoryHighwater(boolean reset) throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_memory_highwater(reset ? 1 : 0);
  }

  /**
   * Requests SQLite to try to release some memory from its heap. This could be called to clear cache.
   *
   * @param bytes the number of bytes requested to be released
   * @return the number of bytes actually released
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/release_memory.html">sqlite3_release_memory</a>
   */
  public static int releaseMemory(int bytes) throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_release_memory(bytes);
  }

  /**
   * Sets the "soft limit" on the amount of memory allocated before SQLite starts trying to free some
   * memory before allocating more memory.
   *
   * @param limit the number of bytes to set the soft memory limit to
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/soft_heap_limit.html">sqlite3_soft_heap_limit</a>
   */
  public static void setSoftHeapLimit(int limit) throws SQLiteException {
    loadLibrary();
    _SQLiteSwigged.sqlite3_soft_heap_limit64(limit);
  }

  /**
   * Sets the "soft limit" on the amount of memory allocated before SQLite starts trying to free some
   * memory before allocating more memory.
   *
   * @param limit the number of bytes to set the soft memory limit to
   * @return size of the soft heap limit prior to the call
   * @throws SQLiteException if native library cannot be loaded
   * @see <a href="http://www.sqlite.org/c3ref/soft_heap_limit64.html">sqlite3_soft_heap_limit64</a>
   */
  public static long softHeapLimit(long limit) throws SQLiteException {
    loadLibrary();
    return _SQLiteSwigged.sqlite3_soft_heap_limit64(limit);
  }

  /**
   * Sets whether <a href="http://www.sqlite.org/sharedcache.html">shared cache mode</a> will be used
   * for the connections that are opened after this call. All existing connections are not affected.
   * <p/>
   * <strong>sqlite4java</strong> explicitly disables shared cache on start. This is also the default declared by SQLite,
   * but it may change in the future, so <strong>sqlite4java</strong> enforces consistency.
   *
   * @param enabled if true, the following calls to {@link SQLiteConnection#open} will used shared-cache mode
   * @throws SQLiteException if native library cannot be loaded, or if call returns an error
   * @see <a href="http://www.sqlite.org/c3ref/enable_shared_cache.html">sqlite3_enable_shared_cache</a>
   */
  public static void setSharedCache(boolean enabled) throws SQLiteException {
    loadLibrary();
    int rc = _SQLiteSwigged.sqlite3_enable_shared_cache(enabled ? 1 : 0);
    if (rc != SQLiteConstants.SQLITE_OK)
      throw new SQLiteException(rc, "SQLite: cannot set shared_cache to " + enabled);
  }

  /**
   * <strong>Only for Windows.</strong> Set the value associated with the
   * <a href="https://www.sqlite.org/c3ref/temp_directory.html">sqlite3_temp_directory</a> or
   * <a href="https://www.sqlite.org/c3ref/data_directory.html">sqlite3_data_directory</a> variables to
   * a path depending on the value of the directoryType parameter.
   *
   * @param directoryType Indicator of which variable to set. Can either be SQLITE_WIN32_DATA_DIRECTORY_TYPE
   * or SQLITE_WIN32_TEMP_DIRECTORY_TYPE.
   * @param path The directory name to set one of the two variables to. If path is null, then the previous
   * value will be freed from memory and no longer effective.
   * @throws SQLiteException If native library cannot be loaded, or if call returns an error.
   * @see <a href="https://www.sqlite.org/c3ref/win32_set_directory.html">sqlite3_win32_set_directory</a>
   */
  public static void setDirectory(int directoryType, String path) throws SQLiteException {
    assert Internal.isWindows();
    loadLibrary();
    int rc = _SQLiteManualJNI.sqlite3_win32_set_directory(directoryType, path);
    if (rc != SQLiteConstants.SQLITE_OK) {
      String errorMessage;
      if (rc == SQLiteConstants.SQLITE_NOMEM) {
        errorMessage = "Memory could not be allocated";
      } else {
        errorMessage = "Error attempting to set win32 directory";
      }
      throw new SQLiteException(rc, errorMessage);
    }
  }

  /**
   * Gets the version of sqlite4java library. The library version equals to the svn version of the sources
   * it's built from (trunk code only). If the version ends in '+', then the library has been built from
   * dirty (uncommitted) sources.
   * <p/>
   * The version string is read from the sqlite4java.jar's manifest.
   *
   * @return <strong>sqlite4java</strong> version, or null if version cannot be read
   */
  public static synchronized String getLibraryVersion() {
    if (jarVersion == null) {
      String name = SQLite.class.getName().replace('.', '/') + ".class";
      URL url = SQLite.class.getClassLoader().getResource(name);
      if (url == null)
        return null;
      String s = url.toString();
      if (!s.startsWith("jar:"))
        return null;
      int k = s.lastIndexOf('!');
      if (k < 0)
        return null;
      s = s.substring(0, k + 1) + "/META-INF/MANIFEST.MF";
      InputStream input = null;
      Manifest manifest = null;
      try {
        input = new URL(s).openStream();
        manifest = new Manifest(input);
      } catch (IOException e) {
        Internal.logWarn(SQLite.class, "error reading jar manifest" + e);
      } finally {
        try {
          if (input != null) input.close();
        } catch (IOException e) { /**/}
      }
      if (manifest != null) {
        Attributes attr = manifest.getMainAttributes();
        jarVersion = attr.getValue("Implementation-Version");
      }
    }
    if (jarVersion == null) {
      Internal.logWarn(SQLite.class, "unknown jar version");
    }
    return jarVersion;
  }

  /**
   * Sets the path where sqlite4java should look for the native library, by modifying <tt>sqlite4java.library.path</tt>
   * system property.
   * <p/>
   * By default, sqlite4java looks for native libraries in the same directory where sqlite4java.jar is located,
   * and it also tries to use {@link java.lang.System#loadLibrary} method, which uses <tt>java.library.path</tt> system
   * property.
   * <p/>
   * Use this method (or explicitly set <tt>sqlite4java.library.path</tt>) system property when the native library
   * is located in the non-default location, and changing <tt>java.library.path</tt> may not have the desired effect.
   * <p/>
   * When <tt>sqlite4java.library.path</tt> property is set, the library will be loaded only from that directory.
   * Default directories will not be tried.
   * <p/>
   * Calling this method when native library has been already loaded has no effect.
   * <p/>
   * This method is thread-safe.
   *
   * @param path local directory that sqlite4java should use to load native libraries from, or null to disable the setting
   */
  public static synchronized void setLibraryPath(String path) {
    if (libraryLoaded) {
      Internal.logWarn(SQLite.class, "cannot set library path, library already loaded");
      return;
    }
    System.setProperty(LIBRARY_PATH_PROPERTY, path);
  }

  private SQLite() {
  }

  /**
   * Main method is called when you run <code>java -jar sqlite4java.jar</code>, and it prints out
   * the version of the library, the version of the SQLite and the compile-time options the binaries were
   * built with
   *
   * @param args command line
   */
  public static void main(String[] args) {
    if (args.length > 0 && "-d".equals(args[0])) {
      // debug
      Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.FINE);
      Handler[] handlers = Logger.getLogger("").getHandlers();
      for (Handler handler : handlers) {
        if (handler instanceof ConsoleHandler) {
          handler.setLevel(Level.FINE);
          handler.setFormatter(new NiceFormatter());
        }
      }
    } else {
      Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.SEVERE);
    }
    String v = getLibraryVersion();
    if (v == null) v = "(UNKNOWN VERSION)";
    System.out.println("sqlite4java " + v);
    Throwable t = libraryLoaded ? null : Internal.loadLibraryX();
    if (t != null) {
      System.out.println("Error: cannot load SQLite");
      t.printStackTrace();
    } else {
      try {
        System.out.println("SQLite " + getSQLiteVersion());
        System.out.println("Compile-time options: " + getSQLiteCompileOptions());
      } catch (SQLiteException e) {
        e.printStackTrace();
      }
    }
  }

  private static class NiceFormatter extends Formatter {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyMMdd:HHmmss.SSS", Locale.US);
    private static final String LINE_SEPARATOR;
    static {
      String s = System.getProperty("line.separator");
      if (s == null) s = "\n";
      LINE_SEPARATOR = s;
    }
    
    @Override
    public String format(LogRecord record) {
      if (record == null) return "";
      StringBuilder r = new StringBuilder();
      r.append(DATE_FORMAT.format(record.getMillis())).append(' ');
      Level level = record.getLevel();
      if (level == null) level = Level.ALL;
      r.append(level.getName()).append(' ');
      r.append(record.getMessage());
      r.append(LINE_SEPARATOR);
      return r.toString();
    }
  }
}
