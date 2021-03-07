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

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

final class Internal {
  private static final Logger logger = Logger.getLogger("com.almworks.sqlite4java");
  private static final String LOG_PREFIX = "[sqlite] ";

  private static final String BASE_LIBRARY_NAME = "sqlite4java";
  private static final String[] DEBUG_SUFFIXES = {"-d", ""};
  private static final String[] RELEASE_SUFFIXES = {"", "-d"};

  private static final AtomicInteger lastConnectionNumber = new AtomicInteger(0);

  static int nextConnectionNumber() {
    return lastConnectionNumber.incrementAndGet();
  }

  static void recoverableError(Object source, String message, boolean throwAssertion) {
    logWarn(source, message);
    assert !throwAssertion : source + " " + message;
  }

  static void log(Level level, Object source, Object message, Throwable exception) {
    if (!logger.isLoggable(level)) {
      return;
    }
    StringBuilder builder = new StringBuilder(LOG_PREFIX);
    if (source != null) {
      if (source instanceof Class) {
        String className = ((Class) source).getName();
        builder.append(className.substring(className.lastIndexOf('.') + 1));
      } else {
        builder.append(source);
      }
      builder.append(": ");
    }
    if (message != null)
      builder.append(message);
    logger.log(level, builder.toString(), exception);
  }

  static void logFine(Object source, Object message) {
    log(Level.FINE, source, message, null);
  }

  static void logInfo(Object source, Object message) {
    log(Level.INFO, source, message, null);
  }

  static void logWarn(Object source, Object message) {
    log(Level.WARNING, source, message, null);
  }

  static boolean isFineLogging() {
    return logger.isLoggable(Level.FINE);
  }

  static Throwable loadLibraryX() {
    if (checkLoaded() == null)
      return null;
    if ("true".equalsIgnoreCase(System.getProperty("sqlite4java.debug"))) {
      logger.setLevel(Level.FINE);
    }
    String classUrl = getClassUrl();
    String defaultPath = getDefaultLibPath(classUrl);
    String versionSuffix = getVersionSuffix(classUrl);
    String forcedPath = getForcedPath();
    if (Internal.isFineLogging()) {
      logFine(Internal.class, "loading library");
      logFine(Internal.class, "java.library.path=" + System.getProperty("java.library.path"));
      logFine(Internal.class, "sqlite4java.library.path=" + System.getProperty("sqlite4java.library.path"));
      logFine(Internal.class, "cwd=" + new File(".").getAbsolutePath());
      logFine(Internal.class, "default path=" + (defaultPath == null ? "null " : new File(defaultPath).getAbsolutePath()));
      logFine(Internal.class, "forced path=" + (forcedPath == null ? "null " : new File(forcedPath).getAbsolutePath()));
    }
    String os = getOs();
    String arch = getArch(os);
    List<String> names = collectLibraryNames(versionSuffix, os, arch);
    Throwable[] failureReason = {null};
    boolean loaded = false;
    if (forcedPath != null) {
      // if forced path is specified with sqlite4java.library.path, load binaries only from there
      for (String name : names) {
        loaded = tryLoadFromPath(name, os, forcedPath, failureReason);
        if (loaded) break;
      }
    } else {
      if (defaultPath != null) {
        // if we found .JAR in non-system path, try load binaries from there first
        for (String name : names) {
          loaded = tryLoadFromPath(name, os, defaultPath, failureReason);
          if (loaded) break;
        }
      }
      if (!loaded) {
        // try load binaries from any system library directory
        for (String name : names) {
          loaded = tryLoadFromSystemPath(name, failureReason);
          if (loaded) break;
        }
      }
    }

    if (loaded) {
      String msg = getLibraryVersionMessage();
      Internal.logInfo(Internal.class, msg);
      return null;
    } else {
      Throwable t = failureReason[0];
      if (t == null) t = new SQLiteException(SQLiteConstants.WRAPPER_CANNOT_LOAD_LIBRARY, "sqlite4java cannot find native library");
      return t;
    }
  }

