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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import net.nuage.vsp.acs.client.api.NuageVspApiClient;
import net.nuage.vsp.acs.client.api.NuageVspElementClient;
import net.nuage.vsp.acs.client.api.NuageVspGuruClient;
import net.nuage.vsp.acs.client.api.NuageVspManagerClient;
import net.nuage.vsp.acs.client.api.NuageVspPluginClientLoader;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspDhcpVMOption;
import net.nuage.vsp.acs.client.api.model.VspHost;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
import com.cloud.agent.api.manager.ImplementNetworkVspAnswer;
import com.cloud.host.Host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NuageVspResourceTest extends NuageTest {
    private NuageVspResource _resource;
    @Mock private NuageVspApiClient _mockNuageVspApiClient;
    @Mock private NuageVspElementClient _mockNuageVspElementClient;
    @Mock private NuageVspGuruClient _mockNuageVspGuruClient;
    @Mock private NuageVspManagerClient _mockNuageVspManagerClient;
    @Mock private NuageVspPluginClientLoader _mockNuageVspPluginClientLoader;
    private NuageVspResourceConfiguration _resourceConfiguration;
    private Map<String, Object> _hostDetails;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        MockitoAnnotations.initMocks(this);

        when(_mockNuageVspPluginClientLoader.getNuageVspApiClient()).thenReturn(_mockNuageVspApiClient);
        when(_mockNuageVspPluginClientLoader.getNuageVspElementClient()).thenReturn(_mockNuageVspElementClient);
        when(_mockNuageVspPluginClientLoader.getNuageVspGuruClient()).thenReturn(_mockNuageVspGuruClient);
        when(_mockNuageVspPluginClientLoader.getNuageVspManagerClient()).thenReturn(_mockNuageVspManagerClient);

        _resource = new NuageVspResource() {
            @Override protected NuageVspPluginClientLoader getClientLoader(VspHost vspHost) {
                return _mockNuageVspPluginClientLoader;
            }

            protected void login() throws ConfigurationException {
            }

        };

        _resourceConfiguration = new NuageVspResourceConfiguration()
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
        _resource.configure("Nuage VSD - nuagevsd", _hostDetails);

        assertEquals("Nuage VSD - nuagevsd", _resource.getName());
        assertEquals(Host.Type.L2Networking, _resource.getType());
    }

    @Test
    public void testInitialization() throws Exception {
        _resource.configure("Nuage VSD - nuagevsd", _hostDetails);

        StartupCommand[] sc = _resource.initialize();
        assertEquals(1, sc.length);
        assertEquals("aaaaa-bbbbb-ccccc", sc[0].getGuid());
        assertEquals("Nuage VSD - nuagevsd", sc[0].getName());
        assertEquals("blublub", sc[0].getDataCenter());
    }

    @Test
    public void testPingCommandStatus() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        PingCommand ping = _resource.getCurrentStatus(42);
        assertNotNull(ping);
        assertEquals(42, ping.getHostId());
        assertEquals(Host.Type.L2Networking, ping.getHostType());
    }

    @Test
    public void testImplementNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        VspDhcpDomainOption vspDhcpOptions = buildspDhcpDomainOption();
        ImplementNetworkVspCommand cmd = new ImplementNetworkVspCommand(vspNetwork, vspDhcpOptions);
        ImplementNetworkVspAnswer implNtwkAns = (ImplementNetworkVspAnswer)_resource.executeRequest(cmd);
        assertTrue(implNtwkAns.getResult());
        verify(_mockNuageVspGuruClient).implement(vspNetwork, vspDhcpOptions);
    }

    @Test
    public void testReserveVmInterfaceVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        VspVm vspVm = buildVspVm();
        VspNic vspNic = buildVspNic();
        VspStaticNat vspStaticNat = buildVspStaticNat();
        VspDhcpVMOption vspDhcpOption = buildspDhcpVMOption();
        ReserveVmInterfaceVspCommand cmd = new ReserveVmInterfaceVspCommand(vspNetwork, vspVm, vspNic, vspStaticNat, vspDhcpOption);
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
        Answer dellocateVmAns = _resource.executeRequest(cmd);
        assertTrue(dellocateVmAns.getResult());
    }

    @Test
    public void testTrashNetworkVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        TrashNetworkVspCommand cmd = new TrashNetworkVspCommand(vspNetwork);
        Answer trashNtwkAns = _resource.executeRequest(cmd);
        assertTrue(trashNtwkAns.getResult());
    }

    @Test
    public void testApplyStaticNatVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        List<VspStaticNat> vspStaticNatDetails = Lists.newArrayList(buildVspStaticNat());
        ApplyStaticNatVspCommand cmd = new ApplyStaticNatVspCommand(vspNetwork, vspStaticNatDetails);
        Answer applyNatAns = _resource.executeRequest(cmd);
        assertTrue(applyNatAns.getResult());
    }

    @Test
    public void testApplyAclRuleVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        VspNetwork vspNetwork = buildVspNetwork();
        List<VspAclRule> vspAclRules = Lists.newArrayList(buildVspAclRule());
        ApplyAclRuleVspCommand cmd = new ApplyAclRuleVspCommand(VspAclRule.ACLType.NetworkACL, vspNetwork, vspAclRules, false);
        Answer applyAclAns = _resource.executeRequest(cmd);
        assertTrue(applyAclAns.getResult());
    }

    @Test
    public void testShutDownVpcVspCommand() throws Exception {
        _resource.configure("NuageVspResource", _hostDetails);

        ShutDownVpcVspCommand cmd = new ShutDownVpcVspCommand("domainUuid", "vpcUuid", "domainTemplateName", Lists.<String>newArrayList());
        Answer shutVpcAns = _resource.executeRequest(cmd);
        assertTrue(shutVpcAns.getResult());
    }
}
