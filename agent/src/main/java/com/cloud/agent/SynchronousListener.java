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
package com.cloud.agent;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Blocks the thread and waits for the answer.
 *
 * @author mprokopchuk
 */
public class SynchronousListener implements ServerListener {
    private static final Logger logger = LogManager.getLogger(SynchronousListener.class);

    protected Answer[] _answers;
    protected boolean _disconnected;
    private int _timeout;

    public synchronized Answer[] getAnswers() {
        return _answers;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    public synchronized boolean isDisconnected() {
        return _disconnected;
    }

    @Override
    public synchronized boolean processAnswers(long seq, Answer[] resp) {
        _answers = resp;
        notifyAll();
        return true;
    }

    @Override
    public boolean processCommands(long seq, Command[] commands) {
        return false;
    }

    @Override
    public synchronized boolean processDisconnect() {
        logger.trace("Server disconnected. Will notify waiters");
        _disconnected = true;
        notifyAll();
        return true;
    }

    public synchronized Answer[] waitFor(int s) throws InterruptedException {
        if (_disconnected) {
            return null;
        }

        if (_answers != null) {
            return _answers;
        }

        Profiler profiler = new Profiler();
        profiler.start();
        if (s <= 0) {
            wait();
        } else {
            int ms = s * 1000;
            wait(ms);
        }
        profiler.stop();

        logger.trace("Synchronized command - sending completed, time: {}, answer: {}",
                profiler.getDurationInMillis(), _answers != null ? _answers[0].toString() : "null");
        return _answers;
    }

    @Override
    public int getTimeout() {
        return _timeout;
    }

    public void setTimeout(int timeout) {
        _timeout = timeout;
    }
}
