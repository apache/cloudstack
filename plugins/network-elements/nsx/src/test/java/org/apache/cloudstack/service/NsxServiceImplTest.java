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

import com.cloud.network.IpAddress;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.Site2SiteVpnGatewayVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.nsx.NsxVpnGatewayResult;
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
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.apache.cloudstack.resourcedetail.UserIpAddressDetailVO;
import org.apache.cloudstack.resourcedetail.dao.UserIpAddressDetailsDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
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
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        AtomicReference<Site2SiteVpnConnection.State> transitionedState = new AtomicReference<>();

        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            public String getVpnConnectionStatus(Vpc vpc, String connectionUuid) {
                return "UP";
            }

            @Override
            protected void transitionVpnConnectionState(Site2SiteVpnConnectionVO connection, VpcVO vpc,
                                                        Site2SiteVpnConnection.State observedState,
                                                        Site2SiteVpnConnection.State newState) {
                transitionedState.set(newState);
            }
        };

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Connected, transitionedState.get());
    }

    @Test
    public void testPollVpnConnectionStatusTransitionsDown() {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        when(connection.getState()).thenReturn(Site2SiteVpnConnection.State.Connected);
        AtomicReference<Site2SiteVpnConnection.State> transitionedState = new AtomicReference<>();

        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            public String getVpnConnectionStatus(Vpc vpc, String connectionUuid) {
                return VPN_SESSION_STATUS_DOWN;
            }

            @Override
            protected void transitionVpnConnectionState(Site2SiteVpnConnectionVO connection, VpcVO vpc,
                                                        Site2SiteVpnConnection.State observedState,
                                                        Site2SiteVpnConnection.State newState) {
                transitionedState.set(newState);
            }
        };

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Disconnected, transitionedState.get());
    }

    @Test
    public void testPollVpnConnectionStatusKeepsPendingConnectionWhenSessionIsNotFound() {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        when(connection.getState()).thenReturn(Site2SiteVpnConnection.State.Pending);
        AtomicBoolean transitioned = new AtomicBoolean();

        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            public String getVpnConnectionStatus(Vpc vpc, String connectionUuid) {
                return VPN_SESSION_STATUS_NOT_FOUND;
            }

            @Override
            protected void transitionVpnConnectionState(Site2SiteVpnConnectionVO connection, VpcVO vpc,
                                                        Site2SiteVpnConnection.State observedState,
                                                        Site2SiteVpnConnection.State newState) {
                transitioned.set(true);
            }
        };

        service.pollVpnConnectionStatus(connection, vpc);

        assertFalse(transitioned.get());
    }

    @Test
    public void testPollVpnConnectionStatusMarksMissingConnectedSessionAsError() {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        when(connection.getState()).thenReturn(Site2SiteVpnConnection.State.Connected);
        AtomicReference<Site2SiteVpnConnection.State> transitionedState = new AtomicReference<>();

        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            public String getVpnConnectionStatus(Vpc vpc, String connectionUuid) {
                return VPN_SESSION_STATUS_NOT_FOUND;
            }

            @Override
            protected void transitionVpnConnectionState(Site2SiteVpnConnectionVO connection, VpcVO vpc,
                                                        Site2SiteVpnConnection.State observedState,
                                                        Site2SiteVpnConnection.State newState) {
                transitionedState.set(newState);
            }
        };

        service.pollVpnConnectionStatus(connection, vpc);

        assertEquals(Site2SiteVpnConnection.State.Error, transitionedState.get());
    }

    @Test
    public void testPollVpnConnectionStatusDoesNotTransitionOnQueryFailure() {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        when(connection.getId()).thenReturn(11L);
        AtomicBoolean transitioned = new AtomicBoolean();

        NsxServiceImpl service = new NsxServiceImpl() {
            @Override
            public String getVpnConnectionStatus(Vpc vpc, String connectionUuid) {
                throw new CloudRuntimeException("NSX unavailable");
            }

            @Override
            protected void transitionVpnConnectionState(Site2SiteVpnConnectionVO connection, VpcVO vpc,
                                                        Site2SiteVpnConnection.State observedState,
                                                        Site2SiteVpnConnection.State newState) {
                transitioned.set(true);
            }
        };

        service.pollVpnConnectionStatus(connection, vpc);

        // A transient management-plane error must not turn a valid connection into Error.
        assertTrue(!transitioned.get());
    }

    @Test
    public void testTransitionVpnConnectionStateIgnoresStaleStatusObservation() {
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        Site2SiteVpnConnectionVO lock = mock(Site2SiteVpnConnectionVO.class);
        Site2SiteVpnConnectionVO current = mock(Site2SiteVpnConnectionVO.class);
        VpcVO vpc = mock(VpcVO.class);
        when(connection.getId()).thenReturn(11L);
        when(lock.getId()).thenReturn(11L);
        when(current.getState()).thenReturn(Site2SiteVpnConnection.State.Disconnected);
        when(site2SiteVpnConnectionDao.acquireInLockTable(11L)).thenReturn(lock);
        when(site2SiteVpnConnectionDao.findById(11L)).thenReturn(current);

        nsxService.transitionVpnConnectionState(connection, vpc, Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected);

        verify(site2SiteVpnConnectionDao, never()).persist(current);
        verify(site2SiteVpnConnectionDao).releaseFromLockTable(11L);
    }

    @Test
    public void testVpnStatusPollerSkipsUnmarkedGatewayRegardlessOfCurrentOffering() {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        Site2SiteVpnGatewayVO gateway = mockVpnGatewayForPoller(connection);
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(gateway.getVpcId())).thenReturn(vpc);
        NsxServiceImpl service = Mockito.spy(nsxService);

        service.new VpnStatusPollTask().runInContext();

        verify(service, never()).pollVpnConnectionStatus(connection, vpc);
    }

    @Test
    public void testVpnStatusPollerUsesPersistedOwnershipAfterOfferingChanges() {
        Site2SiteVpnConnectionVO connection = mockPollableVpnConnection();
        Site2SiteVpnGatewayVO gateway = mockVpnGatewayForPoller(connection);
        VpcVO vpc = mock(VpcVO.class);
        when(vpcDao.findById(gateway.getVpcId())).thenReturn(vpc);
        when(userIpAddressDetailsDao.findDetail(gateway.getAddrId(), NsxElement.NSX_VPN_GATEWAY_IP_DETAIL))
                .thenReturn(mock(UserIpAddressDetailVO.class));
        NsxServiceImpl service = Mockito.spy(nsxService);
        doNothing().when(service).pollVpnConnectionStatus(connection, vpc);

        service.new VpnStatusPollTask().runInContext();

        verify(service).pollVpnConnectionStatus(connection, vpc);
    }

    @Test
    public void testVpnStatusPollerQueriesOnlyPollableStates() {
        nsxService.new VpnStatusPollTask().runInContext();

        verify(site2SiteVpnConnectionDao).listByStates(
                Site2SiteVpnConnection.State.Pending,
                Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected,
                Site2SiteVpnConnection.State.Disconnected);
        verify(site2SiteVpnConnectionDao, never()).listAll();
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
        Site2SiteVpnConnectionVO connection = mock(Site2SiteVpnConnectionVO.class);
        when(connection.getId()).thenReturn(11L);
        when(connection.getVpnGatewayId()).thenReturn(7L);
        when(site2SiteVpnConnectionDao.listByStates(
                Site2SiteVpnConnection.State.Pending,
                Site2SiteVpnConnection.State.Connecting,
                Site2SiteVpnConnection.State.Connected,
                Site2SiteVpnConnection.State.Disconnected)).thenReturn(List.of(connection));
        return connection;
    }

    private Site2SiteVpnGatewayVO mockVpnGatewayForPoller(Site2SiteVpnConnectionVO connection) {
        Site2SiteVpnGatewayVO gateway = mock(Site2SiteVpnGatewayVO.class);
        when(gateway.getVpcId()).thenReturn(9L);
        when(gateway.getAddrId()).thenReturn(30L);
        when(site2SiteVpnGatewayDao.findById(connection.getVpnGatewayId())).thenReturn(gateway);
        return gateway;
    }
}
