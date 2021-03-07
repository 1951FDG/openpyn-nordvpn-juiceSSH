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
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

/**
 * SQLiteQueue is a basic implementation of job queue for an SQLite connection. It provides multi-threaded
 * or GUI application with asynchronous execution of database tasks in a single separate thread with a single
 * {@link SQLiteConnection}.
 * <p/>
 * The queue is started and stopped using {@link #start} and {@link #stop} methods correspondingly.
 * Each database task is represented by a subclass of {@link SQLiteJob}. A task is scheduled for execution
 * by {@link #execute} method. Tasks are served on first-come, first-serve basis.
 * <p/>
 * Public methods of SQLiteQueue are <strong>thread-safe</strong>, unless noted otherwise.
 * </p>
 * When writing tasks, it's a good practice to keep transaction boundaries within single task. That is, if you
 * BEGIN TRANSACTION in the task, make sure you COMMIT or ROLLBACK in the end. Otherwise, your transaction will
 * remain unfinished, locks held, and you possible wouldn't know which job will execute next in the context of
 * this unfinished transaction.
 * <p/>
 * SQLiteQueue may be subclassed in order to change certain behavior. If you need some things to be done
 * differently, look for a protected method to override. For example, you can implement a priority queue
 * instead of FIFO queue.
 * <p/>
 * SQLiteQueue and SQLiteJob are written to handle exceptions and errors in a controlled way. In particular,
 * if the queue thread terminates abnormally, SQLiteQueue will try to "reincarnate" by starting another thread
 * and opening another connection to the database. All queued tasks (except for the one that caused the problem)
 * should survive the reincarnation and execute in the new thread.
 * <p/>
 * Reincarnation is not possible for in-memory database, since the database is lost after connection closes.
 * <p/>
 * Some examples:
 * <pre>
 * void start() {
 *   myQueue = new SQLiteQueue(myDatabaseFile);
 *   myQueue.start();
 * }
 * <p/>
 * int getTableRowCount(final String tableName) {
 *   return myQueue.execute(new SQLiteJob&lt;Integer&gt;() {
 *     protected Integer job(SQLiteConnection connection) throws SQLiteException {
 *       SQLiteStatement st = connection.prepare("SELECT COUNT(*) FROM " + tableName);
 *       try {
 *         st.step();
 *         return st.columnInt(0);
 *       } finally {
 *         st.dispose();
 *       }
 *     }
 *   }).complete();
 * }
 * </pre>
 *
 * @author Igor Sereda
 * @see SQLiteJob
 */
public class SQLiteQueue {
  /**
   * Default timeout for reincarnating database thread.
   */
  public static final long DEFAULT_REINCARNATE_TIMEOUT = 3000;

  /**
   * Database file to open. If null, memory database is used.
   */
  private final File myDatabaseFile;

  /**
   * Used to create queue thread and reincarnator thread.
   */
  private final ThreadFactory myThreadFactory;

  /**
   * Currently running queue thread.
   */
  private volatile Thread myThread;

  /**
   * Lock for protecting the following fields.
   */
  private final Object myLock = new Object();

  /**
   * Stores queued jobs. <p/><i>protected by myLock</i>
   */
  protected Collection<SQLiteJob> myJobs;

  /**
   * If true, queue stop has been requested (or implied). <p/><i>protected by myLock</i>
   */
  private boolean myStopRequested;

  /**
   * If true, non-gracious stop has been required by the user. Bears no sense if {@link #myStopRequested} is false.
   * <p/><i>protected by myLock</i>
   */
  private boolean myStopRequired;

  /**
   * The job currently being executed. <p/><i>protected by myLock</i>
   */
  private SQLiteJob myCurrentJob;

  /**
   * Our running connection. May be null when connection is not yet created or when it's closed. <p/><i>confined to myThread</i>
   */
  private SQLiteConnection myConnection;

  /**
   * Constructs the queue, which will use an in-memory database.
   * <p/>
   * The queue must be started in order for jobs to be executed.
   *
   * @see #start
   */
  public SQLiteQueue() {
    this(null);
  }

