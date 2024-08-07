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
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateNsxDhcpRelayConfigCommand;
import org.apache.cloudstack.agent.api.CreateNsxSegmentCommand;
import org.apache.cloudstack.agent.api.CreateNsxTier1GatewayCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class NsxGuestNetworkGuruTest {

    @Mock
    PhysicalNetworkDao physicalNetworkDao;
    @Mock
    DataCenterDao dcDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Mock
    NsxControllerUtils nsxControllerUtils;
    @Mock
    AccountDao accountDao;
    @Mock
    PhysicalNetworkVO physicalNetwork;
    @Mock
    DataCenterVO dataCenterVO;
    @Mock
    NetworkOffering offering;
    @Mock
    DeploymentPlan plan;
    @Mock
    Network network;
    @Mock
    Account account;
    @Mock
    VpcVO vpcVO;
    @Mock
    NetworkModel networkModel;
    @Mock
    DomainDao domainDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    IpAddressManager ipAddressManager;
    @Mock
    NetworkOfferingDao networkOfferingDao;

    NsxGuestNetworkGuru guru;
    AutoCloseable closeable;

    @Before
    public void setUp() throws IllegalAccessException, NoSuchFieldException {
        closeable = MockitoAnnotations.openMocks(this);
        guru = new NsxGuestNetworkGuru();

        ReflectionTestUtils.setField(guru, "_dcDao", dcDao);
        ReflectionTestUtils.setField(guru, "_networkDao", networkDao);
        ReflectionTestUtils.setField(guru, "_networkModel", networkModel);
        ReflectionTestUtils.setField(guru, "_vpcDao", vpcDao);
        ReflectionTestUtils.setField((GuestNetworkGuru) guru, "_ipAddrMgr", ipAddressManager);
        ReflectionTestUtils.setField((GuestNetworkGuru) guru, "_networkModel", networkModel);
        ReflectionTestUtils.setField((GuestNetworkGuru) guru, "networkOfferingDao", networkOfferingDao);
        ReflectionTestUtils.setField((GuestNetworkGuru) guru, "_physicalNetworkDao", physicalNetworkDao);

        guru.networkOfferingServiceMapDao = networkOfferingServiceMapDao;
        guru.nsxControllerUtils = nsxControllerUtils;
        guru.accountDao = accountDao;
        guru.domainDao = domainDao;

        Mockito.when(dataCenterVO.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);

        when(physicalNetwork.getIsolationMethods()).thenReturn(List.of("NSX"));

        when(offering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(offering.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(offering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED);
        when(offering.getId()).thenReturn(1L);

        when(plan.getDataCenterId()).thenReturn(1L);
        when(plan.getPhysicalNetworkId()).thenReturn(1L);

        when(vpcDao.findById(anyLong())).thenReturn(vpcVO);

        when(vpcVO.getName()).thenReturn("VPC01");

        when(account.getAccountId()).thenReturn(1L);
        when(accountDao.findById(anyLong())).thenReturn(mock(AccountVO.class));
        when(domainDao.findById(anyLong())).thenReturn(mock(DomainVO.class));

        Mockito.when(networkOfferingServiceMapDao.isProviderForNetworkOffering(offering.getId(), Network.Provider.Nsx)).thenReturn(
                true);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testIsMyIsolationMethod() {
        assertTrue(guru.isMyIsolationMethod(physicalNetwork));
    }

    @Test
    public void testCanHandle() {
        assertTrue(guru.canHandle(offering, dataCenterVO.getNetworkType(), physicalNetwork));
    }

    @Test
    public void testNsxNetworkDesign() {
        when(physicalNetworkDao.findById(ArgumentMatchers.anyLong())).thenReturn(physicalNetwork);
        when(dcDao.findById(ArgumentMatchers.anyLong())).thenReturn(dataCenterVO);

        Network designedNetwork = guru.design(offering,  plan, network, "", 1L, account);
        assertNotNull(designedNetwork);
        assertSame(Networks.BroadcastDomainType.NSX, designedNetwork.getBroadcastDomainType());
        assertSame(Network.State.Allocated, designedNetwork.getState());
    }

    @Test
    public void testNsxNetworkSetup() {
        when(dcDao.findById(ArgumentMatchers.anyLong())).thenReturn(dataCenterVO);
        when(networkDao.findById(ArgumentMatchers.anyLong())).thenReturn(mock(NetworkVO.class));
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxSegmentCommand.class), anyLong())).thenReturn(
                new NsxAnswer(new NsxCommand(), true, ""));

        guru.setup(network, 1L);
        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateNsxSegmentCommand.class), anyLong());
    }

    @Test
    public void testNsxNetworkImplementation() {
        final DeployDestination deployDestination = mock(DeployDestination.class);
        final ReservationContext reservationContext = mock(ReservationContext.class);

        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(network.getMode()).thenReturn(Networks.Mode.Dhcp);
        when(network.getGateway()).thenReturn("192.168.1.1");
        when(network.getCidr()).thenReturn("192.168.1.0/24");
        when(network.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.NSX);
        when(network.getNetworkOfferingId()).thenReturn(1L);
        lenient().when(network.getState()).thenReturn(Network.State.Implementing);
        when(network.getDataCenterId()).thenReturn(2L);
        when(network.getPhysicalNetworkId()).thenReturn(3L);
        when(network.getVpcId()).thenReturn(4L);
        when(offering.isRedundantRouter()).thenReturn(false);
        lenient().when(offering.getGuestType()).thenReturn(Network.GuestType.Isolated);


        final Network implemented = guru.implement(network, offering, deployDestination, reservationContext);
        assertEquals(Networks.BroadcastDomainType.NSX.toUri("nsx"), implemented.getBroadcastUri());
        assertEquals("192.168.1.1", implemented.getGateway());
        assertEquals("192.168.1.0/24", implemented.getCidr());
        assertEquals(Networks.Mode.Dhcp, implemented.getMode());
        assertEquals(Networks.BroadcastDomainType.NSX, implemented.getBroadcastDomainType());
        assertEquals(1L, implemented.getNetworkOfferingId());
        assertEquals(Network.State.Implemented, implemented.getState());
        assertEquals(2L, implemented.getDataCenterId());
        assertEquals(3L, implemented.getPhysicalNetworkId().longValue());
        assertEquals(4L, implemented.getVpcId().longValue());
        assertFalse(implemented.isRedundant());
    }

    @Test
    public void testAllocateForUserVM() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Network network = Mockito.mock(Network.class);
        NicProfile nicProfile = Mockito.mock(NicProfile.class);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Pair<String, String> dns = new Pair<>("10.1.5.1", "8.8.8.8");
        String macAddress = "00:00:00:11:1D:1E:CD";

        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(vmProfile.getVirtualMachine()).thenReturn(virtualMachine);
        when(virtualMachine.getType()).thenReturn(VirtualMachine.Type.User);
        when(networkModel.getNetworkIp4Dns(any(Network.class), nullable(DataCenter.class))).thenReturn(dns);
        when(nicProfile.getMacAddress()).thenReturn(macAddress);
        when(networkOfferingDao.isIpv6Supported(anyLong())).thenReturn(false);

        NicProfile profile = guru.allocate(network, nicProfile, vmProfile);
        assertNotNull(profile);
    }

    @Test
    public void testAllocateForDomainRouter() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Network network = Mockito.mock(Network.class);
        NicProfile nicProfile = Mockito.mock(NicProfile.class);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        VirtualMachine virtualMachine = Mockito.mock(VirtualMachine.class);
        Pair<String, String> dns = new Pair<>("10.1.5.1", "8.8.8.8");
        String macAddress = "00:00:00:11:1D:1E:CD";

        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        when(vmProfile.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(vmProfile.getVirtualMachine()).thenReturn(virtualMachine);
        when(virtualMachine.getType()).thenReturn(VirtualMachine.Type.DomainRouter);
        when(network.getId()).thenReturn(2L);
        when(nicProfile.getMacAddress()).thenReturn(macAddress);
        when(networkOfferingDao.isIpv6Supported(anyLong())).thenReturn(false);
        when(network.getDataCenterId()).thenReturn(1L);
        when(network.getAccountId()).thenReturn(5L);
        when(network.getVpcId()).thenReturn(51L);
        when(dcDao.findById(anyLong())).thenReturn(Mockito.mock(DataCenterVO.class));
        when(accountDao.findById(anyLong())).thenReturn(Mockito.mock(AccountVO.class));
        when(vpcDao.findById(anyLong())).thenReturn(Mockito.mock(VpcVO.class));
        when(domainDao.findById(anyLong())).thenReturn(Mockito.mock(DomainVO.class));
        when(nicProfile.getIPv4Address()).thenReturn("10.1.13.10");
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxDhcpRelayConfigCommand.class),
                anyLong())).thenReturn(new NsxAnswer(new NsxCommand(), true, ""));

        NicProfile profile = guru.allocate(network, nicProfile, vmProfile);

        assertNotNull(profile);
        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateNsxDhcpRelayConfigCommand.class),
                anyLong());
    }

    @Test
    public void testCreateNsxSegmentForVpc() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);

        when(networkVO.getAccountId()).thenReturn(1L);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxSegmentCommand.class),
                anyLong())).thenReturn(new NsxAnswer(new NsxCommand(), true, ""));
        guru.createNsxSegment(networkVO, dataCenter);
        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateNsxSegmentCommand.class),
                anyLong());
    }


    @Test
    public void testCreateNsxSegmentForIsolatedNetwork() {
        NetworkVO networkVO = Mockito.mock(NetworkVO.class);
        NetworkOfferingVO offeringVO = Mockito.mock(NetworkOfferingVO.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);

        when(networkVO.getAccountId()).thenReturn(1L);
        when(networkVO.getVpcId()).thenReturn(null);
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxTier1GatewayCommand.class),
                anyLong())).thenReturn(new NsxAnswer(new NsxCommand(), true, ""));
        when(nsxControllerUtils.sendNsxCommand(any(CreateNsxSegmentCommand.class),
                anyLong())).thenReturn(new NsxAnswer(new NsxCommand(), true, ""));
        when(networkVO.getNetworkOfferingId()).thenReturn(1L);
        when(networkOfferingDao.findById(1L)).thenReturn(offeringVO);
        when(offeringVO.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED);
        guru.createNsxSegment(networkVO, dataCenter);
        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateNsxTier1GatewayCommand.class),
                anyLong());
        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateNsxSegmentCommand.class),
                anyLong());
    }
}
