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

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.element.ApplyAclRuleVspCommand;
import com.cloud.agent.api.element.ApplyStaticNatVspCommand;
import com.cloud.agent.api.element.ShutDownVpcVspCommand;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.sync.SyncVspCommand;
import com.cloud.host.Host;
import com.google.common.collect.Maps;
import net.nuage.vsp.acs.client.NuageVspApiClient;
import net.nuage.vsp.acs.client.NuageVspElementClient;
import net.nuage.vsp.acs.client.NuageVspGuruClient;
import net.nuage.vsp.acs.client.NuageVspSyncClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class NuageVspResourceTest {
    NuageVspResource _resource;
    NuageVspApiClient _mockNuageVspApiClient = mock(NuageVspApiClient.class);
    NuageVspElementClient _mockNuageVspElementClient = mock(NuageVspElementClient.class);
    NuageVspGuruClient _mockNuageVspGuruClient = mock(NuageVspGuruClient.class);
    NuageVspSyncClient _mockNuageVspSyncClient = mock(NuageVspSyncClient.class);
    NuageVspResource.Configuration _resourceConfiguration;
    Map<String, Object> _hostDetails;

    org.mockito.stubbing.Answer<Object> genericAnswer = new org.mockito.stubbing.Answer<Object>() {
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

            protected void isNuageVspApiLoaded() throws ConfigurationException {
            }

            protected void isNuageVspGuruLoaded() throws ConfigurationException {
            }

            protected void isNuageVspElementLoaded() throws ConfigurationException {
            }

            protected void isNuageVspSyncLoaded() throws ConfigurationException {
            }

            protected void login() throws ConfigurationException {
            }

        };

        _resourceConfiguration = new NuageVspResource.Configuration()
                .name("nuagevsptestdevice")
                .guid("aaaaa-bbbbb-ccccc")
                .zoneId("blublub")
                .hostName("nuagevsd")
                .cmsUser("cmsuser")
                .cmsUserPassword("cmsuserpass")
                .port("8443")
                .apiVersion("v3_2")
                .apiRelativePath("nuage/api/v3_2")
                .retryCount("3")
                .retryInterval("3");
        _hostDetails = Maps.<String, Object>newHashMap(_resourceConfiguration.build());
    }

    @Test(expected = Exception.class)
    public void resourceConfigureFailure() throws Exception {
        _resource.configure("NuageVspResource", Collections.<String, Object> emptyMap());
    }

    @Test
    public void resourceConfigure() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        assertTrue("nuagevsptestdevice".equals(_resource.getName()));
        assertTrue(_resource.getType() == Host.Type.L2Networking);
    }

    @Test
    public void testInitialization() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        StartupCommand[] sc = _resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("nuagevsptestdevice".equals(sc[0].getName()));
        assertTrue("blublub".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatus() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        PingCommand ping = _resource.getCurrentStatus(42);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 42);
        assertTrue(ping.getHostType() == Host.Type.L2Networking);
    }

    @Test
    public void testImplementNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ImplementNetworkVspCommand.Builder cmdBuilder = new ImplementNetworkVspCommand.Builder().networkDomainName("networkDomainName").networkDomainPath("networkDomainPath")
                .networkDomainUuid("networkDomainUuid").networkAccountName("networkAccountName").networkAccountUuid("networkAccountUuid").networkName("networkName")
                .networkCidr("networkCidr").networkGateway("networkGateway").networkAclId(0L).dnsServers(new ArrayList<String>()).gatewaySystemIds(new ArrayList<String>())
                .networkUuid("networkUuid").isL3Network(true).isVpc(true).isSharedNetwork(true).vpcName("vpcName").vpcUuid("vpcUuid").defaultEgressPolicy(true)
                .ipAddressRange(new ArrayList<String[]>()).domainTemplateName("domainTemplateName");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).implement("networkDomainName", "networkDomainPath", "networkDomainUuid", "networkAccountName",
                "networkAccountUuid", "networkName", "networkCidr", "networkGateway", 0L, new ArrayList<String>(), new ArrayList<String>(), true, true, true, "networkUuid",
                "vpcName", "vpcUuid", true, new ArrayList<String[]>(), "domainTemplateName");
        com.cloud.agent.api.Answer implNtwkAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(implNtwkAns.getResult());
    }

    @Test
    public void testReserveVmInterfaceVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ReserveVmInterfaceVspCommand.Builder cmdBuilder = new ReserveVmInterfaceVspCommand.Builder().nicUuid("nicUuid").nicMacAddress("nicMacAddress")
                .networkUuid("networkUuid").isL3Network(true).isSharedNetwork(true).vpcUuid("vpcUuid").networkDomainUuid("networkDomainUuid")
                .networksAccountUuid("networksAccountUuid").isDomainRouter(false).domainRouterIp("domainRouterIp").vmInstanceName("vmInstanceName").vmUuid("vmUuid")
                .vmUserName("vmUserName").vmUserDomainName("vmUserDomainName").useStaticIp(true).staticIp("staticIp").staticNatIpUuid("staticNatIpUuid")
                .staticNatIpAddress("staticNatIpAddress").isStaticNatIpAllocated(true).isOneToOneNat(true).staticNatVlanUuid("staticNatVlanUuid")
                .staticNatVlanGateway("staticNatVlanGateway").staticNatVlanNetmask("staticNatVlanNetmask");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).reserve("nicUuid", "nicMacAddress", "networkUuid", true, true, "vpcUuid", "networkDomainUuid",
                "networksAccountUuid", false, "domainRouterIp", "vmInstanceName", "vmUuid", true, "staticIp", "staticNatIpUuid", "staticNatIpAddress",
                true, true, "staticNatVlanUuid", "staticNatVlanGateway", "staticNatVlanNetmask");
        Answer rsrvVmInfAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(rsrvVmInfAns.getResult());
    }

    @Test
    public void testDeallocateVmVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        DeallocateVmVspCommand.Builder cmdBuilder = new DeallocateVmVspCommand.Builder().networkUuid("networkUuid").nicFromDbUuid("nicFromDbUuid")
                .nicMacAddress("nicMacAddress").nicIp4Address("nicIp4Address").isL3Network(true).isSharedNetwork(true).vpcUuid("vpcUuid")
                .networksDomainUuid("networksDomainUuid").vmInstanceName("vmInstanceName").vmUuid("vmUuid").isExpungingState(true);
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).deallocate("networkUuid", "nicFrmDdUuid", "nicMacAddress", "nicIp4Address", true, true, "vpcUuid", "networksDomainUuid",
                "vmInstanceName", "vmUuid", true);
        Answer dellocateVmAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(dellocateVmAns.getResult());
    }

    @Test
    public void testTrashNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        TrashNetworkVspCommand.Builder cmdBuilder = new TrashNetworkVspCommand.Builder().domainUuid("domainUuid").networkUuid("networkUuid")
                .isL3Network(true).isSharedNetwork(true).vpcUuid("vpcUuid").domainTemplateName("domainTemplateName");
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).trash("domainUuid", "networkUuid", true, true, "vpcUuid", "domainTemplateName");
        Answer trashNtwkAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(trashNtwkAns.getResult());
    }

    @Test
    public void testApplyStaticNatVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ApplyStaticNatVspCommand.Builder cmdBuilder = new ApplyStaticNatVspCommand.Builder().networkDomainUuid("networkDomainUuid").networkUuid("networkUuid")
                .vpcOrSubnetUuid("vpcOrSubnetUuid").isL3Network(true).isVpc(true).staticNatDetails(new ArrayList<Map<String, Object>>());
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyStaticNats("networkDomainUuid", "networkUuid", "vpcOrSubnetUuid", true, true, new ArrayList<Map<String, Object>>());
        Answer applyNatAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(applyNatAns.getResult());
    }

    @Test
    public void testApplyAclRuleVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ApplyAclRuleVspCommand.Builder cmdBuilder = new ApplyAclRuleVspCommand.Builder().networkAcl(true).networkUuid("networkUuid").networkDomainUuid("networkDomainUuid")
                .vpcOrSubnetUuid("vpcOrSubnetUuid").networkName("networkName").isL2Network(true).aclRules(new ArrayList<Map<String, Object>>()).networkId(100)
                .egressDefaultPolicy(false).acsIngressAcl(true).networkReset(true).domainTemplateName("domainTemplateName");
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyAclRules(true, "networkUuid", "networkDomainUuid", "vpcOrSubnetUuid", "networkName", true,
        new ArrayList<Map<String, Object>>(), 100, false, true, true, "domainTemplateName");
        Answer applyAclAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(applyAclAns.getResult());
    }

    @Test
    public void testShutDownVpcVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ShutDownVpcVspCommand.Builder cmdBuilder = new ShutDownVpcVspCommand.Builder().domainUuid("domainUuid").vpcUuid("vpcUuid").domainTemplateName("domainTemplateName");
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).shutdownVpc("domainUuid", "vpcUuid", "domainTemplateName");
        Answer shutVpcAns = _resource.executeRequest(cmdBuilder.build());
        assertTrue(shutVpcAns.getResult());
    }

    @Test
    public void testSyncVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        SyncVspCommand shutVpcCmd = new SyncVspCommand("nuageVspEntity");
        doAnswer(genericAnswer).when(_mockNuageVspSyncClient).syncWithNuageVsp("nuageVspEntity");
        Answer shutVpcAns = _resource.executeRequest(shutVpcCmd);
        assertTrue(shutVpcAns.getResult());
    }
}
