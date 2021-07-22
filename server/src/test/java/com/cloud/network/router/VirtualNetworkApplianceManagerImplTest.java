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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Site2SiteVpnConnection;
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
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatsLogDao;
import com.cloud.vm.DomainRouterVO;
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
    @Mock
    private ClusterDao _clusterDao;

    @Mock
    private ConfigurationDao _configDao;

    @Mock
    private DataCenterDao _dcDao;

    @Mock
    private GuestOSDao _guestOSDao;

    @Mock
    private HostDao _hostDao;

    @Mock
    private IPAddressDao _ipAddressDao;

    @Mock
    private UserIpv6AddressDao _ipv6Dao;

    @Mock
    private LoadBalancerDao _loadBalancerDao;

    @Mock
    private LoadBalancerVMMapDao _loadBalancerVMMapDao;

    @Mock
    private MonitoringServiceDao _monitorServiceDao;

    @Mock
    private ManagementServerHostDao _msHostDao;

    @Mock
    private NetworkDao _networkDao;

    @Mock
    private NetworkOfferingDao _networkOfferingDao;

    @Mock
    private NicDao _nicDao;

    @Mock
    private NicIpAliasDao _nicIpAliasDao;

    @Mock
    private OpRouterMonitorServiceDao _opRouterMonitorServiceDao;

    @Mock
    private PortForwardingRulesDao _pfRulesDao;

    @Mock
    private PhysicalNetworkServiceProviderDao _physicalProviderDao;

    @Mock
    private HostPodDao _podDao;

    @Mock
    private DomainRouterDao _routerDao;

    @Mock
    private FirewallRulesDao _rulesDao;

    @Mock
    private Site2SiteCustomerGatewayDao _s2sCustomerGatewayDao;

    @Mock
    private Site2SiteVpnConnectionDao _s2sVpnConnectionDao;

    @Mock
    private Site2SiteVpnGatewayDao _s2sVpnGatewayDao;

    @Mock
    private ServiceOfferingDao _serviceOfferingDao;

    @Mock
    private VMTemplateDao _templateDao;

    @Mock
    private UserDao _userDao;

    @Mock
    private UserStatisticsDao _userStatsDao;

    @Mock
    private UserStatsLogDao _userStatsLogDao;

    @Mock
    private UserVmDao _userVmDao;

    @Mock
    private VlanDao _vlanDao;

    @Mock
    private VMInstanceDao _vmDao;

    @Mock
    private UserVmDetailsDao _vmDetailsDao;

    @Mock
    private VolumeDao _volumeDao;

    @Mock
    private RemoteAccessVpnDao _vpnDao;

    @Mock
    private VpnUserDao _vpnUsersDao;

    @Mock
    private VirtualRouterProviderDao _vrProviderDao;

    //@Mock private AccountManager _accountMgr;

    @Mock private VirtualMachineManager _itMgr;

    @Mock
    private AgentManager _agentMgr;

    @Mock
    private CheckS2SVpnConnectionsCommand _s2sConnCommand;

    @Mock
    private CheckS2SVpnConnectionsAnswer _s2sVpnAnswer;

    @Mock
    private DomainRouterVO _router;

    @Mock
    private AlertManager _alertMgr;

    @Mock
    private Site2SiteVpnManager _s2sVpnMgr;

    @Mock
    private RouterControlHelper _routerControlHelper;

    @InjectMocks
    private VirtualNetworkApplianceManagerImpl virtualNetworkApplianceManagerImpl;


    //    @InjectMocks
    //    private VirtualNetworkApplianceManagerImpl virtualNetworkApplianceManagerImpl;

    @Test
    public void testDestroyRouter() throws Exception {
        //        VirtualRouter r = new DomainRouterVO(1L, 0L, 0L, "router", 0L, HypervisorType.Any, 0L, 0L,
        //                1L, false, 0, false, RedundantState.UNKNOWN, false, false, null);
        //        when(_routerDao.findById(1L)).thenReturn((DomainRouterVO)r);
        //        VirtualRouter vr = virtualNetworkApplianceManagerImpl.destroyRouter(1L, new AccountVO(1L), 0L);
        //        assertNotEquals(vr, null);
    }

    @Test
    public void testDeployRouterNotRedundant() throws Exception {
        //        DataCenter dc = new DataCenterVO(1L, "name", "description", "dns", null, null, null, "cidr", "domain", null,
        //                NetworkType.Basic, "zoneToken", "domainSuffix");
        //        when(_routerDao.getNextInSequence(Long.class, "id")).thenReturn(1L);
        //        when(_resourceMgr.getDefaultHypervisor(1L)).thenReturn(HypervisorType.Any);
        //        DeploymentPlan plan = new DataCenterDeployment(1L);
        //        VirtualRouter vr = virtualNetworkApplianceManagerImpl.deployRouter(new AccountVO(1L), new DeployDestination(dc,null,null,null), plan, null, false,
        //                new VirtualRouterProviderVO(), 0L, null, new LinkedHashMap<Network, List<? extends NicProfile>> (), true /* start the router */,
        //                null);
        //        // TODO: more elaborate mocking needed to have a vr returned
        //assertEquals(vr, null);
    }

    @Test
    public void testUpdateSite2SiteVpnConnectionState() throws Exception{

        DomainRouterVO router = new DomainRouterVO(1L, 1L, 1L, "First testing router", 1L, Hypervisor.HypervisorType.XenServer, 1L, 1L, 1L, 1L, false, VirtualRouter.RedundantState.PRIMARY, true, true, 1L);
        router.setState(VirtualMachine.State.Running);
        router.setPrivateIpAddress("192.168.50.15");

        List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        routers.add(router);

        Site2SiteVpnConnectionVO conn = new Site2SiteVpnConnectionVO(1L, 1L, 1L, 1L, false);
        Site2SiteVpnConnectionVO conn1 = new Site2SiteVpnConnectionVO(1L, 1L, 1L, 1L, false);
        conn.setState(Site2SiteVpnConnection.State.Disconnected);
        conn1.setState(Site2SiteVpnConnection.State.Disconnected);
        List<Site2SiteVpnConnectionVO> conns = new ArrayList<Site2SiteVpnConnectionVO>();
        conns.add(conn);
        conns.add(conn1);

        Site2SiteCustomerGatewayVO gw = new Site2SiteCustomerGatewayVO("Testing gateway", 1L, 1L, "192.168.50.15", "Guest List", "ipsecPsk", "ikePolicy", "espPolicy", 1L, 1L, true, true, false, "ike");
        HostVO hostVo = new HostVO(1L, "Testing host", Host.Type.Routing, "192.168.50.15", "privateNetmask", "privateMacAddress", "publicIpAddress", "publicNetmask", "publicMacAddress", "storageIpAddress", "storageNetmask", "storageMacAddress", "deuxStorageIpAddress", "duxStorageNetmask", "deuxStorageMacAddress", "guid", Status.Up, "version", "iqn", new Date() , 1L, 1L, 1L, 1L, "parent", 20L, Storage.StoragePoolType.Gluster);
        hostVo.setManagementServerId(ManagementServerNode.getManagementServerId());

        ArrayList<String> ipList = new ArrayList<>();
        ipList.add("192.168.50.15");

        _s2sConnCommand = new CheckS2SVpnConnectionsCommand(ipList);

        when(_s2sVpnMgr.getConnectionsForRouter(router)).thenReturn(conns);
        when(_s2sVpnConnectionDao.persist(conn)).thenReturn(null);
        when(_s2sCustomerGatewayDao.findById(conn.getCustomerGatewayId())).thenReturn(gw);
        when(_hostDao.findById(router.getHostId())).thenReturn(hostVo);
        when(_routerControlHelper.getRouterControlIp(router.getId())).thenReturn("192.168.50.15");
        doReturn(_s2sVpnAnswer).when(_agentMgr).easySend(nullable(Long.class), nullable(CheckS2SVpnConnectionsCommand.class));
        when(_s2sVpnAnswer.getResult()).thenReturn(true);
        when(_s2sVpnConnectionDao.acquireInLockTable(conn.getId())).thenReturn(conn);
        when(_s2sVpnAnswer.isIPPresent("192.168.50.15")).thenReturn(true);
        when(_s2sVpnAnswer.isConnected("192.168.50.15")).thenReturn(true);
        lenient().doNothing().when(_alertMgr).sendAlert(any(AlertManager.AlertType.class), anyLong(), anyLong(), anyString(), anyString());

        virtualNetworkApplianceManagerImpl.updateSite2SiteVpnConnectionState(routers);

        for(Site2SiteVpnConnection connection : conns){
            assertEquals(Site2SiteVpnConnection.State.Connected, connection.getState());
        }
    }



}
