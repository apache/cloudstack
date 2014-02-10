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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.agent.api.CheckNetworkCommand;
import com.cloud.network.PhysicalNetworkSetupInfo;

public class CheckNetworkCommandTest {
    CheckNetworkCommand cnc;

    @Before
    public void setUp() {
        @SuppressWarnings("unchecked")
        List<PhysicalNetworkSetupInfo> net = Mockito.mock(List.class);
        cnc = new CheckNetworkCommand(net);
    }

    @Test
    public void testGetPhysicalNetworkInfoList() {
        List<PhysicalNetworkSetupInfo> networkInfoList = cnc.getPhysicalNetworkInfoList();
        assertEquals(0, networkInfoList.size());
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = cnc.executeInSequence();
        assertTrue(b);
    }
}
