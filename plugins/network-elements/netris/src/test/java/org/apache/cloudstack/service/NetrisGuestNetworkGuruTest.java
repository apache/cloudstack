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

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEventUtils;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.ReservationContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

public class NetrisGuestNetworkGuruTest {

    @Mock
    private NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    private PhysicalNetworkDao physicalNetworkDao;
    @Mock
    private DataCenterDao dataCenterDao;
    @Mock
    private NetworkModel networkModel;
    @Mock
    private AccountDao accountDao;
    @Mock
    private DomainDao domainDao;
    @Mock
    private NetworkOfferingDao networkOfferingDao;
    @Mock
    private VpcDao vpcDao;
    @Mock
    private NetrisService netrisService;
    @Mock
    private NetworkDetailsDao networkDetailsDao;

    @Spy
    @InjectMocks
    private NetrisGuestNetworkGuru guru = new NetrisGuestNetworkGuru();

    @Mock
    private NetworkOfferingVO networkOffering;
    @Mock
    private PhysicalNetworkVO physicalNetwork;
    @Mock
    private DeploymentPlan plan;
    @Mock
    private NetworkVO network;
    @Mock
    private AccountVO account;
    @Mock
    private DomainVO domain;
    @Mock
    private DataCenterVO zone;
    @Mock
    private VpcVO vpc;

    private AutoCloseable closeable;
    private MockedStatic<ActionEventUtils> actionEventUtilsMocked;

    private static final long networkOfferingId = 10L;
    private static final long physicalNetworkId = 2L;
    private static final long zoneId = 1L;
    private static final long accountId = 2L;
    private static final long domainId = 7L;
    private static final long vpcId = 12L;
    private static final long networkId = 210L;
    private static final String networkName = "test-network";
    private static final String vpcName = "test-vpc";
    private static final String networkCidr = "172.20.10.0/24";

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        Mockito.when(networkOffering.getId()).thenReturn(networkOfferingId);
        Mockito.when(networkOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(networkOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED);
        Mockito.when(networkOffering.isRedundantRouter()).thenReturn(false);
        Mockito.when(networkOfferingDao.findById(networkOfferingId)).thenReturn(networkOffering);
        Mockito.when(physicalNetwork.getIsolationMethods()).thenReturn(List.of("netris"));
        Mockito.when(physicalNetworkDao.findById(physicalNetworkId)).thenReturn(physicalNetwork);
        Mockito.when(networkOfferingServiceMapDao.isProviderForNetworkOffering(networkOfferingId, Network.Provider.Netris)).thenReturn(true);
        Mockito.when(plan.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        Mockito.when(plan.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(dataCenterDao.findById(zoneId)).thenReturn(zone);
        Mockito.when(zone.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        Mockito.when(zone.getGuestNetworkCidr()).thenReturn("172.20.0.0/16");
        Mockito.when(zone.getId()).thenReturn(zoneId);
        List<Network.Service> offeringServices = Arrays.asList(Network.Service.Dns, Network.Service.Dhcp,
                Network.Service.SourceNat, Network.Service.StaticNat, Network.Service.PortForwarding,
                Network.Service.NetworkACL, Network.Service.Vpn);
        Mockito.when(networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(offeringServices);
        Mockito.when(accountDao.findById(accountId)).thenReturn(account);
        Mockito.when(account.getDomainId()).thenReturn(domainId);
        Mockito.when(account.getId()).thenReturn(accountId);
        Mockito.when(domain.getId()).thenReturn(domainId);
        Mockito.when(domainDao.findById(domainId)).thenReturn(domain);
        Mockito.when(network.getAccountId()).thenReturn(accountId);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        Mockito.when(network.getVpcId()).thenReturn(vpcId);
        Mockito.when(network.getName()).thenReturn(networkName);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(network.getCidr()).thenReturn(networkCidr);
        Mockito.when(network.getGateway()).thenReturn("172.20.10.1");
        Mockito.when(network.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(network.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        Mockito.when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Mockito.when(network.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Netris);
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(vpc);
        Mockito.when(vpc.getName()).thenReturn(vpcName);
        Mockito.when(vpc.getId()).thenReturn(vpcId);
        actionEventUtilsMocked = Mockito.mockStatic(ActionEventUtils.class);
        Mockito.when(ActionEventUtils.onCompletedActionEvent(anyLong(), anyLong(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong())).thenReturn(1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
        actionEventUtilsMocked.close();
    }

    @Test
    public void testCanHandleNetrisOfferingNatted() {
        Assert.assertTrue(guru.canHandle(networkOffering, DataCenter.NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void testCanHandleNetrisOfferingRouted() {
        Mockito.when(networkOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.ROUTED);
        Assert.assertTrue(guru.canHandle(networkOffering, DataCenter.NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void testCannotHandleBasicNetwork() {
        Assert.assertFalse(guru.canHandle(networkOffering, DataCenter.NetworkType.Basic, physicalNetwork));
    }

    @Test
    public void testCannotHandleVlanIsolation() {
        Mockito.when(physicalNetwork.getIsolationMethods()).thenReturn(List.of("vlan"));
        Assert.assertFalse(guru.canHandle(networkOffering, DataCenter.NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void testCannotHandleDifferentOfferingProvider() {
        Mockito.when(networkOfferingServiceMapDao.isProviderForNetworkOffering(networkOfferingId, Network.Provider.Netris)).thenReturn(false);
        Assert.assertFalse(guru.canHandle(networkOffering, DataCenter.NetworkType.Advanced, physicalNetwork));
    }

    @Test
    public void testDesignNetrisNetwork() {
        Network designedNetwork = guru.design(networkOffering, plan, network, networkName, 1L, account);
        Assert.assertEquals(Networks.BroadcastDomainType.Netris, designedNetwork.getBroadcastDomainType());
        Assert.assertEquals(Network.State.Allocated, designedNetwork.getState());
        Assert.assertEquals(Networks.TrafficType.Guest, designedNetwork.getTrafficType());
    }

    @Test
    public void testCreateNetrisVnetVpcNetworkRoutedMode() {
        Mockito.when(networkOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.ROUTED);
        Mockito.when(netrisService.createVnetResource(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                Mockito.anyBoolean())).thenReturn(true);
        guru.createNetrisVnet(network, zone);
        Mockito.verify(netrisService).createVnetResource(zoneId, accountId, domainId, vpcName, vpcId,
                networkName, networkId, networkCidr, true);
    }

    @Test
    public void testImplementNetrisVpcNetwork() throws InsufficientVirtualNetworkCapacityException {
        DeployDestination destination = Mockito.mock(DeployDestination.class);
        ReservationContext context = Mockito.mock(ReservationContext.class);
        String vnet = "1234";
        Mockito.when(dataCenterDao.allocateVnet(Mockito.eq(zoneId), Mockito.eq(physicalNetworkId),
                Mockito.eq(accountId), Mockito.nullable(String.class), Mockito.anyBoolean())).thenReturn(vnet);
        actionEventUtilsMocked.when(() -> ActionEventUtils.onCompletedActionEvent(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong())).thenReturn(1L);
        Mockito.when(networkDetailsDao.findDetail(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);
        Network implemented = guru.implement(network, networkOffering, destination, context);
        Assert.assertEquals(String.format("netris://%s", vnet), implemented.getBroadcastUri().toString());
        Assert.assertEquals(Network.State.Implemented, implemented.getState());
    }
}