  /**
   * Constructs the queue. SQLiteQueue will use {@link SQLiteConnection#open} method to create a connection within
   * queue thread.
   * <p/>
   * The queue must be started in order for jobs to be executed.
   *
   * @param databaseFile database file to connect to, or null to open an in-memory database
   * @see #start
   */
  public SQLiteQueue(File databaseFile) {
    this(databaseFile, Executors.defaultThreadFactory());
  }

  /**
   * Constructs the queue and allows to specify a factory for the queue thread.
   *
   * @param databaseFile  database file to connect to, or null to open an in-memory database
   * @param threadFactory the factory for thread(s), cannot be null
   */
  public SQLiteQueue(File databaseFile, ThreadFactory threadFactory) {
    if (threadFactory == null)
      throw new NullPointerException();
    myDatabaseFile = databaseFile;
    myThreadFactory = threadFactory;
  }

  public String toString() {
    return "SQLiteQueue[" + (myDatabaseFile == null ? "" : myDatabaseFile.getName()) + "]";
  }

  /**
   * Get the underlying database file.
   * 
   * @return database file or null if queue is working on an in-memory database
   */
  public File getDatabaseFile() {
    return myDatabaseFile;
  }

  /**
   * Starts the queue by creating a new thread, opening connection in that thread and executing all jobs there.
   * <p/>
   * The queue will remain active until {@link #stop} method is called, or until it is terminated by non-recoverable error.
   * <p/>
   * Calling this method second time does not have any effect. A queue cannot be started after it has stopped.
   * <p/>
   * Any jobs added to the queue prior to start() will be carried out.
   * <p/>
   * This method is thread-safe: it may be called from any thread.
   *
   * @return this queue
   * @throws IllegalStateException if threadFactory failed to produce a new thread
   */
  public SQLiteQueue start() {
    Thread thread;
    synchronized (myLock) {
      if (myThread != null || myStopRequested) {
        Internal.logWarn(this, myStopRequested ? "stopped" : "already started");
        return this;
      }
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "starting");
      }
      thread = myThreadFactory.newThread(new Runnable() {
        public void run() {
          runQueue();
        }
      });
      if (thread == null) {
        throw new IllegalStateException(this + " cannot create new thread");
      }
      String name = thread.getName();
      // override default thread names
      if (name == null || name.startsWith("Thread-") || name.startsWith("pool-")) {
        thread.setName(toString());
      }
      myThread = thread;
    }
    thread.start();
    return this;
  }

  /**
   * Stops the queue. After this method is called, no more jobs are accepted in {@link #execute} method. The thread
   * and connection are finished and disposed.
   * <p/>
   * If <code>gracefully</code> parameter is true, the currently queued jobs will be executed before queue stops.
   * Otherwise, any pending jobs are cancelled, and the currently running job may be cancelled to. (If the currently
   * running job is ignorant of job.isCancelled() status and does not run a long SQL statement, it still may finish
   * normally.)
   * <p/>
   * After call to <code>stop(true)</code> you can call <code>stop(false)</code> to force non-gracefull shutdown.
   * Other than that, calling <code>stop()</code> second time has no effect.
   * <p/>
   * If the queue hasn't been started, it will not be able to start later.
   * <p/>
   * This method is thread-safe: it may be called from any thread. It finishes immediately, while actual stopping
   * of the queue happening asynchronously. If you need to wait until queue is fully stopped, use {@link #join} method
   * after you called stop().
   *
   * @param gracefully if true, jobs already queued will be executed, then the queue will stop
   * @return this queue
   */
  public SQLiteQueue stop(boolean gracefully) {
    SQLiteJob currentJob = null;
    synchronized (myLock) {
      if (!gracefully) {
        if (!myStopRequired && myStopRequested && Internal.isFineLogging()) {
          Internal.logFine(this, "now stopping non-gracefully");
        }
        myStopRequired = true;
      }
      if (myStopRequested) {
        // already stopping
        return this;
      }
      if (Internal.isFineLogging()) {
        Internal.logFine(this, gracefully ? "stopping gracefully" : "stopping non-gracefully");
      }
      myStopRequested = true;
      if (myStopRequired) {
        currentJob = myCurrentJob;
      }
      myLock.notify();
    }
    if (currentJob != null) {
      currentJob.cancel(true);
    }
    return this;
  }

  /**
   * Waits for the queue to stop. The method uses {@link Thread#join} method to join with the queue thread.
   * <p/>
   * Note that this method does not stop the queue. You need to call {@link #stop} explicitly.
   * <p/>
   * If queue has not been started, the method returns immediately.
   *
   * @return this queue
   * @throws InterruptedException  if the current thread is interrupted
   * @throws IllegalStateException if called from the queue thread
   */
  public SQLiteQueue join() throws InterruptedException {
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "waiting for queue to stop");
    }
    Thread thread = myThread;
    if (thread == Thread.currentThread()) {
      throw new IllegalStateException();
    }
    if (thread != null) {
      thread.join();
    }
    return this;
  }

  /**
   * Places a job in the queue for asynchronous execution in database thread.
   * <p/>
   * The added job's {@link SQLiteJob#job} method will be called from the database thread with an instance of
   * {@link SQLiteConnection}. Job may provide a return value, which will be treated as the job result.
   * <p/>
   * The queue must be started in order for jobs to start executing. Jobs may be added to the queue before or after
   * the queue is started. However, if the queue is already stopped, the job will be immediately cancelled. (It will
   * receive {@link SQLiteJob#jobCancelled} and {@link SQLiteJob#jobFinished} callbacks before this method finishes.)
   * <p/>
   * Because this method returns the argument, you can chain this method with other methods in SQLiteJob or in its
   * subclass:
   * <pre>
   *   MyResult r = myQueue.execute(new SQLiteJob&lt;MyResult&gt;() { ... }).complete();
   * </pre>
   *
   * @param job the job to be executed on this queue's database connection, must not be null
   * @param <T> class of the job's result; use Object or Void if no result is needed
   * @param <J> job class
   * @return job
   * @see SQLiteJob
   */
  public <T, J extends SQLiteJob<T>> J execute(J job) {
    if (job == null)
      throw new NullPointerException();
    boolean cancel = false;
    synchronized (myLock) {
      if (myStopRequested) {
        Internal.logFine(this, "job not executed: " + job);
        cancel = true;
      } else {
        if (Internal.isFineLogging()) {
          Internal.logFine(this, "queueing " + job);
        }
        addJob(job);
        myLock.notify();
      }
    }
    if (cancel) {
      job.cancel(true);
    }
    return job;
  }

  /**
   * Waits until all jobs in the queue are executed.
   *
   * @return this instance
   * @throws InterruptedException if the current thread is interrupted
   */
  public SQLiteQueue flush() throws InterruptedException {
    synchronized (myLock) {
      while (!isJobQueueEmpty() || myCurrentJob != null) {
        myLock.wait(1000);
        myLock.notify();
      }
    }
    return this;
  }

  /**
   * Checks if the queue is stopped.
   *
   * @return true if the queue was requested to stop or has stopped
   */
  public boolean isStopped() {
    synchronized (myLock) {
      return myStopRequested;
    }
  }

  /**
   * Checks if the current thread is the thread that runs the queue's database connection.
   * 
   * @return true if the current thread is the database thread
   */
  public boolean isDatabaseThread() {
    return Thread.currentThread() == myThread;
  }

  /**
   * Adds a job to the job collection. Override to change the logic or order of jobs.
   * <p/>
   * This method is called under synchronized lock and must not call any listeners or alien code.
   *
   * @param job the job to be added to myJobs, the latter possible being null
   */
  protected void addJob(SQLiteJob job) {
    assert Thread.holdsLock(myLock) : job;
    Collection<SQLiteJob> jobs = myJobs;
    if (jobs == null) {
      myJobs = jobs = createJobCollection();
    }
    jobs.add(job);
  }

  /**
   * Creates a new collection for storing pending jobs. Override to change the queue logic.
   * <p/>
   * This method is called under synchronized lock and must not call any listeners or alien code.
   *
   * @return an instance of collection for jobs
   */
  protected Collection<SQLiteJob> createJobCollection() {
    return new ArrayList<SQLiteJob>();
  }

  /**
   * Checks if there are no more pending jobs. Override to change the queue logic.
   * <p/>
   * This method is called under synchronized lock and must not call any listeners or alien code.
   *
   * @return true if there are no pending jobs
   */
  protected boolean isJobQueueEmpty() {
    assert Thread.holdsLock(myLock);
    return myJobs == null || myJobs.isEmpty();
  }

  /**
   * Clears the queue and returned removed jobs. Override to change the queue logic.
   * </p>
   * After this method is called, {@link #isJobQueueEmpty} must return true.
   * <p/>
   * This method is called under synchronized lock and must not call any listeners or alien code.
   *
   * @return non-null list of removed jobs
   */
  protected List<SQLiteJob> removeJobsClearQueue() {
    assert Thread.holdsLock(myLock);
    if (myJobs == null) return Collections.emptyList();
    ArrayList<SQLiteJob> r = new ArrayList<SQLiteJob>(myJobs);
    myJobs.clear();
    return r;
  }

  /**
   * Selects the next job from pending jobs to be executed. Override to change the queue logic.
   * <p/>
   * This method is called under synchronized lock and must not call any listeners or alien code.
   * 
   * @return null if there are no pending jobs, or the job for execution
   */
  protected SQLiteJob selectJob() {
    assert Thread.holdsLock(myLock);
    Collection<SQLiteJob> jobs = myJobs;
    if (jobs == null || jobs.isEmpty()) return null;
    Iterator<SQLiteJob> ii = jobs.iterator();
    SQLiteJob r = ii.next();
    ii.remove();
    return r;
  }

  /**
   * Creates and opens a connection to the database. Override to change how database connection is opened.
   * <p/>
   * If this method throws an exception, the queue thread will terminate and possible reincarnate to try again.
   *
   * @return a new connection, not null, that can be used in the current thread
   * @throws SQLiteException if connection cannot be created
   * @see #initConnection
   */
  protected SQLiteConnection openConnection() throws SQLiteException {
    SQLiteConnection connection = new SQLiteConnection(myDatabaseFile);
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "opening " + connection);
    }
    try {
      connection.open();
    } catch (SQLiteException e) {
      Internal.logWarn("cannot open " + connection, e);
      throw e;
    }
    return connection;
  }

  /**
   * Initialize a new connection. Override to provide additional initialization code, for example executing
   * initializing SQL.
   * <p/>
   * If this method throws an exception, the queue thread will terminate and possible reincarnate to try again.
   *
   * @param connection freshly opened database connection
   * @throws SQLiteException if any initialization code fails
   */
  protected void initConnection(SQLiteConnection connection) throws SQLiteException {
  }

  /**
   * Disposes the connection. Override to change how connection is disposed.
   *
   * @param connection database connection no longer in use by the queue
   */
  protected void disposeConnection(SQLiteConnection connection) {
    try {
      if (connection != null) {
        if (Internal.isFineLogging()) {
          Internal.logFine(this, "disposing " + connection);
        }
        connection.dispose();
      }
    } catch (Exception e) {
      Internal.log(Level.SEVERE, this, "error disposing connection", e);
    }
  }

  /**
   * Rolls back current transaction. This method is called after exception is caught from a job, or after
   * job is cancelled. Override to change how to handle these two situations.
   */
  protected void rollback() {
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "rolling back transaction");
    }
    try {
      myConnection.exec("ROLLBACK");
    } catch (SQLiteException e) {
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "exception during rollback: " + e);
      }
    }
  }

  /**
   * Runs the job with the current connection.
   *
   * @param job next job from the queue
   * @throws Throwable any kind of problem
   */
  protected void executeJob(SQLiteJob job) throws Throwable {
    if (job == null) return;
    SQLiteConnection connection = myConnection;
    if (connection == null)
      throw new IllegalStateException(this + ": executeJob: no connection");
    try {
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "executing " + job);
      }
      job.execute(connection, this);
      afterExecute(job);
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "finished executing " + job);
      }
    } catch (Throwable e) {
      handleJobException(job, e);
    }
  }

  /**
   * Do some work after job.execute() finished. By default, performs rollback after a cancelled job.
   *
   * @param job finished job
   * @throws Throwable any kind of problem
   */
  protected void afterExecute(SQLiteJob job) throws Throwable {
    assert job.isDone() : job;
    if (job.isCancelled()) {
      rollback();
    }
  }

  /**
   * Do some work if job threw an exception. By default, rolls back and ignores the exception. 
   *
   * @param job erred job
   * @param e exception thrown by the job
   * @throws Throwable any kind of problem
   */
  protected void handleJobException(SQLiteJob job, Throwable e) throws Throwable {
    rollback();
    if (e instanceof ThreadDeath) throw (ThreadDeath) e;
  }

  /**
   * Provides reincarnation timeout (the period to wait before reincarnating abnormally stopped queue thread).
   *
   * @return reincarnation timeout
   */
  protected long getReincarnationTimeout() {
    return DEFAULT_REINCARNATE_TIMEOUT;
  }

  /**
   * Checks if reincarnation should be attempted after queue thread terminates abnormally.
   *
   * @return true if reincarnation should be attempted
   */
  protected boolean isReincarnationPossible() {
    return myDatabaseFile != null && getReincarnationTimeout() >= 0;
  }

  /**
   * Reincarnates the queue. This implementation starts a new thread, which waits for some time and then restarts
   * database thread.
   *
   * @param reincarnateTimeout time to wait
   */
  protected void reincarnate(final long reincarnateTimeout) {
    Internal.logWarn(this, "stopped abnormally, reincarnating in " + reincarnateTimeout + "ms");
    Thread reincarnator = myThreadFactory.newThread(new Runnable() {
      public void run() {
        try {
          synchronized (myLock) {
            long now = System.currentTimeMillis();
            long wake = now + reincarnateTimeout;
            while (now < wake) {
              myLock.wait(wake - now);
              if (myStopRequested) {
                Internal.logWarn(SQLiteQueue.this, "stopped, will not reincarnate");
                return;
              }
              now = System.currentTimeMillis();
            }
          }
          SQLiteQueue.this.start();
        } catch (InterruptedException e) {
          Internal.log(Level.WARNING, SQLiteQueue.this, "not reincarnated", e);
        }
      }
    });
    reincarnator.setName("reincarnate " + this + " in " + reincarnateTimeout + "ms");
    reincarnator.start();
  }


  private void runQueue() {
    try {
      queueFunction();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Internal.logWarn(this + " interrupted", e);
    } catch (Throwable e) {
      Internal.log(Level.SEVERE, this, "error running job queue", e);
      if (e instanceof ThreadDeath)
        throw (ThreadDeath)e;
    } finally {
      threadStopped();
    }
  }

  private void queueFunction() throws Throwable {
    if (Internal.isFineLogging())
      Internal.logFine(this, "started");
    disposeConnection(myConnection);
    myConnection = null;
    myConnection = openConnection();
    initConnection(myConnection);

    while (true) {
      if (Thread.interrupted())
        throw new InterruptedException();
      SQLiteJob job;
      synchronized (myLock) {
        myCurrentJob = null;
        myLock.notify();
        while (true) {
          if (myStopRequested && (myStopRequired || isJobQueueEmpty())) {
            if (Internal.isFineLogging()) {
              Internal.logFine(this, "thread exiting");
            }
            return;
          }
          job = selectJob();
          if (job != null) {
            myCurrentJob = job;
            break;
          }
          myLock.wait(1000);
          myLock.notify();
        }
      }
      executeJob(job);
    }
  }

  private void cancelJobs(List<SQLiteJob> jobs) {
    if (jobs != null) {
      for (SQLiteJob job : jobs) {
        job.cancel(true);
      }
    }
  }

  private void threadStopped() {
    assert Thread.currentThread() == myThread : Thread.currentThread() + " " + myThread;
    disposeConnection(myConnection);
    myConnection = null;
    boolean reincarnate;
    List<SQLiteJob> droppedJobs = null;
    synchronized (myLock) {
      reincarnate = !myStopRequested;
      if (reincarnate && !isReincarnationPossible()) {
        Internal.log(Level.SEVERE, this, "stopped abnormally, reincarnation is not possible for in-memory database", null);
        reincarnate = false;
        myStopRequested = true;
      }
      if (!reincarnate) {
        droppedJobs = removeJobsClearQueue();
      }
      myThread = null;
    }
    if (!reincarnate) {
      cancelJobs(droppedJobs);
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "stopped");
      }
    } else {
      reincarnate(getReincarnationTimeout());
    }
  }
}
