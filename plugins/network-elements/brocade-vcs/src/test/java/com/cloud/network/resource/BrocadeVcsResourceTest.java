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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.agent.api.AssociateMacToNetworkAnswer;
import com.cloud.agent.api.AssociateMacToNetworkCommand;
import com.cloud.agent.api.CreateNetworkAnswer;
import com.cloud.agent.api.CreateNetworkCommand;
import com.cloud.agent.api.DeleteNetworkAnswer;
import com.cloud.agent.api.DeleteNetworkCommand;
import com.cloud.agent.api.DisassociateMacFromNetworkAnswer;
import com.cloud.agent.api.DisassociateMacFromNetworkCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.network.brocade.BrocadeVcsApi;
import com.cloud.network.brocade.BrocadeVcsApiException;
import com.cloud.network.schema.showvcs.Output;
import com.cloud.network.schema.showvcs.VcsNodeInfo;
import com.cloud.network.schema.showvcs.VcsNodes;

public class BrocadeVcsResourceTest {

    private static final long NETWORK_ID = 42L;
    private static final int VLAN_ID = 14;
    private static final String MAC_ADDRESS_32 = "0050.56bf.0002";
    private static final String MAC_ADDRESS_64 = "00:50:56:bf:00:02";
    BrocadeVcsApi api = mock(BrocadeVcsApi.class);
    BrocadeVcsResource resource;
    Map<String, Object> parameters;

    @Before
    public void setUp() throws ConfigurationException {
        resource = new BrocadeVcsResource() {
            @Override
            protected BrocadeVcsApi createBrocadeVcsApi(String ip, String username, String password) {
                return api;
            }
        };

        parameters = new HashMap<String, Object>();
        parameters.put("name", "vcstestdevice");
        parameters.put("ip", "127.0.0.1");
        parameters.put("adminuser", "adminuser");
        parameters.put("guid", "aaaaa-bbbbb-ccccc");
        parameters.put("zoneId", "blublub");
        parameters.put("adminpass", "adminpass");
    }

    @Test(expected = ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException {
        resource.configure("BrocadeVcsResource", Collections.<String, Object> emptyMap());
    }

    @Test
    public void resourceConfigure() throws ConfigurationException {
        resource.configure("BrocadeVcsResource", parameters);
        assertTrue("Incorrect resource name", "vcstestdevice".equals(resource.getName()));

        assertTrue("Incorrect resource type", resource.getType() == Host.Type.L2Networking);
    }

    @Test
    public void testInitialization() throws ConfigurationException {
        resource.configure("BrocadeVcsResource", parameters);

        final StartupCommand[] sc = resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("Incorrect startup command GUID", "aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("Incorrect Brocade device name", "vcstestdevice".equals(sc[0].getName()));
        assertTrue("Incorrect Data Center", "blublub".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatusOk() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        final VcsNodeInfo nodeInfo = mock(VcsNodeInfo.class);
        when(nodeInfo.getNodeState()).thenReturn("Online");

        List<VcsNodeInfo> nodes = new ArrayList<VcsNodeInfo>();
        nodes.add(nodeInfo);

        final VcsNodes vcsNodes = mock(VcsNodes.class);
        final Output output = mock(Output.class);
        when(output.getVcsNodes()).thenReturn(vcsNodes);
        when(vcsNodes.getVcsNodeInfo()).thenReturn(nodes);
        when(api.getSwitchStatus()).thenReturn(output);

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 42);
        assertTrue(ping.getHostType() == Host.Type.L2Networking);

    }

    @Test
    public void testPingCommandStatusFail() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        final VcsNodeInfo nodeInfo = mock(VcsNodeInfo.class);
        when(nodeInfo.getNodeState()).thenReturn("Offline");

        List<VcsNodeInfo> nodes = new ArrayList<VcsNodeInfo>();
        nodes.add(nodeInfo);

        final VcsNodes vcsNodes = mock(VcsNodes.class);
        final Output output = mock(Output.class);
        when(output.getVcsNodes()).thenReturn(vcsNodes);
        when(vcsNodes.getVcsNodeInfo()).thenReturn(nodes);
        when(api.getSwitchStatus()).thenReturn(output);

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testPingCommandStatusApiException() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);
        when(api.getSwitchStatus()).thenThrow(new BrocadeVcsApiException());

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testRetries() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.createNetwork(VLAN_ID, NETWORK_ID)).thenThrow(new BrocadeVcsApiException()).thenThrow(new BrocadeVcsApiException()).thenReturn(true);

