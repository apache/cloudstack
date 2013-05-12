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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.CheckNetworkAnswer;
import com.cloud.agent.api.CheckNetworkCommand;

public class CheckNetworkAnswerTest {
    CheckNetworkCommand cnc;
    CheckNetworkAnswer cna;

    @Before
    public void setUp() {
        cnc = Mockito.mock(CheckNetworkCommand.class);
        cna = new CheckNetworkAnswer(cnc, true, "details", true);
    }

    @Test
    public void testGetResult() {
        boolean b = cna.getResult();
        assertTrue(b);
    }

    @Test
    public void testGetDetails() {
        String d = cna.getDetails();
        assertTrue(d.equals("details"));
    }

    @Test
    public void testNeedReconnect() {
        boolean b = cna.needReconnect();
        assertTrue(b);
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = cna.executeInSequence();
        assertFalse(b);
    }
}
