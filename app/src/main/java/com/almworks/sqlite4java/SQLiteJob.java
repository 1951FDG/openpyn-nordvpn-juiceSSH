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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

/**
 * SQLiteJob is a unit of work accepted by {@link SQLiteQueue}. You can
 * implement {@link #job} method and add the job to the queue with {@link SQLiteQueue#execute} method.
 * <p/>
 * A job can optionally have a result. Type parameter <code>&lt;T&gt;</code> defines the type of the result, and the
 * value of the result is returned by the <code>job()</code> method. If job finishes unsuccessfully or is cancelled,
 * the result is always null. If you don't need the job to have a result, define it as
 * <code>SQLiteJob&lt;Object&gt;</code> or <code>SQLiteJob&lt;Void&gt;</code> and return null from the <code>job()</code>
 * method.
 * <p/>
 * Job implements {@link Future} interface and can be used along with different types of future results.
 * <p/>
 * Also, you can override methods {@link #jobStarted}, {@link #jobFinished}, {@link #jobCancelled} and
 * {@link #jobError} to implement callbacks during the job's lifecycle.
 * <p/>
 * SQLiteJob is a one-time object. Once the job is finished, it cannot be executed again.
 * <p/>
 * Public methods of SQLiteJob are thread-safe unless specified otherwise. Protected methods are mostly called
 * from the database thread and must be overridden carefully.
 * <p/>
 * When programming a job, it's a good practice to keep transaction boundaries within single job. That is, if you
 * BEGIN TRANSACTION in the job, make sure you COMMIT or ROLLBACK in the end. Otherwise, your transaction will
 * remain unfinished, locks held, and you possible wouldn't know which job will execute next in the context of
 * this unfinished transaction.
 *
 * @param <T> type of the result
 * @see SQLiteQueue
 * @author Igor Sereda
 */
public abstract class SQLiteJob<T> implements Future<T> {
  // internal state constants
  private static final int PENDING = 0;
  private static final int RUNNING = 1;
  private static final int SUCCEEDED = 2;
  private static final int ERROR = 3;
  private static final int CANCELLED = 4;

  /**
   * Protection for fields
   */
  private final Object myLock = new Object();

  /**
   * Current state. Protected by myLock.
   */
  private int myState = PENDING;

  /**
   * Error thrown by job()
   */
  private Throwable myError;

  /**
   * Keeps connection while the job is being executed (in order to interrupt SQL)
   */
  private SQLiteConnection myConnection;

  /**
   * Keeps a reference to the queue while the job is being executed. May be null.
   */
  private SQLiteQueue myQueue;

  /**
   * The result of the job 
   */
  private T myResult;

  /**
   * Performs work on the SQLite database.
   * <p/>
   * This method is called only once from the database thread, when the job is selected and executed
   * by the queue. After job is completed, it is removed from the queue and next job is executed.
   * <p/>
   * If job method throws any exception, it's recorded, logged, but otherwise it does not affect other jobs (except
   * for side-effects of unfinished SQL work). This may be changed by overriding job's or queue's related methods.
   * 
   * @param connection an open connection to the database, not null
   * @return the result, or null
   * @throws Throwable on any problem
   * @see SQLiteQueue#execute
   */
  protected abstract T job(SQLiteConnection connection) throws Throwable;

  /**
   * This method is called when the job is about to be executed, before call to {@link #job} method.
   * <p/>
   * This method may not be called at all if a job is cancelled before execution starts.
   *
   * @param connection an open connection to the database, not null
   * @throws Throwable on any problem
   */
  protected void jobStarted(SQLiteConnection connection) throws Throwable {
  }

  /**
   * This method is called when the job is no longer in the queue.
   * Overriding this method is the best way to asynchronously process the result of the job.
   * <p/>
   * This method is called <strong>always</strong>, regardless
   * of the job execution result, and even if the job is cancelled before execution. More strictly, it is called
   * once between the time {@link SQLiteQueue#execute} is called and the time when this job is no longer in the queue
   * nor being executed.
   * <p/>
   * The result of the job is passed as a parameter.
   *
   * @param result the result of the job, or null if the job was cancelled or has thrown exception
   * @throws Throwable on any problem
   */
  protected void jobFinished(T result) throws Throwable {
  }

