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

import com.cloud.NuageTest;
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
import com.cloud.host.Host;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.nuage.vsp.acs.client.api.NuageVspApiClient;
import net.nuage.vsp.acs.client.api.NuageVspElementClient;
import net.nuage.vsp.acs.client.api.NuageVspGuruClient;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class NuageVspResourceTest extends NuageTest {
    private NuageVspResource _resource;
    private NuageVspApiClient _mockNuageVspApiClient = mock(NuageVspApiClient.class);
    private NuageVspElementClient _mockNuageVspElementClient = mock(NuageVspElementClient.class);
    private NuageVspGuruClient _mockNuageVspGuruClient = mock(NuageVspGuruClient.class);
    private NuageVspResource.Configuration _resourceConfiguration;
    private Map<String, Object> _hostDetails;

    org.mockito.stubbing.Answer<Object> genericAnswer = new org.mockito.stubbing.Answer<Object>() {
        public Object answer(InvocationOnMock invocation) {
            return null;
        }
    };

    @Before
    public void setUp() throws Exception {
        super.setUp();

        _resource = new NuageVspResource() {

            @Override
            protected void loadNuageClient() {
                _isNuageVspClientLoaded = true;
                _nuageVspApiClient = _mockNuageVspApiClient;
                _nuageVspElementClient = _mockNuageVspElementClient;
                _nuageVspGuruClient = _mockNuageVspGuruClient;
            }

            protected void isNuageVspApiLoaded() throws ConfigurationException {
            }

            protected void isNuageVspGuruLoaded() throws ConfigurationException {
            }

            protected void isNuageVspElementLoaded() throws ConfigurationException {
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

        VspNetwork vspNetwork = buildVspNetwork();
        ImplementNetworkVspCommand cmd = new ImplementNetworkVspCommand(vspNetwork, new ArrayList<String>());
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).implement(vspNetwork, new ArrayList<String>());
        com.cloud.agent.api.Answer implNtwkAns = _resource.executeRequest(cmd);
        assertTrue(implNtwkAns.getResult());
    }

    @Test
    public void testReserveVmInterfaceVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        VspVm vspVm = buildVspVm();
        VspNic vspNic = buildVspNic();
        VspStaticNat vspStaticNat = buildVspStaticNat();
        ReserveVmInterfaceVspCommand cmd = new ReserveVmInterfaceVspCommand(vspNetwork, vspVm, vspNic, vspStaticNat);
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).reserve(vspNetwork, vspVm, vspNic, vspStaticNat);
        Answer rsrvVmInfAns = _resource.executeRequest(cmd);
        assertTrue(rsrvVmInfAns.getResult());
    }

    @Test
    public void testDeallocateVmVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        VspVm vspVm = buildVspVm();
        VspNic vspNic = buildVspNic();
        DeallocateVmVspCommand cmd = new DeallocateVmVspCommand(vspNetwork, vspVm, vspNic);
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).deallocate(vspNetwork, vspVm, vspNic);
        Answer dellocateVmAns = _resource.executeRequest(cmd);
        assertTrue(dellocateVmAns.getResult());
    }

    @Test
    public void testTrashNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        TrashNetworkVspCommand cmd = new TrashNetworkVspCommand(vspNetwork);
        doAnswer(genericAnswer).when(_mockNuageVspGuruClient).trash(vspNetwork);
        Answer trashNtwkAns = _resource.executeRequest(cmd);
        assertTrue(trashNtwkAns.getResult());
    }

    @Test
    public void testApplyStaticNatVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        List<VspStaticNat> vspStaticNatDetails = Lists.newArrayList(buildVspStaticNat());
        ApplyStaticNatVspCommand cmd = new ApplyStaticNatVspCommand(vspNetwork, vspStaticNatDetails);
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyStaticNats(vspNetwork, vspStaticNatDetails);
        Answer applyNatAns = _resource.executeRequest(cmd);
        assertTrue(applyNatAns.getResult());
    }

    @Test
    public void testApplyAclRuleVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        List<VspAclRule> vspAclRules = Lists.newArrayList(buildVspAclRule());
        ApplyAclRuleVspCommand cmd = new ApplyAclRuleVspCommand(VspAclRule.ACLType.NetworkACL, vspNetwork, vspAclRules, false);
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).applyAclRules(VspAclRule.ACLType.NetworkACL, vspNetwork, vspAclRules, false);
        Answer applyAclAns = _resource.executeRequest(cmd);
        assertTrue(applyAclAns.getResult());
    }

    @Test
    public void testShutDownVpcVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ShutDownVpcVspCommand cmd = new ShutDownVpcVspCommand("domainUuid", "vpcUuid", "domainTemplateName", Lists.<String>newArrayList());
        doAnswer(genericAnswer).when(_mockNuageVspElementClient).shutdownVpc("domainUuid", "vpcUuid", "domainTemplateName", Lists.<String>newArrayList());
        Answer shutVpcAns = _resource.executeRequest(cmd);
        assertTrue(shutVpcAns.getResult());
    }
}
