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
package com.cloud.network.element;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.cloud.network.router.deployment.RouterDeploymentDefinitionBuilder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkModelImpl;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.MonitoringServiceDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.OpRouterMonitorServiceDao;
import com.cloud.network.dao.OvsProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VpcVirtualNetworkApplianceManagerImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatsLogDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

@RunWith(MockitoJUnitRunner.class)
public class VirtualRouterElementTest {
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
    @Mock private NetworkDetailsDao _networkDetailsDao;
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
    @Mock private LoadBalancerDao _lbDao;
    @Mock private NetworkDao _networksDao;
    @Mock private OvsProviderDao _ovsProviderDao;

    @Mock private ServiceOfferingVO _offering;
    @Mock private NetworkModelImpl _networkModel;

    @Mock private AccountManager _accountMgr;
    @Mock private ConfigurationManager _configMgr;
    @Mock private NetworkModel _networkMdl;
    @Mock private NetworkOrchestrationService _networkMgr;
    @Mock private ResourceManager _resourceMgr;
    @Mock private UserVmManager _userVmMgr;
    @Mock private VirtualMachineManager _itMgr;

    @InjectMocks
    private RouterDeploymentDefinitionBuilder routerDeploymentDefinitionBuilder;

    @InjectMocks
    private VpcVirtualNetworkApplianceManagerImpl _routerMgr;

    @InjectMocks
    private VirtualRouterElement virtualRouterElement;

    DataCenter testDataCenter = new DataCenterVO(/* id */ 1L,
            "name",
            "description",
            "dns1",
            /* dns2 */ null,
            /* dns3 */ null,
            /* dns4 */ null,
            "cidr",
            "domain",
            /*domainid */ null,
            NetworkType.Advanced,
            "zoneToken",
            "domainSuffix");
    DeployDestination testDestination = new DeployDestination(testDataCenter,null,null,null);
    ReservationContext testContext = new ReservationContextImpl(null, null, null);
    NetworkVO testNetwork = new NetworkVO();
    NicProfile testNicProfile = new NicProfile();
    NetworkOfferingVO testOffering = new NetworkOfferingVO();
    @Mock VirtualMachineProfile testVMProfile;

    @Test
    @Ignore("Ignore it until it's fixed in order not to brake the build")
    public void testImplementInAdvancedZoneOnXenServer() throws Exception {
        virtualRouterElement._routerMgr = _routerMgr;
        mockDAOs(testNetwork, testOffering);
        mockMgrs();

        final boolean done = virtualRouterElement.implement(testNetwork, testOffering, testDestination, testContext);
        assertTrue("no cigar for network daddy",done);
    }

