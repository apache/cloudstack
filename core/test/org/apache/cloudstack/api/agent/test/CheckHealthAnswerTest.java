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

import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;

public class CheckHealthAnswerTest {
    CheckHealthCommand chc = new CheckHealthCommand();
    CheckHealthAnswer cha = new CheckHealthAnswer(chc, true);

    @Test
    public void testGetResult() {
        boolean r = cha.getResult();
        assertTrue(r);
    }

    @Test
    public void testGetDetails() {
        String d = cha.getDetails();
        boolean r = cha.getResult();
        assertTrue(d.equals("resource is " + (r ? "alive" : "not alive")));
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = cha.executeInSequence();
        assertFalse(b);
    }
}
