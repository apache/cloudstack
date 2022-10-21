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
package org.apache.cloudstack.engine.orchestration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Matchers;
import org.mockito.Mockito;

import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.RouterNetworkDao;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicExtraDhcpOptionDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicSecondaryIpDao;

import junit.framework.TestCase;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@RunWith(JUnit4.class)
public class NetworkOrchestratorTest extends TestCase {
    static final Logger s_logger = Logger.getLogger(NetworkOrchestratorTest.class);

    NetworkOrchestrator testOrchastrator = Mockito.spy(new NetworkOrchestrator());

    private String guruName = "GuestNetworkGuru";
    private String dhcpProvider = "VirtualRouter";
    private NetworkGuru guru = mock(NetworkGuru.class);

    NetworkOfferingVO networkOffering = mock(NetworkOfferingVO.class);

    private static final long networkOfferingId = 1l;

    final String[] ip4Dns = {"5.5.5.5", "6.6.6.6"};
    final String[] ip6Dns = {"2001:4860:4860::5555", "2001:4860:4860::6666"};
    final String[] ip4AltDns = {"7.7.7.7", "8.8.8.8"};
    final String[] ip6AltDns = {"2001:4860:4860::7777", "2001:4860:4860::8888"};

    @Override
    @Before
    public void setUp() {
        // make class-scope mocks
        testOrchastrator._nicDao = mock(NicDao.class);
        testOrchastrator._networksDao = mock(NetworkDao.class);
        testOrchastrator._networkModel = mock(NetworkModel.class);
        testOrchastrator._nicSecondaryIpDao = mock(NicSecondaryIpDao.class);
        testOrchastrator._ntwkSrvcDao = mock(NetworkServiceMapDao.class);
        testOrchastrator._nicIpAliasDao = mock(NicIpAliasDao.class);
        testOrchastrator._ipAddressDao = mock(IPAddressDao.class);
        testOrchastrator._vlanDao = mock(VlanDao.class);
        testOrchastrator._networkModel = mock(NetworkModel.class);
        testOrchastrator._nicExtraDhcpOptionDao = mock(NicExtraDhcpOptionDao.class);
        testOrchastrator.routerDao = mock(DomainRouterDao.class);
        testOrchastrator.routerNetworkDao = mock(RouterNetworkDao.class);
        testOrchastrator._vpcMgr = mock(VpcManager.class);
        DhcpServiceProvider provider = mock(DhcpServiceProvider.class);

        Map<Network.Capability, String> capabilities = new HashMap<Network.Capability, String>();
        Map<Network.Service, Map<Network.Capability, String>> services = new HashMap<Network.Service, Map<Network.Capability, String>>();
        services.put(Network.Service.Dhcp, capabilities);
        when(provider.getCapabilities()).thenReturn(services);
        capabilities.put(Network.Capability.DhcpAccrossMultipleSubnets, "true");

        when(testOrchastrator._ntwkSrvcDao.getProviderForServiceInNetwork(Matchers.anyLong(), Matchers.eq(Service.Dhcp))).thenReturn(dhcpProvider);
        when(testOrchastrator._networkModel.getElementImplementingProvider(dhcpProvider)).thenReturn(provider);

        when(guru.getName()).thenReturn(guruName);
        List<NetworkGuru> networkGurus = new ArrayList<NetworkGuru>();
        networkGurus.add(guru);
        testOrchastrator.networkGurus = networkGurus;

        when(networkOffering.getGuestType()).thenReturn(GuestType.L2);
        when(networkOffering.getId()).thenReturn(networkOfferingId);
    }

