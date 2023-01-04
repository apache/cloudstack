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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.cloud.agent.api.CheckGuestOsMappingCommand;
import org.junit.Test;

import com.cloud.agent.api.AgentControlCommand;

public class CheckGuestOsMappingCommandTest {

    @Test
    public void testExecuteInSequence() {
        CheckGuestOsMappingCommand cmd = new CheckGuestOsMappingCommand();
        boolean b = cmd.executeInSequence();
        assertFalse(b);
    }

    @Test
    public void testCommandParams() {
        CheckGuestOsMappingCommand cmd = new CheckGuestOsMappingCommand("CentOS 7.2", "centos64Guest", "6.0");
        assertEquals("CentOS 7.2", cmd.getGuestOsName());
        assertEquals("centos64Guest", cmd.getGuestOsHypervisorMappingName());
        assertEquals("6.0", cmd.getHypervisorVersion());
    }
}