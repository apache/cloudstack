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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.nuage.vsp.acs.client.NuageVspApiClient;
import net.nuage.vsp.acs.client.NuageVspElementClient;
import net.nuage.vsp.acs.client.NuageVspGuruClient;
import net.nuage.vsp.acs.client.NuageVspSyncClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspAnswer;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspAnswer;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspAnswer;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspAnswer;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspAnswer;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReleaseVmVspAnswer;
import com.cloud.agent.api.guru.ReleaseVmVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspAnswer;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspAnswer;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.sync.SyncVspAnswer;
import com.cloud.agent.api.sync.SyncVspCommand;
import com.cloud.host.Host;

public class NuageVspResourceTest {
    NuageVspResource _resource;
    NuageVspApiClient _mockNuageVspApiClient = mock(NuageVspApiClient.class);
    NuageVspElementClient _mockNuageVspElementClient = mock(NuageVspElementClient.class);
    NuageVspGuruClient _mockNuageVspGuruClient = mock(NuageVspGuruClient.class);
    NuageVspSyncClient _mockNuageVspSyncClient = mock(NuageVspSyncClient.class);
    Map<String, Object> _parameters;

    Answer<Object> genericAnswer = new Answer<Object>() {
        public Object answer(InvocationOnMock invocation) {
            return null;
        }
    };

    @Before
    public void setUp() throws Exception {
        _resource = new NuageVspResource() {

            @Override
            protected void loadNuageClient() {
                _isNuageVspClientLoaded = true;
                _nuageVspApiClient = _mockNuageVspApiClient;
                _nuageVspElementClient = _mockNuageVspElementClient;
                _nuageVspGuruClient = _mockNuageVspGuruClient;
                _nuageVspSyncClient = _mockNuageVspSyncClient;

            }

            protected void isNuageVspApiLoaded() throws Exception {
            }

            protected void isNuageVspGuruLoaded() throws Exception {
            }

            protected void isNuageVspElementLoaded() throws Exception {
            }

            protected void isNuageVspSyncLoaded() throws Exception {
            }

            protected void login() throws Exception {
            }

        };

        _parameters = new HashMap<String, Object>();
        _parameters.put("name", "nuagevsptestdevice");
        _parameters.put("guid", "aaaaa-bbbbb-ccccc");
        _parameters.put("zoneId", "blublub");
        _parameters.put("hostname", "nuagevsd");
        _parameters.put("cmsuser", "cmsuser");
        _parameters.put("cmsuserpass", "cmsuserpass");
        _parameters.put("port", "8443");
        _parameters.put("apirelativepath", "nuage/api/v1_0");
        _parameters.put("retrycount", "3");
        _parameters.put("retryinterval", "3");
    }

    @Test(expected = Exception.class)
    public void resourceConfigureFailure() throws Exception {
        _resource.configure("NuageVspResource", Collections.<String, Object> emptyMap());
    }

