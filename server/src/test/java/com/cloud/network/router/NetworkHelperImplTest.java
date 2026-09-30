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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.network.router.deployment.RouterDeploymentDefinition;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkModel;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

import java.util.List;
import java.util.Map;


@RunWith(MockitoJUnitRunner.class)
public class NetworkHelperImplTest {

    private static final long HOST_ID = 10L;

    @Mock
    protected AgentManager agentManager;

    @Mock
    DomainRouterDao routerDao;

    @Spy
    @InjectMocks
    protected NetworkHelperImpl networkHelperSpy = new NetworkHelperImpl();

    @Mock
    NetworkOrchestrationService networkOrchestrationService;

    @Mock
    NetworkDao networkDao;

    @Mock
    NetworkModel networkModel;

    @Mock
    private NicDao nicDaoMock;

    @Mock
    private RouterDeploymentDefinition routerDeploymentDefinition;

    @Mock
    private VirtualRouterProvider virtualProvider;

    @Mock
    private Account owner;

    @Mock
    private ServiceOfferingVO routerOffering;

    @Mock
    private VMTemplateVO template;

    @Mock
    private PublicIp publicIpMock;

    @Mock
    private RouterDeploymentDefinition routerDeploymentDefinitionMock;

    @Mock
    private Ipv6Service ipv6ServiceMock;

    @Mock
    private NicVO nicVoMock;

    @Mock
    private Network networkMock;

    private NicProfile nicProfile = new NicProfile();

    @Before
    public void setUp() {
        networkHelperSpy._networkDao = networkDao;
        networkHelperSpy._networkModel = networkModel;

        when(template.getId()).thenReturn(1L);
        when(template.isDynamicallyScalable()).thenReturn(true);
        when(virtualProvider.getId()).thenReturn(1L);
        when(routerDeploymentDefinition.getVirtualProvider()).thenReturn(virtualProvider);
        when(routerDeploymentDefinition.isRedundant()).thenReturn(true);
        when(routerOffering.getLimitCpuUse()).thenReturn(true);
    }

    @Test(expected=ResourceUnavailableException.class)
    public void testSendCommandsToRouterWrongRouterVersion()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = networkHelperSpy;
        VirtualRouter vr = mock(VirtualRouter.class);
        doReturn(false).when(nwHelperUT).checkRouterVersion(vr);

        // Execute
        nwHelperUT.sendCommandsToRouter(vr, null);

