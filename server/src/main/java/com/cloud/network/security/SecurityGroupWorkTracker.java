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
package com.cloud.network.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.Answer;

public class SecurityGroupWorkTracker {
    protected Logger logger = LogManager.getLogger(getClass());
    protected AtomicLong _discardCount = new AtomicLong(0);
    AgentManager _agentMgr;
    Listener _answerListener;
    int _bufferLength;

    Map<Long, Integer> _unackedMessages = new ConcurrentHashMap<Long, Integer>();

    public SecurityGroupWorkTracker(AgentManager agentMgr, Listener answerListener, int bufferLength) {
        super();
        assert (bufferLength >= 1) : "SecurityGroupWorkTracker: Cannot have a zero length buffer";
        this._agentMgr = agentMgr;
        this._answerListener = answerListener;
        this._bufferLength = bufferLength;
    }

    public boolean canSend(long agentId) {
        int currLength = 0;
        synchronized (this) {
            Integer outstanding = _unackedMessages.get(agentId);
            if (outstanding == null) {
                outstanding = 0;
                _unackedMessages.put(agentId, outstanding);
            }
            currLength = outstanding.intValue();
            if (currLength + 1 > _bufferLength) {
                long discarded = _discardCount.incrementAndGet();
                //drop it on the floor
                logger.debug("SecurityGroupManager: dropping a message because there are more than " + currLength + " outstanding messages, total dropped=" + discarded);
                return false;
            }
            _unackedMessages.put(agentId, ++currLength);
        }
        return true;
    }

    public void handleException(long agentId) {
        synchronized (this) {
            Integer outstanding = _unackedMessages.get(agentId);
            if (outstanding != null && outstanding != 0) {
                _unackedMessages.put(agentId, --outstanding);
            }
        }
    }

    public void processAnswers(long agentId, long seq, Answer[] answers) {
        synchronized (this) {
            Integer outstanding = _unackedMessages.get(agentId);
            if (outstanding != null && outstanding != 0) {
                _unackedMessages.put(agentId, --outstanding);
            }
        }
    }

    public void processTimeout(long agentId, long seq) {
        synchronized (this) {
            Integer outstanding = _unackedMessages.get(agentId);
            if (outstanding != null && outstanding != 0) {
                _unackedMessages.put(agentId, --outstanding);
            }
        }
    }

    public void processDisconnect(long agentId) {
        synchronized (this) {
            _unackedMessages.put(agentId, 0);
        }
    }

    public void processConnect(long agentId) {
        synchronized (this) {
            _unackedMessages.put(agentId, 0);
        }
    }

    public long getDiscardCount() {
        return _discardCount.get();
    }

    public int getUnackedCount(long agentId) {
        Integer outstanding = _unackedMessages.get(agentId);
        if (outstanding == null) {
            return 0;
        }
        return outstanding.intValue();
    }

}
