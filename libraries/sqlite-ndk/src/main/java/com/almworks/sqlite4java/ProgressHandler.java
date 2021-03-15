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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

class ProgressHandler {
  public static final ProgressHandler DISPOSED = new ProgressHandler();

  private static final int OFFSET_STEPCOUNT = 1;
  private static final int OFFSET_CANCEL = 0;

  private final int myStepsPerCallback;

  private SWIGTYPE_p_direct_buffer myPointer;
  private ByteBuffer myBuffer;
  private LongBuffer myLongs;

  public ProgressHandler(SWIGTYPE_p_direct_buffer pointer, ByteBuffer buffer, int stepsPerCallback) {
    myStepsPerCallback = stepsPerCallback;
    assert buffer.isDirect();
    assert buffer.capacity() == 16 : buffer.capacity();
    myPointer = pointer;
    myBuffer = buffer;
    myLongs = buffer.order(ByteOrder.nativeOrder()).asLongBuffer();
    assert myLongs.capacity() == 2;
  }

  private ProgressHandler() {
    myStepsPerCallback = 0;
  }

  public synchronized SWIGTYPE_p_direct_buffer dispose() {
    SWIGTYPE_p_direct_buffer ptr = myPointer;
    myBuffer = null;
    myPointer = null;
    myLongs = null;
    return ptr;
  }

  public synchronized void reset() {
    if (myLongs == null)
      return;
    myLongs.put(OFFSET_CANCEL, 0L);
    myLongs.put(OFFSET_STEPCOUNT, 0L);
  }

  public synchronized void cancel() {
    if (myLongs == null)
      return;
    myLongs.put(OFFSET_CANCEL, 1L);
  }

  public synchronized long getSteps() {
    if (myLongs == null)
      return -1;
    return myLongs.get(OFFSET_STEPCOUNT) * myStepsPerCallback;
  }
}
