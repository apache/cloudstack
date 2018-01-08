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

import java.util.ArrayList;

import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspDhcpVMOption;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

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

import static com.cloud.network.manager.NuageVspManager.NuageVspIsolatedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspSharedNetworkDomainTemplateName;
import static com.cloud.network.manager.NuageVspManager.NuageVspVpcDomainTemplateName;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class NuageTest {

    protected static final long NETWORK_ID = 42L;

    @Mock protected NetworkModel _networkModel;
    @Mock protected ConfigurationDao _configDao;
    @Mock protected NuageVspEntityBuilder _nuageVspEntityBuilder;

    @InjectMocks
    ConfigDepotImpl configDepot = new ConfigDepotImpl();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Standard responses
        when(_networkModel.isProviderForNetwork(Network.Provider.NuageVsp, NETWORK_ID)).thenReturn(true);

        mockConfigValue(NuageVspIsolatedNetworkDomainTemplateName, "IsolatedDomainTemplate");
        mockConfigValue(NuageVspVpcDomainTemplateName, "VpcDomainTemplate");
        mockConfigValue(NuageVspSharedNetworkDomainTemplateName, "SharedDomainTemplate");

        ConfigKey.init(configDepot);

        when(_nuageVspEntityBuilder.buildVspDomain(any(Domain.class))).thenReturn(buildVspDomain());

        VspNetwork vspNetwork = buildVspNetwork();
        when(_nuageVspEntityBuilder.buildVspNetwork(any(Network.class))).thenReturn(vspNetwork);
        when(_nuageVspEntityBuilder.buildVspNetwork(anyLong(), any(Network.class))).thenReturn(vspNetwork);

        when(_nuageVspEntityBuilder.buildVspVm(any(VirtualMachine.class), any(Network.class))).thenReturn(buildVspVm());

        when(_nuageVspEntityBuilder.buildVspNic(anyString(), any(NicProfile.class))).thenReturn(buildVspNic());
        when(_nuageVspEntityBuilder.buildVspNic(any(NicVO.class))).thenReturn(buildVspNic());

        when(_nuageVspEntityBuilder.buildVspStaticNat(anyBoolean(), any(IPAddressVO.class), any(VlanVO.class), any(NicVO.class))).thenReturn(buildVspStaticNat());
        when(_nuageVspEntityBuilder.buildVspAclRule(any(FirewallRule.class), any(Network.class))).thenReturn(buildVspAclRule());
        when(_nuageVspEntityBuilder.buildVspAclRule(any(NetworkACLItem.class))).thenReturn(buildVspAclRule());
    }

    protected  <T> void mockConfigValue(ConfigKey<T> configKey, T value) {
        ConfigurationVO vo = new ConfigurationVO("test", configKey);
        vo.setValue(value.toString());
        when(_configDao.getValue(configKey.key())).thenReturn(value.toString());
        when(_configDao.findById(configKey.key())).thenReturn(vo);
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
                .networkRelatedVsdIds(buildNetworkRelatedIds())
                .build();
    }

    protected NetworkRelatedVsdIds buildNetworkRelatedIds() {
        return new NetworkRelatedVsdIds.Builder()
                .vsdZoneId("vsdZoneId")
                .vsdDomainId("vsdDomainId")
                .vsdSubnetId("vsdSubnetId")
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

    protected VspDhcpDomainOption buildspDhcpDomainOption () {
        return new VspDhcpDomainOption.Builder()
                .vrIsDnsProvider(true)
                .networkDomain("networkDomain")
                .dnsServers(Lists.newArrayList("10.10.10.10", "20.20.20.20"))
                .build();
    }

    protected VspDhcpVMOption buildspDhcpVMOption () {
        return new VspDhcpVMOption.Builder()
                .defaultHasDns(true)
                .hostname("VMx")
                .networkHasDns(true)
                .isDefaultInterface(true)
                .domainRouter(false)
                .nicUuid("aaaa-bbbbbbbb-ccccccc")
                .build();
    }

}
