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
package com.cloud.agent.manager;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.utils.Profiler;

public class SynchronousListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(SynchronousListener.class);

    protected Answer[] _answers;
    protected boolean _disconnected;
    protected String _peer;

    public SynchronousListener(Listener listener) {
        _answers = null;
        _peer = null;
    }

    public void setPeer(String peer) {
        _peer = peer;
    }

    public String getPeer() {
        return _peer;
    }

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
    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
        _answers = resp;
        notifyAll();
        return true;
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
        if (s_logger.isTraceEnabled())
            s_logger.trace("Agent disconnected, agent id: " + agentId + ", state: " + state + ". Will notify waiters");

        _disconnected = true;
        notifyAll();
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host agent, StartupCommand cmd, boolean forRebalance) {
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] req) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    public Answer[] waitFor() throws InterruptedException {
        return waitFor(-1);
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

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Synchronized command - sending completed, time: " + profiler.getDurationInMillis() + ", answer: " +
                (_answers != null ? _answers[0].toString() : "null"));
        }
        return _answers;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

}
