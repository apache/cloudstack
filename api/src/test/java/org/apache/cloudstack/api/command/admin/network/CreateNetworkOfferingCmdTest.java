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

package org.apache.cloudstack.api.command.admin.network;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class CreateNetworkOfferingCmdTest {

    @Test
    public void testEgressDefaultPolicyIsAllowWhenParameterOmitted() {
        CreateNetworkOfferingCmd cmd = new CreateNetworkOfferingCmd();
        assertEquals("createNetworkOffering must default to egress Allow when egressdefaultpolicy is not specified",
                Boolean.TRUE, cmd.getEgressDefaultPolicy());
    }

    @Test
    public void testEgressDefaultPolicyExplicitDenyIsHonored() {
        CreateNetworkOfferingCmd cmd = new CreateNetworkOfferingCmd();
        ReflectionTestUtils.setField(cmd, "egressDefaultPolicy", Boolean.FALSE);
        assertEquals(Boolean.FALSE, cmd.getEgressDefaultPolicy());
    }

    @Test
    public void testEgressDefaultPolicyExplicitAllowIsHonored() {
        CreateNetworkOfferingCmd cmd = new CreateNetworkOfferingCmd();
        ReflectionTestUtils.setField(cmd, "egressDefaultPolicy", Boolean.TRUE);
        assertEquals(Boolean.TRUE, cmd.getEgressDefaultPolicy());
    }
}
