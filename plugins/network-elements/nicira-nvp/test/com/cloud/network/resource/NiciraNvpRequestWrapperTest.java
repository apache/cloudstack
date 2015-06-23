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

package com.cloud.network.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;

public class NiciraNvpRequestWrapperTest {

    @Mock
    private final NiciraNvpResource niciraNvpResource = Mockito.mock(NiciraNvpResource.class);

    @Test
    public void testReadyCommandWrapper() {
        final ReadyCommand command = new ReadyCommand();

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testMaintainCommandWrapper() {
        final MaintainCommand command = new MaintainCommand();

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testCreateLogicalSwitchCommandWrapper() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);
        final NiciraNvpUtilities niciraNvpUtilities = Mockito.mock(NiciraNvpUtilities.class);
        final LogicalSwitch logicalSwitch = Mockito.mock(LogicalSwitch.class);

        final String transportUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String transportType = "stt";
        final String name = "logicalswitch";
        final String ownerName = "owner";

        final CreateLogicalSwitchCommand command = new CreateLogicalSwitchCommand(transportUuid, transportType, name, ownerName);

        final String truncated = "lswitch-" + command.getName();

        when(niciraNvpResource.getNiciraNvpUtilities()).thenReturn(niciraNvpUtilities);
        when(niciraNvpUtilities.createLogicalSwitch()).thenReturn(logicalSwitch);
        when(niciraNvpResource.truncate("lswitch-" + command.getName(), NiciraNvpResource.NAME_MAX_LEN)).thenReturn(truncated);
        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            when(niciraNvpApi.createLogicalSwitch(logicalSwitch)).thenReturn(logicalSwitch);
            when(logicalSwitch.getUuid()).thenReturn(transportUuid);
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }
}