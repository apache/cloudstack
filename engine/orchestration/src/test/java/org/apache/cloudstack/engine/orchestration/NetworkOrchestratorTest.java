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

import static org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService.NetworkLockTimeout;
import static org.mockito.ArgumentMatchers.any;
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

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.utils.Pair;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.cloud.api.query.dao.DomainRouterJoinDao;
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
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
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

    NetworkOrchestrator testOrchestrator = Mockito.spy(new NetworkOrchestrator());

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
        testOrchestrator._nicDao = mock(NicDao.class);
        testOrchestrator._networksDao = mock(NetworkDao.class);
        testOrchestrator._networkModel = mock(NetworkModel.class);
        testOrchestrator._nicSecondaryIpDao = mock(NicSecondaryIpDao.class);
        testOrchestrator._ntwkSrvcDao = mock(NetworkServiceMapDao.class);
        testOrchestrator._nicIpAliasDao = mock(NicIpAliasDao.class);
        testOrchestrator._ipAddressDao = mock(IPAddressDao.class);
        testOrchestrator._vlanDao = mock(VlanDao.class);
        testOrchestrator._networkModel = mock(NetworkModel.class);
        testOrchestrator._nicExtraDhcpOptionDao = mock(NicExtraDhcpOptionDao.class);
        testOrchestrator.routerDao = mock(DomainRouterDao.class);
        testOrchestrator.routerNetworkDao = mock(RouterNetworkDao.class);
        testOrchestrator._vpcMgr = mock(VpcManager.class);
        testOrchestrator.routerJoinDao = mock(DomainRouterJoinDao.class);
        testOrchestrator._ipAddrMgr = mock(IpAddressManager.class);
        testOrchestrator._entityMgr = mock(EntityManager.class);
        DhcpServiceProvider provider = mock(DhcpServiceProvider.class);

        Map<Network.Capability, String> capabilities = new HashMap<Network.Capability, String>();
        Map<Network.Service, Map<Network.Capability, String>> services = new HashMap<Network.Service, Map<Network.Capability, String>>();
        services.put(Network.Service.Dhcp, capabilities);
        when(provider.getCapabilities()).thenReturn(services);
        capabilities.put(Network.Capability.DhcpAccrossMultipleSubnets, "true");

        when(testOrchestrator._ntwkSrvcDao.getProviderForServiceInNetwork(ArgumentMatchers.anyLong(), ArgumentMatchers.eq(Service.Dhcp))).thenReturn(dhcpProvider);
        when(testOrchestrator._networkModel.getElementImplementingProvider(dhcpProvider)).thenReturn(provider);

        when(guru.getName()).thenReturn(guruName);
        List<NetworkGuru> networkGurus = new ArrayList<NetworkGuru>();
        networkGurus.add(guru);
        testOrchestrator.networkGurus = networkGurus;

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
        when(testOrchestrator._networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)).thenReturn(true);
        when(network.getTrafficType()).thenReturn(TrafficType.Guest);
        when(network.getGuestType()).thenReturn(GuestType.Shared);
        when(testOrchestrator._nicDao.listByNetworkIdTypeAndGatewayAndBroadcastUri(nic.getNetworkId(), VirtualMachine.Type.User, nic.getIPv4Gateway(), nic.getBroadcastUri()))
                .thenReturn(new ArrayList<NicVO>());

        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchestrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchestrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchestrator._networkModel, times(2)).getElementImplementingProvider(dhcpProvider);
        verify(testOrchestrator._ntwkSrvcDao, times(2)).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchestrator._networksDao, times(2)).findById(nic.getNetworkId());
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
        when(testOrchestrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchestrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchestrator._networkModel, never()).getElementImplementingProvider(dhcpProvider);
        verify(testOrchestrator._ntwkSrvcDao, never()).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchestrator._networksDao, times(1)).findById(nic.getNetworkId());
    }
    @Test
    public void testDontRemoveDhcpServiceWhenNotProvided() {
        // make local mocks
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        // make sure that release dhcp will *not* be called
        when(vm.getType()).thenReturn(Type.User);
        when(testOrchestrator._networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp)).thenReturn(false);

        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchestrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        testOrchestrator.removeNic(vm, nic);

        verify(nic, times(1)).setState(Nic.State.Deallocating);
        verify(testOrchestrator._networkModel, never()).getElementImplementingProvider(dhcpProvider);
        verify(testOrchestrator._ntwkSrvcDao, never()).getProviderForServiceInNetwork(network.getId(), Service.Dhcp);
        verify(testOrchestrator._networksDao, times(1)).findById(nic.getNetworkId());
    }

    @Test
    public void testCheckL2OfferingServicesEmptyServices() {
        when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(new ArrayList<>());
        when(testOrchestrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(false);
        testOrchestrator.checkL2OfferingServices(networkOffering);
    }

    @Test
    public void testCheckL2OfferingServicesUserDataOnly() {
        when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.UserData));
        when(testOrchestrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(true);
        testOrchestrator.checkL2OfferingServices(networkOffering);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckL2OfferingServicesMultipleServicesIncludingUserData() {
        when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.UserData, Service.Dhcp));
        when(testOrchestrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(true);
        testOrchestrator.checkL2OfferingServices(networkOffering);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testCheckL2OfferingServicesMultipleServicesNotIncludingUserData() {
        when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.Dns, Service.Dhcp));
        when(testOrchestrator._networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.UserData)).thenReturn(false);
        testOrchestrator.checkL2OfferingServices(networkOffering);
    }

    @Test
    public void testConfigureNicProfileBasedOnRequestedIpTestMacNull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, null, "192.168.100.150");

        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

        verifyAndAssert("192.168.100.150", "192.168.100.1", "255.255.255.0", nicProfile, 1, 1);
    }

    @Test
    public void testConfigureNicProfileBasedOnRequestedIpTestNicProfileMacNotNull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");

        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

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
        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);

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
        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
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
        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
        verifyAndAssert(null, null, null, nicProfile, 1, 0);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testConfigureNicProfileBasedOnRequestedIpTestIPAddressVONull() {
        Network network = mock(Network.class);
        NicProfile requestedNicProfile = new NicProfile();
        NicProfile nicProfile = Mockito.spy(new NicProfile());

        configureTestConfigureNicProfileBasedOnRequestedIpTests(nicProfile, 0l, false, IPAddressVO.State.Free, "192.168.100.1", "255.255.255.0", "00-88-14-4D-4C-FB",
                requestedNicProfile, "00-88-14-4D-4C-FB", "192.168.100.150");
        when(testOrchestrator._vlanDao.findByNetworkIdAndIpv4(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);

        testOrchestrator.configureNicProfileBasedOnRequestedIp(requestedNicProfile, nicProfile, network);
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
            when(testOrchestrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        } else {
            when(testOrchestrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        }

        VlanVO vlanSpy = Mockito.spy(new VlanVO(Vlan.VlanType.DirectAttached, "vlanTag", vlanGateway, vlanNetmask, 0l, "192.168.100.100 - 192.168.100.200", 0l, new Long(0l),
                "ip6Gateway", "ip6Cidr", "ip6Range"));

        Mockito.doReturn(0l).when(vlanSpy).getId();
        when(testOrchestrator._vlanDao.findByNetworkIdAndIpv4(Mockito.anyLong(), Mockito.anyString())).thenReturn(vlanSpy);
        when(testOrchestrator._ipAddressDao.acquireInLockTable(Mockito.anyLong())).thenReturn(ipVoSpy);
        when(testOrchestrator._ipAddressDao.update(Mockito.anyLong(), Mockito.any(IPAddressVO.class))).thenReturn(true);
        when(testOrchestrator._ipAddressDao.releaseFromLockTable(Mockito.anyLong())).thenReturn(true);
        try {
            when(testOrchestrator._networkModel.getNextAvailableMacAddressInNetwork(Mockito.anyLong())).thenReturn(macAddress);
        } catch (InsufficientAddressCapacityException e) {
            e.printStackTrace();
        }
    }

    private void verifyAndAssert(String requestedIpv4Address, String ipv4Gateway, String ipv4Netmask, NicProfile nicProfile, int acquireLockAndCheckIfIpv4IsFreeTimes,
            int nextMacAddressTimes) {
        verify(testOrchestrator, times(acquireLockAndCheckIfIpv4IsFreeTimes)).acquireLockAndCheckIfIpv4IsFree(Mockito.any(Network.class), Mockito.anyString());
        try {
            verify(testOrchestrator._networkModel, times(nextMacAddressTimes)).getNextAvailableMacAddressInNetwork(Mockito.anyLong());
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
            when(testOrchestrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(null);
        } else {
            when(testOrchestrator._ipAddressDao.findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString())).thenReturn(ipVoSpy);
        }
        when(testOrchestrator._ipAddressDao.acquireInLockTable(Mockito.anyLong())).thenReturn(ipVoSpy);
        when(testOrchestrator._ipAddressDao.releaseFromLockTable(Mockito.anyLong())).thenReturn(true);
        when(testOrchestrator._ipAddressDao.update(Mockito.anyLong(), Mockito.any(IPAddressVO.class))).thenReturn(true);

        testOrchestrator.acquireLockAndCheckIfIpv4IsFree(network, "192.168.100.150");

        verify(testOrchestrator._ipAddressDao, Mockito.times(findByIpTimes)).findByIpAndSourceNetworkId(Mockito.anyLong(), Mockito.anyString());
        verify(testOrchestrator._ipAddressDao, Mockito.times(acquireLockTimes)).acquireInLockTable(Mockito.anyLong());
        verify(testOrchestrator._ipAddressDao, Mockito.times(releaseFromLockTimes)).releaseFromLockTable(Mockito.anyLong());
        verify(testOrchestrator._ipAddressDao, Mockito.times(updateTimes)).update(Mockito.anyLong(), Mockito.any(IPAddressVO.class));
        verify(testOrchestrator, Mockito.times(validateTimes)).validateLockedRequestedIp(Mockito.any(IPAddressVO.class), Mockito.any(IPAddressVO.class));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateLockedRequestedIpTestNullLockedIp() {
        IPAddressVO ipVoSpy = Mockito.spy(new IPAddressVO(new Ip("192.168.100.100"), 0l, 0l, 0l, true));
        testOrchestrator.validateLockedRequestedIp(ipVoSpy, null);
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
                testOrchestrator.validateLockedRequestedIp(ipVoSpy, lockedIp);
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
        testOrchestrator.validateLockedRequestedIp(ipVoSpy, lockedIp);
    }

    @Test
    public void testDontReleaseNicWhenPreserveNicsSettingEnabled() {
        VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
        NicVO nic = mock(NicVO.class);
        NetworkVO network = mock(NetworkVO.class);

        when(vm.getType()).thenReturn(Type.User);
        when(network.getGuruName()).thenReturn(guruName);
        when(testOrchestrator._networksDao.findById(nic.getNetworkId())).thenReturn(network);

        Long nicId = 1L;
        when(nic.getId()).thenReturn(nicId);
        when(vm.getParameter(VirtualMachineProfile.Param.PreserveNics)).thenReturn(true);

        testOrchestrator.removeNic(vm, nic);

        verify(nic, never()).setState(Nic.State.Deallocating);
        verify(testOrchestrator._nicDao, never()).remove(nicId);
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
        URI resultUri = testOrchestrator.encodeVlanIdIntoBroadcastUri("vxlan://123", null);
    }

    private void encodeVlanIdIntoBroadcastUriPrepareAndTest(String vlanId, String isolationMethod, String expectedIsolation, String expectedUri) {
        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO();
        List<String> isolationMethods = new ArrayList<>();
        isolationMethods.add(isolationMethod);
        physicalNetwork.setIsolationMethods(isolationMethods);

        URI resultUri = testOrchestrator.encodeVlanIdIntoBroadcastUri(vlanId, physicalNetwork);

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
        Mockito.when(testOrchestrator._networkModel.getNetworkRate(networkId, vmId)).thenReturn(networkRate);
        NicVO nicVO = Mockito.mock(NicVO.class);
        Mockito.when(nicVO.isDefaultNic()).thenReturn(isDefaultNic);
        Mockito.when(testOrchestrator._nicDao.findById(nicId)).thenReturn(nicVO);
        Mockito.when(testOrchestrator._nicDao.update(nicId, nicVO)).thenReturn(true);
        Mockito.when(testOrchestrator._networkModel.isSecurityGroupSupportedInNetwork(network)).thenReturn(false);
        Mockito.when(testOrchestrator._networkModel.getNetworkTag(hypervisorType, network)).thenReturn(null);
        Mockito.when(testOrchestrator._ntwkSrvcDao.getDistinctProviders(networkId)).thenReturn(new ArrayList<>());
        testOrchestrator.networkElements = new ArrayList<>();
        Mockito.when(testOrchestrator._nicExtraDhcpOptionDao.listByNicId(nicId)).thenReturn(new ArrayList<>());
        Mockito.when(testOrchestrator._ntwkSrvcDao.areServicesSupportedInNetwork(networkId, Service.Dhcp)).thenReturn(false);
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
            Mockito.when(testOrchestrator._vpcMgr.getActiveVpc(vpcId)).thenReturn(vpcVO);
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
            Mockito.when(testOrchestrator.routerNetworkDao.getRouterNetworks(vmId)).thenReturn(List.of(routerNetworkId));
            Mockito.when(testOrchestrator._networksDao.findById(routerNetworkId)).thenReturn(routerNetworkVO);
        }
        Mockito.when(testOrchestrator.routerDao.findById(vmId)).thenReturn(routerVO);
        NicProfile profile = null;
        try {
            profile = testOrchestrator.prepareNic(virtualMachineProfile, deployDestination, reservationContext, nicId, network);
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

    @Test
    public void testGetNetworkGatewayAndNetmaskForNicImportAdvancedZone() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        String ipAddress = "10.1.1.10";

        String networkGateway = "10.1.1.1";
        String networkNetmask = "255.255.255.0";
        String networkCidr = "10.1.1.0/24";
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        Mockito.when(network.getGateway()).thenReturn(networkGateway);
        Mockito.when(network.getCidr()).thenReturn(networkCidr);
        Pair<String, String> pair = testOrchestrator.getNetworkGatewayAndNetmaskForNicImport(network, dataCenter, ipAddress);
        Assert.assertNotNull(pair);
        Assert.assertEquals(networkGateway, pair.first());
        Assert.assertEquals(networkNetmask, pair.second());
    }

    @Test
    public void testGetNetworkGatewayAndNetmaskForNicImportBasicZone() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        String ipAddress = "172.1.1.10";

        String defaultNetworkGateway = "172.1.1.1";
        String defaultNetworkNetmask = "255.255.255.0";
        VlanVO vlan = Mockito.mock(VlanVO.class);
        Mockito.when(vlan.getVlanGateway()).thenReturn(defaultNetworkGateway);
        Mockito.when(vlan.getVlanNetmask()).thenReturn(defaultNetworkNetmask);
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        Mockito.when(ipAddressVO.getVlanId()).thenReturn(1L);
        Mockito.when(testOrchestrator._vlanDao.findById(1L)).thenReturn(vlan);
        Mockito.when(testOrchestrator._ipAddressDao.findByIp(ipAddress)).thenReturn(ipAddressVO);
        Pair<String, String> pair = testOrchestrator.getNetworkGatewayAndNetmaskForNicImport(network, dataCenter, ipAddress);
        Assert.assertNotNull(pair);
        Assert.assertEquals(defaultNetworkGateway, pair.first());
        Assert.assertEquals(defaultNetworkNetmask, pair.second());
    }

    @Test
    public void testGetGuestIpForNicImportL2Network() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.L2);
        Assert.assertNull(testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses));
    }

    @Test
    public void testGetGuestIpForNicImportAdvancedZone() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Isolated);
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);
        String ipAddress = "10.1.10.10";
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(ipAddress);
        Mockito.when(testOrchestrator._ipAddrMgr.acquireGuestIpAddress(network, ipAddress)).thenReturn(ipAddress);
        String guestIp = testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses);
        Assert.assertEquals(ipAddress, guestIp);
    }

    @Test
    public void testGetGuestIpForNicImportBasicZoneAutomaticIP() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Shared);
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        long networkId = 1L;
        long dataCenterId = 1L;
        String freeIp = "172.10.10.10";
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        Ip ip = mock(Ip.class);
        Mockito.when(ip.addr()).thenReturn(freeIp);
        Mockito.when(ipAddressVO.getAddress()).thenReturn(ip);
        Mockito.when(ipAddressVO.getState()).thenReturn(State.Free);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(dataCenter.getId()).thenReturn(dataCenterId);
        Mockito.when(testOrchestrator._ipAddressDao.findBySourceNetworkIdAndDatacenterIdAndState(networkId, dataCenterId, State.Free)).thenReturn(ipAddressVO);
        String ipAddress = testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses);
        Assert.assertEquals(freeIp, ipAddress);
    }

    @Test
    public void testGetGuestIpForNicImportBasicZoneManualIP() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Shared);
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        long networkId = 1L;
        long dataCenterId = 1L;
        String requestedIp = "172.10.10.10";
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        Ip ip = mock(Ip.class);
        Mockito.when(ip.addr()).thenReturn(requestedIp);
        Mockito.when(ipAddressVO.getAddress()).thenReturn(ip);
        Mockito.when(ipAddressVO.getState()).thenReturn(State.Free);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(dataCenter.getId()).thenReturn(dataCenterId);
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(requestedIp);
        Mockito.when(testOrchestrator._ipAddressDao.findByIp(requestedIp)).thenReturn(ipAddressVO);
        String ipAddress = testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses);
        Assert.assertEquals(requestedIp, ipAddress);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetGuestIpForNicImportBasicUsedIP() {
        Network network = Mockito.mock(Network.class);
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Shared);
        Mockito.when(dataCenter.getNetworkType()).thenReturn(DataCenter.NetworkType.Basic);
        long networkId = 1L;
        long dataCenterId = 1L;
        String requestedIp = "172.10.10.10";
        IPAddressVO ipAddressVO = Mockito.mock(IPAddressVO.class);
        Ip ip = mock(Ip.class);
        Mockito.when(ip.addr()).thenReturn(requestedIp);
        Mockito.when(ipAddressVO.getAddress()).thenReturn(ip);
        Mockito.when(ipAddressVO.getState()).thenReturn(State.Allocated);
        Mockito.when(network.getId()).thenReturn(networkId);
        Mockito.when(dataCenter.getId()).thenReturn(dataCenterId);
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(requestedIp);
        Mockito.when(testOrchestrator._ipAddressDao.findByIp(requestedIp)).thenReturn(ipAddressVO);
        testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses);
    }

    @Test
    public void testShutdownNetworkAcquireLockFailed() {
        ReservationContext reservationContext = Mockito.mock(ReservationContext.class);
        NetworkVO network = mock(NetworkVO.class);
        long networkId = 1;
        when(testOrchestrator._networksDao.acquireInLockTable(Mockito.anyLong(), Mockito.anyInt())).thenReturn(null);

        boolean shutdownNetworkStatus = testOrchestrator.shutdownNetwork(networkId, reservationContext, false);
        Assert.assertFalse(shutdownNetworkStatus);

        verify(testOrchestrator._networksDao, times(1)).acquireInLockTable(networkId, NetworkLockTimeout.value());
    }

    @Test
    public void testShutdownNetworkInAllocatedState() {
        ReservationContext reservationContext = Mockito.mock(ReservationContext.class);
        NetworkVO network = mock(NetworkVO.class);
        long networkId = 1;
        when(testOrchestrator._networksDao.acquireInLockTable(Mockito.anyLong(), Mockito.anyInt())).thenReturn(network);
        when(network.getId()).thenReturn(networkId);
        when(network.getState()).thenReturn(Network.State.Allocated);

        boolean shutdownNetworkStatus = testOrchestrator.shutdownNetwork(networkId, reservationContext, false);
        Assert.assertTrue(shutdownNetworkStatus);

        verify(network, times(1)).getState();
        verify(testOrchestrator._networksDao, times(1)).acquireInLockTable(networkId, NetworkLockTimeout.value());
        verify(testOrchestrator._networksDao, times(1)).releaseFromLockTable(networkId);
    }

    @Test
    public void testShutdownNetworkInImplementingState() {
        ReservationContext reservationContext = Mockito.mock(ReservationContext.class);
        NetworkVO network = mock(NetworkVO.class);
        long networkId = 1;
        when(testOrchestrator._networksDao.acquireInLockTable(Mockito.anyLong(), Mockito.anyInt())).thenReturn(network);
        when(network.getId()).thenReturn(networkId);
        when(network.getState()).thenReturn(Network.State.Implementing);

        boolean shutdownNetworkStatus = testOrchestrator.shutdownNetwork(networkId, reservationContext, false);
        Assert.assertFalse(shutdownNetworkStatus);

        verify(network, times(3)).getState();
        verify(testOrchestrator._networksDao, times(1)).acquireInLockTable(networkId, NetworkLockTimeout.value());
        verify(testOrchestrator._networksDao, times(1)).releaseFromLockTable(networkId);
    }

    @Test(expected = InsufficientVirtualNetworkCapacityException.class)
    public void testImportNicAcquireGuestIPFailed() throws Exception {
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Isolated);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        long dataCenterId = 1L;
        Mockito.when(network.getDataCenterId()).thenReturn(dataCenterId);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        String ipAddress = "10.1.10.10";
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(ipAddress);
        Mockito.when(testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses)).thenReturn(null);
        Mockito.when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.Dns, Service.Dhcp));
        String macAddress = "02:01:01:82:00:01";
        int deviceId = 0;
        testOrchestrator.importNic(macAddress, deviceId, network, true, vm, ipAddresses, dataCenter, false);
    }

    @Test(expected = InsufficientVirtualNetworkCapacityException.class)
    public void testImportNicAutoAcquireGuestIPFailed() throws Exception {
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        VirtualMachine vm = mock(VirtualMachine.class);
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getGuestType()).thenReturn(GuestType.Isolated);
        Mockito.when(network.getNetworkOfferingId()).thenReturn(networkOfferingId);
        long dataCenterId = 1L;
        Mockito.when(network.getDataCenterId()).thenReturn(dataCenterId);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        String ipAddress = "auto";
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(ipAddress);
        Mockito.when(testOrchestrator.getSelectedIpForNicImport(network, dataCenter, ipAddresses)).thenReturn(null);
        Mockito.when(testOrchestrator._networkModel.listNetworkOfferingServices(networkOfferingId)).thenReturn(Arrays.asList(Service.Dns, Service.Dhcp));
        String macAddress = "02:01:01:82:00:01";
        int deviceId = 0;
        testOrchestrator.importNic(macAddress, deviceId, network, true, vm, ipAddresses, dataCenter, false);
    }

    @Test
    public void testImportNicNoIP4Address() throws Exception {
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Long vmId = 1L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        VirtualMachine vm = mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getHypervisorType()).thenReturn(hypervisorType);
        Long networkId = 1L;
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(null);
        URI broadcastUri = URI.create("vlan://123");
        NicVO nic = mock(NicVO.class);
        Mockito.when(nic.getBroadcastUri()).thenReturn(broadcastUri);
        String macAddress = "02:01:01:82:00:01";
        int deviceId = 1;
        Integer networkRate = 200;
        Mockito.when(testOrchestrator._networkModel.getNetworkRate(networkId, vmId)).thenReturn(networkRate);
        Mockito.when(testOrchestrator._networkModel.isSecurityGroupSupportedInNetwork(network)).thenReturn(false);
        Mockito.when(testOrchestrator._networkModel.getNetworkTag(hypervisorType, network)).thenReturn("testtag");
        try (MockedStatic<Transaction> transactionMocked = Mockito.mockStatic(Transaction.class)) {
            transactionMocked.when(() -> Transaction.execute(any(TransactionCallback.class))).thenReturn(nic);
            Pair<NicProfile, Integer> nicProfileIntegerPair = testOrchestrator.importNic(macAddress, deviceId, network, true, vm, ipAddresses, dataCenter, false);
            verify(testOrchestrator._networkModel, times(1)).getNetworkRate(networkId, vmId);
            verify(testOrchestrator._networkModel, times(1)).isSecurityGroupSupportedInNetwork(network);
            verify(testOrchestrator._networkModel, times(1)).getNetworkTag(Hypervisor.HypervisorType.KVM, network);
            assertEquals(deviceId, nicProfileIntegerPair.second().intValue());
            NicProfile nicProfile = nicProfileIntegerPair.first();
            assertEquals(broadcastUri, nicProfile.getBroadCastUri());
            assertEquals(networkRate, nicProfile.getNetworkRate());
            assertFalse(nicProfile.isSecurityGroupEnabled());
            assertEquals("testtag", nicProfile.getName());
        }
    }

    @Test
    public void testImportNicWithIP4Address() throws Exception {
        DataCenter dataCenter = Mockito.mock(DataCenter.class);
        Long vmId = 1L;
        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.KVM;
        VirtualMachine vm = mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(vmId);
        Mockito.when(vm.getHypervisorType()).thenReturn(hypervisorType);
        Long networkId = 1L;
        Network network = Mockito.mock(Network.class);
        Mockito.when(network.getId()).thenReturn(networkId);
        String ipAddress = "10.1.10.10";
        Network.IpAddresses ipAddresses = Mockito.mock(Network.IpAddresses.class);
        Mockito.when(ipAddresses.getIp4Address()).thenReturn(ipAddress);
        URI broadcastUri = URI.create("vlan://123");
        NicVO nic = mock(NicVO.class);
        Mockito.when(nic.getBroadcastUri()).thenReturn(broadcastUri);
        String macAddress = "02:01:01:82:00:01";
        int deviceId = 1;
        Integer networkRate = 200;
        Mockito.when(testOrchestrator._networkModel.getNetworkRate(networkId, vmId)).thenReturn(networkRate);
        Mockito.when(testOrchestrator._networkModel.isSecurityGroupSupportedInNetwork(network)).thenReturn(false);
        Mockito.when(testOrchestrator._networkModel.getNetworkTag(hypervisorType, network)).thenReturn("testtag");
        try (MockedStatic<Transaction> transactionMocked = Mockito.mockStatic(Transaction.class)) {
            transactionMocked.when(() -> Transaction.execute(any(TransactionCallback.class))).thenReturn(nic);
            Pair<NicProfile, Integer> nicProfileIntegerPair = testOrchestrator.importNic(macAddress, deviceId, network, true, vm, ipAddresses, dataCenter, false);
            verify(testOrchestrator, times(1)).getSelectedIpForNicImport(network, dataCenter, ipAddresses);
            verify(testOrchestrator._networkModel, times(1)).getNetworkRate(networkId, vmId);
            verify(testOrchestrator._networkModel, times(1)).isSecurityGroupSupportedInNetwork(network);
            verify(testOrchestrator._networkModel, times(1)).getNetworkTag(Hypervisor.HypervisorType.KVM, network);
            assertEquals(deviceId, nicProfileIntegerPair.second().intValue());
            NicProfile nicProfile = nicProfileIntegerPair.first();
            assertEquals(broadcastUri, nicProfile.getBroadCastUri());
            assertEquals(networkRate, nicProfile.getNetworkRate());
            assertFalse(nicProfile.isSecurityGroupEnabled());
            assertEquals("testtag", nicProfile.getName());
        }
    }
}
