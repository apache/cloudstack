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
package com.cloud.network.router;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.MonitoringServiceDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.OpRouterMonitorServiceDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.element.VirtualRouterProviderVO;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatsLogDao;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VirtualNetworkApplianceManagerImplTest {

    // mock dao/db access
    @Mock private ClusterDao _clusterDao;
    @Mock private ConfigurationDao _configDao;
    @Mock private DataCenterDao _dcDao;
    @Mock private GuestOSDao _guestOSDao;
    @Mock private HostDao _hostDao;
    @Mock private IPAddressDao _ipAddressDao;
    @Mock private UserIpv6AddressDao _ipv6Dao;
    @Mock private LoadBalancerDao _loadBalancerDao;
    @Mock private LoadBalancerVMMapDao _loadBalancerVMMapDao;
    @Mock private MonitoringServiceDao _monitorServiceDao;
    @Mock private ManagementServerHostDao _msHostDao;
    @Mock private NetworkDao _networkDao;
    @Mock private NetworkOfferingDao _networkOfferingDao;
    @Mock private NicDao _nicDao;
    @Mock private NicIpAliasDao _nicIpAliasDao;
    @Mock private OpRouterMonitorServiceDao _opRouterMonitorServiceDao;
    @Mock private PortForwardingRulesDao _pfRulesDao;
    @Mock private PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Mock private HostPodDao _podDao;
    @Mock private DomainRouterDao _routerDao;
    @Mock private FirewallRulesDao _rulesDao;
    @Mock private Site2SiteCustomerGatewayDao _s2sCustomerGatewayDao;
    @Mock private Site2SiteVpnConnectionDao _s2sVpnConnectionDao;
    @Mock private Site2SiteVpnGatewayDao _s2sVpnGatewayDao;
    @Mock private ServiceOfferingDao _serviceOfferingDao;
    @Mock private VMTemplateDao _templateDao;
    @Mock private UserDao _userDao;
    @Mock private UserStatisticsDao _userStatsDao;
    @Mock private UserStatsLogDao _userStatsLogDao;
    @Mock private UserVmDao _userVmDao;
    @Mock private VlanDao _vlanDao;
    @Mock private VMInstanceDao _vmDao;
    @Mock private UserVmDetailsDao _vmDetailsDao;
    @Mock private VolumeDao _volumeDao;
    @Mock private RemoteAccessVpnDao _vpnDao;
    @Mock private VpnUserDao _vpnUsersDao;
    @Mock private VirtualRouterProviderDao _vrProviderDao;

    // mock the managers that have no influence on this functionality
    @Mock private AccountManager _accountMgr;
    @Mock private VirtualMachineManager _itMgr;
    @Mock private ResourceManager _resourceMgr;

    @InjectMocks
    private VirtualNetworkApplianceManagerImpl virtualNetworkApplianceManagerImpl;

    @Test
    public void testDestroyRouter() throws Exception {
        VirtualRouter r = new DomainRouterVO(1L, 0L, 0L, "router", 0L, HypervisorType.Any, 0L, 0L,
                1L, false, 0, false, RedundantState.UNKNOWN, false, false, null);
        when(_routerDao.findById(1L)).thenReturn((DomainRouterVO)r);
        VirtualRouter vr = virtualNetworkApplianceManagerImpl.destroyRouter(1L, new AccountVO(1L), 0L);
        assertEquals(vr, r);
    }

    @Test
    public void testDeployRouterNotRedundant() throws Exception {
        ServiceOfferingVO svcoff = new ServiceOfferingVO("name",
                /* cpu */ 1,
                /* ramsize */ 1024*1024,
                /* (clock?)speed */ 1024*1024*1024,
                /* rateMbps */ 1,
                /* multicastRateMbps */ 0,
                /* offerHA */ false,
                "displayText",
                ProvisioningType.THIN,
                /* useLocalStorage */ false,
                /* recreatable */ false,
                "tags",
                /* systemUse */ false,
                VirtualMachine.Type.DomainRouter,
                /* defaultUse */ false);

        DataCenter dc = new DataCenterVO(/* id */ 1L,
                "name",
                "description",
                "dns1",
                /* dns2 */ null,
                /* dns3 */ null,
                /* dns4 */ null,
                "cidr",
                "domain",
                /*domainid */ null,
                NetworkType.Basic,
                "zoneToken",
                "domainSuffix");

        DomainRouterVO router = new DomainRouterVO(/* id */ 1L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* isRedundantRouter */ false,
                /* priority */ 0,
                /* isPriorityBumpUp */ false,
                RedundantState.UNKNOWN,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);

        DeploymentPlan plan = new DataCenterDeployment(1L);

        when(_serviceOfferingDao.findById(1L)).thenReturn(svcoff);
        when(_routerDao.getNextInSequence(Long.class, "id")).thenReturn(1L);
// being anti-social and testing my own case first
        when(_resourceMgr.getDefaultHypervisor(1L)).thenReturn(HypervisorType.XenServer);
        when(_templateDao.findRoutingTemplate(HypervisorType.XenServer, "SystemVM Template (XenServer)")).thenReturn(new VMTemplateVO());
        when(_routerDao.persist(any(DomainRouterVO.class))).thenReturn(router);
        when(_routerDao.findById(router.getId())).thenReturn(router);

        VirtualRouter vr = virtualNetworkApplianceManagerImpl.deployRouter(new AccountVO(1L), new DeployDestination(dc,null,null,null), plan, null, false,
                new VirtualRouterProviderVO(), 1L, null, new LinkedHashMap<Network, List<? extends NicProfile>> (), true /* start the router */,
                null);
        // TODO: more elaborate mocking needed to have a vr returned
        assertEquals(vr, router);
    }
}