  private static List<String> collectLibraryNames(String versionSuffix, String os, String arch) {
    List<String> baseSuffixes = collectBaseLibraryNames(os, arch);
    ArrayList<String> r = new ArrayList<String>(24);
    String[] configurationSuffixes = SQLite.isDebugBinaryPreferred() ? DEBUG_SUFFIXES : RELEASE_SUFFIXES;
    if (versionSuffix != null) {
      for (String configurationSuffix : configurationSuffixes) {
        for (String baseSuffix : baseSuffixes) {
          r.add(baseSuffix + configurationSuffix + versionSuffix);
        }
      }
    }
    for (String configurationSuffix : configurationSuffixes) {
      for (String baseSuffix : baseSuffixes) {
        r.add(baseSuffix + configurationSuffix);
      }
    }
    return r;
  }

  private static List<String> collectBaseLibraryNames(String os, String arch) {
    ArrayList<String> r = new ArrayList<String>(6);
    String base = BASE_LIBRARY_NAME + "-" + os;
    r.add(base + "-" + arch);
    if (arch.equals("x86_64") || arch.equals("x64")) {
      r.add(base + "-amd64");
    } else if (arch.equals("x86")) {
      r.add(base + "-i386");
    } else if (arch.equals("i386")) {
      r.add(base + "-x86");
    } else if (arch.startsWith("arm") && arch.length() > 3) {
      if (arch.length() > 5 && arch.startsWith("armv") && Character.isDigit(arch.charAt(4))) {
        r.add(base + "-" + arch.substring(0, 5));
      }
      r.add(base + "-arm");
    }
    r.add(base);
    r.add(BASE_LIBRARY_NAME);
    return r;
  }

  private static String getForcedPath() {
    String r = System.getProperty(SQLite.LIBRARY_PATH_PROPERTY);
    if (r == null || r.length() == 0)
      return null;
    r = r.replace('\\', File.separatorChar).replace('/', File.separatorChar);
    return r;
  }

