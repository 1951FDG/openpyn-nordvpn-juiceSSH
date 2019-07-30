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

import java.io.*;
import java.util.*;

import static com.almworks.sqlite4java.SQLiteConstants.SQLITE_DONE;
import static com.almworks.sqlite4java.SQLiteConstants.SQLITE_ROW;

/**
 * SQLiteProfiler measures and accumulates statistics for various SQLite methods. The statistics is then available
 * in a report form.
 * <p>
 * To start profiling, call {@link SQLiteConnection#profile} and get the profiler. After profiling is done, call
 * {@link SQLiteConnection#stopProfiling} and inspect the profiler's results.
 * <p>
 * This is pure Java-based profiling, not related to <code>sqlite3_profile</code> method.
 *
 * @author Igor Sereda
 * @see SQLiteConnection#profile
 * @see SQLiteConnection#stopProfiling
 */
public class SQLiteProfiler {
  private static final String HEADER = "-----------------------------------------------------------------------------";
  private final Map<String, SQLStat> myStats = new HashMap<String, SQLStat>();

  /**
   * Outputs current report into PrintWriter.
   *
   * @param out report writer
   */
  public void printReport(PrintWriter out) {
    ArrayList<SQLStat> stats = new ArrayList<SQLStat>(myStats.values());
    Collections.sort(stats, new Comparator<SQLStat>() {
      public int compare(SQLStat o1, SQLStat o2) {
        return o1.getTotalTime() < o2.getTotalTime() ? 1 : -1;
      }
    });
    for (SQLStat stat : stats) {
      stat.printReport(out);
    }
  }

  /**
   * Returns current report as a String.
   *
   * @return current report
   */
  public String printReport() {
    StringWriter sw = new StringWriter();
    printReport(new PrintWriter(sw));
    return sw.toString();
  }

