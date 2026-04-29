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

package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.UnsupportedAnswer;

public class AnswerTest {
    AgentControlCommand acc = new AgentControlCommand();
    Answer a = new Answer(acc, true, "details");

    @Test
    public void testExecuteInSequence() {
        final boolean b = a.executeInSequence();
        assertFalse(b);
    }

    @Test
    public void testGetResult() {
        final boolean b = a.getResult();
        assertTrue(b);
    }

    @Test
    public void testGetDetails() {
        final String d = a.getDetails();
        assertTrue(d.equals("details"));
    }

    @Test
    public void testCreateUnsupportedCommandAnswer() {
        UnsupportedAnswer usa = Answer.createUnsupportedCommandAnswer(acc);
        boolean b = usa.executeInSequence();
        assertFalse(b);

        b = usa.getResult();
        assertFalse(b);

        String d = usa.getDetails();
        assertTrue(d.contains("Unsupported command issued: " + acc.toString() + ".  Are you sure you got the right type of server?"));

        usa = Answer.createUnsupportedVersionAnswer(acc);
        b = usa.executeInSequence();
        assertFalse(b);

        b = usa.getResult();
        assertFalse(b);

        d = usa.getDetails();
        assertTrue(d.equals("Unsupported Version."));
    }
}
