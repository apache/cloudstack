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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.VifAttachment;

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

    @Test
    public void testDeleteLogicalSwitchCommandWrapper() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);

        final String logicalSwitchUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";

        final DeleteLogicalSwitchCommand command = new DeleteLogicalSwitchCommand(logicalSwitchUuid);

        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            doNothing().when(niciraNvpApi).deleteLogicalSwitch(command.getLogicalSwitchUuid());
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testConfigurePublicIpsOnLogicalRouterCommand() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);
        final LogicalRouterPort port1 = Mockito.mock(LogicalRouterPort.class);

        final List<LogicalRouterPort> listPorts = new ArrayList<LogicalRouterPort>();
        listPorts.add(port1);

        final String logicalRouterUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String l3GatewayServiceUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final List<String> publicCidrs = new ArrayList<String>();
        publicCidrs.add("10.1.1.0/24");

        final ConfigurePublicIpsOnLogicalRouterCommand command = new ConfigurePublicIpsOnLogicalRouterCommand(logicalRouterUuid, l3GatewayServiceUuid, publicCidrs);

        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            when(niciraNvpApi.findLogicalRouterPortByGatewayServiceUuid(command.getLogicalRouterUuid(), command.getL3GatewayServiceUuid())).thenReturn(listPorts);
            doNothing().when(niciraNvpApi).updateLogicalRouterPort(command.getLogicalRouterUuid(), port1);
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteLogicalSwitchPortCommand() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);

        final String logicalSwitchUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String logicalSwitchPortUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";

        final DeleteLogicalSwitchPortCommand command = new DeleteLogicalSwitchPortCommand(logicalSwitchUuid, logicalSwitchPortUuid);

        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            doNothing().when(niciraNvpApi).deleteLogicalSwitchPort(command.getLogicalSwitchUuid(), command.getLogicalSwitchPortUuid());
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteLogicalRouterCommand() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);

        final String logicalRouterUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";

        final DeleteLogicalRouterCommand command = new DeleteLogicalRouterCommand(logicalRouterUuid);

        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            doNothing().when(niciraNvpApi).deleteLogicalRouter(command.getLogicalRouterUuid());
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }

    @Test
    public void testUpdateLogicalSwitchPortCommand() {
        final NiciraNvpApi niciraNvpApi = Mockito.mock(NiciraNvpApi.class);
        final NiciraNvpUtilities niciraNvpUtilities = Mockito.mock(NiciraNvpUtilities.class);
        final VifAttachment vifAttachment = Mockito.mock(VifAttachment.class);

        final String logicalSwitchPortUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String logicalSwitchUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String attachmentUuid = "d2e05a9e-7120-4487-a5fc-414ab36d9345";
        final String ownerName = "admin";
        final String nicName = "eth0";

        final UpdateLogicalSwitchPortCommand command = new UpdateLogicalSwitchPortCommand(logicalSwitchPortUuid, logicalSwitchUuid, attachmentUuid, ownerName, nicName);

        when(niciraNvpResource.getNiciraNvpUtilities()).thenReturn(niciraNvpUtilities);
        when(niciraNvpResource.getNiciraNvpApi()).thenReturn(niciraNvpApi);

        try {
            when(niciraNvpUtilities.createVifAttachment(attachmentUuid)).thenReturn(vifAttachment);
            doNothing().when(niciraNvpApi).updateLogicalSwitchPortAttachment(logicalSwitchUuid, logicalSwitchPortUuid, vifAttachment);
        } catch (final NiciraNvpApiException e) {
            fail(e.getMessage());
        }

        final NiciraNvpRequestWrapper wrapper = NiciraNvpRequestWrapper.getInstance();
        assertNotNull(wrapper);

        final Answer answer = wrapper.execute(command, niciraNvpResource);

        assertTrue(answer.getResult());
    }
}