        // Assert
        verify(this.agentManager, times(0)).send((Long) any(), (Command) any());
    }

    @Test
    public void testSendCommandsToRouter()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = networkHelperSpy;
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(false);
        lenient().when(answer3.getResult()).thenReturn(false);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(0)).getResult();
        assertFalse(result);
    }

    /**
     * The only way result can be true is if each and every command receive a true result
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithTrueResult()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = networkHelperSpy;
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        Answer answer3 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2, answer3};
        when(answer1.getResult()).thenReturn(true);
        when(answer2.getResult()).thenReturn(true);
        when(answer3.getResult()).thenReturn(true);
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(1)).getResult();
        verify(answer2, times(1)).getResult();
        verify(answer3, times(1)).getResult();
        assertTrue(result);
    }

    /**
     * If the number of answers is different to the number of commands the result is false
     *
     * @throws AgentUnavailableException
     * @throws OperationTimedoutException
     */
    @Test
    public void testSendCommandsToRouterWithNoAnswers()
            throws AgentUnavailableException, OperationTimedoutException, ResourceUnavailableException {
        // Prepare
        NetworkHelperImpl nwHelperUT = networkHelperSpy;
        VirtualRouter vr = mock(VirtualRouter.class);
        when(vr.getHostId()).thenReturn(HOST_ID);
        doReturn(true).when(nwHelperUT).checkRouterVersion(vr);

        Commands commands = mock(Commands.class);
        when(commands.size()).thenReturn(3);
        Answer answer1 = mock(Answer.class);
        Answer answer2 = mock(Answer.class);
        // In the second iteration it should match and return, without invoking the third
        Answer[] answers = {answer1, answer2};
        when(this.agentManager.send(HOST_ID, commands)).thenReturn(answers);

        // Execute
        final boolean result = nwHelperUT.sendCommandsToRouter(vr, commands);

        // Assert
        verify(this.agentManager, times(1)).send(HOST_ID, commands);
        verify(answer1, times(0)).getResult();
        assertFalse(result);
    }

    @Test
    public void testCreateDomainRouter_New() {
        long id = 1L;
        long userId = 800L;
        boolean offerHA = false;
        Long vpcId = 900L;
        when(routerDao.persist(any(DomainRouterVO.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DomainRouterVO result = networkHelperSpy.createOrUpdateDomainRouter(
                null, id, routerDeploymentDefinition, owner, userId, routerOffering, offerHA, vpcId, template);
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals(routerOffering.getId(), result.getServiceOfferingId());
        assertEquals(1L, result.getElementId());
        assertEquals(VirtualMachineName.getRouterName(id, NetworkHelperImpl.s_vmInstanceName), result.getInstanceName());
        assertEquals(template.getId(), result.getTemplateId());
        assertEquals(template.getHypervisorType(), result.getHypervisorType());
        assertEquals(template.getGuestOSId(), result.getGuestOSId());
        assertEquals(owner.getDomainId(), result.getDomainId());
        assertEquals(owner.getId(), result.getAccountId());
        assertEquals(userId, result.getUserId());
        assertTrue(result.getIsRedundantRouter());
        assertEquals(VirtualRouter.RedundantState.UNKNOWN, result.getRedundantState());
        assertEquals(offerHA, result.isHaEnabled());
        assertEquals(vpcId, result.getVpcId());
        assertTrue(result.isDynamicallyScalable());
        assertEquals(VirtualRouter.Role.VIRTUAL_ROUTER, result.getRole());
        assertTrue(result.limitCpuUse());
        verify(routerDao).persist(any(DomainRouterVO.class));
    }

    @Test
    public void testUpdateDomainRouter() {
        long id = 1L;
        long userId = 800L;
        boolean offerHA = false;
        Long vpcId = 900L;
        DomainRouterVO existing = new DomainRouterVO(id, routerOffering.getId(), virtualProvider.getId(),
                VirtualMachineName.getRouterName(id, NetworkHelperImpl.s_vmInstanceName), 999L, Hypervisor.HypervisorType.KVM, 888L,
                owner.getDomainId(), owner.getId(), userId, routerDeploymentDefinition.isRedundant(), VirtualRouter.RedundantState.UNKNOWN,
                offerHA, false, vpcId);
        existing.setDynamicallyScalable(false);
        DomainRouterVO result = networkHelperSpy.createOrUpdateDomainRouter(
                existing, id, routerDeploymentDefinition, owner, userId, routerOffering, offerHA, vpcId, template);
        verify(routerDao).update(existing.getId(), existing);
        assertEquals(template.getId(), result.getTemplateId());
        assertEquals(Hypervisor.HypervisorType.KVM, result.getHypervisorType());
        assertTrue(result.isDynamicallyScalable());
    }


    private NicProfile getExpectedNicProfile(boolean vxlan, String vlanTag) {
        NicProfile nic = new NicProfile();
        nic.setDefaultNic(true);
        nic.setIPv4Address("192.168.0.10");
        nic.setIPv4Gateway("192.168.0.1");
        nic.setIPv4Netmask("255.255.255.0");
        nic.setMacAddress("ff-ff-ff-ff-ff-ff");

        if (vxlan) {
            nic.setBroadcastType(Networks.BroadcastDomainType.Vxlan);
            nic.setBroadcastUri(Networks.BroadcastDomainType.Vxlan.toUri(vlanTag));
            nic.setIsolationUri(Networks.BroadcastDomainType.Vxlan.toUri(vlanTag));
        } else {
            nic.setBroadcastType(Networks.BroadcastDomainType.Vlan);
            nic.setBroadcastUri(vlanTag != null ? Networks.BroadcastDomainType.Vlan.toUri(vlanTag) : null);
            nic.setIsolationUri(vlanTag != null ? Networks.IsolationType.Vlan.toUri(vlanTag) : null);
        }

        return nic;
    }

    @Test
    public void configurePublicVrNicBasedOnSourceNatIpTestConfigureVxLanNic() {
        String vlanTag = "200";
        NicProfile expected = getExpectedNicProfile(true, vlanTag);

        Ip ipMock = Mockito.mock(Ip.class);
        NetworkVO publicNetworkMock = Mockito.mock(NetworkVO.class);
        long networkId = 1L;
        Mockito.when(publicIpMock.getAddress()).thenReturn(ipMock);
        Mockito.when(ipMock.addr()).thenReturn(expected.getIPv4Address());
        Mockito.when(publicIpMock.getGateway()).thenReturn(expected.getIPv4Gateway());
        Mockito.when(publicIpMock.getNetmask()).thenReturn(expected.getIPv4Netmask());
        Mockito.when(publicIpMock.getMacAddress()).thenReturn(expected.getMacAddress());
        Mockito.when(publicIpMock.getNetworkId()).thenReturn(networkId);
        Mockito.when(networkDao.findById(networkId)).thenReturn(publicNetworkMock);
        Mockito.when(publicNetworkMock.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Vxlan);
        Mockito.when(publicIpMock.getVlanTag()).thenReturn(vlanTag);

        networkHelperSpy.configurePublicVrNicBasedOnSourceNatIp(nicProfile, publicIpMock);

        Assert.assertTrue(nicProfile.isDefaultNic());
        Assert.assertEquals(expected.getIPv4Address(), nicProfile.getIPv4Address());
        Assert.assertEquals(expected.getIPv4Gateway(), nicProfile.getIPv4Gateway());
        Assert.assertEquals(expected.getIPv4Netmask(), nicProfile.getIPv4Netmask());
        Assert.assertEquals(expected.getMacAddress(), nicProfile.getMacAddress());
        Assert.assertEquals(expected.getBroadcastType(), nicProfile.getBroadcastType());
        Assert.assertEquals(expected.getBroadCastUri(), nicProfile.getBroadCastUri());
        Assert.assertEquals(expected.getIsolationUri(), nicProfile.getIsolationUri());
    }

    @Test
    public void configurePublicVrNicBasedOnSourceNatIpTestConfigureVlanNicWithVlanTag() {
        String vlanTag = "200";
        NicProfile expected = getExpectedNicProfile(false, vlanTag);

        Ip ipMock = Mockito.mock(Ip.class);
        NetworkVO publicNetworkMock = Mockito.mock(NetworkVO.class);
        long networkId = 1L;
        Mockito.when(publicIpMock.getAddress()).thenReturn(ipMock);
        Mockito.when(ipMock.addr()).thenReturn(expected.getIPv4Address());
        Mockito.when(publicIpMock.getGateway()).thenReturn(expected.getIPv4Gateway());
        Mockito.when(publicIpMock.getNetmask()).thenReturn(expected.getIPv4Netmask());
        Mockito.when(publicIpMock.getMacAddress()).thenReturn(expected.getMacAddress());
        Mockito.when(publicIpMock.getNetworkId()).thenReturn(networkId);
        Mockito.when(networkDao.findById(networkId)).thenReturn(publicNetworkMock);
        Mockito.when(publicNetworkMock.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Vlan);
        Mockito.when(publicIpMock.getVlanTag()).thenReturn(vlanTag);

        networkHelperSpy.configurePublicVrNicBasedOnSourceNatIp(nicProfile, publicIpMock);

        Assert.assertTrue(nicProfile.isDefaultNic());
        Assert.assertEquals(expected.getIPv4Address(), nicProfile.getIPv4Address());
        Assert.assertEquals(expected.getIPv4Gateway(), nicProfile.getIPv4Gateway());
        Assert.assertEquals(expected.getIPv4Netmask(), nicProfile.getIPv4Netmask());
        Assert.assertEquals(expected.getMacAddress(), nicProfile.getMacAddress());
        Assert.assertEquals(expected.getBroadcastType(), nicProfile.getBroadcastType());
        Assert.assertEquals(expected.getBroadCastUri(), nicProfile.getBroadCastUri());
        Assert.assertEquals(expected.getIsolationUri(), nicProfile.getIsolationUri());
    }

    @Test
    public void configurePublicVrNicBasedOnSourceNatIpTestConfigureVlanNicWithNoVlanTag() {
        String vlanTag = null;
        NicProfile expected = getExpectedNicProfile(false, vlanTag);

        Ip ipMock = Mockito.mock(Ip.class);
        NetworkVO publicNetworkMock = Mockito.mock(NetworkVO.class);
        long networkId = 1L;
        Mockito.when(publicIpMock.getAddress()).thenReturn(ipMock);
        Mockito.when(ipMock.addr()).thenReturn(expected.getIPv4Address());
        Mockito.when(publicIpMock.getGateway()).thenReturn(expected.getIPv4Gateway());
        Mockito.when(publicIpMock.getNetmask()).thenReturn(expected.getIPv4Netmask());
        Mockito.when(publicIpMock.getMacAddress()).thenReturn(expected.getMacAddress());
        Mockito.when(publicIpMock.getNetworkId()).thenReturn(networkId);
        Mockito.when(networkDao.findById(networkId)).thenReturn(publicNetworkMock);
        Mockito.when(publicNetworkMock.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Vlan);
        Mockito.when(publicIpMock.getVlanTag()).thenReturn(vlanTag);

        networkHelperSpy.configurePublicVrNicBasedOnSourceNatIp(nicProfile, publicIpMock);

        Assert.assertTrue(nicProfile.isDefaultNic());
        Assert.assertEquals(expected.getIPv4Address(), nicProfile.getIPv4Address());
        Assert.assertEquals(expected.getIPv4Gateway(), nicProfile.getIPv4Gateway());
        Assert.assertEquals(expected.getIPv4Netmask(), nicProfile.getIPv4Netmask());
        Assert.assertEquals(expected.getMacAddress(), nicProfile.getMacAddress());
        Assert.assertEquals(expected.getBroadcastType(), nicProfile.getBroadcastType());
        Assert.assertEquals(expected.getBroadCastUri(), nicProfile.getBroadCastUri());
        Assert.assertEquals(expected.getIsolationUri(), nicProfile.getIsolationUri());
    }

    @Test
    public void setPublicNicMacAddressSameAsPeerNicTestDoNothingWhenThereIsNoPeer() throws InsufficientAddressCapacityException {
        String newMacAddress = "ff-ff-ff-ff-ff-ff";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress(newMacAddress);
        Mockito.when(nicDaoMock.findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong())).thenReturn(null);

        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock, routerDeploymentDefinitionMock);

        Assert.assertEquals(newMacAddress, nicProfile.getMacAddress());
    }

    @Test
    public void setPublicNicMacAddressSameAsPeerNicTestKeepMacAddress() throws InsufficientAddressCapacityException {
        String peerMacAddress = "ff-ff-ff-ff-ff-ff";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress("ff-ff-ff-ff-ff-f1");

        Mockito.when(nicDaoMock.findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong())).thenReturn(nicVoMock);
        Mockito.when(nicVoMock.getMacAddress()).thenReturn(peerMacAddress);
        Mockito.when(routerDeploymentDefinitionMock.getKeepMacAddressOnPublicNic()).thenReturn(true);

        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock, routerDeploymentDefinitionMock);

        Assert.assertEquals(peerMacAddress, nicProfile.getMacAddress());
    }

    @Test
    public void setPublicNicMacAddressSameAsPeerNicTestDifferentMacAddressFetchingNewSourceNatIp() throws InsufficientAddressCapacityException {
        String macAddress = "ff-ff-ff-ff-ff-f1";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress(macAddress);
        PublicIp publicIpMock = Mockito.mock(PublicIp.class);

        Mockito.when(nicDaoMock.findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong())).thenReturn(nicVoMock);
        Mockito.when(nicVoMock.getMacAddress()).thenReturn(macAddress);
        Mockito.when(routerDeploymentDefinitionMock.getKeepMacAddressOnPublicNic()).thenReturn(false);
        Mockito.when(routerDeploymentDefinitionMock.getSourceNatIP()).thenReturn(publicIpMock);
        Mockito.doNothing().when(networkHelperSpy).configurePublicVrNicBasedOnSourceNatIp(nicProfile, publicIpMock);

        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock, routerDeploymentDefinitionMock);

        Mockito.verify(routerDeploymentDefinitionMock).findSourceNatIP();
        Mockito.verify(networkHelperSpy).configurePublicVrNicBasedOnSourceNatIp(nicProfile, publicIpMock);
    }

    @Test
    public void setPublicNicMacAddressSameAsPeerNicTestDifferentMacAddressNotFetchingNewSourceNatIp() throws InsufficientAddressCapacityException {
        String newMacAddress = "ff-ff-ff-ff-ff-ff";
        nicProfile.setIPv4Address("10.0.0.1");
        nicProfile.setMacAddress(newMacAddress);

        Mockito.when(nicDaoMock.findByIp4AddressAndNetworkId(Mockito.anyString(), Mockito.anyLong())).thenReturn(nicVoMock);
        Mockito.when(nicVoMock.getMacAddress()).thenReturn("ff-ff-ff-ff-ff-f1");
        Mockito.when(routerDeploymentDefinitionMock.getKeepMacAddressOnPublicNic()).thenReturn(false);

        networkHelperSpy.setPublicNicMacAddressSameAsPeerNic(nicProfile, networkMock, routerDeploymentDefinitionMock);

        Mockito.verify(routerDeploymentDefinitionMock, Mockito.never()).findSourceNatIP();
        Mockito.verify(networkHelperSpy, Mockito.never()).configurePublicVrNicBasedOnSourceNatIp(Mockito.any(), Mockito.any());
        Assert.assertEquals(newMacAddress, nicProfile.getMacAddress());
    }

    @Test
    public void configurePublicNicTestReturnEmptyMapWhenNetworkIsNotPublic() throws InsufficientAddressCapacityException {
        Mockito.when(routerDeploymentDefinitionMock.isPublicNetwork()).thenReturn(false);

        Map<Network, List<? extends NicProfile>> nic = networkHelperSpy.configurePublicNic(routerDeploymentDefinitionMock, false);
        Assert.assertTrue(nic.isEmpty());
    }

    @Test
    public void configurePublicNicTestConfigureDeviceId() throws InsufficientAddressCapacityException {
        PublicIp publicIpMock = Mockito.mock(PublicIp.class);
        NetworkOffering networkOfferingMock = Mockito.mock(NetworkOffering.class);

        Mockito.when(routerDeploymentDefinitionMock.isPublicNetwork()).thenReturn(true);
        Mockito.when(routerDeploymentDefinitionMock.getSourceNatIP()).thenReturn(publicIpMock);
        Mockito.doNothing().when(networkHelperSpy).configurePublicVrNicBasedOnSourceNatIp(Mockito.any(), Mockito.any());
        Mockito.doReturn(List.of(networkOfferingMock)).when(networkModel).getSystemAccountNetworkOfferings(Mockito.any());
        Mockito.doReturn(List.of(networkMock)).when(networkOrchestrationService).setupNetwork(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()
        );
        Mockito.doNothing().when(networkHelperSpy).setPublicNicMacAddressSameAsPeerNic(Mockito.any(), Mockito.any(), Mockito.any());

        Map<Network, List<? extends NicProfile>> nic = networkHelperSpy.configurePublicNic(routerDeploymentDefinitionMock, true);
        Integer nicDeviceId = nic.get(networkMock).get(0).getDeviceId();
        Assert.assertEquals(2, nicDeviceId.intValue());
    }

    @Test
    public void configurePublicNicTestUpdateGuestNetworksIpv6Nic() throws InsufficientAddressCapacityException {
        PublicIp publicIpMock = Mockito.mock(PublicIp.class);
        NetworkOffering networkOfferingMock = Mockito.mock(NetworkOffering.class);
        DeployDestination deployDestinationMock = Mockito.mock(DeployDestination.class);

        Mockito.when(routerDeploymentDefinitionMock.isPublicNetwork()).thenReturn(true);
        Mockito.when(routerDeploymentDefinitionMock.getSourceNatIP()).thenReturn(publicIpMock);
        Mockito.doNothing().when(networkHelperSpy).configurePublicVrNicBasedOnSourceNatIp(Mockito.any(), Mockito.any());
        Mockito.doReturn(List.of(networkOfferingMock)).when(networkModel).getSystemAccountNetworkOfferings(Mockito.any());
        Mockito.doReturn(List.of(networkMock)).when(networkOrchestrationService).setupNetwork(
                Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyBoolean()
        );
        Mockito.doNothing().when(networkHelperSpy).setPublicNicMacAddressSameAsPeerNic(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.when(routerDeploymentDefinitionMock.getGuestNetwork()).thenReturn(networkMock);
        Mockito.when(routerDeploymentDefinitionMock.getDest()).thenReturn(deployDestinationMock);

        Map<Network, List<? extends NicProfile>> nic = networkHelperSpy.configurePublicNic(routerDeploymentDefinitionMock, false);
        Mockito.verify(ipv6ServiceMock).updateNicIpv6(Mockito.eq(nic.get(networkMock).get(0)), Mockito.any(), Mockito.eq(networkMock));
    }
}