  private static String getClassUrl() {
    Class c = Internal.class;
    String name = c.getName().replace('.', '/') + ".class";
    URL url = c.getClassLoader().getResource(name);
    if (url == null)
      return null;
    String classUrl = url.toString();
    try {
      return URLDecoder.decode(classUrl, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      recoverableError(Internal.class, e.getMessage(), true);
      return classUrl;
    }
  }

  private static String getArch(String os) {
    String arch = System.getProperty("os.arch");
    if (arch == null) {
      logWarn(Internal.class, "os.arch is null");
      arch = "x86";
    } else {
      arch = arch.toLowerCase(Locale.US);
      if ("win32".equals(os) && "amd64".equals(arch)) {
        arch = "x64";
      }
    }
    logFine(Internal.class, "os.arch=" + arch);
    return arch;
  }

  private static String getOs() {
    String osname = System.getProperty("os.name");
    String os;
    if (osname == null) {
      logWarn(Internal.class, "os.name is null");
      os = "linux";
    } else {
      osname = osname.toLowerCase(Locale.US);
      if (osname.startsWith("mac") || osname.startsWith("darwin") || osname.startsWith("os x")) {
        os = "osx";
      } else if (osname.startsWith("windows")) {
        os = "win32";
      } else {
        String runtimeName = System.getProperty("java.runtime.name");
        if (runtimeName != null && runtimeName.toLowerCase(Locale.US).contains("android")) {
          os = "android";
        } else {
          os = "linux";
        }
      }
    }
    logFine(Internal.class, "os.name=" + osname + "; os=" + os);
    return os;
  }

  static boolean isWindows() {
    return getOs().equals("win32");
  }

  private static String getDefaultLibPath(String classUrl) {
    return getDefaultLibPath(System.getProperty("java.library.path"), classUrl);
  }

  /**
   * Returns a possible path to the binary library based on the .JAR file location where this class is loaded from.
   * Binaries are supposed to be stored in the same directory. If this .JAR is located in one of the directories
   * mentioned in java.library.path, returns null to force loading with System.loadLibrary().
   *
   * @param libraryPath java library path
   * @param classUrl sample sqlite4java class url
   * @return path to the .JAR file location or null if this path should not be used
   */
  static String getDefaultLibPath(String libraryPath, String classUrl) {
    File jar = getJarFileFromClassUrl(classUrl);
    if (jar == null)
      return null;
    File loadDir = jar.getParentFile();
    if (loadDir.getPath().length() == 0 || !loadDir.isDirectory())
      return null;
    if (libraryPath == null)
      libraryPath = "";
    boolean contains = false;
    char sep = File.pathSeparatorChar;
    if (libraryPath.length() > 0) {
      int k = 0;
      while (k < libraryPath.length()) {
        int p = libraryPath.indexOf(sep, k);
        if (p < 0) {
          p = libraryPath.length();
        }
        if (loadDir.equals(new File(libraryPath.substring(k, p)))) {
          contains = true;
          break;
        }
        k = p + 1;
      }
    }
    String loadPath = loadDir.getPath();
    if (contains) {
      return null;
    } else {
      return loadPath;
    }
  }

  private static File getJarFileFromClassUrl(String classUrl) {
    if (classUrl == null)
      return null;
    String s = classUrl;
    String prefix = "jar:file:";
    if (!s.startsWith(prefix))
      return null;
    s = s.substring(prefix.length());
    int k = s.lastIndexOf('!');
    if (k < 0)
      return null;
    File jar = new File(s.substring(0, k));
    if (!jar.isFile())
      return null;
    return jar;
  }

  // gets the suffix used in jar file - will check libs with that suffix too - or null

  static String getVersionSuffix(String classUrl) {
    File jar = getJarFileFromClassUrl(classUrl);
    if (jar == null)
      return null;
    String name = jar.getName();
    String lower = name.toLowerCase(Locale.US);
    if (!lower.startsWith(BASE_LIBRARY_NAME))
      return null;
    if (!lower.endsWith(".jar"))
      return null;
    int f = BASE_LIBRARY_NAME.length();
    int t = name.length() - 4; // ".jar".length()
    return f + 1 < t && name.charAt(f) == '-' ? name.substring(f, t) : null;
  }

  private static boolean tryLoadFromPath(String libname, String os, String path, Throwable[] failureReason) {
    String libFile = System.mapLibraryName(libname);
    if (os.equals("osx")) {
      String oldSuffix = ".jnilib";
      if (libFile.endsWith(oldSuffix)) {
        String newSuffix = ".dylib";
        libFile = libFile.substring(0, libFile.length() - oldSuffix.length()) + newSuffix;
      }
    }
    File lib = new File(new File(path), libFile);
    logFine(Internal.class, "checking " + lib);
    if (!lib.isFile() || !lib.canRead())
      return false;
    String logname = libname + " from " + lib;
    logFine(Internal.class, "trying to load " + logname);
    try {
      System.load(lib.getAbsolutePath());
      return verifyLoading(failureReason, logname);
    } catch (Throwable t) {
      logFine(Internal.class, "cannot load " + logname + ": " + t);
      updateLoadFailureReason(failureReason, t);
    }
    return false;
  }

  private static boolean tryLoadFromSystemPath(String libname, Throwable[] failureReason) {
    logFine(Internal.class, "trying to load " + libname);
    try {
      System.loadLibrary(libname);
      return verifyLoading(failureReason, libname + " from system path");
    } catch (Throwable t) {
      logFine(Internal.class, "cannot load " + libname + ": " + t);
      updateLoadFailureReason(failureReason, t);
    }
    return false;
  }

  private static boolean verifyLoading(Throwable[] failureReason, String logname) {
    logInfo(Internal.class, "loaded " + logname);
    LinkageError linkError = checkLoaded();
    if (linkError == null) {
      return true;
    }
    logFine(Internal.class, "cannot use " + logname + ": " + linkError);
    updateLoadFailureReason(failureReason, linkError);
    return false;
  }

  /**
   * This method is used to decide which exception describes the real problem. If the file is simply not found
   * (which we gather from the fact that message contains "java.library.path"), then it may or may not be the
   * real reason. If there is another exception which has something else to say, it's given the priority.
   */
  private static void updateLoadFailureReason(Throwable[] bestReason, Throwable currentReason) {
    if (bestReason[0] == null) {
      bestReason[0] = currentReason;
    } else if (currentReason != null) {
      String m1 = bestReason[0].getMessage();
      if (m1 != null && m1.contains("java.library.path")) {
        String m2 = currentReason.getMessage();
        if (m2 != null && !m2.contains("java.library.path")) {
          bestReason[0] = currentReason;
        }
      }
    }
  }

  private static LinkageError checkLoaded() {
    try {
      getLibraryVersionMessage();
      return null;
    } catch (LinkageError e) {
      return e;
    }
  }

  private static String getLibraryVersionMessage() {
    String version = _SQLiteSwigged.sqlite3_libversion();
    String wrapper = _SQLiteManual.wrapper_version();
    return "loaded sqlite " + version + ", wrapper " + wrapper;
  }
}
