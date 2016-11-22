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

package com.cloud;

import com.cloud.dc.VlanVO;
import com.cloud.domain.Domain;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.util.NuageVspEntityBuilder;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;
import net.nuage.vsp.acs.client.common.model.Pair;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Before;

import java.util.ArrayList;

import static com.cloud.network.manager.NuageVspManager.NuageVspIsolatedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspSharedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspVpcDomainTemplateName;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageTest {

    protected static final long NETWORK_ID = 42L;
    protected NetworkModel _networkModel = mock(NetworkModel.class);
    protected ConfigurationDao _configurationDao = mock(ConfigurationDao.class);
    protected NuageVspEntityBuilder _nuageVspEntityBuilder = mock(NuageVspEntityBuilder.class);

    @Before
    public void setUp() throws Exception {
        // Standard responses
        when(_networkModel.isProviderForNetwork(Network.Provider.NuageVsp, NETWORK_ID)).thenReturn(true);
        when(_configurationDao.getValue(NuageVspIsolatedNetworkDomainTemplateName.key())).thenReturn("IsolatedDomainTemplate");
        when(_configurationDao.getValue(NuageVspVpcDomainTemplateName.key())).thenReturn("VpcDomainTemplate");
        when(_configurationDao.getValue(NuageVspSharedNetworkDomainTemplateName.key())).thenReturn("SharedDomainTemplate");

        when(_nuageVspEntityBuilder.buildVspDomain(any(Domain.class))).thenReturn(buildVspDomain());
        when(_nuageVspEntityBuilder.buildVspNetwork(any(Network.class), anyBoolean())).thenReturn(buildVspNetwork());
        when(_nuageVspEntityBuilder.buildVspVm(any(VirtualMachine.class), any(Network.class))).thenReturn(buildVspVm());
        when(_nuageVspEntityBuilder.buildVspNic(anyString(), any(NicProfile.class))).thenReturn(buildVspNic());
        when(_nuageVspEntityBuilder.buildVspNic(any(NicVO.class))).thenReturn(buildVspNic());
        when(_nuageVspEntityBuilder.buildVspStaticNat(anyBoolean(), any(IPAddressVO.class), any(VlanVO.class), any(NicVO.class))).thenReturn(buildVspStaticNat());
        when(_nuageVspEntityBuilder.buildVspAclRule(any(FirewallRule.class), any(Network.class))).thenReturn(buildVspAclRule());
        when(_nuageVspEntityBuilder.buildVspAclRule(any(NetworkACLItem.class))).thenReturn(buildVspAclRule());
    }

    protected VspDomain buildVspDomain() {
        return new VspDomain.Builder()
                .uuid("domainUuid")
                .name("domainName")
                .path("domainPath")
                .build();
    }

    protected VspNetwork buildVspNetwork() {
        return new VspNetwork.Builder()
                .id(NETWORK_ID)
                .uuid("networkUuid")
                .name("networkName")
                .domain(buildVspDomain())
                .accountUuid("networkAccountUuid")
                .accountName("networkAccountName")
                .vpcUuid("vpcUuid")
                .vpcName("vpcName")
                .networkType(VspNetwork.NetworkType.L3)
                .firewallServiceSupported(true)
                .egressDefaultPolicy(true)
                .domainTemplateName("domainTemplateName")
                .cidr("networkCidr")
                .gateway("networkGateway")
                .virtualRouterIp("virtualRouterIp")
                .ipAddressRanges(new ArrayList<Pair<String, String>>())
                .build();
    }

    protected VspVm buildVspVm() {
        return new VspVm.Builder()
                .state(VspVm.State.Running)
                .uuid("vmUuid")
                .name("vmName")
                .domainRouter(true)
                .domainRouterIp("domainRouterIp")
                .build();
    }

    protected VspNic buildVspNic() {
        return new VspNic.Builder()
                .uuid("nicUuid")
                .macAddress("macAddress")
                .useStaticIp(true)
                .ip("ip")
                .build();
    }

    protected VspStaticNat buildVspStaticNat() {
        return new VspStaticNat.Builder()
                .state(VspStaticNat.State.Allocating)
                .ipUuid("ipUuid")
                .ipAddress("ipAddress")
                .nic(buildVspNic())
                .revoke(false)
                .oneToOneNat(true)
                .vlanUuid("vlanUuid")
                .vlanGateway("vlanGateway")
                .vlanNetmask("vlanNetmask")
                .build();
    }

    protected VspAclRule buildVspAclRule() {
        return new VspAclRule.Builder()
                .uuid("aclRuleUuid")
                .protocol("protcol")
                .startPort(1)
                .endPort(9)
                .state(VspAclRule.ACLState.Add)
                .trafficType(VspAclRule.ACLTrafficType.Ingress)
                .action(VspAclRule.ACLAction.Allow)
                .sourceIpAddress("sourceIpAddress")
                .sourceCidrList(new ArrayList<String>())
                .priority(1)
                .type(VspAclRule.ACLType.NetworkACL)
                .build();
    }

}
