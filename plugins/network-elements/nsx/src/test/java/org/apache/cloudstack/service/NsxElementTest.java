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
package org.apache.cloudstack.service;

import com.cloud.api.ApiDBUtils;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.element.PortForwardingServiceProvider;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.nsx.NsxVpnGatewayResult;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.NetworkACLItemVO;
import com.cloud.network.vpn.Site2SiteVpnManagerImpl;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.network.vpc.dao.VpcServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.apache.cloudstack.agent.api.CreateNsxVpnGatewayCommand;
import org.apache.cloudstack.resource.NsxNetworkRule;
import org.apache.cloudstack.resource.NsxResource;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.Site2SiteVpnGatewayDetailVO;
import org.apache.cloudstack.resourcedetail.dao.Site2SiteVpnGatewayDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxElementTest {

    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    NsxServiceImpl nsxService;
    @Mock
    AccountManager accountManager;
    @Mock
    NetworkDao networkDao;
    @Mock
    ResourceManager resourceManager;
    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    VpcVO vpc;
    @Mock
    DataCenterVO zone;
    @Mock
    DataCenterVO dataCenterVO;
    @Mock
    Account account;
    @Mock
    DomainVO domain;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    UserVmDao userVmDao;
    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    LoadBalancerVMMapDao lbVmMapDao;
    @Mock
    LoadBalancerDao loadBalancerDao;
    @Mock
    FirewallRuleDetailsDao firewallRuleDetailsDao;
    @Mock
    IpAddressManager ipAddressManager;
    @Mock
    VpcService vpcService;
    @Mock
    VpcManager vpcManager;
    @Mock
    Site2SiteVpnGatewayDao vpnGatewayDao;
    @Mock
    Site2SiteVpnConnectionDao vpnConnectionDao;
    @Mock
    Site2SiteCustomerGatewayDao customerGatewayDao;
    @Mock
    UserIpAddressDetailsDao userIpAddressDetailsDao;
    @Mock
    FirewallRulesDao firewallRulesDao;
    @Mock
    PortForwardingRulesDao portForwardingRulesDao;

    NsxElement nsxElement;
    ReservationContext reservationContext;
    DeployDestination deployDestination;
    @Mock
    DomainDao domainDao;

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        nsxElement = new NsxElement();

        nsxElement.dataCenterDao = dataCenterDao;
        nsxElement.nsxService = nsxService;
        nsxElement.accountMgr = accountManager;
        nsxElement.networkDao = networkDao;
        nsxElement.resourceManager = resourceManager;
        nsxElement.physicalNetworkDao = physicalNetworkDao;
        nsxElement.domainDao = domainDao;
        nsxElement.networkModel = networkModel;
        nsxElement.vpcOfferingServiceMapDao = vpcOfferingServiceMapDao;
        nsxElement.ipAddressDao = ipAddressDao;
        nsxElement.vmInstanceDao = vmInstanceDao;
        nsxElement.vpcDao = vpcDao;
        nsxElement.lbVmMapDao = lbVmMapDao;
        nsxElement.loadBalancerDao = loadBalancerDao;
        nsxElement.firewallRuleDetailsDao = firewallRuleDetailsDao;
        nsxElement.ipAddressManager = ipAddressManager;
        nsxElement.vpcService = vpcService;
        nsxElement.vpcManager = vpcManager;
        nsxElement.vpnGatewayDao = vpnGatewayDao;
        nsxElement.vpnConnectionDao = vpnConnectionDao;
        nsxElement.customerGatewayDao = customerGatewayDao;
        nsxElement.userIpAddressDetailsDao = userIpAddressDetailsDao;
        nsxElement.firewallRulesDao = firewallRulesDao;
        nsxElement.portForwardingRulesDao = portForwardingRulesDao;
        Mockito.lenient().when(ipAddressManager.disassociatePublicIpAddress(any(), anyLong(), any())).thenReturn(true);
        Mockito.lenient().when(ipAddressDao.lockInLockTable(anyString())).thenReturn(true);
        Mockito.lenient().when(loadBalancerDao.listByIpAddress(anyLong())).thenReturn(List.of());
        Mockito.lenient().when(firewallRulesDao.listByIpAndNotRevoked(anyLong())).thenReturn(List.of());
        Mockito.lenient().when(portForwardingRulesDao.listByIpAndNotRevoked(anyLong())).thenReturn(List.of());

        Field field = ApiDBUtils.class.getDeclaredField("s_ipAddressDao");
        field.setAccessible(true);
        field.set(null, ipAddressDao);

        field = ApiDBUtils.class.getDeclaredField("s_userVmDao");
        field.setAccessible(true);
        field.set(null, userVmDao);
        reservationContext = mock(ReservationContext.class);
        deployDestination = mock(DeployDestination.class);

        when(vpc.getZoneId()).thenReturn(1L);
        Mockito.lenient().when(vpcDao.acquireInLockTable(anyLong())).thenReturn(vpc);
        when(vpc.getAccountId()).thenReturn(2L);
        when(dataCenterVO.getId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("VPC01");
        when(accountManager.getAccount(2L)).thenReturn(account);
        when(dataCenterDao.findById(anyLong())).thenReturn(dataCenterVO);
        when(domainDao.findById(anyLong())).thenReturn(domain);
        when(vpc.getZoneId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("testVPC");

        PhysicalNetworkVO physicalNetworkVO = new PhysicalNetworkVO();
        physicalNetworkVO.setIsolationMethods(List.of("NSX"));
        List<PhysicalNetworkVO> physicalNetworkVOList = List.of(physicalNetworkVO);

        when(physicalNetworkDao.listByZoneAndTrafficType(1L, Networks.TrafficType.Guest)).thenReturn(physicalNetworkVOList);
    }

    @Test
    public void testImplementVpc() throws ResourceUnavailableException, InsufficientCapacityException {
        assertTrue(nsxElement.implementVpc(vpc, deployDestination, reservationContext));
    }

    @Test
    public void testShutdownEnabledVpcWithoutNativeEndpointDeletesTier1() {
        when(vpc.getState()).thenReturn(Vpc.State.Enabled);
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString())).thenReturn(true);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        verify(nsxService).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    public void testCleanupRestartPreservesNativeVpnWithOperatorOwnedEndpoint() throws Exception {
        verifyCleanupRestartPreservesNativeVpn("false");
    }

    @Test
    public void testShutdownVpcUsesFreshLockedStateInsteadOfStaleCallerState() {
        VpcVO staleVpc = new VpcVO();
        ReflectionTestUtils.setField(staleVpc, "id", 9L);
        staleVpc.setState(Vpc.State.Inactive);
        when(vpc.getId()).thenReturn(9L);
        when(vpc.getState()).thenReturn(Vpc.State.Enabled);
        IPAddressVO endpoint = mock(IPAddressVO.class);
        when(endpoint.getId()).thenReturn(30L);
        when(ipAddressDao.listByAssociatedVpc(9L, null)).thenReturn(List.of(endpoint));
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false));

        assertTrue(nsxElement.shutdownVpc(staleVpc, reservationContext));

        org.mockito.InOrder order = Mockito.inOrder(vpcDao, ipAddressDao);
        order.verify(vpcDao).acquireInLockTable(9L);
        order.verify(ipAddressDao).listByAssociatedVpc(9L, null);
        order.verify(vpcDao).releaseFromLockTable(9L);
        verify(nsxService, never()).deleteVpnConnection(any(), anyLong());
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), any());
    }

    @Test
    public void testShutdownVpcCannotDeleteBackendWithoutVpcLock() {
        when(vpc.getId()).thenReturn(9L);
        when(vpcDao.acquireInLockTable(9L)).thenReturn(null);
        when(vpcDao.findById(9L)).thenReturn(vpc);

        Assert.assertThrows(ConcurrentOperationException.class, () -> nsxElement.shutdownVpc(vpc, reservationContext));

        verify(nsxService, never()).deleteVpnConnection(any(), anyLong());
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), any());
        verify(vpcDao, never()).releaseFromLockTable(anyLong());
    }

    @Test
    public void testCleanupRestartPreservesRetainedAutoAllocatedEndpointWithoutGatewayRow() throws Exception {
        verifyCleanupRestartPreservesNativeVpn("true");
    }

    private void verifyCleanupRestartPreservesNativeVpn(String ownershipValue) throws Exception {
        VpcManagerImpl manager = Mockito.spy(new VpcManagerImpl());
        VpcServiceMapDao serviceMapDao = mock(VpcServiceMapDao.class);
        ReflectionTestUtils.setField(manager, "vpcDao", vpcDao);
        ReflectionTestUtils.setField(manager, "_accountMgr", accountManager);
        ReflectionTestUtils.setField(manager, "_vpcSrvcDao", serviceMapDao);
        ReflectionTestUtils.setField(manager, "_ntwkModel", networkModel);
        ReflectionTestUtils.setField(manager, "extensionHelper", mock(ExtensionHelper.class));
        manager.setVpcElements(List.of(nsxElement));
        VpcVO enabledVpc = new VpcVO();
        ReflectionTestUtils.setField(enabledVpc, "id", 9L);
        ReflectionTestUtils.setField(enabledVpc, "zoneId", 1L);
        ReflectionTestUtils.setField(enabledVpc, "accountId", 2L);
        enabledVpc.setState(Vpc.State.Enabled);
        User user = mock(User.class);
        when(user.getAccountId()).thenReturn(2L);
        when(accountManager.getActiveAccountById(2L)).thenReturn(account);
        when(accountManager.getActiveUser(anyLong())).thenReturn(user);
        when(vpcDao.acquireInLockTable(9L)).thenReturn(enabledVpc);
        when(vpcDao.getActiveVpcById(9L)).thenReturn(enabledVpc);
        when(vpcDao.findById(9L)).thenReturn(enabledVpc);
        when(serviceMapDao.getDistinctProviders(9L)).thenReturn(List.of(Network.Provider.Nsx.getName()));
        when(networkModel.resolveProvider(Network.Provider.Nsx.getName())).thenReturn(Network.Provider.Nsx);
        IPAddressVO endpoint = mock(IPAddressVO.class);
        when(endpoint.getId()).thenReturn(30L);
        when(ipAddressDao.listByAssociatedVpc(9L, null)).thenReturn(List.of(endpoint));
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", ownershipValue, false));
        Mockito.doReturn(true).when(manager).startVpc(9L, false);
        Mockito.doNothing().when(manager).reconfigStaticNatForVpcVr(9L);

        ConfigDepotImpl priorDepot = (ConfigDepotImpl) ReflectionTestUtils.getField(ConfigKey.class, "s_depot");
        Object priorValue = ReflectionTestUtils.getField(NetworkOrchestrationService.RollingRestartEnabled, "_value");
        ConfigDepotImpl depot = mock(ConfigDepotImpl.class);
        when(depot.getConfigStringValue("network.rolling.restart", ConfigKey.Scope.Global, null)).thenReturn("false");
        CallContext.register(user, account);
        ConfigKey.init(depot);
        try {
            assertTrue(manager.restartVpc(9L, true, false, false, user));
        } finally {
            ConfigKey.init(priorDepot);
            ReflectionTestUtils.setField(NetworkOrchestrationService.RollingRestartEnabled, "_value", priorValue);
            CallContext.unregister();
        }

        org.mockito.InOrder order = Mockito.inOrder(vpcDao, manager, ipAddressDao);
        order.verify(vpcDao).getActiveVpcById(9L);
        order.verify(manager).shutdownVpc(9L);
        order.verify(vpcDao).acquireInLockTable(9L);
        order.verify(ipAddressDao).listByAssociatedVpc(9L, null);
        order.verify(vpcDao).releaseFromLockTable(9L);
        order.verify(manager).startVpc(9L, false);
        assertEquals(Vpc.State.Enabled, enabledVpc.getState());
        verify(nsxService, never()).deleteVpnConnection(any(), anyLong());
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), any());
        verify(vpnConnectionDao, never()).remove(anyLong());
        verify(vpnGatewayDao, never()).findByVpcId(anyLong());
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
    }

    @Test
    public void testShutdownVpcDeletesEveryNativeConnectionBeforeTier1WithoutRemovingRows() {
        when(vpc.getId()).thenReturn(9L);
        when(vpc.getState()).thenReturn(Vpc.State.Inactive);
        Site2SiteVpnConnectionVO first = mockConnectionForVpcShutdown(5L, 7L);
        Site2SiteVpnConnectionVO second = mockConnectionForVpcShutdown(6L, 7L);
        Site2SiteVpnConnectionVO legacy = mockConnectionForVpcShutdown(8L, 8L);
        when(vpnConnectionDao.listByVpcId(9L)).thenReturn(List.of(first, second, legacy));
        mockGatewayForVpcShutdown(7L, 30L, true);
        mockGatewayForVpcShutdown(8L, 31L, false);
        when(nsxService.deleteVpnConnection(vpc, 5L)).thenReturn(true);
        when(nsxService.deleteVpnConnection(vpc, 6L)).thenReturn(true);
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString())).thenReturn(true);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        org.mockito.InOrder order = Mockito.inOrder(vpnConnectionDao, nsxService);
        order.verify(vpnConnectionDao).acquireInLockTable(5L);
        order.verify(nsxService).deleteVpnConnection(vpc, 5L);
        order.verify(vpnConnectionDao).releaseFromLockTable(5L);
        order.verify(vpnConnectionDao).acquireInLockTable(6L);
        order.verify(nsxService).deleteVpnConnection(vpc, 6L);
        order.verify(vpnConnectionDao).releaseFromLockTable(6L);
        order.verify(vpnConnectionDao).acquireInLockTable(8L);
        order.verify(vpnConnectionDao).releaseFromLockTable(8L);
        order.verify(nsxService).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString());
        verify(nsxService, never()).deleteVpnConnection(vpc, 8L);
        verify(vpnConnectionDao, never()).remove(anyLong());
    }

    @Test
    public void testShutdownVpcRetriesConnectionCleanupWithDurableIdBeforeDeletingTier1() {
        when(vpc.getId()).thenReturn(9L);
        when(vpc.getState()).thenReturn(Vpc.State.Inactive);
        Site2SiteVpnConnectionVO connection = mockConnectionForVpcShutdown(5L, 7L);
        when(vpnConnectionDao.listByVpcId(9L)).thenReturn(List.of(connection));
        mockGatewayForVpcShutdown(7L, 30L, true);
        // First attempt deletes the session but cannot remove all its global profiles.
        when(nsxService.deleteVpnConnection(vpc, 5L)).thenReturn(false, true);
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString())).thenReturn(true);

        assertFalse(nsxElement.shutdownVpc(vpc, reservationContext));
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString());
        verify(vpnConnectionDao, never()).remove(anyLong());

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        verify(nsxService, Mockito.times(2)).deleteVpnConnection(vpc, 5L);
        verify(vpnConnectionDao, Mockito.times(2)).acquireInLockTable(5L);
        verify(vpnConnectionDao, Mockito.times(2)).releaseFromLockTable(5L);
        verify(nsxService).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString());
        verify(vpnConnectionDao, never()).remove(anyLong());
    }

    @Test
    public void testShutdownVpcPreservesRowsAndReleasesLockOnBackendException() {
        when(vpc.getId()).thenReturn(9L);
        Site2SiteVpnConnectionVO connection = mockConnectionForVpcShutdown(5L, 7L);
        when(vpnConnectionDao.listByVpcId(9L)).thenReturn(List.of(connection));
        mockGatewayForVpcShutdown(7L, 30L, true);
        when(nsxService.deleteVpnConnection(vpc, 5L)).thenThrow(new CloudRuntimeException("profile cleanup failed"));

        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.shutdownVpc(vpc, reservationContext));

        verify(vpnConnectionDao).releaseFromLockTable(5L);
        verify(vpnConnectionDao, never()).remove(anyLong());
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString());
    }

    @Test
    public void testShutdownVpcDoesNotDeleteTier1WhenAnExistingConnectionCannotBeLocked() {
        when(vpc.getId()).thenReturn(9L);
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        when(connection.getId()).thenReturn(5L);
        when(vpnConnectionDao.listByVpcId(9L)).thenReturn(List.of(connection));
        when(vpnConnectionDao.findById(5L)).thenReturn(connection);

        Assert.assertThrows(ConcurrentOperationException.class, () -> nsxElement.shutdownVpc(vpc, reservationContext));

        verify(nsxService, never()).deleteVpnConnection(any(), anyLong());
        verify(nsxService, never()).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString());
        verify(vpnConnectionDao, never()).releaseFromLockTable(anyLong());
    }

    @Test
    public void testPermanentVpcShutdownReleasesOrphanedPortableEndpointReservation() {
        IPAddressVO ip = mockPortableVpnEndpointForShutdown(true);
        when(ipAddressDao.acquireInLockTable(30L)).thenReturn(ip);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        org.mockito.InOrder order = Mockito.inOrder(nsxService, ipAddressDao, userIpAddressDetailsDao);
        order.verify(nsxService).deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString());
        order.verify(ipAddressDao).acquireInLockTable(30L);
        order.verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        order.verify(ipAddressDao).releaseFromLockTable(30L);
        assertTrue(ip.isPortable());
        assertEquals(Long.valueOf(9L), ip.getVpcId());
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testFailedVpcShutdownPreservesOrphanedPortableEndpointReservation() {
        IPAddressVO ip = mockPortableVpnEndpointForShutdown(false);

        assertFalse(nsxElement.shutdownVpc(vpc, reservationContext));

        assertTrue(nsxElement.isIpAddressReserved(ip));
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(ipAddressDao, never()).acquireInLockTable(anyLong());
    }

    @Test
    public void testPermanentVpcShutdownPreservesPortableGatewayOwnershipForGatewayCleanup() {
        IPAddressVO ip = mockPortableVpnEndpointForShutdown(true);
        Site2SiteVpnGatewayVO gateway = mock(Site2SiteVpnGatewayVO.class);
        when(gateway.getAddrId()).thenReturn(30L);
        when(vpnGatewayDao.findByVpcId(9L)).thenReturn(gateway);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        assertTrue(nsxElement.isIpAddressReserved(ip));
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(ipAddressDao, never()).acquireInLockTable(anyLong());
    }

    @Test
    public void testPermanentVpcShutdownDoesNotClearPortableEndpointMovedToAnotherVpc() {
        IPAddressVO listedIp = mockPortableVpnEndpointForShutdown(true);
        IPAddressVO freshIp = new IPAddressVO(new Ip("203.0.113.30"), 1L, 1L, 1L, false);
        freshIp.setPortable(true);
        freshIp.setVpcId(10L);
        when(ipAddressDao.acquireInLockTable(30L)).thenReturn(freshIp);

        assertTrue(nsxElement.shutdownVpc(vpc, reservationContext));

        assertTrue(nsxElement.isIpAddressReserved(listedIp));
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(ipAddressDao).releaseFromLockTable(30L);
    }

    private IPAddressVO mockPortableVpnEndpointForShutdown(boolean backendSuccess) {
        when(vpc.getId()).thenReturn(9L);
        when(vpc.getState()).thenReturn(Vpc.State.Inactive);
        when(nsxService.deleteVpcNetwork(anyLong(), anyLong(), anyLong(), eq(9L), anyString())).thenReturn(backendSuccess);
        IPAddressVO ip = new IPAddressVO(new Ip("203.0.113.30"), 1L, 1L, 1L, false);
        ReflectionTestUtils.setField(ip, "id", 30L);
        ip.setPortable(true);
        ip.setVpcId(9L);
        Mockito.lenient().when(ipAddressDao.listByAssociatedVpc(9L, null)).thenReturn(List.of(ip));
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false));
        return ip;
    }

    private Site2SiteVpnConnectionVO mockConnectionForVpcShutdown(long id, long gatewayId) {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        when(connection.getId()).thenReturn(id);
        when(connection.getVpnGatewayId()).thenReturn(gatewayId);
        when(vpnConnectionDao.acquireInLockTable(id)).thenReturn(connection);
        return connection;
    }

    private void mockGatewayForVpcShutdown(long gatewayId, long ipId, boolean nativeGateway) {
        Site2SiteVpnGatewayVO gateway = mock(Site2SiteVpnGatewayVO.class);
        when(gateway.getAddrId()).thenReturn(ipId);
        when(gateway.getVpcId()).thenReturn(9L);
        mockIpAddressVO(ipId, "10.1.13.30");
        when(vpnGatewayDao.findById(gatewayId)).thenReturn(gateway);
        if (nativeGateway) {
            when(userIpAddressDetailsDao.findDetail(ipId, "nsxVpnGatewayIp"))
                    .thenReturn(new UserIpAddressDetailVO(ipId, "nsxVpnGatewayIp", "false", false));
        }
    }

    @Test
    public void testTransformActionValue() {
        NsxNetworkRule.NsxRuleAction action = nsxElement.transformActionValue(NetworkACLItem.Action.Deny);
        Assert.assertEquals(NsxNetworkRule.NsxRuleAction.DROP, action);
    }

    @Test
    public void testTransformCidrListValuesEmptyList() {
        List<String> values = nsxElement.transformCidrListValues(null);
        Assert.assertNotNull(values);
        Assert.assertTrue(values.isEmpty());
    }

    @Test
    public void testTransformCidrListValuesList() {
        List<String> values = nsxElement.transformCidrListValues(List.of("0.0.0.0/0"));
        Assert.assertEquals(1, values.size());
        Assert.assertEquals("ANY", values.get(0));
    }

    @Test
    public void testCanHandleService() {
        when(networkModel.isProviderForNetwork(any(Network.Provider.class), anyLong())).thenReturn(true);

        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));
        NetworkVO network = new NetworkVO();
        network.setName("network1");
        assertTrue(nsxElement.canHandle(network, service));
    }

    @Test
    public void testApplyStaticNatRules() throws ResourceUnavailableException {
        StaticNatImpl rule = new StaticNatImpl(1L , 1L, 3L, 7L, "172.30.10.15", false);
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );

        Ip ip = new Ip("10.1.13.15");
        IPAddressVO ipAddress = new IPAddressVO(ip, 2L, 0xaabbccddeeffL, 3L, false);
        ipAddress.setAssociatedWithVmId(10L);

        VMInstanceVO vm = new VMInstanceVO(10L, 9L, "vm1", "i-5-10-VM" , VirtualMachine.Type.User,
                18L, Hypervisor.HypervisorType.VMware, 26L,
        2L, 5L, 6L, false, false);

        NicVO nic = Mockito.mock(NicVO.class);
        VpcVO vpc = Mockito.mock(VpcVO.class);

        when(ipAddressDao.findByIdIncludingRemoved(anyLong())).thenReturn(ipAddress);
        when(vmInstanceDao.findByIdIncludingRemoved(anyLong())).thenReturn(vm);
        when(networkModel.getNicInNetworkIncludingRemoved(anyLong(), anyLong())).thenReturn(nic);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getId()).thenReturn(1L);
        when(vpc.getName()).thenReturn("vpc1");
        when(nsxService.createStaticNatRule(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyBoolean(), anyLong(), anyString(), anyString())).thenReturn(true);

        assertTrue(nsxElement.applyStaticNats(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyPFRules_add() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        PortForwardingRuleVO rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
        5L, 2L, 15L);
        rule.setState(FirewallRule.State.Add);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        assertTrue(nsxElement.applyPFRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyPFRules_delete() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        PortForwardingRuleVO rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        rule.setState(FirewallRule.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        when(vpcDao.findById(1L)).thenReturn(vpcVO);
        when(vpcVO.getDomainId()).thenReturn(2L);
        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(ApiDBUtils.findIpAddressById(anyLong())).thenReturn(ipAddress);
        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deletePortForwardRule(any(NsxNetworkRule.class))).thenReturn(true);
        assertTrue(nsxElement.applyPFRules(networkVO, List.of(rule)));
    }

    @Test
    public void testGetVpcOrNetworkReturnsVpcIfVpcIdPresent() {
        VpcVO vpc = new VpcVO();
        when(vpcDao.findById(anyLong())).thenReturn(vpc);

        Pair<VpcVO, NetworkVO> vpcNetworkPair = nsxElement.getVpcOrNetwork(1L, 1L);
        assertNotNull(vpcNetworkPair.first());
        assertNull(vpcNetworkPair.second());
    }

    @Test
    public void testGetVpcOrNetworkReturnsNetworkIfVpcIdNotPresent() {
        NetworkVO network = new NetworkVO();
        when(networkDao.findById(anyLong())).thenReturn(network);

        Pair<VpcVO, NetworkVO> vpcNetworkPair = nsxElement.getVpcOrNetwork(null, 1L);
        assertNull(vpcNetworkPair.first());
        assertNotNull(vpcNetworkPair.second());
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80-90", PortForwardingServiceProvider.getPublicPortRange(rule));
    }

    @Test
    public void testGetPublicPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("80", PortForwardingServiceProvider.getPublicPortRange(rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 90, new Ip("172.30.10.11"), 8080, 8090, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080-8090", PortForwardingServiceProvider.getPrivatePFPortRange(rule));
    }

    @Test
    public void testGetPrivatePFPortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        PortForwardingRule rule = new PortForwardingRuleVO("1", 11L, 80, 80, new Ip("172.30.10.11"), 8080, 8080, "tcp", 12L,
                5L, 2L, 15L);
        assertEquals("8080", PortForwardingServiceProvider.getPrivatePFPortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
        FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80", PortForwardingServiceProvider.getPrivatePortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 90, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        assertEquals("80-90", PortForwardingServiceProvider.getPrivatePortRange(rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreSame() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
        2, null);
        assertEquals("80", PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule));
    }

    @Test
    public void testGetPrivatePortRangeForACLWhenStartAndEndPortNumbersAreDifferent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        NetworkACLItem rule = new NetworkACLItemVO(80, 90, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        assertEquals("80-90", PortForwardingServiceProvider.getPrivatePortRangeForACLRule(rule));
    }

    @Test
    public void testApplyLBRules_add() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 8080, 8081, null, 0L, 0L, 1L, null, null);
        lb.setState(FirewallRule.State.Add);
        LoadBalancingRule.LbDestination destination = new LoadBalancingRule.LbDestination(6443, 6443, "172.30.110.11", false);
        LoadBalancingRule rule = new LoadBalancingRule(lb, List.of(destination), null, null, new Ip("10.1.13.10"), null, "TCP");

        VpcVO vpc = Mockito.mock(VpcVO.class);

        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getDomainId()).thenReturn(2L);
        when(vpc.getAccountId()).thenReturn(5L);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddress);
        when(nsxService.createLbRule(any(NsxNetworkRule.class))).thenReturn(true);

        assertTrue(nsxElement.applyLBRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyLBRules_delete() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        LoadBalancerVO lb = new LoadBalancerVO(null, null, null, 0L, 8080, 8081, null, 0L, 0L, 1L, null, null);
        lb.setState(FirewallRule.State.Revoke);
        LoadBalancingRule.LbDestination destination = new LoadBalancingRule.LbDestination(6443, 6443, "172.30.110.11", false);
        LoadBalancingRule rule = new LoadBalancingRule(lb, List.of(destination), null, null, new Ip("10.1.13.10"), null, "TCP");

        VpcVO vpc = Mockito.mock(VpcVO.class);

        IPAddressVO ipAddress = new IPAddressVO(new Ip("10.1.13.10"), 1L, 1L, 1L,false);
        when(vpcDao.findById(anyLong())).thenReturn(vpc);
        when(vpc.getDomainId()).thenReturn(2L);
        when(vpc.getAccountId()).thenReturn(5L);
        when(ipAddressDao.findByIpAndDcId(anyLong(), anyString())).thenReturn(ipAddress);
        when(nsxService.deleteLbRule(any(NsxNetworkRule.class))).thenReturn(true);

        assertTrue(nsxElement.applyLBRules(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyNetworkAclRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        NetworkACLItem rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        assertTrue(nsxElement.applyNetworkACLs(networkVO, List.of(rule)));
    }

    @Test
    public void testDeleteNetworkAclRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        NetworkACLItemVO rule = new NetworkACLItemVO(80, 80, "udp", 10L, List.of("172.30.10.0/24"), null, null, NetworkACLItem.TrafficType.Ingress, NetworkACLItem.Action.Allow,
                2, null);
        rule.setState(NetworkACLItem.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deleteFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyNetworkACLs(networkVO, List.of(rule)));
    }

    @Test
    public void testApplyFirewallRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.addFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyFWRules(networkVO, List.of(rule)));
    }

    @Test
    public void testRevokeFirewallRules() throws ResourceUnavailableException {
        NetworkVO networkVO = new NetworkVO(1L, Networks.TrafficType.Public, Networks.Mode.Static,
                Networks.BroadcastDomainType.NSX, 12L, 2L, 5L, 1L, "network1",
                "network1", null, Network.GuestType.Isolated, 2L, 2L,
                ControlledEntity.ACLType.Domain, false, 1L, false );
        FirewallRuleVO rule = new FirewallRuleVO("1", 11L, 80, 80, "tcp", 23L, 5L, 2L,
                FirewallRule.Purpose.Firewall, List.of("172.30.10.0/24"), null, null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.User);
        rule.setState(FirewallRule.State.Revoke);
        Network.Service service = new Network.Service("service1", new Network.Capability("capability"));

        when(nsxElement.canHandle(networkVO, service)).thenReturn(true);
        when(nsxService.deleteFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        when(nsxService.addFirewallRules(any(Network.class), any(List.class))).thenReturn(true);
        assertTrue(nsxElement.applyFWRules(networkVO, List.of(rule)));
    }

    private VpcVO mockVpcWithNsxVpnSupport() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Mockito.lenient().when(vpcVO.getId()).thenReturn(9L);
        when(vpcManager.isProviderSupportServiceInVpc(9L, Network.Service.Vpn, Network.Provider.Nsx))
                .thenReturn(true);
        return vpcVO;
    }

    private IPAddressVO mockIpAddressVO(long id, String address) {
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        when(ipAddressDao.findById(id)).thenReturn(ipAddressVO);
        Mockito.lenient().when(ipAddressDao.acquireInLockTable(id)).thenReturn(ipAddressVO);
        Mockito.lenient().when(ipAddressVO.getId()).thenReturn(id);
        Mockito.lenient().when(ipAddressVO.getVpcId()).thenReturn(9L);
        Mockito.lenient().when(ipAddressVO.getAllocatedToAccountId()).thenReturn(0L);
        Mockito.lenient().when(ipAddressVO.getAssociatedWithNetworkId()).thenReturn(null);
        Mockito.lenient().when(ipAddressVO.getAddress()).thenReturn(new Ip(address));
        Mockito.lenient().when(ipAddressVO.readyToUse()).thenReturn(true);
        Mockito.lenient().when(ipAddressVO.getRemoved()).thenReturn(null);
        return ipAddressVO;
    }

    private IPAddressVO mockNewlyAllocatedVpnGatewayIp(VpcVO vpcVO, long id, String address) throws Exception {
        IPAddressVO ip = mockIpAddressVO(id, address);
        AtomicBoolean associated = new AtomicBoolean();
        long accountId = vpcVO.getAccountId();
        when(ip.getAllocatedToAccountId()).thenReturn(accountId);
        when(ip.getVpcId()).thenAnswer(invocation -> associated.get() ? vpcVO.getId() : null);
        when(ip.getState()).thenAnswer(invocation -> associated.get() ? IpAddress.State.Allocated : IpAddress.State.Allocating);
        when(ip.readyToUse()).thenAnswer(invocation -> associated.get());
        when(ipAddressManager.allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any())).thenReturn(ip);
        when(vpcService.associateIPToVpc(id, vpcVO.getId())).thenAnswer(invocation -> {
            associated.set(true);
            return ip;
        });
        return ip;
    }

    @Test
    public void testAcquireVpnGatewayIpWithRequestedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(true, true));

        IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
        assertEquals(ipAddressVO, result);
        org.mockito.InOrder order = Mockito.inOrder(ipAddressDao, userIpAddressDetailsDao, nsxService);
        order.verify(ipAddressDao).acquireInLockTable(20L);
        order.verify(ipAddressDao).findById(20L);
        order.verify(userIpAddressDetailsDao).addDetail(20L, "nsxVpnGatewayIp", "false", false);
        order.verify(nsxService).createVpnGateway(vpcVO, "10.1.13.20", false);
        order.verify(ipAddressDao).releaseFromLockTable(20L);
    }

    @Test
    public void testAcquireVpnGatewayIpDoesNotCallNsxWhenRequestedIpOwnershipCannotBeRecorded() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        Mockito.doThrow(new CloudRuntimeException("marker write failed")).when(userIpAddressDetailsDao)
                .addDetail(20L, "nsxVpnGatewayIp", "false", false);

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(nsxService, never()).createVpnGateway(any(Vpc.class), anyString(), anyBoolean());
    }

    @Test
    public void testAcquireVpnGatewayIpRemovesRequestedIpOwnershipWhenNsxRejectsGateway() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao).removeDetail(20L, "nsxVpnGatewayIp");
    }

    @Test
    public void testAcquireVpnGatewayIpRetainsRequestedIpOwnershipWhenNsxResultIsAmbiguous() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", false)).thenReturn(new NsxVpnGatewayResult(false, true));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao, never()).removeDetail(20L, "nsxVpnGatewayIp");
    }

    @Test
    public void testAcquireVpnGatewayIpPreservesExistingRequestedIpOwnershipOnUnambiguousFailure() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(loadBalancerDao.listByIpAddress(20L)).thenReturn(List.of());
        UserIpAddressDetailVO existingMarker = new UserIpAddressDetailVO(
                20L, "nsxVpnGatewayIp", "false", false);
        when(userIpAddressDetailsDao.findDetail(20L, "nsxVpnGatewayIp")).thenReturn(existingMarker);
        when(nsxService.createVpnGateway(vpcVO, "10.1.13.20", true))
                .thenReturn(new NsxVpnGatewayResult(false, false));

        Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(userIpAddressDetailsDao, never()).removeDetail(20L, "nsxVpnGatewayIp");
        verify(nsxService).createVpnGateway(vpcVO, "10.1.13.20", true);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsSourceNatIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(ipAddressVO.readyToUse()).thenReturn(true);
        when(ipAddressVO.getRemoved()).thenReturn(null);
        when(ipAddressVO.isSourceNat()).thenReturn(true);
        when(ipAddressVO.getAddress()).thenReturn(new Ip("10.1.13.20"));

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsIpWithPortForwardingRule() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(20L);
        IPAddressVO ipAddressVO = mockIpAddressVO(20L, "10.1.13.20");
        when(ipAddressVO.getId()).thenReturn(20L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(portForwardingRulesDao.listByIpAndNotRevoked(20L))
                .thenReturn(List.of(Mockito.mock(PortForwardingRuleVO.class)));

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireVpnGatewayIpRejectsAnUnallocatedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IpAddress requestedIp = Mockito.mock(IpAddress.class);
        when(requestedIp.getId()).thenReturn(21L);
        IPAddressVO ipAddressVO = mockIpAddressVO(21L, "10.1.13.21");
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        when(ipAddressVO.readyToUse()).thenReturn(false);

        nsxElement.acquireVpnGatewayIp(vpcVO, requestedIp);
    }

    @Test
    public void testAcquireVpnGatewayIpRejectsRouterIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IPAddressVO ip = mockIpAddressVO(20L, "10.1.13.20");
        when(ip.isForRouter()).thenReturn(true);

        Assert.assertThrows(InvalidParameterValueException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, ip));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(nsxService, never()).createVpnGateway(any(), anyString(), anyBoolean());
        verify(ipAddressDao).releaseFromLockTable(20L);
    }

    @Test
    public void testAcquireVpnGatewayIpRejectsTierAssignedIpBeforeClaimingOwnership() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IPAddressVO ip = mockIpAddressVO(20L, "10.1.13.20");
        when(ip.getAssociatedWithNetworkId()).thenReturn(4L);

        Assert.assertThrows(InvalidParameterValueException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, ip));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(nsxService, never()).createVpnGateway(any(), anyString(), anyBoolean());
        verify(ipAddressDao).releaseFromLockTable(20L);
    }

    @Test
    public void testAcquireVpnGatewayIpRevalidatesAfterWaitingForIpLock() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        IPAddressVO ip = mockIpAddressVO(20L, "10.1.13.20");
        AtomicBoolean sourceNat = new AtomicBoolean();
        when(ip.isSourceNat()).thenAnswer(invocation -> sourceNat.get());
        when(ipAddressDao.acquireInLockTable(20L)).thenAnswer(invocation -> {
            // A normal source-NAT update completes before this attempt obtains the IP lock.
            sourceNat.set(true);
            return ip;
        });

        Assert.assertThrows(InvalidParameterValueException.class,
                () -> nsxElement.acquireVpnGatewayIp(vpcVO, ip));

        verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
        verify(nsxService, never()).createVpnGateway(any(), anyString(), anyBoolean());
        verify(ipAddressDao).releaseFromLockTable(20L);
    }

    @Test
    public void testAcquireVpnGatewayIpReturnsNullWhenVpnIsNotProvidedByNsx() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);

        assertNull(nsxElement.acquireVpnGatewayIp(vpcVO, null));
    }

    @Test
    public void testAcquireVpnGatewayIpAutoAcquiresWhenNoIpIsRequested() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IPAddressVO ipAddressVO = mockNewlyAllocatedVpnGatewayIp(vpcVO, 30L, "10.1.13.30");
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.30", false)).thenReturn(new NsxVpnGatewayResult(true, true));

            IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, null);
            assertEquals(ipAddressVO, result);
            org.mockito.InOrder order = Mockito.inOrder(ipAddressDao, vpcService, userIpAddressDetailsDao, nsxService);
            order.verify(ipAddressDao).acquireInLockTable(30L);
            order.verify(vpcService).associateIPToVpc(30L, 9L);
            order.verify(userIpAddressDetailsDao).addDetail(30L, "nsxVpnGatewayIp", "true", false);
            order.verify(ipAddressDao).releaseFromLockTable(30L);
            order.verify(ipAddressDao).acquireInLockTable(30L);
            order.verify(nsxService).createVpnGateway(vpcVO, "10.1.13.30", false);
            order.verify(ipAddressDao).releaseFromLockTable(30L);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testAcquireVpnGatewayIpDoesNotReleaseAllocationThatChangedOwnerBeforeLock() throws Exception {
        CallContext.register(mock(User.class), mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IPAddressVO ip = mockNewlyAllocatedVpnGatewayIp(vpcVO, 30L, "10.1.13.30");
            when(ip.getAllocatedToAccountId()).thenReturn(44L);

            Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.acquireVpnGatewayIp(vpcVO, null));

            verify(vpcService, never()).associateIPToVpc(anyLong(), anyLong());
            verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
            verify(nsxService, never()).createVpnGateway(any(), anyString(), anyBoolean());
            verify(ipAddressDao).releaseFromLockTable(30L);
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testAcquireVpnGatewayIpReusesRetainedIpAfterAmbiguousCreate() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            IPAddressVO retainedIp = mockIpAddressVO(32L, "10.1.13.32");
            when(retainedIp.getId()).thenReturn(32L);
            when(retainedIp.getVpcId()).thenReturn(9L);
            UserIpAddressDetailVO retainedMarker = new UserIpAddressDetailVO(
                    32L, "nsxVpnGatewayIp", "true", false);
            when(ipAddressDao.listByAssociatedVpc(9L, false)).thenReturn(List.of(retainedIp));
            when(userIpAddressDetailsDao.findDetail(32L, "nsxVpnGatewayIp")).thenReturn(retainedMarker);
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.32", true))
                    .thenReturn(new NsxVpnGatewayResult(true, true));

            IpAddress result = nsxElement.acquireVpnGatewayIp(vpcVO, null);

            assertEquals(retainedIp, result);
            verify(nsxService).createVpnGateway(vpcVO, "10.1.13.32", true);
            verify(ipAddressManager, never()).allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any());
            verify(vpcService, never()).associateIPToVpc(anyLong(), anyLong());
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testRetainedAutoIpSurvivesResourcePreflightFailure() throws Exception {
        CallContext.register(mock(User.class), mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            IPAddressVO retainedIp = mockIpAddressVO(32L, "10.1.13.32");
            when(ipAddressDao.listByAssociatedVpc(9L, false)).thenReturn(List.of(retainedIp));
            UserIpAddressDetailVO marker = new UserIpAddressDetailVO(32L, "nsxVpnGatewayIp", "true", false);
            when(userIpAddressDetailsDao.findDetail(32L, "nsxVpnGatewayIp")).thenReturn(marker);
            NsxApiClient apiClient = mock(NsxApiClient.class);
            when(apiClient.isVpnServicePresent(anyString())).thenThrow(new CloudRuntimeException("NSX unavailable"));
            NsxResource resource = new NsxResource() {
                {
                    nsxApiClient = apiClient;
                }
            };
            NsxControllerUtils controller = mock(NsxControllerUtils.class);
            when(controller.sendNsxCommandForResult(any(CreateNsxVpnGatewayCommand.class), anyLong()))
                    .thenAnswer(invocation -> resource.executeRequest(invocation.getArgument(0)));
            NsxServiceImpl service = new NsxServiceImpl();
            service.nsxControllerUtils = controller;
            nsxElement.nsxService = service;

            Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.acquireVpnGatewayIp(vpcVO, null));

            verify(apiClient).isVpnServicePresent(anyString());
            verify(ipAddressManager, never()).allocateIp(any(), anyBoolean(), any(), any(), any(), any(), any());
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
            verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
            verify(userIpAddressDetailsDao, never()).addDetail(anyLong(), anyString(), anyString(), anyBoolean());
            verify(ipAddressDao).releaseFromLockTable(32L);
        } finally {
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAcquireVpnGatewayIpReleasesAutoAcquiredIpWhenNsxRejectsGateway() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            IPAddressVO ipAddressVO = mockNewlyAllocatedVpnGatewayIp(vpcVO, 31L, "10.1.13.31");
            when(ipAddressVO.getId()).thenReturn(31L);
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.31", false)).thenReturn(new NsxVpnGatewayResult(false, false));
            when(ipAddressManager.disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any())).thenReturn(true);

            nsxElement.acquireVpnGatewayIp(vpcVO, null);
        } finally {
            verify(userIpAddressDetailsDao, never()).removeDetail(31L, "nsxVpnGatewayIp");
            verify(ipAddressManager).disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any());
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAcquireVpnGatewayIpRetainsAutoAcquiredIpWhenEndpointMayBeInUse() throws Exception {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcVO.getAccountId()).thenReturn(2L);
            when(vpcVO.getZoneId()).thenReturn(1L);
            mockNewlyAllocatedVpnGatewayIp(vpcVO, 32L, "10.1.13.32");
            when(nsxService.createVpnGateway(vpcVO, "10.1.13.32", false)).thenReturn(new NsxVpnGatewayResult(false, true));

            nsxElement.acquireVpnGatewayIp(vpcVO, null);
        } finally {
            verify(userIpAddressDetailsDao, never()).removeDetail(32L, "nsxVpnGatewayIp");
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(IPAddressVO.class), anyLong(), any());
            CallContext.unregister();
        }
    }

    @Test
    public void testReleaseVpnGatewayIpReleasesAutoAcquiredIp() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(vpnGateway.getAddrId()).thenReturn(30L);
            when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
            IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
            when(ipAddressVO.getId()).thenReturn(30L);
            UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
            when(detail.getValue()).thenReturn("true");
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

            nsxElement.releaseVpnGatewayIp(vpnGateway);
            verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
            verify(ipAddressManager).disassociatePublicIpAddress(eq(ipAddressVO), anyLong(), any());
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testReleaseVpnGatewayIpLeavesUnmarkedIpAllocated() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(null);

        nsxElement.releaseVpnGatewayIp(vpnGateway);
        verify(nsxService).deleteVpnGateway(vpcVO);
        verify(userIpAddressDetailsDao).findDetail(30L, "nsxVpnGatewayIp");
        verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpKeepsOperatorSpecifiedIp() {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        Mockito.lenient().when(vpnGateway.getAccountId()).thenReturn(2L);
        when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        Mockito.lenient().when(ipAddressVO.getAllocatedToAccountId()).thenReturn(2L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false));

        nsxElement.releaseVpnGatewayIp(vpnGateway);

        verify(nsxService).deleteVpnGateway(vpcVO);
        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpRejectsInvalidOwnershipStateWhenVpcRowIsGone() {
        when(vpcDao.findById(9L)).thenReturn(null);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        when(ipAddressVO.getVpcId()).thenReturn(9L);
        UserIpAddressDetailVO detail = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "invalid", false);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.releaseVpnGatewayIp(vpnGateway));

        assertTrue(exception.getMessage().contains("VPC 9"));
        verify(nsxService, never()).deleteVpnGateway(any(Vpc.class));
        verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testNsxVpnGatewayOwnershipRejectsIpReallocatedToAnotherVpc() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getAddrId()).thenReturn(30L);
        Mockito.lenient().when(gateway.getVpcId()).thenReturn(9L);
        Mockito.lenient().when(gateway.getAccountId()).thenReturn(2L);
        IPAddressVO reallocatedIp = mockIpAddressVO(30L, "10.1.13.30");
        Mockito.lenient().when(reallocatedIp.getVpcId()).thenReturn(10L);
        Mockito.lenient().when(reallocatedIp.getAllocatedToAccountId()).thenReturn(2L);
        Mockito.lenient().when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false));

        assertFalse(nsxElement.ownsVpnGateway(gateway));
    }

    @Test
    public void testReleaseVpnGatewayIpPreservesReallocatedAutoIp() {
        assertReallocatedVpnGatewayIpIsPreserved("true");
    }

    @Test
    public void testReleaseVpnGatewayIpPreservesReallocatedOperatorMarker() {
        assertReallocatedVpnGatewayIpIsPreserved("false");
    }

    @Test
    public void testNsxVpnGatewayOwnershipRejectsAnotherAccount() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getAddrId()).thenReturn(30L);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAccountId()).thenReturn(2L);
        IPAddressVO ip = mockIpAddressVO(30L, "10.1.13.30");
        when(ip.getAllocatedToAccountId()).thenReturn(3L);

        assertFalse(nsxElement.ownsVpnGateway(gateway));
        verify(userIpAddressDetailsDao, never()).findDetail(anyLong(), anyString());
    }

    @Test
    public void testReleaseVpnGatewayIpCannotMutateWithoutIpLock() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAddrId()).thenReturn(30L);
        when(vpcDao.findById(9L)).thenReturn(null);
        when(ipAddressDao.lockInLockTable("30")).thenReturn(false);

        CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.releaseVpnGatewayIp(gateway));

        assertTrue(exception.getMessage().contains("Unable to lock VPN gateway IP"));
        verify(ipAddressDao, never()).findById(30L);
        verify(ipAddressDao, never()).unlockFromLockTable("30");
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpReadsAllocationAfterLock() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAddrId()).thenReturn(30L);
        when(gateway.getAccountId()).thenReturn(2L);
        when(vpcDao.findById(9L)).thenReturn(null);
        IPAddressVO currentIp = mockIpAddressVO(30L, "10.1.13.30");
        AtomicBoolean locked = new AtomicBoolean();
        when(ipAddressDao.lockInLockTable("30")).thenAnswer(invocation -> {
            locked.set(true);
            return true;
        });
        when(ipAddressDao.findById(30L)).thenAnswer(invocation -> {
            assertTrue("Allocation must be read while the IP lock is held", locked.get());
            return currentIp;
        });
        when(currentIp.getAllocatedToAccountId()).thenReturn(2L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false));
        Mockito.doAnswer(invocation -> {
            assertTrue("Marker removal must run while the IP lock is held", locked.get());
            return null;
        }).when(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        Mockito.doAnswer(invocation -> {
            locked.set(false);
            return true;
        }).when(ipAddressDao).unlockFromLockTable("30");

        nsxElement.releaseVpnGatewayIp(gateway);

        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        assertFalse(locked.get());
    }

    @Test
    public void testReleaseVpnGatewayIpUnlocksWhenMarkerRemovalFails() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAddrId()).thenReturn(30L);
        when(vpcDao.findById(9L)).thenReturn(null);
        mockIpAddressVO(30L, "10.1.13.30");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false));
        CloudRuntimeException failure = new CloudRuntimeException("marker removal failed");
        Mockito.doThrow(failure).when(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");

        assertEquals(failure, Assert.assertThrows(CloudRuntimeException.class,
                () -> nsxElement.releaseVpnGatewayIp(gateway)));

        verify(ipAddressDao).unlockFromLockTable("30");
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpUnlocksWhenDisassociationThrows() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(gateway.getVpcId()).thenReturn(9L);
            when(gateway.getAddrId()).thenReturn(30L);
            when(vpcDao.findById(9L)).thenReturn(null);
            mockIpAddressVO(30L, "10.1.13.30");
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                    .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false));
            CloudRuntimeException failure = new CloudRuntimeException("IP release failed");
            when(ipAddressManager.disassociatePublicIpAddress(any(), anyLong(), any())).thenThrow(failure);

            assertEquals(failure, Assert.assertThrows(CloudRuntimeException.class,
                    () -> nsxElement.releaseVpnGatewayIp(gateway)));

            verify(ipAddressDao).unlockFromLockTable("30");
            verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testReleaseVpnGatewayIpMissingAddressIsIdempotent() {
        Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAddrId()).thenReturn(30L);
        when(vpcDao.findById(9L)).thenReturn(null);
        when(ipAddressDao.findById(30L)).thenReturn(null);

        nsxElement.releaseVpnGatewayIp(gateway);

        verify(ipAddressDao).unlockFromLockTable("30");
        Mockito.verifyNoInteractions(userIpAddressDetailsDao, ipAddressManager);
    }

    @Test
    public void testReleaseVpnGatewayIpAllowsReleasingAllocation() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            Site2SiteVpnGateway gateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(gateway.getVpcId()).thenReturn(9L);
            when(gateway.getAddrId()).thenReturn(30L);
            when(vpcDao.findById(9L)).thenReturn(null);
            IPAddressVO ip = mockIpAddressVO(30L, "10.1.13.30");
            Mockito.lenient().when(ip.getState()).thenReturn(IpAddress.State.Releasing);
            Mockito.lenient().when(ip.readyToUse()).thenReturn(false);
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                    .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false));

            assertTrue(nsxElement.ownsVpnGateway(gateway));
            nsxElement.releaseVpnGatewayIp(gateway);

            verify(ipAddressManager).disassociatePublicIpAddress(eq(ip), anyLong(), any());
            verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
            verify(ipAddressDao).unlockFromLockTable("30");
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testGatewayDeleteRetryPreservesReallocatedIp() {
        Site2SiteVpnManagerImpl manager = new Site2SiteVpnManagerImpl();
        ReflectionTestUtils.setField(manager, "_vpcDao", vpcDao);
        ReflectionTestUtils.setField(manager, "_vpnGatewayDao", vpnGatewayDao);
        Site2SiteVpnGatewayDetailsDao gatewayDetailsDao = mock(Site2SiteVpnGatewayDetailsDao.class);
        ReflectionTestUtils.setField(manager, "_vpnGatewayDetailsDao", gatewayDetailsDao);
        when(gatewayDetailsDao.findDetail(7L, Site2SiteVpnServiceProvider.VPN_GATEWAY_PROVIDER_DETAIL))
                .thenReturn(new Site2SiteVpnGatewayDetailVO(7L, Site2SiteVpnServiceProvider.VPN_GATEWAY_PROVIDER_DETAIL,
                        Network.Provider.Nsx.getName(), false));
        VpcVO oldVpc = mock(VpcVO.class);
        when(vpcDao.findById(9L)).thenReturn(oldVpc);
        when(nsxService.deleteVpnGateway(oldVpc)).thenReturn(true);
        ReflectionTestUtils.setField(manager, "_vpnConnectionDao", vpnConnectionDao);
        ReflectionTestUtils.setField(manager, "_s2sProviders", List.of(nsxElement, new VpcVirtualRouterElement()));
        Site2SiteVpnGatewayVO oldGateway = new Site2SiteVpnGatewayVO(2L, 1L, 30L, 9L);
        ReflectionTestUtils.setField(oldGateway, "id", 7L);
        when(vpcDao.lockInLockTable("9")).thenReturn(true);
        when(vpnGatewayDao.findById(7L)).thenReturn(oldGateway);
        when(vpnConnectionDao.listByVpnGatewayId(7L)).thenReturn(List.of());
        IPAddressVO reallocatedIp = mockIpAddressVO(30L, "10.1.13.30");
        when(reallocatedIp.getVpcId()).thenReturn(10L);
        Mockito.lenient().when(reallocatedIp.getAllocatedToAccountId()).thenReturn(2L);
        Mockito.lenient().when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false));

        ReflectionTestUtils.invokeMethod(manager, "doDeleteVpnGateway", oldGateway);

        verify(vpnGatewayDao).remove(7L);
        verify(vpcDao).releaseFromLockTable(9L);
        verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
        verify(userIpAddressDetailsDao, never()).removeDetail(anyLong(), anyString());
        verify(nsxService).deleteVpnGateway(oldVpc);
    }

    private void assertReallocatedVpnGatewayIpIsPreserved(String markerValue) {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO oldVpc = Mockito.mock(VpcVO.class);
            when(vpcDao.findById(9L)).thenReturn(oldVpc);
            when(nsxService.deleteVpnGateway(oldVpc)).thenReturn(true);
            Site2SiteVpnGateway oldGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(oldGateway.getVpcId()).thenReturn(9L);
            when(oldGateway.getAddrId()).thenReturn(30L);
            Mockito.lenient().when(oldGateway.getAccountId()).thenReturn(2L);
            IPAddressVO reallocatedIp = mockIpAddressVO(30L, "10.1.13.30");
            Mockito.lenient().when(reallocatedIp.getVpcId()).thenReturn(10L);
            Mockito.lenient().when(reallocatedIp.getAllocatedToAccountId()).thenReturn(2L);
            Mockito.lenient().when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                    .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", markerValue, false));

            nsxElement.releaseVpnGatewayIp(oldGateway);

            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
            verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
        } finally {
            CallContext.unregister();
        }
    }

    @Test
    public void testNsxVpnGatewayOwnershipDoesNotDependOnMarkerValue() {
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        mockIpAddressVO(30L, "10.1.13.30");
        UserIpAddressDetailVO operatorSpecified = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false);
        UserIpAddressDetailVO autoAcquired = new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(operatorSpecified, autoAcquired);

        assertTrue(nsxElement.ownsVpnGateway(vpnGateway));
        assertTrue(nsxElement.ownsVpnGateway(vpnGateway));
    }

    @Test
    public void testNsxVpnGatewayOwnershipRequiresMarker() {
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        mockIpAddressVO(30L, "10.1.13.30");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(null);

        assertFalse(nsxElement.ownsVpnGateway(vpnGateway));
    }

    @Test
    public void testIpReservationUsesMarkerWithoutGatewayRowOrOfferingMapping() {
        IpAddress ip = mock(IpAddress.class);
        when(ip.getId()).thenReturn(30L);
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                .thenReturn(new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "true", false),
                        new UserIpAddressDetailVO(30L, "nsxVpnGatewayIp", "false", false),
                        null);

        assertTrue(nsxElement.isIpAddressReserved(ip));
        assertTrue(nsxElement.isIpAddressReserved(ip));
        assertFalse(nsxElement.isIpAddressReserved(ip));

        Mockito.verifyNoInteractions(vpnGatewayDao, vpcManager, nsxService);
    }

    @Test
    public void testReleaseVpnGatewayIpUsesPersistedOwnershipWhenOfferingMappingIsGone() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
        when(detail.getValue()).thenReturn("false");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        nsxElement.releaseVpnGatewayIp(vpnGateway);

        verify(nsxService).deleteVpnGateway(vpcVO);
        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpRemovesOperatorMarkerWhenVpcRowIsGone() {
        when(vpcDao.findById(9L)).thenReturn(null);
        Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
        when(ipAddressVO.getId()).thenReturn(30L);
        UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
        when(detail.getValue()).thenReturn("false");
        when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);

        nsxElement.releaseVpnGatewayIp(vpnGateway);

        verify(nsxService, Mockito.never()).deleteVpnGateway(any(Vpc.class));
        verify(userIpAddressDetailsDao).removeDetail(30L, "nsxVpnGatewayIp");
        verify(ipAddressManager, Mockito.never()).disassociatePublicIpAddress(any(), anyLong(), any());
    }

    @Test
    public void testReleaseVpnGatewayIpPreservesOwnershipWhenNsxDeletionFails() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(nsxService.deleteVpnGateway(vpcVO))
                    .thenThrow(new CloudRuntimeException("NSX Tier-1 lookup failed"));

            CloudRuntimeException exception = Assert.assertThrows(CloudRuntimeException.class,
                    () -> nsxElement.releaseVpnGatewayIp(vpnGateway));

            assertTrue(exception.getMessage().contains("Failed to delete the NSX VPN gateway"));
            verify(ipAddressDao, never()).findById(30L);
            verify(userIpAddressDetailsDao, never()).removeDetail(30L, "nsxVpnGatewayIp");
            verify(ipAddressManager, never()).disassociatePublicIpAddress(any(), anyLong(), any());
        } finally {
            CallContext.unregister();
        }
    }

    @Test(expected = CloudRuntimeException.class)
    public void testReleaseVpnGatewayIpPropagatesFailureWithoutSecondMarkerRemoval() {
        CallContext.register(Mockito.mock(User.class), Mockito.mock(Account.class));
        try {
            VpcVO vpcVO = mockVpcWithNsxVpnSupport();
            when(vpcDao.findById(9L)).thenReturn(vpcVO);
            Site2SiteVpnGateway vpnGateway = Mockito.mock(Site2SiteVpnGateway.class);
            when(vpnGateway.getVpcId()).thenReturn(9L);
            when(vpnGateway.getAddrId()).thenReturn(30L);
            when(nsxService.deleteVpnGateway(vpcVO)).thenReturn(true);
            IPAddressVO ipAddressVO = mockIpAddressVO(30L, "10.1.13.30");
            when(ipAddressVO.getId()).thenReturn(30L);
            UserIpAddressDetailVO detail = Mockito.mock(UserIpAddressDetailVO.class);
            when(detail.getValue()).thenReturn("true");
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp")).thenReturn(detail);
            when(ipAddressManager.disassociatePublicIpAddress(any(), anyLong(), any())).thenReturn(false);

            nsxElement.releaseVpnGatewayIp(vpnGateway);
        } finally {
            verify(userIpAddressDetailsDao, Mockito.never()).removeDetail(30L, "nsxVpnGatewayIp");
            verify(ipAddressDao).unlockFromLockTable("30");
            CallContext.unregister();
        }
    }

    private Site2SiteCustomerGatewayVO mockCustomerGateway(String ikePolicy, String espPolicy) {
        Site2SiteCustomerGatewayVO customerGateway = Mockito.mock(Site2SiteCustomerGatewayVO.class);
        when(customerGatewayDao.findById(3L)).thenReturn(customerGateway);
        when(customerGateway.getIkePolicy()).thenReturn(ikePolicy);
        when(customerGateway.getEspPolicy()).thenReturn(espPolicy);
        when(customerGateway.getIkeVersion()).thenReturn("ikev2");
        when(customerGateway.getIkeLifetime()).thenReturn(86400L);
        when(customerGateway.getEspLifetime()).thenReturn(3600L);
        when(customerGateway.getIpsecPsk()).thenReturn("presharedkey");
        return customerGateway;
    }

    private Site2SiteVpnConnection mockVpnConnection(VpcVO vpcVO) {
        return mockVpnConnection(vpcVO, true);
    }

    private Site2SiteVpnConnection mockVpnConnection(VpcVO vpcVO, boolean nsxOwned) {
        Site2SiteVpnConnection connection = Mockito.mock(Site2SiteVpnConnection.class);
        when(connection.getVpnGatewayId()).thenReturn(7L);
        Mockito.lenient().when(connection.getCustomerGatewayId()).thenReturn(3L);
        Site2SiteVpnGatewayVO vpnGateway = Mockito.mock(Site2SiteVpnGatewayVO.class);
        when(vpnGatewayDao.findById(7L)).thenReturn(vpnGateway);
        when(vpnGateway.getVpcId()).thenReturn(9L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        IPAddressVO gatewayIp = Mockito.mock(IPAddressVO.class);
        Mockito.lenient().when(ipAddressDao.findById(30L)).thenReturn(gatewayIp);
        Mockito.lenient().when(gatewayIp.getVpcId()).thenReturn(9L);
        Mockito.lenient().when(gatewayIp.getAllocatedToAccountId()).thenReturn(0L);
        if (nsxOwned) {
            when(userIpAddressDetailsDao.findDetail(30L, "nsxVpnGatewayIp"))
                    .thenReturn(Mockito.mock(UserIpAddressDetailVO.class));
        }
        when(vpcDao.findById(9L)).thenReturn(vpcVO);
        return connection;
    }

    @Test
    public void testStartSite2SiteVpnUsesImmutableConnectionId() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        Site2SiteVpnGatewayVO vpnGateway = vpnGatewayDao.findById(7L);
        when(vpnGateway.getAddrId()).thenReturn(30L);
        mockIpAddressVO(30L, "10.1.13.30");
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("aes256-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getGatewayIp()).thenReturn("203.0.113.10");
        when(customerGateway.getGuestCidrList()).thenReturn("192.168.100.0/24,192.168.200.0/24");
        when(nsxService.createVpnConnection(any(Vpc.class), eq(5L), anyString(), anyString(), anyString(), anyString(),
                anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                eq("169.254.64.21"), eq("169.254.64.22"), anyInt(), eq("10.1.13.30"))).thenReturn(true);

        assertTrue(nsxElement.startSite2SiteVpn(connection));
    }

    @Test
    public void testStartSite2SiteVpnReportsBackendExceptionAsFailedProgramming() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        mockIpAddressVO(30L, "10.1.13.30");
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("aes256-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getGatewayIp()).thenReturn("203.0.113.10");
        when(customerGateway.getGuestCidrList()).thenReturn("192.168.100.0/24");
        when(nsxService.createVpnConnection(any(Vpc.class), eq(5L), anyString(), anyString(), anyString(), anyString(),
                anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                anyString(), anyString(), anyInt(), anyString()))
                .thenThrow(new CloudRuntimeException("provider may have created partial resources"));

        assertFalse(nsxElement.startSite2SiteVpn(connection));
    }

    @Test
    public void testStartSite2SiteVpnRejectsUnsupportedCrypto() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        IPAddressVO endpoint = Mockito.mock(IPAddressVO.class);
        Mockito.lenient().when(ipAddressDao.findById(30L)).thenReturn(endpoint);
        Mockito.lenient().when(endpoint.getVpcId()).thenReturn(9L);
        Mockito.lenient().when(endpoint.getAllocatedToAccountId()).thenReturn(0L);
        Mockito.lenient().when(endpoint.getAddress()).thenReturn(new Ip("10.1.13.30"));
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("3des-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getGatewayIp()).thenReturn("203.0.113.10");
        Mockito.lenient().when(customerGateway.getGuestCidrList()).thenReturn("192.168.100.0/24");

        InvalidParameterValueException failure = Assert.assertThrows(InvalidParameterValueException.class,
                () -> nsxElement.startSite2SiteVpn(connection));

        assertTrue(failure.getMessage().contains("Encryption algorithm 3des is not supported"));
        verify(nsxService, never()).createVpnConnection(any(Vpc.class), anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                anyString(), anyString(), anyInt(), anyString());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testStartSite2SiteVpnRejectsDnsPeerAddress() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        Site2SiteCustomerGatewayVO customerGateway = mockCustomerGateway("aes256-sha256;modp2048", "aes128-sha1");
        when(customerGateway.getName()).thenReturn("remote-site");
        when(customerGateway.getGatewayIp()).thenReturn("vpn.example.test");

        nsxElement.startSite2SiteVpn(connection);
    }

    @Test
    public void testSite2SiteVpnOperationsRejectGatewayNotOwnedByNsx() {
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO, false);

        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.startSite2SiteVpn(connection));
        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.stopSite2SiteVpn(connection));
        Assert.assertThrows(CloudRuntimeException.class, () -> nsxElement.deleteSite2SiteVpn(connection));
        verify(nsxService, Mockito.never()).createVpnConnection(any(Vpc.class), anyLong(), anyString(), anyString(),
                anyString(), anyString(), anyLong(), anyLong(), anyBoolean(), anyString(), anyBoolean(), anyList(),
                anyString(), anyString(), anyInt(), anyString());
        verify(nsxService, Mockito.never()).updateVpnConnectionState(any(Vpc.class), anyLong(), anyBoolean());
        verify(nsxService, Mockito.never()).deleteVpnConnection(any(Vpc.class), anyLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStartSite2SiteVpnThrowsWhenVpcIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = mockVpnConnection(null);

        nsxElement.startSite2SiteVpn(connection);
    }

    @Test
    public void testStopSite2SiteVpn() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        when(nsxService.updateVpnConnectionState(vpcVO, 5L, false)).thenReturn(true);

        assertTrue(nsxElement.stopSite2SiteVpn(connection));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStopSite2SiteVpnThrowsWhenVpcIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = mockVpnConnection(null);

        nsxElement.stopSite2SiteVpn(connection);
    }

    @Test
    public void testDeleteSite2SiteVpnRemovesTheProviderConnection() throws ResourceUnavailableException {
        VpcVO vpcVO = mockVpcWithNsxVpnSupport();
        Site2SiteVpnConnection connection = mockVpnConnection(vpcVO);
        when(connection.getId()).thenReturn(5L);
        when(nsxService.deleteVpnConnection(vpcVO, 5L)).thenReturn(true);

        assertTrue(nsxElement.deleteSite2SiteVpn(connection));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testStopSite2SiteVpnThrowsWhenVpnGatewayIsMissing() throws ResourceUnavailableException {
        Site2SiteVpnConnection connection = Mockito.mock(Site2SiteVpnConnection.class);
        when(connection.getVpnGatewayId()).thenReturn(7L);
        when(vpnGatewayDao.findById(7L)).thenReturn(null);

        nsxElement.stopSite2SiteVpn(connection);
    }
}