    @Test
    @Ignore("Ignore it until it's fixed in order not to brake the build")
    public void testPrepare() {
        virtualRouterElement._routerMgr = _routerMgr;
        virtualRouterElement.routerDeploymentDefinitionBuilder = routerDeploymentDefinitionBuilder;
        mockDAOs(testNetwork, testOffering);
        mockMgrs();

        boolean done = false;
        try {
            done = virtualRouterElement.prepare(testNetwork, testNicProfile, testVMProfile, testDestination, testContext);
        } catch (ConcurrentOperationException | InsufficientCapacityException | ResourceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        assertTrue("can not even start to create a router", done);

    }

    @Test
    public void testGetRouters1(){
        Network networkUpdateInprogress=new NetworkVO(1l,null,null,null,1l,1l,1l,1l,"d","d","d",null,1l,1l,null,true,null,true);
        mockDAOs((NetworkVO)networkUpdateInprogress,testOffering);
        //getRoutes should always return the router that is updating.
        List<DomainRouterVO> routers=virtualRouterElement.getRouters(networkUpdateInprogress);
        assertTrue(routers.size()==1);
        assertTrue(routers.get(0).getUpdateState()== VirtualRouter.UpdateState.UPDATE_IN_PROGRESS);
    }

    @Test
    public void testGetRouters2(){
        Network networkUpdateInprogress=new NetworkVO(2l,null,null,null,1l,1l,1l,1l,"d","d","d",null,1l,1l,null,true,null,true);
        mockDAOs((NetworkVO)networkUpdateInprogress,testOffering);
        //alwyas return backup routers first when both primary and backup need update.
        List<DomainRouterVO> routers=virtualRouterElement.getRouters(networkUpdateInprogress);
        assertTrue(routers.size()==1);
        assertTrue(routers.get(0).getRedundantState()==RedundantState.BACKUP && routers.get(0).getUpdateState()==VirtualRouter.UpdateState.UPDATE_IN_PROGRESS);
    }

    @Test
    public void testGetRouters3(){
        Network network=new NetworkVO(3l,null,null,null,1l,1l,1l,1l,"d","d","d",null,1l,1l,null,true,null,true);
        mockDAOs((NetworkVO)network,testOffering);
        //alwyas return backup routers first when both primary and backup need update.
        List<DomainRouterVO> routers=virtualRouterElement.getRouters(network);
        assertTrue(routers.size()==4);
    }

    @Test
    public void getResourceCountTest(){
        Network network=new NetworkVO(3l,null,null,null,1l,1l,1l,1l,"d","d","d",null,1l,1l,null,true,null,true);
        mockDAOs((NetworkVO)network,testOffering);
        int routers=virtualRouterElement.getResourceCount(network);
        assertTrue(routers==4);
    }

    @Test
    public void completeAggregationCommandTest1() throws AgentUnavailableException,ResourceUnavailableException {
        virtualRouterElement._routerMgr = Mockito.mock(VpcVirtualNetworkApplianceManagerImpl.class);
        virtualRouterElement.routerDeploymentDefinitionBuilder = routerDeploymentDefinitionBuilder;
        Network network = new NetworkVO(6l, null, null, null, 1l, 1l, 1l, 1l, "d", "d", "d", null, 1l, 1l, null, true, null, true);
        when(virtualRouterElement._routerMgr.completeAggregatedExecution(any(Network.class), anyList())).thenReturn(true);
        mockDAOs((NetworkVO) network, testOffering);
        when(virtualRouterElement._routerDao.persist(any(DomainRouterVO.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object args[] = invocationOnMock.getArguments();
                DomainRouterVO router = (DomainRouterVO) args[0];
                if (router.getUpdateState() != VirtualRouter.UpdateState.UPDATE_COMPLETE) {
                    throw new CloudRuntimeException("TestFailed: completeAggregationCommandTest1 failed");
                } else return null;
            }
        });
        virtualRouterElement.completeAggregatedExecution(network, testDestination);
    }
    /**
     * @param networks
     * @param offerings
     * @throws ConcurrentOperationException
     */
    private void mockMgrs() throws ConcurrentOperationException {
        final Service service = Service.Connectivity;
        testNetwork.setState(Network.State.Implementing);
        testNetwork.setTrafficType(TrafficType.Guest);
        when(_networkMdl.isProviderEnabledInPhysicalNetwork(0L, "VirtualRouter")).thenReturn(true);
        when(_networkMdl.isProviderSupportServiceInNetwork(testNetwork.getId(), service, Network.Provider.VirtualRouter)).thenReturn(true);
        when(_networkMdl.isProviderForNetwork(Network.Provider.VirtualRouter, 0L)).thenReturn(true);
        when(testVMProfile.getType()).thenReturn(VirtualMachine.Type.User);
        when(testVMProfile.getHypervisorType()).thenReturn(HypervisorType.XenServer);
        final List<NetworkVO> networks = new ArrayList<NetworkVO>(1);
        networks.add(testNetwork);
        final List<NetworkOfferingVO> offerings = new ArrayList<NetworkOfferingVO>(1);
        offerings.add(testOffering);
        doReturn(offerings).when(_networkModel).getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        doReturn(networks).when(_networkMgr).setupNetwork(any(Account.class), any(NetworkOffering.class), any(DeploymentPlan.class), any(String.class), any(String.class), anyBoolean());
        // being anti-social and testing my own case first
        doReturn(HypervisorType.XenServer).when(_resourceMgr).getDefaultHypervisor(anyLong());
        doReturn(new AccountVO()).when(_accountMgr).getAccount(testNetwork.getAccountId());
    }

    /**
     * @param network
     */
    private void mockDAOs(final NetworkVO network, final NetworkOfferingVO offering) {
        lenient().when(_networkDao.acquireInLockTable(network.getId(), NetworkOrchestrationService.NetworkLockTimeout.value())).thenReturn(network);
        lenient().when(_networksDao.acquireInLockTable(network.getId(), NetworkOrchestrationService.NetworkLockTimeout.value())).thenReturn(network);
        lenient().when(_physicalProviderDao.findByServiceProvider(0L, "VirtualRouter")).thenReturn(new PhysicalNetworkServiceProviderVO());
        lenient().when(_vrProviderDao.findByNspIdAndType(0L, Type.VirtualRouter)).thenReturn(new VirtualRouterProviderVO());
        lenient().when(_networkOfferingDao.findById(0L)).thenReturn(offering);
        // watchit: (in this test) there can be only one
        when(_routerDao.getNextInSequence(Long.class, "id")).thenReturn(0L);
        final ServiceOfferingVO svcoff = new ServiceOfferingVO("name",
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
        lenient().when(_serviceOfferingDao.findById(0L)).thenReturn(svcoff);
        lenient().when(_serviceOfferingDao.findByName(Matchers.anyString())).thenReturn(svcoff);
        final DomainRouterVO router = new DomainRouterVO(/* id */ 1L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* userId */ 1L,
                /* isRedundantRouter */ false,
                RedundantState.UNKNOWN,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);
        final DomainRouterVO routerNeedUpdateBackup = new DomainRouterVO(/* id */ 2L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* userId */ 1L,
                /* isRedundantRouter */ false,
                RedundantState.BACKUP,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);
                routerNeedUpdateBackup.setUpdateState(VirtualRouter.UpdateState.UPDATE_NEEDED);
        final DomainRouterVO routerNeedUpdatePrimary = new DomainRouterVO(/* id */ 3L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* userId */ 1L,
                /* isRedundantRouter */ false,
                RedundantState.PRIMARY,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);
        routerNeedUpdatePrimary.setUpdateState(VirtualRouter.UpdateState.UPDATE_NEEDED);
        final DomainRouterVO routerUpdateComplete = new DomainRouterVO(/* id */ 4L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* userId */ 1L,
                /* isRedundantRouter */ false,
                RedundantState.UNKNOWN,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);
                routerUpdateComplete.setUpdateState(VirtualRouter.UpdateState.UPDATE_COMPLETE);
        final DomainRouterVO routerUpdateInProgress = new DomainRouterVO(/* id */ 5L,
                /* serviceOfferingId */ 1L,
                /* elementId */ 0L,
                "name",
                /* templateId */0L,
                HypervisorType.XenServer,
                /* guestOSId */ 0L,
                /* domainId */ 0L,
                /* accountId */ 1L,
                /* userId */ 1L,
                /* isRedundantRouter */ false,
                RedundantState.UNKNOWN,
                /* haEnabled */ false,
                /* stopPending */ false,
                /* vpcId */ null);
                routerUpdateInProgress.setUpdateState(VirtualRouter.UpdateState.UPDATE_IN_PROGRESS);
        List<DomainRouterVO> routerList1=new ArrayList<>();
        routerList1.add(routerUpdateComplete);
        routerList1.add(routerNeedUpdateBackup);
        routerList1.add(routerNeedUpdatePrimary);
        routerList1.add(routerUpdateInProgress);
        List<DomainRouterVO> routerList2=new ArrayList<>();
        routerList2.add(routerUpdateComplete);
        routerList2.add(routerNeedUpdateBackup);
        routerList2.add(routerNeedUpdatePrimary);
        List<DomainRouterVO> routerList3=new ArrayList<>();
        routerList3.add(routerUpdateComplete);
        routerList3.add(routerUpdateInProgress);
        lenient().when(_routerDao.getNextInSequence(Long.class, "id")).thenReturn(1L);
        lenient().when(_templateDao.findRoutingTemplate(HypervisorType.XenServer, "SystemVM Template (XenServer)")).thenReturn(new VMTemplateVO());
        lenient().when(_routerDao.persist(any(DomainRouterVO.class))).thenReturn(router);
        lenient().when(_routerDao.findById(router.getId())).thenReturn(router);
        when(_routerDao.listByNetworkAndRole(1l, VirtualRouter.Role.VIRTUAL_ROUTER)).thenReturn(routerList1);
        when(_routerDao.listByNetworkAndRole(2l, VirtualRouter.Role.VIRTUAL_ROUTER)).thenReturn(routerList2);
        when(_routerDao.listByNetworkAndRole(3l, VirtualRouter.Role.VIRTUAL_ROUTER)).thenReturn(routerList1);
        when(_routerDao.listByNetworkAndRole(6l, VirtualRouter.Role.VIRTUAL_ROUTER)).thenReturn(routerList3);
        when(_networkDetailsDao.findDetail(1l, Network.updatingInSequence)).thenReturn(new NetworkDetailVO(1l,Network.updatingInSequence,"true",true));
        when(_networkDetailsDao.findDetail(2l, Network.updatingInSequence)).thenReturn(new NetworkDetailVO(2l,Network.updatingInSequence,"true",true));
        when(_networkDetailsDao.findDetail(6l, Network.updatingInSequence)).thenReturn(new NetworkDetailVO(2l,Network.updatingInSequence,"true",true));
        when(_routerDao.persist(any(DomainRouterVO.class))).thenReturn(router);
    }

    @Test
    public void testCanHandle() {
        Network network = Mockito.mock(Network.class);

        final long networkId = 1;
        final long physicalNetworkId = 42;
        final long networkOfferingId = 10;
        final long dataCenterId = 33;

        when(network.getId()).thenReturn(networkId);
        lenient().when(network.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        lenient().when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        lenient().when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        lenient().when(network.getDataCenterId()).thenReturn(dataCenterId);
        when(network.getVpcId()).thenReturn(null);

        when(virtualRouterElement._networkMdl.getPhysicalNetworkId(network)).thenReturn(physicalNetworkId);
        when(virtualRouterElement._networkMdl.isProviderEnabledInPhysicalNetwork(physicalNetworkId, Network.Provider.VirtualRouter.getName())).thenReturn(true);
        when(virtualRouterElement._networkMdl.isProviderForNetwork(Network.Provider.VirtualRouter, networkId)).thenReturn(true);

        assertTrue(virtualRouterElement.canHandle(network, null));
    }

    @Test
    public void testAddPasswordAndUserdata() throws Exception {
        Network network = Mockito.mock(Network.class);
        VirtualMachineProfile vm = Mockito.mock(VirtualMachineProfile.class);
        NicProfile nic = Mockito.mock(NicProfile.class);
        DeployDestination dest = Mockito.mock(DeployDestination.class);
        ReservationContext context = Mockito.mock(ReservationContext.class);
        Service service = Service.UserData;

        final long networkId = 1;
        final long physicalNetworkId = 42;
        final long networkOfferingId = 10;
        final long dataCenterId = 33;

        when(network.getId()).thenReturn(networkId);
        lenient().when(network.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        lenient().when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        lenient().when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        lenient().when(network.getDataCenterId()).thenReturn(dataCenterId);
        when(network.getVpcId()).thenReturn(null);

        lenient().when(vm.getType()).thenReturn(VirtualMachine.Type.User);

        when(virtualRouterElement._networkMdl.getPhysicalNetworkId(network)).thenReturn(physicalNetworkId);
        when(virtualRouterElement._networkMdl.isProviderEnabledInPhysicalNetwork(physicalNetworkId, Network.Provider.VirtualRouter.getName())).thenReturn(true);
        when(virtualRouterElement._networkMdl.isProviderSupportServiceInNetwork(networkId, service, Network.Provider.VirtualRouter)).thenReturn(true);

        lenient().when(virtualRouterElement._dcDao.findById(dataCenterId)).thenReturn(Mockito.mock(DataCenterVO.class));

        when(virtualRouterElement.canHandle(network, service)).thenReturn(false);

        assertTrue(virtualRouterElement.addPasswordAndUserdata(network, nic, vm, dest, context));
    }
}
