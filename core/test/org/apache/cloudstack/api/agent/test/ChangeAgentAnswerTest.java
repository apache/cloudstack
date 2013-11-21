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
package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.agent.api.ChangeAgentAnswer;
import com.cloud.agent.api.ChangeAgentCommand;
import com.cloud.host.Status.Event;

public class ChangeAgentAnswerTest {
    ChangeAgentCommand cac = new ChangeAgentCommand(123456789L, Event.AgentConnected);
    ChangeAgentAnswer caa = new ChangeAgentAnswer(cac, true);

    @Test
    public void testGetResult() {
        boolean b = caa.getResult();
        assertTrue(b);
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = caa.executeInSequence();
        assertFalse(b);
    }
}