  /**
   * This method is called after {@link #job} method has thrown an exception. The exception is passed
   * as a parameter.
   *
   * @param error exception thrown by the job
   * @throws Throwable on any problem, or the rethrown exception
   */
  protected void jobError(Throwable error) throws Throwable {
  }

  /**
   * This method is called after job has been cancelled, either due to call to the {@link #cancel} method,
   * or because queue has stopped, or for any other reason.
   *
   * @throws Throwable on any problem
   */
  protected void jobCancelled() throws Throwable {
  }

  /**
   * Returns the instance of the queue that is currently running the job. May return null.
   * @return the queue that is currently running this job, if available. 
   */
  protected final SQLiteQueue getQueue() {
    synchronized (myLock) {
      return myQueue;
    }
  }

  /**
   * Returns the error thrown by the job.
   * 
   * @return the error thrown by the {@link #job} method, or null.
   */
  public Throwable getError() {
    synchronized (myLock) {
      return myError;
    }
  }

  /**
   * Returns <tt>true</tt> if this job completed.
   *
   * Completion may be due to normal termination, an exception, or
   * cancellation -- in all of these cases, this method will return
   * <tt>true</tt>.
   *
   * @return <tt>true</tt> if this task completed
   */
  public boolean isDone() {
    synchronized (myLock) {
      return myState == SUCCEEDED || myState == CANCELLED || myState == ERROR;
    }
  }

