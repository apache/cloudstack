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

import com.cloud.alert.AlertManager;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.nsx.NsxVpnGatewayResult;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxStaticNatCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.CreateNsxVpnGatewayCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxNatRuleCommand;
import org.apache.cloudstack.agent.api.DeleteNsxSegmentCommand;
import org.apache.cloudstack.agent.api.DeleteNsxTier1GatewayCommand;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.Site2SiteVpnGatewayDetailVO;
import org.apache.cloudstack.resourcedetail.dao.Site2SiteVpnGatewayDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.apache.cloudstack.resource.NsxResource;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NsxServiceImplTest {
    @Mock
    private NsxControllerUtils nsxControllerUtils;
    @Mock
    private VpcDao vpcDao;
    @Mock
    private Site2SiteVpnConnectionDao site2SiteVpnConnectionDao;
    @Mock
    private Site2SiteVpnGatewayDao site2SiteVpnGatewayDao;
    @Mock
    private UserIpAddressDetailsDao userIpAddressDetailsDao;
    @Mock
    private IPAddressDao ipAddressDao;
    @Mock
    private Site2SiteVpnGatewayDetailsDao site2SiteVpnGatewayDetailsDao;
    @Mock
    private AlertManager alertManager;
    NsxServiceImpl nsxService;

    AutoCloseable closeable;

    private static final long domainId = 1L;
    private static final long accountId = 2L;
    private static final long zoneId = 1L;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        nsxService = new NsxServiceImpl();
        nsxService.nsxControllerUtils = nsxControllerUtils;
        nsxService.vpcDao = vpcDao;
        nsxService.site2SiteVpnConnectionDao = site2SiteVpnConnectionDao;
        nsxService.site2SiteVpnGatewayDao = site2SiteVpnGatewayDao;
        nsxService.userIpAddressDetailsDao = userIpAddressDetailsDao;
        nsxService.ipAddressDao = ipAddressDao;
        nsxService.site2SiteVpnGatewayDetailsDao = site2SiteVpnGatewayDetailsDao;
        nsxService.alertManager = alertManager;
    }

    @After
    public void teardown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCreateVpcNetwork() {
        NsxAnswer createNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxTier1GatewayCommand.class), anyLong())).thenReturn(createNsxTier1GatewayAnswer);
        when(createNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.createVpcNetwork(1L, 3L, 2L, 5L, "VPC01", false));
    }

    @Test
    public void testCreateVpnGatewayPreservesStructuredFailureResult() {
        Vpc vpc = mock(Vpc.class);
        when(vpc.getDomainId()).thenReturn(domainId);
        when(vpc.getAccountId()).thenReturn(accountId);
        when(vpc.getZoneId()).thenReturn(zoneId);
        when(vpc.getId()).thenReturn(3L);
        NsxAnswer answer = mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(false);
        when(answer.isEndpointMayBeInUse()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommandForResult(any(CreateNsxVpnGatewayCommand.class), eq(zoneId)))
                .thenReturn(answer);

        NsxVpnGatewayResult result = nsxService.createVpnGateway(vpc, "203.0.113.20");

        assertFalse(result.isSuccessful());
        assertTrue(result.isEndpointMayBeInUse());
    }

    @Test
    public void testCreateVpnGatewayPropagatesReconciliationPreflightOwnership() {
        Vpc vpc = mock(Vpc.class);
        when(vpc.getDomainId()).thenReturn(domainId);
        when(vpc.getAccountId()).thenReturn(accountId);
        when(vpc.getZoneId()).thenReturn(zoneId);
        when(vpc.getId()).thenReturn(3L);
        NsxApiClient apiClient = mock(NsxApiClient.class);
        when(apiClient.isVpnServicePresent("D1-A2-Z1-V3"))
                .thenThrow(new CloudRuntimeException("NSX unavailable"));
        NsxResource resource = new NsxResource() {
            {
                nsxApiClient = apiClient;
            }
        };
        when(nsxControllerUtils.sendNsxCommandForResult(any(CreateNsxVpnGatewayCommand.class), eq(zoneId)))
                .thenAnswer(invocation -> resource.executeRequest(invocation.getArgument(0)));

        NsxVpnGatewayResult retained = nsxService.createVpnGateway(vpc, "203.0.113.20", true);
        NsxVpnGatewayResult fresh = nsxService.createVpnGateway(vpc, "203.0.113.21", false);

        assertFalse(retained.isSuccessful());
        assertTrue(retained.isEndpointMayBeInUse());
        assertFalse(fresh.isSuccessful());
        assertFalse(fresh.isEndpointMayBeInUse());
    }

    @Test
    public void testDeleteVpcNetwork() {
        NsxAnswer deleteNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxTier1GatewayCommand.class), anyLong())).thenReturn(deleteNsxTier1GatewayAnswer);
        when(deleteNsxTier1GatewayAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteVpcNetwork(1L, 2L, 3L, 10L, "VPC01"));
    }

    @Test
    public void testDeleteNetworkOnVpc() {
        NetworkVO network = new NetworkVO();
        network.setVpcId(1L);
        when(vpcDao.findById(1L)).thenReturn(mock(VpcVO.class));
        NsxAnswer deleteNsxSegmentAnswer = mock(NsxAnswer.class);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxSegmentCommand.class), anyLong())).thenReturn(deleteNsxSegmentAnswer);
        when(deleteNsxSegmentAnswer.getResult()).thenReturn(true);

        assertTrue(nsxService.deleteNetwork(zoneId, accountId, domainId, network));
    }

    @Test
    public void testDeleteNetwork() {
        NetworkVO network = new NetworkVO();
        network.setVpcId(null);
        NsxAnswer deleteNsxSegmentAnswer = mock(NsxAnswer.class);
        when(deleteNsxSegmentAnswer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxSegmentCommand.class), anyLong())).thenReturn(deleteNsxSegmentAnswer);
        NsxAnswer deleteNsxTier1GatewayAnswer = mock(NsxAnswer.class);
        when(deleteNsxTier1GatewayAnswer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxTier1GatewayCommand.class), anyLong())).thenReturn(deleteNsxTier1GatewayAnswer);
        assertTrue(nsxService.deleteNetwork(zoneId, accountId, domainId, network));
    }

    @Test
    public void testUpdateVpcSourceNatIp() {
        VpcVO vpc = mock(VpcVO.class);
        IpAddress ipAddress = mock(IpAddress.class);
        Ip ip = Mockito.mock(Ip.class);
        when(ip.addr()).thenReturn("10.1.10.10");
        when(ipAddress.getAddress()).thenReturn(ip);
        long vpcId = 1L;
        when(vpc.getAccountId()).thenReturn(accountId);
        when(vpc.getDomainId()).thenReturn(domainId);
        when(vpc.getZoneId()).thenReturn(zoneId);
        when(vpc.getId()).thenReturn(vpcId);
        NsxAnswer answer = mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.updateVpcSourceNatIp(vpc, ipAddress);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class), eq(zoneId));
    }

    @Test
    public void testCreateStaticNatRule() {
        long networkId = 1L;
        String networkName = "Network-Test";
        long vmId = 1L;
        String publicIp = "10.10.1.10";
        String vmIp = "192.168.1.20";
        NsxAnswer answer = Mockito.mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxStaticNatCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.createStaticNatRule(zoneId, domainId, accountId,
                networkId, networkName, true, vmId, publicIp, vmIp);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(CreateNsxStaticNatCommand.class), eq(zoneId));
    }

    @Test
    public void testDeleteStaticNatRule() {
        long networkId = 1L;
        String networkName = "Network-Test";
        NsxAnswer answer = Mockito.mock(NsxAnswer.class);
        when(answer.getResult()).thenReturn(true);
        when(nsxControllerUtils.sendNsxCommand(any(DeleteNsxNatRuleCommand.class), eq(zoneId))).thenReturn(answer);
        nsxService.deleteStaticNatRule(zoneId, domainId, accountId, networkId, networkName, true);
        Mockito.verify(nsxControllerUtils).sendNsxCommand(any(DeleteNsxNatRuleCommand.class), eq(zoneId));
    }

    @Test
    public void testPollVpnConnectionStatusTransitionsUp() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connecting);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doReturn("UP").when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Connected, connection.getState());
        org.mockito.InOrder order = Mockito.inOrder(site2SiteVpnConnectionDao, service, alertManager);
        order.verify(site2SiteVpnConnectionDao).acquireInLockTable(connection.getId());
        order.verify(service).getVpnConnectionStatus(vpc, connection.getId());
        order.verify(site2SiteVpnConnectionDao).persist(connection);
        order.verify(alertManager).sendAlert(any(), anyLong(), any(), any(), any());
        order.verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
    }

    @Test
    public void testPollVpnConnectionStatusTransitionsDown() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connected);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doReturn("DOWN").when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Disconnected, connection.getState());
        verify(site2SiteVpnConnectionDao).persist(connection);
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
    }

    @Test
    public void testPollVpnConnectionStatusKeepsPendingConnectionWhenSessionIsNotFound() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Pending);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doReturn("NOT_FOUND").when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Pending, connection.getState());
        verify(service).getVpnConnectionStatus(vpc, connection.getId());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
    }

    @Test
    public void testPollVpnConnectionStatusMarksMissingConnectedSessionAsError() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connected);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doReturn("NOT_FOUND").when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Error, connection.getState());
        verify(site2SiteVpnConnectionDao).persist(connection);
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
    }

    @Test
    public void testPollVpnConnectionStatusDoesNotTransitionOnQueryFailure() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connected);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doThrow(new CloudRuntimeException("NSX unavailable"))
                .when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Connected, connection.getState());
        verify(service).getVpnConnectionStatus(vpc, connection.getId());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
    }

    @Test
    public void testPollVpnConnectionStatusAlertsAfterThreeConsecutiveQueryFailures() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connected);
        VpcVO vpc = mockPollingOwnership(connection, "true").vpc;
        when(vpc.getName()).thenReturn("production-vpc");
        when(vpc.getZoneId()).thenReturn(7L);
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doThrow(new CloudRuntimeException("NSX unavailable"))
                .when(service).getVpnConnectionStatus(vpc, connection.getId());

        service.pollVpnConnectionStatus(connection, vpc);
        service.pollVpnConnectionStatus(connection, vpc);
        verify(alertManager, never()).sendAlert(any(), anyLong(), any(), any(), any());

        service.pollVpnConnectionStatus(connection, vpc);
        verify(alertManager).sendAlert(eq(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER), eq(7L),
                eq(null), any(String.class), any(String.class));

        // The counter resets after the alert, so the next failure starts a new sequence.
        service.pollVpnConnectionStatus(connection, vpc);
        verify(alertManager, times(1)).sendAlert(any(), anyLong(), any(), any(), any());
        verify(service, times(4)).getVpnConnectionStatus(vpc, connection.getId());
        verify(site2SiteVpnConnectionDao, times(4)).releaseFromLockTable(connection.getId());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
    }

    @Test
    public void testPollQueriesReplacementSessionAfterResetReturnsToSameState() {
        Site2SiteVpnConnectionVO listed = new Site2SiteVpnConnectionVO(accountId, domainId, 7L, 9L, false);
        listed.setState(Site2SiteVpnConnection.State.Connecting);
        Site2SiteVpnConnectionVO current = new Site2SiteVpnConnectionVO(accountId, domainId, 7L, 9L, false);
        current.setState(Site2SiteVpnConnection.State.Connecting);
        VpcVO vpc = mockPollingOwnership(current, "true").vpc;
        AtomicBoolean resetCompleted = new AtomicBoolean();
        when(site2SiteVpnConnectionDao.acquireInLockTable(listed.getId())).thenAnswer(invocation -> {
            // A reset finishes while the poller waits for its lock. Both generations have the
            // same CloudStack state, but the replacement session has a different NSX status.
            resetCompleted.set(true);
            return current;
        });
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doAnswer(invocation -> resetCompleted.get() ? "DOWN" : "UP")
                .when(service).getVpnConnectionStatus(vpc, listed.getId());

        service.pollVpnConnectionStatus(listed, vpc);

        assertEquals(Site2SiteVpnConnection.State.Disconnected, current.getState());
        assertEquals(Site2SiteVpnConnection.State.Connecting, listed.getState());
        verify(site2SiteVpnConnectionDao).persist(current);
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(current.getId());
    }

    @Test
    public void testPollSkipsConnectionThatLeftPolledStatesBeforeLock() {
        Site2SiteVpnConnectionVO listed = mock(Site2SiteVpnConnectionVO.class);
        Site2SiteVpnConnectionVO current = preparePollingConnection(Site2SiteVpnConnection.State.Error);
        NsxServiceImpl service = Mockito.spy(nsxService);

        service.pollVpnConnectionStatus(listed, mock(VpcVO.class));

        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(current.getId());
    }

    @Test
    public void testPollSkipsMissingOrLockedConnectionBeforeQuery() {
        Site2SiteVpnConnectionVO listed = mock(Site2SiteVpnConnectionVO.class);
        NsxServiceImpl service = Mockito.spy(nsxService);

        service.pollVpnConnectionStatus(listed, mock(VpcVO.class));

        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(site2SiteVpnConnectionDao, never()).releaseFromLockTable(anyLong());
    }

    private Site2SiteVpnConnectionVO preparePollingConnection(Site2SiteVpnConnection.State state) {
        Site2SiteVpnConnectionVO connection = new Site2SiteVpnConnectionVO(accountId, domainId, 7L, 9L, false);
        connection.setState(state);
        when(site2SiteVpnConnectionDao.acquireInLockTable(connection.getId())).thenReturn(connection);
        return connection;
    }

    @Test
    public void testVpnStatusPollerSkipsUnmarkedGatewayRegardlessOfCurrentOffering() {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        PollingOwnership ownership = mockPollingOwnership(connection, null);
        NsxServiceImpl service = Mockito.spy(nsxService);

        runVpnStatusPollTaskWithLockAcquired(service);

        verify(service, never()).pollVpnConnectionStatus(connection, ownership.vpc);
        verify(userIpAddressDetailsDao).findDetail(30L, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL);
        verify(site2SiteVpnConnectionDao, never()).acquireInLockTable(anyLong());
        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        assertEquals(Site2SiteVpnConnection.State.Connecting, connection.getState());
    }

    @Test
    public void testVpnStatusPollerUsesPersistedOwnershipAfterOfferingChanges() {
        assertPollerQueriesOwnedAllocation("false", Network.Provider.Nsx.getName());
    }

    @Test
    public void testVpnStatusPollerUsesAutoAcquiredAllocationWithoutProviderDetail() {
        assertPollerQueriesOwnedAllocation("true", null);
    }

    @Test
    public void testVpnStatusPollerUsesOperatorSpecifiedAllocationWithoutProviderDetail() {
        assertPollerQueriesOwnedAllocation("false", null);
    }

    @Test
    public void testVpnStatusPollerUsesAutoAcquiredAllocationWithSavedNsxProvider() {
        assertPollerQueriesOwnedAllocation("true", Network.Provider.Nsx.getName());
    }

    @Test
    public void testVpnStatusPollerSkipsMissingIp() {
        assertPollerSkipsGateway(ownership -> ownership.currentIp = null);
    }

    @Test
    public void testVpnStatusPollerSkipsIpReallocatedToAnotherVpc() {
        assertPollerSkipsGateway(ownership -> ownership.ipVpcId = 10L);
    }

    @Test
    public void testVpnStatusPollerSkipsIpReallocatedToAnotherAccount() {
        assertPollerSkipsGateway(ownership -> ownership.ipAccountId = accountId + 1);
    }

    @Test
    public void testVpnStatusPollerSkipsGatewayRecordedForVirtualRouter() {
        assertPollerSkipsGateway(ownership -> ownership.providerDetail =
                pollingProviderDetail(Network.Provider.VPCVirtualRouter.getName()));
    }

    @Test
    public void testVpnStatusPollerSkipsGatewayRecordedForUnknownProvider() {
        assertPollerSkipsGateway(ownership -> ownership.providerDetail = pollingProviderDetail("UnknownVpnProvider"));
    }

    @Test
    public void testPollerSkipsIpRemovedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.currentIp = null);
    }

    @Test
    public void testPollerSkipsIpReallocatedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.ipVpcId = 10L);
    }

    @Test
    public void testPollerSkipsChangedAccountWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.ipAccountId = accountId + 1);
    }

    @Test
    public void testPollerSkipsGatewayRemovedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.currentGateway = null);
    }

    @Test
    public void testPollerSkipsVpcRemovedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.currentVpc = null);
    }

    @Test
    public void testPollerSkipsMarkerRemovedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.marker = null);
    }

    @Test
    public void testPollerSkipsProviderChangedWhileWaitingForConnectionLock() {
        assertPollerRechecksOwnershipAfterLock(ownership -> ownership.providerDetail =
                pollingProviderDetail(Network.Provider.VPCVirtualRouter.getName()));
    }

    @Test
    public void testPollReleasesConnectionLockWhenAllocationReadFails() {
        Site2SiteVpnConnectionVO connection = preparePollingConnection(Site2SiteVpnConnection.State.Connecting);
        PollingOwnership ownership = mockPollingOwnership(connection, "true");
        CloudRuntimeException failure = new CloudRuntimeException("IP allocation lookup failed");
        when(ipAddressDao.findById(30L)).thenThrow(failure);
        NsxServiceImpl service = Mockito.spy(nsxService);

        assertSame(failure, assertThrows(CloudRuntimeException.class,
                () -> service.pollVpnConnectionStatus(connection, ownership.vpc)));

        verify(site2SiteVpnConnectionDao).acquireInLockTable(connection.getId());
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        assertEquals(Site2SiteVpnConnection.State.Connecting, connection.getState());
    }

    @Test
    public void testVpnStatusPollerQueriesOnlyPollableStates() {
        runVpnStatusPollTaskWithLockAcquired(nsxService);

        verify(site2SiteVpnConnectionDao).listByStates(
                Site2SiteVpnConnection.State.Pending,
                Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected,
                Site2SiteVpnConnection.State.Disconnected);
        verify(site2SiteVpnConnectionDao, never()).listAll();
    }

    @Test
    public void testVpnStatusPollerSkipsScanWhenAnotherManagementServerOwnsGlobalLock() {
        GlobalLock scanLock = mock(GlobalLock.class);
        try (MockedStatic<GlobalLock> globalLocks = Mockito.mockStatic(GlobalLock.class)) {
            globalLocks.when(() -> GlobalLock.getInternLock(NsxServiceImpl.VPN_STATUS_POLL_LOCK_NAME)).thenReturn(scanLock);
            when(scanLock.lock(NsxServiceImpl.VPN_STATUS_POLL_LOCK_TIMEOUT)).thenReturn(false);

            nsxService.new VpnStatusPollTask().runInContext();

            verify(site2SiteVpnConnectionDao, never()).listByStates(any(Site2SiteVpnConnection.State[].class));
            verify(scanLock, never()).unlock();
            verify(scanLock).releaseRef();
        }
    }

    @Test
    public void testVpnStatusPollerCanRestartInSameJvm() throws Exception {
        ScheduledExecutorService firstExecutor = mock(ScheduledExecutorService.class);
        ScheduledExecutorService secondExecutor = mock(ScheduledExecutorService.class);
        AtomicInteger executorIndex = new AtomicInteger();
        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            protected ScheduledExecutorService createVpnStatusPollExecutor() {
                return executorIndex.getAndIncrement() == 0 ? firstExecutor : secondExecutor;
            }
        };
        service.configure("NsxService", Map.of());
        try {
            assertTrue(service.start());
            verify(firstExecutor).scheduleWithFixedDelay(any(Runnable.class), eq(60L), eq(60L), eq(java.util.concurrent.TimeUnit.SECONDS));

            assertTrue(service.stop());
            verify(firstExecutor).shutdownNow();

            assertTrue(service.start());
            verify(secondExecutor).scheduleWithFixedDelay(any(Runnable.class), eq(60L), eq(60L), eq(java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(2, executorIndex.get());
        } finally {
            service.stop();
        }
        verify(secondExecutor).shutdownNow();
        verify(firstExecutor, times(1)).shutdownNow();
    }

    private Site2SiteVpnConnectionVO mockPollableVpnConnection() {
        Site2SiteVpnConnectionVO connection = new Site2SiteVpnConnectionVO(accountId, domainId, 7L, 9L, false);
        connection.setState(Site2SiteVpnConnection.State.Connecting);
        when(site2SiteVpnConnectionDao.listByStates(
                Site2SiteVpnConnection.State.Pending,
                Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected,
                Site2SiteVpnConnection.State.Disconnected)).thenReturn(List.of(connection));
        return connection;
    }

    private static class PollingOwnership {
        final Site2SiteVpnGatewayVO gateway = mock(Site2SiteVpnGatewayVO.class);
        final IPAddressVO ip = mock(IPAddressVO.class);
        final VpcVO vpc = mock(VpcVO.class);
        Site2SiteVpnGatewayVO currentGateway = gateway;
        IPAddressVO currentIp = ip;
        VpcVO currentVpc = vpc;
        Long ipVpcId = 9L;
        Long ipAccountId = accountId;
        UserIpAddressDetailVO marker;
        Site2SiteVpnGatewayDetailVO providerDetail;
    }

    private PollingOwnership mockPollingOwnership(Site2SiteVpnConnectionVO connection, String markerValue) {
        PollingOwnership ownership = new PollingOwnership();
        // The same fixture supports early rejection and a change while the poller waits for its lock.
        Mockito.lenient().when(ownership.gateway.getId()).thenReturn(7L);
        Mockito.lenient().when(ownership.gateway.getVpcId()).thenReturn(9L);
        Mockito.lenient().when(ownership.gateway.getAddrId()).thenReturn(30L);
        Mockito.lenient().when(ownership.gateway.getAccountId()).thenReturn(accountId);
        Mockito.lenient().when(ownership.vpc.getId()).thenReturn(9L);
        Mockito.lenient().when(ownership.vpc.getAccountId()).thenReturn(accountId);
        Mockito.lenient().when(ownership.vpc.getDomainId()).thenReturn(domainId);
        Mockito.lenient().when(ownership.vpc.getZoneId()).thenReturn(zoneId);
        Mockito.lenient().when(ownership.ip.getVpcId()).thenAnswer(invocation -> ownership.ipVpcId);
        Mockito.lenient().when(ownership.ip.getAllocatedToAccountId()).thenAnswer(invocation -> ownership.ipAccountId);
        Mockito.lenient().when(site2SiteVpnGatewayDao.findById(connection.getVpnGatewayId()))
                .thenAnswer(invocation -> ownership.currentGateway);
        Mockito.lenient().when(vpcDao.findById(9L)).thenAnswer(invocation -> ownership.currentVpc);
        Mockito.lenient().when(ipAddressDao.findById(30L)).thenAnswer(invocation -> ownership.currentIp);
        if (markerValue != null) {
            ownership.marker = new UserIpAddressDetailVO(30L, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL, markerValue, false);
        }
        Mockito.lenient().when(userIpAddressDetailsDao.findDetail(30L, NsxElement.NSX_VPN_GATEWAY_IP_DETAIL))
                .thenAnswer(invocation -> ownership.marker);
        Mockito.lenient().when(site2SiteVpnGatewayDetailsDao.findDetail(7L,
                Site2SiteVpnServiceProvider.VPN_GATEWAY_PROVIDER_DETAIL)).thenAnswer(invocation -> ownership.providerDetail);
        return ownership;
    }

    private Site2SiteVpnGatewayDetailVO pollingProviderDetail(String providerName) {
        return new Site2SiteVpnGatewayDetailVO(7L, Site2SiteVpnServiceProvider.VPN_GATEWAY_PROVIDER_DETAIL, providerName, false);
    }

    private void assertPollerQueriesOwnedAllocation(String markerValue, String providerName) {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        PollingOwnership ownership = mockPollingOwnership(connection, markerValue);
        if (providerName != null) {
            ownership.providerDetail = pollingProviderDetail(providerName);
        }
        when(site2SiteVpnConnectionDao.acquireInLockTable(connection.getId())).thenReturn(connection);
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doReturn("UP").when(service).getVpnConnectionStatus(ownership.vpc, connection.getId());

        runVpnStatusPollTaskWithLockAcquired(service);

        verify(service).pollVpnConnectionStatus(connection, ownership.vpc);
        org.mockito.InOrder order = Mockito.inOrder(ipAddressDao, site2SiteVpnConnectionDao, service);
        order.verify(ipAddressDao).findById(30L);
        order.verify(site2SiteVpnConnectionDao).acquireInLockTable(connection.getId());
        order.verify(ipAddressDao).findById(30L);
        order.verify(service).getVpnConnectionStatus(ownership.vpc, connection.getId());
        order.verify(site2SiteVpnConnectionDao).persist(connection);
        order.verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
        assertEquals(Site2SiteVpnConnection.State.Connected, connection.getState());
    }

    private void assertPollerSkipsGateway(Consumer<PollingOwnership> change) {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        PollingOwnership ownership = mockPollingOwnership(connection, "true");
        change.accept(ownership);
        NsxServiceImpl service = Mockito.spy(nsxService);

        runVpnStatusPollTaskWithLockAcquired(service);

        verify(service, never()).pollVpnConnectionStatus(any(), any());
        verify(site2SiteVpnConnectionDao, never()).acquireInLockTable(anyLong());
        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(alertManager, never()).sendAlert(any(), anyLong(), any(), any(), any());
        assertEquals(Site2SiteVpnConnection.State.Connecting, connection.getState());
    }

    private void assertPollerRechecksOwnershipAfterLock(Consumer<PollingOwnership> change) {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        PollingOwnership ownership = mockPollingOwnership(connection, "true");
        AtomicBoolean lockAcquired = new AtomicBoolean();
        when(site2SiteVpnConnectionDao.acquireInLockTable(connection.getId())).thenAnswer(invocation -> {
            change.accept(ownership);
            lockAcquired.set(true);
            return connection;
        });
        NsxServiceImpl service = Mockito.spy(nsxService);
        Mockito.doAnswer(invocation -> {
            try {
                return invocation.callRealMethod();
            } catch (Exception e) {
                // The outer task logs exceptions; an accidental exception must not satisfy a skip assertion.
                throw new AssertionError("Ownership revalidation must skip without throwing", e);
            }
        }).when(service).pollVpnConnectionStatus(connection, ownership.vpc);

        runVpnStatusPollTaskWithLockAcquired(service);

        assertTrue(lockAcquired.get());
        verify(service).pollVpnConnectionStatus(connection, ownership.vpc);
        verify(site2SiteVpnConnectionDao).acquireInLockTable(connection.getId());
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(connection.getId());
        verify(service, never()).getVpnConnectionStatus(any(), anyLong());
        verify(site2SiteVpnConnectionDao, never()).persist(any());
        verify(alertManager, never()).sendAlert(any(), anyLong(), any(), any(), any());
        assertEquals(Site2SiteVpnConnection.State.Connecting, connection.getState());
    }

    private void runVpnStatusPollTaskWithLockAcquired(NsxServiceImpl service) {
        GlobalLock scanLock = mock(GlobalLock.class);
        try (MockedStatic<GlobalLock> globalLocks = Mockito.mockStatic(GlobalLock.class)) {
            globalLocks.when(() -> GlobalLock.getInternLock(NsxServiceImpl.VPN_STATUS_POLL_LOCK_NAME)).thenReturn(scanLock);
            when(scanLock.lock(NsxServiceImpl.VPN_STATUS_POLL_LOCK_TIMEOUT)).thenReturn(true);

            service.new VpnStatusPollTask().runInContext();

            verify(scanLock).unlock();
            verify(scanLock).releaseRef();
        }
    }
}
