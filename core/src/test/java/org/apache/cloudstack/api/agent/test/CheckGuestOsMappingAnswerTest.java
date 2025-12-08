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

import com.cloud.agent.api.CheckGuestOsMappingAnswer;
import com.cloud.agent.api.CheckGuestOsMappingCommand;
import org.junit.Assert;
import org.junit.Test;

import org.mockito.Mockito;

public class CheckGuestOsMappingAnswerTest {
    CheckGuestOsMappingCommand cmd = new CheckGuestOsMappingCommand("CentOS 7.2", "centos64Guest", "6.0");
    CheckGuestOsMappingAnswer answer = new CheckGuestOsMappingAnswer(cmd);

    @Test
    public void testGetResult() {
        boolean b = answer.getResult();
        assertTrue(b);
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = answer.executeInSequence();
        assertFalse(b);
    }

    @Test
    public void testGuestOsMappingAnswerDetails() {
        CheckGuestOsMappingCommand cmd = new CheckGuestOsMappingCommand("CentOS 7.2", "centos64Guest", "6.0");
        CheckGuestOsMappingAnswer answer = new CheckGuestOsMappingAnswer(cmd, "details");
        String details = answer.getDetails();
        Assert.assertEquals("details", details);
    }

    @Test
    public void testGuestOsMappingAnswerFailure() {
        Throwable th = Mockito.mock(Throwable.class);
        Mockito.when(th.getMessage()).thenReturn("Failure");
        CheckGuestOsMappingCommand cmd = new CheckGuestOsMappingCommand("CentOS 7.2", "centos64Guest", "6.0");
        CheckGuestOsMappingAnswer answer = new CheckGuestOsMappingAnswer(cmd, th);
        assertFalse(answer.getResult());
        Assert.assertEquals("Failure", answer.getDetails());

    }
}
