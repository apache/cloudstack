/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.log4j;

import org.apache.log4j.helpers.AppenderAttachableImpl;
import org.apache.log4j.spi.LoggingEvent;


/**
 * Obsolete AsyncAppender dispatcher provided for compatibility only.
 *
 * @deprecated Since 1.3.
 */
class Dispatcher extends Thread {
    /**
     * @deprecated
     */
  private org.apache.log4j.helpers.BoundedFIFO bf;
  private AppenderAttachableImpl aai;
  private boolean interrupted = false;
  AsyncAppender container;

    /**
     *
     * @param bf
     * @param container
     * @deprecated
     */
  Dispatcher(org.apache.log4j.helpers.BoundedFIFO bf, AsyncAppender container) {
    this.bf = bf;
    this.container = container;
    this.aai = container.aai;

    // It is the user's responsibility to close appenders before
    // exiting.
    this.setDaemon(true);

    // set the dispatcher priority to lowest possible value
    this.setPriority(Thread.MIN_PRIORITY);
    this.setName("Dispatcher-" + getName());

    // set the dispatcher priority to MIN_PRIORITY plus or minus 2
    // depending on the direction of MIN to MAX_PRIORITY.
    //+ (Thread.MAX_PRIORITY > Thread.MIN_PRIORITY ? 1 : -1)*2);
  }

  void close() {
    synchronized (bf) {
      interrupted = true;

      // We have a waiting dispacther if and only if bf.length is
      // zero.  In that case, we need to give it a death kiss.
      if (bf.length() == 0) {
        bf.notify();
      }
    }
  }

  /**
   * The dispatching strategy is to wait until there are events in the buffer
   * to process. After having processed an event, we release the monitor
   * (variable bf) so that new events can be placed in the buffer, instead of
   * keeping the monitor and processing the remaining events in the buffer.
   *
   * <p>
   * Other approaches might yield better results.
   * </p>
   */
  public void run() {
    //Category cat = Category.getInstance(Dispatcher.class.getName());
    LoggingEvent event;

    while (true) {
      synchronized (bf) {
        if (bf.length() == 0) {
          // Exit loop if interrupted but only if the the buffer is empty.
          if (interrupted) {
            //cat.info("Exiting.");
            break;
          }

          try {
            //LogLog.debug("Waiting for new event to dispatch.");
            bf.wait();
          } catch (InterruptedException e) {
            break;
          }
        }

        event = bf.get();

        if (bf.wasFull()) {
          //LogLog.debug("Notifying AsyncAppender about freed space.");
          bf.notify();
        }
      }

      // synchronized
      synchronized (container.aai) {
        if ((aai != null) && (event != null)) {
          aai.appendLoopOnAppenders(event);
        }
      }
    }

    // while
    // close and remove all appenders
    aai.removeAllAppenders();
  }
}