  /**
   * Prints report to a file. If IOException occurs, write warning log message, but does not throw it on the caller.
   *
   * @param file target file
   */
  public void printReport(String file) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(fos));
      printReport(writer);
      writer.close();
    } catch (IOException e) {
      Internal.logWarn(this, e);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }
  }

  void reportExec(String sql, long nfrom, long nto, int rc) {
    getStat(sql).report(rc == 0 ? "exec" : "exec:error(" + rc + ")", nfrom, nto);
  }

  void reportPrepare(String sql, long nfrom, long nto, int rc) {
    getStat(sql).report(rc == 0 ? "prepare" : "prepare:error(" + rc + ")", nfrom, nto);
  }

  void reportStep(boolean alreadyStepped, String sql, long nfrom, long nto, int rc) {
    SQLStat stat = getStat(sql);
    if (rc != SQLITE_ROW && rc != SQLITE_DONE) {
      stat.report("step:error(" + rc + ")", nfrom, nto);
      return;
    }
    stat.report("step", nfrom, nto);
    if (alreadyStepped || rc == SQLITE_ROW) {
      stat.report(alreadyStepped ? "step:next" : "step:first", nfrom, nto);
    }
  }

  void reportLoadInts(boolean alreadyStepped, String sql, long nfrom, long nto, int rc, int count) {
    SQLStat stat = getStat(sql);
    if (rc != SQLITE_ROW && rc != SQLITE_DONE) {
      stat.report("loadInts:error(" + rc + ")", nfrom, nto);
      return;
    }
    stat.report("loadInts", nfrom, nto);
    if (alreadyStepped || rc == SQLITE_ROW) {
      stat.report(alreadyStepped ? "loadInts:next" : "loadInts:first", nfrom, nto);
    }
    // todo count
  }

  void reportLoadLongs(boolean alreadyStepped, String sql, long nfrom, long nto, int rc, int count) {
    SQLStat stat = getStat(sql);
    if (rc != SQLITE_ROW && rc != SQLITE_DONE) {
      stat.report("loadLongs:error(" + rc + ")", nfrom, nto);
      return;
    }
    stat.report("loadLongs", nfrom, nto);
    if (alreadyStepped || rc == SQLITE_ROW) {
      stat.report(alreadyStepped ? "loadLongs:next" : "loadLongs:first", nfrom, nto);
    }
    // todo count
  }

  private SQLStat getStat(String sql) {
    SQLStat stat = myStats.get(sql);
    if (stat == null) {
      stat = new SQLStat(sql);
      myStats.put(sql, stat);
    }
    return stat;
  }

  private static String formatDuration(long nanos) {
    if (nanos > 1000000000L) {
      return String.format(Locale.US, "%.1fs", ((double) nanos) / 1000000000.0);
    } else if (nanos > 100000000L) {
      return String.format(Locale.US, "%dms", nanos / 1000000L);
    } else if (nanos > 10000000L) {
      return String.format(Locale.US, "%.1fms", ((double) nanos) / 1000000.0);
    } else if (nanos > 100000L) {
      return String.format(Locale.US, "%.2fms", ((double) nanos) / 1000000.0);
    } else {
      return String.format(Locale.US, "%.2fmks", ((double) nanos) / 1000.0);
    }
  }


  private static class SQLStat {
    private final String mySQL;
    private final Map<String, Stat> myStats = new TreeMap<String, Stat>();

    public SQLStat(String sql) {
      mySQL = sql;
    }

    public String getSQL() {
      return mySQL;
    }

    public void report(String name, long nfrom, long nto) {
      Stat stat = myStats.get(name);
      if (stat == null) {
        stat = new Stat();
        myStats.put(name, stat);
      }
      stat.report(nfrom, nto);
    }

    public long getTotalTime() {
      long total = 0;
      for (Stat stat : myStats.values()) {
        total += stat.myTotalNanos;
      }
      return total;
    }

    public void printReport(PrintWriter out) {
      out.println(HEADER);
      out.println(mySQL);
      out.println(HEADER);
      String totalPrefix = "total time";
      int maxPrefix = totalPrefix.length();
      for (String s : myStats.keySet()) {
        maxPrefix = Math.max(maxPrefix, s.length());
      }
      StringBuilder b = new StringBuilder();
      addLeftColumn(b, totalPrefix, maxPrefix);
      b.append(formatDuration(getTotalTime()));
      out.println(b.toString());
      for (Map.Entry<String, Stat> e : myStats.entrySet()) {
        b.setLength(0);
        addLeftColumn(b, e.getKey(), maxPrefix);
        Stat stat = e.getValue();
        b.append("total:").append(formatDuration(stat.getTotalNanos())).append(' ');
        b.append("count:").append(stat.getTotalCount()).append(' ');
        b.append("min|avg|max:").append(formatDuration(stat.getMinNanos())).append('|').append(formatDuration(stat.getAvgNanos())).append('|').append(formatDuration(stat.getMaxNanos())).append(' ');
        b.append("freq:").append(stat.getFrequency());
        out.println(b.toString());
      }
      out.println();
    }

    private void addLeftColumn(StringBuilder b, String name, int maxPrefix) {
      b.append("    ");
      b.append(name);
      for (int add = maxPrefix + 4 - b.length(); add > 0; add--) b.append(' ');
      b.append("   ");
    }
  }

  private static class Stat {
    private int myTotalCount;
    private long myTotalNanos;
    private long myMinNanos = -1;
    private long myMaxNanos = -1;
    private long myFirstTime;
    private long myLastTime;

    public void report(long nfrom, long nto) {
      long duration = nto - nfrom;
      if (duration < 0) return;
      myTotalCount++;
      myTotalNanos += duration;
      if (myMinNanos < 0 || duration < myMinNanos)
        myMinNanos = duration;
      if (myMaxNanos < 0 || duration > myMaxNanos)
        myMaxNanos = duration;
      myLastTime = System.currentTimeMillis();
      if (myFirstTime == 0)
        myFirstTime = myLastTime;
    }

    public long getTotalNanos() {
      return myTotalNanos;
    }

    public int getTotalCount() {
      return myTotalCount;
    }

    public long getMinNanos() {
      return myMinNanos;
    }

    public long getAvgNanos() {
      return myTotalCount > 0 ? myTotalNanos / myTotalCount : 0;
    }

    public long getMaxNanos() {
      return myMaxNanos;
    }

    public String getFrequency() {
      if (myTotalCount < 10) return "-";
      long millis = myLastTime - myFirstTime;
      long t = millis / myTotalCount;
      if (t == 0) return "-";
      return "1/" + formatDuration(t * 1000000L);
    }
  }
}