    @Test
    public void testRemoveDhcpServiceWithNic() {
        // make local mocks
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        // make sure that release dhcp will be called
        when(vm.getType()).thenReturn(Type.User);
        when(testOrchastrator._networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)).thenReturn(true);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(network.getGuestType()).thenReturn(GuestType.Shared);
        when(testOrchastrator._nicDao.listByNetworkIdTypeAndGatewayAndBroadcastUri(nic.getNetworkId(), VirtualMachine.Type.User, nic.getIPv4Gateway(), nic.getBroadcastUri()))
                .thenReturn(new ArrayList<NicVO>());

        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchastrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchastrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchastrator._networkModel, times(2)).getElementImplementingProvider(dhcpProvider);
        verify(testOrchastrator._ntwkSrvcDao, times(2)).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchastrator._networksDao, times(2)).findById(nic.getNetworkId());
    }
    @Test
    public void testDontRemoveDhcpServiceFromDomainRouter() {
        // make local mocks
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        // make sure that release dhcp won't be called
        when(vm.getType()).thenReturn(Type.DomainRouter);

        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchastrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchastrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchastrator._networkModel, never()).getElementImplementingProvider(dhcpProvider);
        verify(testOrchastrator._ntwkSrvcDao, never()).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchastrator._networksDao, times(1)).findById(nic.getNetworkId());
    }
    @Test
    public void testDontRemoveDhcpServiceWhenNotProvided() {
        // make local mocks
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        // make sure that release dhcp will *not* be called
        when(vm.getType()).thenReturn(Type.User);
        when(testOrchastrator._networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)).thenReturn(false);

        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchastrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchastrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchastrator._networkModel, never()).getElementImplementingProvider(dhcpProvider);
        verify(testOrchastrator._ntwkSrvcDao, never()).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchastrator._networksDao, times(1)).findById(nic.getNetworkId());
    }

    @Test
    public void testCheckL2OfferingServicesEmptyServices() {
        when(testOrchastrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(new ArrayList<>());
        when(testOrchastrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(false);
        testOrchastrator.checkL2OfferingServices(networkOffering);
    }

    @Test
    public void testCheckL2OfferingServicesUserDataOnly() {
        when(testOrchastrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.UserData));
        when(testOrchastrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(true);
        testOrchastrator.checkL2OfferingServices(networkOffering);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckL2OfferingServicesMultipleServicesIncludingUserData() {
        when(testOrchastrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.UserData, Service.Dhcp));
        when(testOrchastrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(true);
        testOrchastrator.checkL2OfferingServices(networkOffering);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckL2OfferingServicesMultipleServicesNotIncludingUserData() {
        when(testOrchastrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.Dns, Service.Dhcp));
        when(testOrchastrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(false);
        testOrchastrator.checkL2OfferingServices(networkOffering);
    }

    @Test
    public void testConfigureNicProfileBasedOnRequestedIpTestMacNull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, null, "192.168.100.150");

        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

        verifyAndAssert("192.168.100.150", "192.168.100.1", "255.255.255.0", nicProfile, 1, 1);
    }

    @Test
    public void testConfigureNicProfileBasedOnRequestedIpTestNicProfileMacNotNull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");

        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

        verifyAndAssert("192.168.100.150", "192.168.100.1", "255.255.255.0", nicProfile, 1, 0);
    }

    @Test
    public void testConfigureNicProfileBasedOnRequestedIpTestRequestedIpNull() {
        testConfigureNicProfileBasedOnRequestedIpTestRequestedIp(null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestRequestedIpIsBlank() {
        testConfigureNicProfileBasedOnRequestedIpTestRequestedIp("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestRequestedIpIsNotValid() {
        testConfigureNicProfileBasedOnRequestedIpTestRequestedIp("123");
    }

    private void testConfigureNicProfileBasedOnRequestedIpTestRequestedIp(String requestedIpv4Address) {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, null, requestedIpv4Address);
        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

        verifyAndAssert(null, null, null, nicProfile, 0, 0);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestGatewayIsBlank() {
        testConfigureNicProfileBasedOnRequestedIpTestGateway("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestGatewayIsNotValid() {
        testConfigureNicProfileBasedOnRequestedIpTestGateway("123");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestGatewayIsNull() {
        testConfigureNicProfileBasedOnRequestedIpTestGateway(null);
    }

    private void testConfigureNicProfileBasedOnRequestedIpTestGateway(String ipv4Gateway) {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, ipv4Gateway, "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");
        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
        verifyAndAssert(null, null, null, nicProfile, 1, 0);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestNetmaskIsNull() {
        testConfigureNicProfileBasedOnRequestedIpTestNetmask(null);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestNetmaskIsBlank() {
        testConfigureNicProfileBasedOnRequestedIpTestNetmask("");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestNetmaskIsNotValid() {
        testConfigureNicProfileBasedOnRequestedIpTestNetmask("123");
    }

    private void testConfigureNicProfileBasedOnRequestedIpTestNetmask(String ipv4Netmask) {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", ipv4Netmask, "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");
        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
        verifyAndAssert(null, null, null, nicProfile, 1, 0);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestIPAddressVONull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");
        when(testOrchastrator._vlanDao.findByNetworkIdAndIpv4(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);

        testOrchastrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
        verifyAndAssert(null, null, null, nicProfile, 0, 0);
    }

    private void configureTestConfigureNicProfileBasedOnRequestedIpTests(NicProfile nicProfile, long ipvoId, boolean ipVoIsNull, IPAddressVO.State state, String vlanGateway,
            String vlanNetmask, String macAddress, NicProfile requestedNicProfile, String nicProfileMacAddress, String requestedIpv4Address) {
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        ipVoSpy.setState(state);

        requestedNicProfile.setRequestedIPv4(requestedIpv4Address);
        nicProfile.setMacAddress(nicProfileMacAddress);

        when(ipVoSpy.getId()).thenReturn(ipvoId);
        when(ipVoSpy.getState()).thenReturn(state);

        if (ipVoIsNull) {
            when(testOrchastrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        } else {
            when(testOrchastrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        }

        VlanVO vlanSpy = Mockito.spy(new VlanVO(Vlan.VlanType.DirectAttached, "vlanTag", vlanGateway, vlanNetmask, 0l, "192.168.100.100 - 192.168.100.200", 0l, new Long(0l),
                "ip6Gateway", "ip6Cidr", "ip6Range"));

        Mockito.doReturn(0l).when(vlanSpy).getId();
        when(testOrchastrator._vlanDao.findByNetworkIdAndIpv4(Mockito.anyLong(), Mockito.anyString())).thenReturn(vlanSpy);
        when(testOrchastrator._ipAddressDao.acquireInLockTable(Mockito.anyLong())).thenReturn(ipVoSpy);
        when(testOrchastrator._ipAddressDao.update(Mockito.anyLong(), Mockito.any(IPAddressVO.class))).thenReturn(true);
        when(testOrchastrator._ipAddressDao.releaseFromLockTable(Mockito.anyLong())).thenReturn(true);
        try {
            when(testOrchastrator._networkModel.getNextAvailableMacAddressInNetwork(Mockito.anyLong())).thenReturn(macAddress);
        } catch (InsufficientAddressCapacityException e) {
            e.printStackTrace();
        }
    }

    private void verifyAndAssert(String requestedIpv4Address, String ipv4Gateway, String ipv4Netmask, NicProfile nicProfile, int acquireLockAndCheckIfIpv4IsFreeTimes,
            int nextMacAddressTimes) {
        verify(testOrchastrator, times(acquireLockAndCheckIfIpv4IsFreeTimes)).acquireLockAndCheckIfIpv4IsFree(Mockito.any(Network.class), Mockito.anyString());
        try {
            verify(testOrchastrator._networkModel, times(nextMacAddressTimes)).getNextAvailableMacAddressInNetwork(Mockito.anyLong());
        } catch (InsufficientAddressCapacityException e) {
            e.printStackTrace();
        }

        assertEquals(requestedIpv4Address, nicProfile.getIPv4Address());
        assertEquals(ipv4Gateway, nicProfile.getIPv4Gateway());
        assertEquals(ipv4Netmask, nicProfile.getIPv4Netmask());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireLockAndCheckIfIpv4IsFreeTestIpvoNull() {
        executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State.Free, true, 1, 0, 0, 0, 0);
    }

    @Test
    public void testAcquireLockAndCheckIfIpv4IsFreeTestExpectedFlow() {
        executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State.Free, false, 1, 1, 1, 1, 1);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireLockAndCheckIfIpv4IsFreeTestAllocatedIp() {
        executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State.Allocated, false, 1, 1, 1, 0, 1);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireLockAndCheckIfIpv4IsFreeTestAllocatingIp() {
        executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State.Allocating, false, 1, 1, 1, 0, 1);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testAcquireLockAndCheckIfIpv4IsFreeTestReleasingIp() {
        executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State.Releasing, false, 1, 1, 1, 0, 1);
    }

    private void executeTestAcquireLockAndCheckIfIpv4IsFree(IPAddressVO.State state, boolean isIPAddressVONull, int findByIpTimes, int acquireLockTimes, int releaseFromLockTimes,
            int updateTimes, int validateTimes) {
        Network network = Mockito.spy(new NetworkVO());
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        ipVoSpy.setState(state);
        ipVoSpy.setState(state);
        if (isIPAddressVONull) {
            when(testOrchastrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);
        } else {
            when(testOrchastrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        }
        when(testOrchastrator._ipAddressDao.acquireInLockTable(Mockito.anyLong())).thenReturn(ipVoSpy);
        when(testOrchastrator._ipAddressDao.releaseFromLockTable(Mockito.anyLong())).thenReturn(true);
        when(testOrchastrator._ipAddressDao.update(Mockito.anyLong(), Mockito.any(IPAddressVO.class))).thenReturn(true);

        testOrchastrator.acquireLockAndCheckIfIpv4IsFree(network, "192.168.100.150");

        verify(testOrchastrator._ipAddressDao, Mockito.times(findByIpTimes)).findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString());
        verify(testOrchastrator._ipAddressDao, Mockito.times(acquireLockTimes)).acquireInLockTable(Mockito.anyLong());
        verify(testOrchastrator._ipAddressDao, Mockito.times(releaseFromLockTimes)).releaseFromLockTable(Mockito.anyLong());
        verify(testOrchastrator._ipAddressDao, Mockito.times(updateTimes)).update(Mockito.anyLong(), Mockito.any(IPAddressVO.class));
        verify(testOrchastrator, Mockito.times(validateTimes)).validateLockedRequestedIp(Mockito.any(IPAddressVO.class), Mockito.any(IPAddressVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateLockedRequestedIpTestNullLockedIp() {
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        testOrchastrator.validateLockedRequestedIp(ipVoSpy, null);
    }

    @Test
    public void validateLockedRequestedIpTestNotFreeLockedIp() {
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        State[] states = State.values();
        for(int i=0; i < states.length;i++) {
            boolean expectedException = false;
            if (states[i] == State.Free) {
                continue;
            }
            IPAddressVO lockedIp = ipVoSpy;
            lockedIp.setState(states[i]);
            try {
                testOrchastrator.validateLockedRequestedIp(ipVoSpy, lockedIp);
            } catch (InvalidParameterValueException e) {
                expectedException = true;
            }
            Assert.assertTrue(expectedException);
        }
    }

    @Test
    public void validateLockedRequestedIpTestFreeAndNotNullIp() {
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        IPAddressVO lockedIp = ipVoSpy;
        lockedIp.setState(State.Free);
        testOrchastrator.validateLockedRequestedIp(ipVoSpy, lockedIp);
    }

    @Test
    public void testDontReleaseNicWhenPreserveNicsSettingEnabled() {
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        when(vm.getType()).thenReturn(Type.User);
        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchastrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        Long nicId = 1L;
        when(nic.getId()).thenReturn(nicId);
        when(vm.getParameter(VirtualMachineProfile.Param.PreserveNics)).thenReturn(true);

        testOrchastrator.removeNic(vm, nic);

        verify(nic, never()).setState(Nic.State.Deallocating);
        verify(testOrchastrator._nicDao, never()).remove(nicId);
    }

    public void encodeVlanIdIntoBroadcastUriTestVxlan() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("123", "VXLAN", "vxlan", "vxlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestVlan() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("123", "VLAN", "vlan", "vlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestEmpty() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("123", "", "vlan", "vlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestNull() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("123", null, "vlan", "vlan://123");
    }

    @Test(expected = CloudRuntimeException.class)
    public void encodeVlanIdIntoBroadcastUriTestEmptyVlanId() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("", "vxlan", "vlan", "vlan://123");
    }

    @Test(expected = CloudRuntimeException.class)
    public void encodeVlanIdIntoBroadcastUriTestNullVlanId() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest(null, "vlan", "vlan", "vlan://123");
    }

    @Test(expected = CloudRuntimeException.class)
    public void encodeVlanIdIntoBroadcastUriTestBlankVlanId() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest(" ", "vlan", "vlan", "vlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestNullVlanIdWithSchema() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("vlan://123", "vlan", "vlan", "vlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestNullVlanIdWithSchemaIsolationVxlan() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("vlan://123", "vxlan", "vlan", "vlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestNullVxlanIdWithSchema() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("vxlan://123", "vxlan", "vxlan", "vxlan://123");
    }

    @Test
    public void encodeVlanIdIntoBroadcastUriTestNullVxlanIdWithSchemaIsolationVlan() {
        encodeVlanIdIntoBroadcastUriPrepareAndTest("vxlan://123", "vlan", "vxlan", "vxlan://123");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void encodeVlanIdIntoBroadcastUriTestNullNetwork() {
        URI resultUri = testOrchastrator.encodeVlanIdIntoBroadcastUri("vxlan://123", null);
    }

    private void encodeVlanIdIntoBroadcastUriPrepareAndTest(String vlanId, String isolationMethod, String expectedIsolation, String expectedUri) {
        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO();
        List<String> isolationMethods = new ArrayList<>();
        isolationMethods.add(isolationMethod);
        physicalNetwork.setIsolationMethods(isolationMethods);

        URI resultUri = testOrchastrator.encodeVlanIdIntoBroadcastUri(vlanId, physicalNetwork);

        Assert.assertEquals(expectedIsolation, resultUri.getScheme());
        Assert.assertEquals(expectedUri, resultUri.toString());
    }

    private NicProfile prepareMocksAndRunPrepareNic(VirtualMachine.Type vmType, boolean isDefaultNic, boolean isVpcRouter, boolean routerResourceHasCustomDns) {
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        Long nicId = 1L;
        Long vmId = 1L;
        Long networkId = 1L;
        Integer networkRate = 200;
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getGuruName()).thenReturn(GuestNetworkGuru.class.getSimpleName());
        Mockito.when(network.getDns1()).thenReturn(ip4Dns[0]);
        Mockito.when(network.getDns2()).thenReturn(ip4Dns[1]);
        Mockito.when(network.getIp6Dns1()).thenReturn(ip6Dns[0]);
        Mockito.when(network.getIp6Dns2()).thenReturn(ip6Dns[1]);
        Mockito.when(testOrchastrator._networkModel.getNetworkRate(networkId, vmId)).thenReturn(networkRate);
        NicVO nicVO = Mockito.mock(NicVO.class);
        Mockito.when(nicVO.isDefaultNic()).thenReturn(isDefaultNic);
        Mockito.when(testOrchastrator._nicDao.findById(nicId)).thenReturn(nicVO);
        Mockito.when(testOrchastrator._nicDao.update(nicId, nicVO)).thenReturn(true);
        Mockito.when(testOrchastrator._networkModel.isSecurityGroupSupportedInNetwork(network)).thenReturn(false);
        Mockito.when(testOrchastrator._networkModel.getNetworkTag(hypervisorType, network)).thenReturn(null);
        Mockito.when(testOrchastrator._ntwkSrvcDao.getDistinctProviders(networkId)).thenReturn(new ArrayList<>());
        testOrchastrator.networkElements = new ArrayList<>();
        Mockito.when(testOrchastrator._nicExtraDhcpOptionDao.listByNicId(nicId)).thenReturn(new ArrayList<>());
        Mockito.when(testOrchastrator._ntwkSrvcDao.areServicesSupportedInNetwork(networkId, Service.Dhcp)).thenReturn(false);
        VirtualMachineProfile virtualMachineProfile = Mockito.mock(VirtualMachineProfile.class);
        Mockito.when(virtualMachineProfile.getType()).thenReturn(vmType);
        Mockito.when(virtualMachineProfile.getId()).thenReturn(vmId);
        DeployDestination deployDestination = Mockito.mock(DeployDestination.class);
        ReservationContext reservationContext = Mockito.mock(ReservationContext.class);
        Mockito.doAnswer((org.mockito.stubbing.Answer<Void>) invocation -> {
            NicProfile nicProfile = (NicProfile) invocation.getArguments()[0];
            Network ntwk = (Network) invocation.getArguments()[1];
            nicProfile.setIPv4Dns1(ntwk.getDns1());
            nicProfile.setIPv4Dns2(ntwk.getDns2());
            nicProfile.setIPv6Dns1(ntwk.getIp6Dns1());
            nicProfile.setIPv6Dns2(ntwk.getIp6Dns2());
            return null;
        }).when(guru).updateNicProfile(Mockito.any(NicProfile.class), Mockito.any(Network.class));
        DomainRouterVO routerVO = Mockito.mock(DomainRouterVO.class);
        if (isVpcRouter) {
            Long vpcId = 1L;
            Mockito.when(routerVO.getVpcId()).thenReturn(vpcId);
            VpcVO vpcVO = Mockito.mock(VpcVO.class);
            if (routerResourceHasCustomDns) {
                Mockito.when(vpcVO.getIp4Dns1()).thenReturn(ip4AltDns[0]);
                Mockito.when(vpcVO.getIp4Dns2()).thenReturn(ip4AltDns[1]);
                Mockito.when(vpcVO.getIp6Dns1()).thenReturn(ip6AltDns[0]);
                Mockito.when(vpcVO.getIp6Dns2()).thenReturn(ip6AltDns[1]);
            } else {
                Mockito.when(vpcVO.getIp4Dns1()).thenReturn(null);
                Mockito.when(vpcVO.getIp6Dns1()).thenReturn(null);
            }
            Mockito.when(testOrchastrator._vpcMgr.getActiveVpc(vpcId)).thenReturn(vpcVO);
        } else {
            Mockito.when(routerVO.getVpcId()).thenReturn(null);
            Long routerNetworkId = 2L;
            NetworkVO routerNetworkVO = Mockito.mock(NetworkVO.class);
            if (routerResourceHasCustomDns) {
                Mockito.when(routerNetworkVO.getDns1()).thenReturn(ip4AltDns[0]);
                Mockito.when(routerNetworkVO.getDns2()).thenReturn(ip4AltDns[1]);
                Mockito.when(routerNetworkVO.getIp6Dns1()).thenReturn(ip6AltDns[0]);
                Mockito.when(routerNetworkVO.getIp6Dns2()).thenReturn(ip6AltDns[1]);
            } else {
                Mockito.when(routerNetworkVO.getDns1()).thenReturn(null);
                Mockito.when(routerNetworkVO.getIp6Dns1()).thenReturn(null);
            }
            Mockito.when(testOrchastrator.routerNetworkDao.getRouterNetworks(vmId)).thenReturn(List.of(routerNetworkId));
            Mockito.when(testOrchastrator._networksDao.findById(routerNetworkId)).thenReturn(routerNetworkVO);
        }
        Mockito.when(testOrchastrator.routerDao.findById(vmId)).thenReturn(routerVO);
        NicProfile profile = null;
        try {
            profile = testOrchastrator.prepareNic(virtualMachineProfile, deployDestination, reservationContext, nicId, network);
        } catch (InsufficientCapacityException | ResourceUnavailableException e) {
            Assert.fail(String.format("Failure with exception %s", e.getMessage()));
        }
        return profile;
    }

    @Test
    public void testPrepareNicUserVm() {
        NicProfile profile = prepareMocksAndRunPrepareNic(Type.User, false, false, false);
        Assert.assertNotNull(profile);
        Assert.assertEquals(ip4Dns[0], profile.getIPv4Dns1());
        Assert.assertEquals(ip4Dns[1], profile.getIPv4Dns2());
        Assert.assertEquals(ip6Dns[0], profile.getIPv6Dns1());
        Assert.assertEquals(ip6Dns[1], profile.getIPv6Dns2());
    }

    @Test
    public void testPrepareNicVpcRouterVm() {
        NicProfile profile = prepareMocksAndRunPrepareNic(Type.DomainRouter, true, true, true);
        Assert.assertNotNull(profile);
        Assert.assertEquals(ip4AltDns[0], profile.getIPv4Dns1());
        Assert.assertEquals(ip4AltDns[1], profile.getIPv4Dns2());
        Assert.assertEquals(ip6AltDns[0], profile.getIPv6Dns1());
        Assert.assertEquals(ip6AltDns[1], profile.getIPv6Dns2());
    }

    @Test
    public void testPrepareNicVpcRouterNoDnsVm() {
        NicProfile profile = prepareMocksAndRunPrepareNic(Type.DomainRouter, true, true, false);
        Assert.assertNotNull(profile);
        Assert.assertEquals(ip4Dns[0], profile.getIPv4Dns1());
        Assert.assertEquals(ip4Dns[1], profile.getIPv4Dns2());
        Assert.assertEquals(ip6Dns[0], profile.getIPv6Dns1());
        Assert.assertEquals(ip6Dns[1], profile.getIPv6Dns2());
    }

    @Test
    public void testPrepareNicNetworkRouterVm() {
        NicProfile profile = prepareMocksAndRunPrepareNic(Type.DomainRouter, true, false, true);
        Assert.assertNotNull(profile);
        Assert.assertEquals(ip4AltDns[0], profile.getIPv4Dns1());
        Assert.assertEquals(ip4AltDns[1], profile.getIPv4Dns2());
        Assert.assertEquals(ip6AltDns[0], profile.getIPv6Dns1());
        Assert.assertEquals(ip6AltDns[1], profile.getIPv6Dns2());
    }

    @Test
    public void testPrepareNicNetworkRouterNoDnsVm() {
        NicProfile profile = prepareMocksAndRunPrepareNic(Type.DomainRouter, true, false, false);
        Assert.assertNotNull(profile);
        Assert.assertEquals(ip4Dns[0], profile.getIPv4Dns1());
        Assert.assertEquals(ip4Dns[1], profile.getIPv4Dns2());
        Assert.assertEquals(ip6Dns[0], profile.getIPv6Dns1());
        Assert.assertEquals(ip6Dns[1], profile.getIPv6Dns2());
    }
}