        final CreateNetworkCommand cmd = new CreateNetworkCommand(VLAN_ID, NETWORK_ID, "owner");
        final CreateNetworkAnswer answer = (CreateNetworkAnswer)resource.executeRequest(cmd);
        assertTrue(answer.getResult());

    }

    @Test
    public void testCreateNetworkApi() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.createNetwork(VLAN_ID, NETWORK_ID)).thenReturn(true);

        final CreateNetworkCommand cmd = new CreateNetworkCommand(VLAN_ID, NETWORK_ID, "owner");
        final CreateNetworkAnswer answer = (CreateNetworkAnswer)resource.executeRequest(cmd);
        //verify(api).createNetwork(VLAN_ID, NETWORK_ID);
        assertTrue(answer.getResult());

    }

    @Test
    public void testCreateNetworkApiException() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.createNetwork(VLAN_ID, NETWORK_ID)).thenThrow(new BrocadeVcsApiException());

        final CreateNetworkCommand cmd = new CreateNetworkCommand(VLAN_ID, NETWORK_ID, "owner");
        final CreateNetworkAnswer answer = (CreateNetworkAnswer)resource.executeRequest(cmd);
        assertFalse(answer.getResult());

    }

    @Test
    public void testDeleteNetworkApi() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.deleteNetwork(VLAN_ID, NETWORK_ID)).thenReturn(true);

        final DeleteNetworkCommand cmd = new DeleteNetworkCommand(VLAN_ID, NETWORK_ID);
        final DeleteNetworkAnswer answer = (DeleteNetworkAnswer)resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDeleteNetworkApiException() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.deleteNetwork(VLAN_ID, NETWORK_ID)).thenThrow(new BrocadeVcsApiException());

        final DeleteNetworkCommand cmd = new DeleteNetworkCommand(VLAN_ID, NETWORK_ID);
        final DeleteNetworkAnswer answer = (DeleteNetworkAnswer)resource.executeRequest(cmd);
        assertFalse(answer.getResult());
    }

    @Test
    public void testAssociateMacToNetworkApi() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.associateMacToNetwork(NETWORK_ID, MAC_ADDRESS_32)).thenReturn(true);

        final AssociateMacToNetworkCommand cmd = new AssociateMacToNetworkCommand(NETWORK_ID, MAC_ADDRESS_64, "owner");
        final AssociateMacToNetworkAnswer answer = (AssociateMacToNetworkAnswer)resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testAssociateMacToNetworkApiException() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.associateMacToNetwork(NETWORK_ID, MAC_ADDRESS_32)).thenThrow(new BrocadeVcsApiException());

        final AssociateMacToNetworkCommand cmd = new AssociateMacToNetworkCommand(NETWORK_ID, MAC_ADDRESS_64, "owner");
        final AssociateMacToNetworkAnswer answer = (AssociateMacToNetworkAnswer)resource.executeRequest(cmd);
        assertFalse(answer.getResult());
    }

    @Test
    public void testDisassociateMacFromNetworkApi() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.disassociateMacFromNetwork(NETWORK_ID, MAC_ADDRESS_32)).thenReturn(true);

        final DisassociateMacFromNetworkCommand cmd = new DisassociateMacFromNetworkCommand(NETWORK_ID, MAC_ADDRESS_64);
        final DisassociateMacFromNetworkAnswer answer = (DisassociateMacFromNetworkAnswer)resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testDisassociateMacFromNetworkApiException() throws ConfigurationException, BrocadeVcsApiException {
        resource.configure("BrocadeVcsResource", parameters);

        when(api.disassociateMacFromNetwork(NETWORK_ID, MAC_ADDRESS_32)).thenThrow(new BrocadeVcsApiException());

        final DisassociateMacFromNetworkCommand cmd = new DisassociateMacFromNetworkCommand(NETWORK_ID, MAC_ADDRESS_64);
        final DisassociateMacFromNetworkAnswer answer = (DisassociateMacFromNetworkAnswer)resource.executeRequest(cmd);
        assertFalse(answer.getResult());
    }
}