    @Test
    public void resourceConfigure() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        assertTrue("nuagevsptestdevice".equals(_resource.getName()));
        assertTrue(_resource.getType() == Host.Type.L2Networking);
    }

    @Test
    public void testInitialization() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("nuagevsptestdevice".equals(sc[0].getName()));
        assertTrue("blublub".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatus() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        PingCommand ping = _resource.getCurrentStatus(42);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 42);
        assertTrue(ping.getHostType() == Host.Type.L2Networking);
    }

    @Test
    public void testImplementNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ImplementNetworkVspCommand impNtwkCmd = new ImplementNetworkVspCommand("networkDomainName", "networkDomainPath", "networkDomainUuid", "networkAccountName",
                "networkAccountUuid", "networkName", "networkCidr", "networkGateway", "networkUuid", true, "vpcName", "vpcUuid", true, new ArrayList<String>());
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).implement("networkDomainName", "networkDomainPath", "networkDomainUuid", "networkAccountName", "networkAccountUuid",
                "networkName", "networkCidr", "networkGateway", "networkUuid", true, "vpcName", "vpcUuid", true, new ArrayList<String>());
        ImplementNetworkVspAnswer implNtwkAns = (ImplementNetworkVspAnswer)_resource.executeRequest(impNtwkCmd);
        assertTrue(implNtwkAns.getResult());
    }

    @Test
    public void testReserveVmInterfaceVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ReserveVmInterfaceVspCommand rsrvVmInfCmd = new ReserveVmInterfaceVspCommand("nicUuid", "nicMacAddress", "networkUuid", true, "vpcUuid", "networkDomainUuid",
                "networksAccountUuid", false, "domainRouterIp", "vmInstanceName", "vmUuid", "vmUserName", "vmUserDomainName");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).reserve("nicUuid", "nicMacAddress", "networkUuid", true, "vpcUuid", "networkDomainUuid", "networksAccountUuid",
                false, "domainRouterIp", "vmInstanceName", "vmUuid");
        ReserveVmInterfaceVspAnswer rsrvVmInfAns = (ReserveVmInterfaceVspAnswer)_resource.executeRequest(rsrvVmInfCmd);
        assertTrue(rsrvVmInfAns.getResult());
    }

    @Test
    public void testReleaseVmVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ReleaseVmVspCommand releaseVmCmd = new ReleaseVmVspCommand("networkUuid", "vmUuid", "vmInstanceName");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).release("networkUuid", "vmUuid", "vmInstanceName");
        ReleaseVmVspAnswer releaseVmAns = (ReleaseVmVspAnswer)_resource.executeRequest(releaseVmCmd);
        assertTrue(releaseVmAns.getResult());
    }

    @Test
    public void testDeallocateVmVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        DeallocateVmVspCommand dellocateVmCmd = new DeallocateVmVspCommand("networkUuid", "nicFrmDdUuid", "nicMacAddress", "nicIp4Address", true, "vpcUuid", "networksDomainUuid",
                "vmInstanceName", "vmUuid");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).deallocate("networkUuid", "nicFrmDdUuid", "nicMacAddress", "nicIp4Address", true, "vpcUuid", "networksDomainUuid",
                "vmInstanceName", "vmUuid");
        DeallocateVmVspAnswer dellocateVmAns = (DeallocateVmVspAnswer)_resource.executeRequest(dellocateVmCmd);
        assertTrue(dellocateVmAns.getResult());
    }

    @Test
    public void testTrashNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        TrashNetworkVspCommand trashNtwkCmd = new TrashNetworkVspCommand("domainUuid", "networkUuid", true, "vpcUuid");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).trash("domainUuid", "networkUuid", true, "vpcUuid");
        TrashNetworkVspAnswer trashNtwkAns = (TrashNetworkVspAnswer)_resource.executeRequest(trashNtwkCmd);
        assertTrue(trashNtwkAns.getResult());
    }

    @Test
    public void testApplyStaticNatVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ApplyStaticNatVspCommand applyNatCmd = new ApplyStaticNatVspCommand("networkDomainUuid", "vpcOrSubnetUuid", true, new ArrayList<Map<String, Object>>());
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyStaticNats("networkDomainUuid", "vpcOrSubnetUuid", true, new ArrayList<Map<String, Object>>());
        ApplyStaticNatVspAnswer applyNatAns = (ApplyStaticNatVspAnswer)_resource.executeRequest(applyNatCmd);
        assertTrue(applyNatAns.getResult());
    }

    @Test
    public void testApplyAclRuleVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ApplyAclRuleVspCommand applyAclCmd = new ApplyAclRuleVspCommand("networkUuid", "networkDomainUuid", "vpcOrSubnetUuid", true, new ArrayList<Map<String, Object>>(), false,
                100);
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyAclRules("networkUuid", "networkDomainUuid", "vpcOrSubnetUuid", true, new ArrayList<Map<String, Object>>(),
                false, 100);
        ApplyAclRuleVspAnswer applyAclAns = (ApplyAclRuleVspAnswer)_resource.executeRequest(applyAclCmd);
        assertTrue(applyAclAns.getResult());
    }

    @Test
    public void testShutDownVpcVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        ShutDownVpcVspCommand shutVpcCmd = new ShutDownVpcVspCommand("domainUuid", "vpcUuid");
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).shutDownVpc("domainUuid", "vpcUuid");
        ShutDownVpcVspAnswer shutVpcAns = (ShutDownVpcVspAnswer)_resource.executeRequest(shutVpcCmd);
        assertTrue(shutVpcAns.getResult());
    }

    @Test
    public void testSyncVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _parameters);

        SyncVspCommand shutVpcCmd = new SyncVspCommand("nuageVspEntity");
        doAnswer(genericAnswer).when(_mockNuageVspSyncClient).syncWithNuageVsp("nuageVspEntity");
        SyncVspAnswer shutVpcAns = (SyncVspAnswer)_resource.executeRequest(shutVpcCmd);
        assertTrue(shutVpcAns.getResult());
    }
}