  /**
   * Attempts to cancel execution of this job.  This attempt will
   * fail if the job has already completed, has already been cancelled,
   * or could not be cancelled for some other reason. If successful,
   * and this job has not started when <tt>cancel</tt> is called,
   * this job should never run.  If the job has already started,
   * then the <tt>mayInterruptIfRunning</tt> parameter determines
   * whether the thread executing this task should be interrupted in
   * an attempt to stop the task.
   * <p/>
   * When an active job is being cancelled with <tt>mayInterruptIfRunning</tt> parameter,
   *  {@link SQLiteConnection#interrupt} method is called to cancel a potentially long-running SQL. If there's
   * no SQL running, it will have no effect. The running job may check {@link #isCancelled} method and finish
   * prematurely. There are no other means to cancel a running job.
   * <p/>
   * If the job is still pending, then {@link #jobCancelled} and {@link #jobFinished} callbacks are called during
   * the execution of this method.
   *
   * @param mayInterruptIfRunning <tt>true</tt> if the thread executing this
   * task should be interrupted; otherwise, in-progress tasks are allowed
   * to complete
   * @return <tt>false</tt> if the task could not be cancelled,
   * typically because it has already completed normally;
   * <tt>true</tt> otherwise
   */
  public boolean cancel(boolean mayInterruptIfRunning) {
    SQLiteConnection connection;
    synchronized (myLock) {
      if (isDone()) {
        return false;
      }
      if (myState == RUNNING && !mayInterruptIfRunning) {
        return false;
      }
      assert myConnection == null || myState == RUNNING : myState + " " + myConnection;
      myState = CANCELLED;
      connection = myConnection;
    }
    if (connection != null) {
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "interrupting");
      }
      try {
        connection.interrupt();
      } catch (SQLiteException e) {
        Internal.log(Level.WARNING, this, "exception when interrupting", e);
      }
    } else {
      if (Internal.isFineLogging()) {
        Internal.logFine(this, "cancelling");
      }
      // job never ran
      finishJob(null);
    }
    return true;
  }

  /**
   * Cancels this job. Convenience method to call <code>cancel(true)</code>.
   *
   * @see #cancel(boolean)
   */
  public void cancel() {
    cancel(true);
  }

  /**
   * Returns <tt>true</tt> if this job was cancelled before it completed
   * normally.
   *
   * @return <tt>true</tt> if this job was cancelled before it completed
   */
  public boolean isCancelled() {
    synchronized (myLock) {
      return myState == CANCELLED;
    }
  }

  /**
   * Waits if necessary for the job to complete, and then
   * retrieves its result.
   * <p/>
   * Calling this method, as well as convenience method {@link #complete}, is a way to block the current thread
   * and wait for the result.
   *
   * @return the result
   * @throws java.util.concurrent.CancellationException if the job was cancelled
   * @throws ExecutionException if the job threw an  exception
   * @throws InterruptedException if the current thread was interrupted while waiting
   */
  public T get() throws InterruptedException, ExecutionException {
    try {
      return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new AssertionError(e + " cannot happen");
    }
  }

  /**
   * Waits if necessary for at most the given time for the job
   * to complete, and then retrieves its result, if available.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return the result
   * @throws java.util.concurrent.CancellationException if the job was cancelled
   * @throws ExecutionException if the job threw an exception
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws TimeoutException if the wait timed out
   */
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    synchronized (myLock) {
      if (!isDone()) {
        SQLiteQueue queue = myQueue;
        if (queue != null && queue.isDatabaseThread()) {
          throw new IllegalStateException("called from the database thread, would block forever");
        }
        long now = System.currentTimeMillis();
        long stop;
        if (timeout <= 0) {
          stop = now - 1;
        } else {
          stop = now + unit.toMillis(timeout);
          if (stop < now) {
            // overflow
            stop = Long.MAX_VALUE;
          }
        }
        while (now < stop) {
          if (isDone()) break;
          if (Thread.interrupted())
            throw new InterruptedException();
          myLock.wait(Math.min(1000L, stop - now));
          now = System.currentTimeMillis();
        }
      }
      if (isDone()) {
        if (myState == ERROR) {
          throw new ExecutionException(myError);
        }
        return myResult;
      }
    }
    throw new TimeoutException();
  }

  /**
   * Wait if necessary for the job to complete and return the result.
   * <p/>
   * This is a convenience method for calling {@link #get()} without having to catch exceptions.
   *
   * @return the result of the job, or null if it has been cancelled or has erred
   */
  public T complete() {
    try {
      return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Internal.log(Level.WARNING, this, "complete() consumed exception", e);
      Thread.currentThread().interrupt();
      return null;
    } catch (ExecutionException e) {
      Internal.log(Level.WARNING, this, "complete() consumed exception", e);
      return null;
    } catch (TimeoutException e) {
      Internal.log(Level.WARNING, this, "complete() timeout?", e);
      return null;
    }
  }

  @Override
  public String toString() {
    String r = super.toString();
    int k = r.lastIndexOf('.');
    if (k >= 0) r = r.substring(k + 1);
    return r;
  }

  void execute(SQLiteConnection connection, SQLiteQueue queue) throws Throwable {
    if (!startJob(connection, queue)) return;
    T result = null;
    try {
      result = job(connection);
    } catch (Throwable e) {
      processJobError(e);
    } finally {
      finishJob(result);
    }
  }

  private boolean startJob(SQLiteConnection connection, SQLiteQueue queue) {
    synchronized (myLock) {
      if (myState != PENDING) {
        if (myState != CANCELLED) {
          Internal.logWarn(this, "was already executed");
        }
        return false;
      }
      myState = RUNNING;
      myConnection = connection;
      myQueue = queue;
    }
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "started");
    }
    try {
      jobStarted(connection);
    } catch (Throwable e) {
      Internal.log(Level.SEVERE, this, "callback exception", e);
    }
    return true;
  }

  private void processJobError(Throwable e) throws Throwable {
    synchronized (myLock) {
      if (e instanceof SQLiteInterruptedException) {
        myState = CANCELLED;
        if (Internal.isFineLogging()) {
          Internal.log(Level.FINE, this, "cancelled", e);
        }
      } else {
        Internal.log(Level.WARNING, this, "job exception", e);
        myError = e;
        myState = ERROR;
        throw e;
      }
    }
  }

  private void finishJob(T result) {
    int state;
    Throwable error;
    synchronized (myLock) {
      myConnection = null;
      if (myState == RUNNING) {
        myState = SUCCEEDED;
        myResult = result;
      }
      state = myState;
      error = myError;
    }
    try {
      if (state == CANCELLED) {
        jobCancelled();
      } else if (state == ERROR) {
        jobError(error);
      }
    } catch (Throwable e) {
      Internal.log(Level.WARNING, this, "callback exception", e);
      if (e instanceof ThreadDeath) throw (ThreadDeath) e;
    }
    try {
      jobFinished(result);
    } catch (Throwable e) {
      Internal.log(Level.WARNING, this, "callback exception", e);
      if (e instanceof ThreadDeath) throw (ThreadDeath) e;
    }
    synchronized (myLock) {
      myQueue = null;
      myLock.notifyAll();
    }
    if (Internal.isFineLogging()) {
      Internal.logFine(this, "finished");
    }
  }
}
