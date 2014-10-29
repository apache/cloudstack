//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package com.cloud.utils.concurrency;

import org.apache.log4j.Logger;

public class SynchronizationEvent {
    protected final static Logger s_logger = Logger.getLogger(SynchronizationEvent.class);

    private boolean signalled;

    public SynchronizationEvent() {
        signalled = false;
    }

    public SynchronizationEvent(boolean signalled) {
        this.signalled = signalled;
    }

    public void setEvent() {
        synchronized (this) {
            signalled = true;
            notifyAll();
        }
    }

    public void resetEvent() {
        synchronized (this) {
            signalled = false;
        }
    }

    public boolean waitEvent() throws InterruptedException {
        synchronized (this) {
            if (signalled)
                return true;

            while (true) {
                try {
                    wait();
                    assert (signalled);
                    return signalled;
                } catch (InterruptedException e) {
                    s_logger.debug("unexpected awaken signal in wait()");
                    throw e;
                }
            }
        }
    }

    public boolean waitEvent(long timeOutMiliseconds) throws InterruptedException {
        synchronized (this) {
            if (signalled)
                return true;

            try {
                wait(timeOutMiliseconds);
                return signalled;
            } catch (InterruptedException e) {
                // TODO, we don't honor time out semantics when the waiting thread is interrupted
                s_logger.debug("unexpected awaken signal in wait(...)");
                throw e;
            }
        }
    }

    public boolean isSignalled() {
        synchronized (this) {
            return signalled;
        }
    }
}
