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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.transport.Request;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Status;

public class AgentAttacheSendNextTest {

    /**
     * Minimal concrete AgentAttache: send() fails for one designated sequence and succeeds otherwise,
     * recording the sequence that was actually dispatched.
     */
    static class TestAgentAttache extends AgentAttache {
        Long sentSeq;
        final long failSeq;

        TestAgentAttache(long failSeq) {
            super(null, 1L, "uuid-1", "host-1", false);
            this.failSeq = failSeq;
        }

        @Override
        public void send(Request req) throws AgentUnavailableException {
            if (req.getSequence() == failSeq) {
                throw new AgentUnavailableException("simulated transient link failure", _id);
            }
            sentSeq = req.getSequence();
        }

        @Override
        public void disconnect(Status state) {
        }

        @Override
        protected boolean isClosed() {
            return false;
        }
    }

    @Test
    public void sendNextAdvancesPastAFailedCommandToTheNextQueued() {
        long failSeq = 100L;
        long goodSeq = 200L;

        Request failing = Mockito.mock(Request.class);
        Mockito.when(failing.getSequence()).thenReturn(failSeq);
        Request good = Mockito.mock(Request.class);
        Mockito.when(good.getSequence()).thenReturn(goodSeq);

        TestAgentAttache attache = new TestAgentAttache(failSeq);
        attache._requests.add(failing);
        attache._requests.add(good);

        attache.sendNext(1L);

        // A command whose send() failed (and was cancelled) must NOT become _currentSequence: no answer
        // will ever arrive for it, so every later in-sequence command to this host would queue behind it
        // and time out. sendNext must move on and dispatch the next queued command instead.
        Assert.assertEquals("the next queued command should have been dispatched", Long.valueOf(goodSeq), attache.sentSeq);
        Assert.assertEquals("current sequence must be the successfully sent command, not the failed one",
                Long.valueOf(goodSeq), attache._currentSequence);
        Assert.assertTrue("the request queue should be drained", attache._requests.isEmpty());
    }
}
