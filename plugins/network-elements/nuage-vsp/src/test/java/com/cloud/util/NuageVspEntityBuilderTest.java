//
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
//

package com.cloud.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.nuage.vsp.acs.client.api.model.Protocol;
import net.nuage.vsp.acs.client.api.model.VspAclRule;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Lists;

import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.NuageTest;
import com.cloud.dc.VlanDetailsVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.VlanDao;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.NetworkACLItem;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;


public class NuageVspEntityBuilderTest extends NuageTest {

    private static final long DOMAIN_ID = 1L;
    private static final long ACCOUNT_ID = 1L;
    private static final long NETWORK_OFFERING_ID = 1L;
    private static final long SHARED_NETWORK_OFFERING_ID = 2L;
    private static final long L2_NETWORK_OFFERING_ID = 3L;
    private static final long VPC_ID = 1L;
    private static final long SOURCE_IP_ADDRESS_ID = 1L;
    private static final long VM_ID = 4L;
    private static final long VLAN_ID = 5L;
    public static final String VM_IP = "192.168.0.24";

    @Mock private AccountDao _accountDao;
    @Mock private DomainDao _domainDao;
    @Mock private IPAddressDao _ipAddressDao;
    @Mock private NetworkDao _networkDao;
    @Mock private NetworkDetailsDao _networkDetailsDao;
    @Mock private NetworkOfferingDao _networkOfferingDao;
    @Mock private NetworkOfferingServiceMapDao _networkOfferingServiceMapDao;
    @Mock private NicDao _nicDao;
    @Mock private NicSecondaryIpDao _nicSecondaryIpDao;
    @Mock private VlanDao _vlanDao;
    @Mock private VlanDetailsDao _vlanDetailsDao;
    @Mock private VpcDao _vpcDao;
    @Mock private VpcDetailsDao _vpcDetailsDao;

    @Mock private NuageVspManager _nuageVspManager;

    @InjectMocks
    private NuageVspEntityBuilder _nuageVspEntityBuilder = new NuageVspEntityBuilder();

    private DomainVO _mockedDomain = mock(DomainVO.class);
    private AccountVO _mockedAccount = mock(AccountVO.class);
    private NetworkOfferingVO _mockedNetworkOffering = mock(NetworkOfferingVO.class);
    private NetworkOfferingVO _mockedSharedNetworkOffering = mock(NetworkOfferingVO.class);
    private NetworkOfferingVO _mockedL2NetworkOffering = mock(NetworkOfferingVO.class);
    private VlanVO _mockedVlan = mock(VlanVO.class);
    private VlanDetailsVO _mockedVlanDetail = mock(VlanDetailsVO.class);
    private VpcVO _mockedVpc = mock(VpcVO.class);
    private NetworkVO _mockedNetwork = mock(NetworkVO.class);
    private NetworkVO _mockedVpcNetwork = mock(NetworkVO.class);
    private NetworkVO _mockedSharedNetwork = mock(NetworkVO.class);
    private NetworkVO _mockedL2Network = mock(NetworkVO.class);
    private VirtualMachine _mockedUserVirtualMachine = mock(VirtualMachine.class);
    private VirtualMachine _mockedDomainRouterVirtualMachine = mock(VirtualMachine.class);
    private NicProfile _mockedNicProfile = mock(NicProfile.class);
    private NicVO _mockedNic = mock(NicVO.class);
    private IPAddressVO _mockedStaticNatIp = mock(IPAddressVO.class);
    private VlanVO _mockedStaticNatVlan = mock(VlanVO.class);
    private FirewallRule _mockedFirewallRule = mock(FirewallRule.class);
    private NetworkACLItem _mockedNetworkAclItem = mock(NetworkACLItem.class);

    @Before
    public void setUp() throws Exception {
        super.setUp();

        setUpMockedDomain();
        setUpMockedAccount();
        setUpMockedNetworkOffering(_mockedNetworkOffering, Network.GuestType.Isolated);
        setUpMockedNetworkOffering(_mockedSharedNetworkOffering, Network.GuestType.Shared);
        setUpMockedNetworkOffering(_mockedL2NetworkOffering, Network.GuestType.Isolated);
        setUpMockedVlan();
        setUpMockedVlanDetail();
        setUpMockedVpc();
        setUpMockedNetwork(_mockedNetwork, NETWORK_OFFERING_ID, null);
        setUpMockedNetwork(_mockedVpcNetwork, NETWORK_OFFERING_ID, VPC_ID);
        setUpMockedNetwork(_mockedSharedNetwork, SHARED_NETWORK_OFFERING_ID, null);
        setUpMockedNetwork(_mockedL2Network, L2_NETWORK_OFFERING_ID, null);
        setUpMockedVirtualMachine(_mockedUserVirtualMachine, false);
        setUpMockedVirtualMachine(_mockedDomainRouterVirtualMachine, true);
        setUpMockedNicProfile();
        setUpMockedNic();
        setUpMockedStaticNatIp();
        setUpMockedStaticNatVlan();
        setUpMockedFirewallRule();
        setUpMockedNetworkAclItem();
        setUpMockedDaoCalls();
    }

    @Test
    public void testBuildVspDomain() {
        VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(_mockedDomain);
        validateVspDomain(vspDomain);
    }

    @Test
    public void testBuildVspNetwork() {
        VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedL2Network);
        validateVspNetwork(vspNetwork, true, false, false, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedL2Network);
        validateVspNetwork(vspNetwork, true, false, false, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedNetwork);
        validateVspNetwork(vspNetwork, false, true, false, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedNetwork);
        validateVspNetwork(vspNetwork, false, true, false, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedVpcNetwork);
        validateVspNetwork(vspNetwork, false, false, true, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedVpcNetwork);
        validateVspNetwork(vspNetwork, false, false, true, false);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedSharedNetwork);
        validateVspNetwork(vspNetwork, false, false, false, true);

        vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(_mockedSharedNetwork);
        validateVspNetwork(vspNetwork, false, false, false, true);
    }

    @Test
    public void testBuildVspVm() {
        VspVm vspVm = _nuageVspEntityBuilder.buildVspVm(_mockedUserVirtualMachine, _mockedNetwork);
        validateVspVm(vspVm, false);

        vspVm = _nuageVspEntityBuilder.buildVspVm(_mockedDomainRouterVirtualMachine, _mockedNetwork);
        validateVspVm(vspVm, true);
    }

    @Test
    public void testBuildVspNic() {
        VspNic vspNic = _nuageVspEntityBuilder.buildVspNic("nicUuid", _mockedNicProfile);
        validateVspNic(vspNic);

        vspNic = _nuageVspEntityBuilder.buildVspNic(_mockedNic);
        validateVspNic(vspNic);
    }

    @Test
    public void testBuildVspStaticNat() {
        VspStaticNat vspStaticNat = _nuageVspEntityBuilder.buildVspStaticNat(true, _mockedStaticNatIp, _mockedStaticNatVlan, _mockedNic);
        validateVspStaticNat(vspStaticNat, true);
    }

    @Test
    public void testBuildVspAclRuleAcl() {
        VspAclRule vspAclRule = _nuageVspEntityBuilder.buildVspAclRule(_mockedNetworkAclItem);
        validateVspAclRule(vspAclRule, false);
    }

    @Test
    public void testBuildVspAclRuleFirewall() {
        VspAclRule vspAclRule = _nuageVspEntityBuilder.buildVspAclRule(_mockedFirewallRule, _mockedNetwork);
        validateVspAclRule(vspAclRule, true);
    }

    private void validateVspDomain(VspDomain vspDomain) {
        assertEquals("domainUuid", vspDomain.getUuid());
        assertEquals("domainName", vspDomain.getName());
        assertEquals("domainPath", vspDomain.getPath());
    }

    private void validateVspNetwork(VspNetwork vspNetwork, boolean isL2, boolean isL3, boolean isVpc, boolean isShared) {
        assertEquals(NETWORK_ID, vspNetwork.getId());
        assertEquals("networkUuid", vspNetwork.getUuid());
        assertEquals("networkName", vspNetwork.getName());
        assertNotNull(vspNetwork.getVspDomain());
        validateVspDomain(vspNetwork.getVspDomain());

        assertEquals("accountName", vspNetwork.getAccountName());
        assertEquals("accountUuid", vspNetwork.getAccountUuid());

        if (isVpc) {
            assertEquals("vpcUuid", vspNetwork.getVpcUuid());
            assertEquals("vpcName", vspNetwork.getVpcName());
        } else {
            assertNull(vspNetwork.getVpcUuid());
            assertNull(vspNetwork.getVpcName());
        }

        assertEquals(isL2, vspNetwork.isL2());
        assertEquals(isL3, vspNetwork.isL3());
        assertEquals(isVpc, vspNetwork.isVpc());
        assertEquals(isShared, vspNetwork.isShared());
        assertEquals(true, vspNetwork.isFirewallServiceSupported());
        assertEquals(true, vspNetwork.isEgressDefaultPolicy());
        assertEquals("10.10.10.0/24", vspNetwork.getCidr());
        assertEquals("10.10.10.1", vspNetwork.getGateway());
    }

    private void validateVspVm(VspVm vspVm, boolean isDomainRouter) {
        assertEquals("virtualMachineUuid", vspVm.getUuid());
        assertEquals("virtualMachineInstanceName", vspVm.getName());
        assertEquals(VspVm.State.Running, vspVm.getState());
        assertEquals(isDomainRouter, vspVm.getDomainRouter());
    }

    private void validateVspNic(VspNic vspNic) {
        assertEquals("nicUuid", vspNic.getUuid());
        assertEquals("macAddress", vspNic.getMacAddress());
        assertEquals(true, vspNic.getUseStaticIp());
        assertEquals("192.168.0.24", vspNic.getIp());
    }

    private void validateVspStaticNat(VspStaticNat vspStaticNat, Boolean forRevoke) {
        assertEquals("staticNatIpUuid", vspStaticNat.getIpUuid());
        assertEquals("10.10.10.2", vspStaticNat.getIpAddress());
        assertEquals(forRevoke, vspStaticNat.getRevoke());
        assertEquals(VspStaticNat.State.Allocated, vspStaticNat.getState());
        assertEquals(true, vspStaticNat.getOneToOneNat());
        assertEquals("staticNatVlanUuid", vspStaticNat.getVlanUuid());
        assertEquals("10.10.10.1", vspStaticNat.getVlanGateway());
        assertEquals("255.255.255.0", vspStaticNat.getVlanNetmask());
    }

    private void validateVspAclRule(VspAclRule vspAclRule, boolean isFirewall) {
        assertEquals("aclUuid", vspAclRule.getUuid());
        assertEquals(Protocol.TCP, vspAclRule.getProtocol());
        assertEquals(new Integer(1), vspAclRule.getStartPort());
        assertEquals(new Integer(20), vspAclRule.getEndPort());
        assertEquals(Lists.newArrayList("10.10.0.0/16"), vspAclRule.getSourceCidrList());
        assertEquals(VspAclRule.ACLState.Active, vspAclRule.getState());
        assertEquals(VspAclRule.ACLTrafficType.Egress, vspAclRule.getTrafficType());

        if (isFirewall) {
            assertEquals(VspAclRule.ACLType.Firewall, vspAclRule.getType());
            final VspStaticNat staticNat = vspAclRule.getStaticNat();
            assertNotNull(staticNat);
            assertEquals("192.168.0.24/32", staticNat.getDestinationIp());
            assertEquals(VspAclRule.ACLAction.Deny, vspAclRule.getAction());
        } else {
            assertEquals(VspAclRule.ACLType.NetworkACL, vspAclRule.getType());
            assertNull(vspAclRule.getStaticNat());
            assertNull(vspAclRule.getSourceIpAddress());
            assertEquals(VspAclRule.ACLAction.Allow, vspAclRule.getAction());
        }
    }

    private void setUpMockedDomain() {
        when(_mockedDomain.getUuid()).thenReturn("domainUuid");
        when(_mockedDomain.getName()).thenReturn("domainName");
        when(_mockedDomain.getPath()).thenReturn("domainPath");
    }

    private void setUpMockedAccount() {
        when(_mockedAccount.getUuid()).thenReturn("accountUuid");
        when(_mockedAccount.getAccountName()).thenReturn("accountName");
    }

    private void setUpMockedNetworkOffering(NetworkOfferingVO networkOfferingToMock, Network.GuestType guestType) {
        when(networkOfferingToMock.isEgressDefaultPolicy()).thenReturn(true);
        when(networkOfferingToMock.getGuestType()).thenReturn(guestType);
    }

    private void setUpMockedVlan() {
        when(_mockedVlan.getIpRange()).thenReturn("192.168.2.2-192.168.2.200");
    }

    private void setUpMockedVlanDetail() {
        when(_mockedVlanDetail.getValue()).thenReturn("true");
    }

    private void setUpMockedVpc() {
        when(_mockedVpc.getUuid()).thenReturn("vpcUuid");
        when(_mockedVpc.getName()).thenReturn("vpcName");
    }

    private void setUpMockedNetwork(NetworkVO networkToMock, long networkOfferingId, Long vpcId) {
        when(networkToMock.getId()).thenReturn(NETWORK_ID);
        when(networkToMock.getUuid()).thenReturn("networkUuid");
        when(networkToMock.getName()).thenReturn("networkName");
        when(networkToMock.getCidr()).thenReturn("10.10.10.0/24");
        when(networkToMock.getGateway()).thenReturn("10.10.10.1");
        when(networkToMock.getDomainId()).thenReturn(DOMAIN_ID);
        when(networkToMock.getAccountId()).thenReturn(ACCOUNT_ID);
        when(networkToMock.getNetworkOfferingId()).thenReturn(networkOfferingId);
        when(networkToMock.getVpcId()).thenReturn(vpcId != null ? vpcId : null);
    }

    private void setUpMockedVirtualMachine(VirtualMachine virtualMachineToMock, boolean isDomainRouter) {
        when(virtualMachineToMock.getUuid()).thenReturn("virtualMachineUuid");
        when(virtualMachineToMock.getInstanceName()).thenReturn("virtualMachineInstanceName");
        when(virtualMachineToMock.getState()).thenReturn(VirtualMachine.State.Running);
        when(virtualMachineToMock.getType()).thenReturn(isDomainRouter ? VirtualMachine.Type.DomainRouter : VirtualMachine.Type.User);
    }

    private void setUpMockedNicProfile() {
        when(_mockedNicProfile.getMacAddress()).thenReturn("macAddress");
        when(_mockedNicProfile.getIPv4Address()).thenReturn(VM_IP);
        when(_mockedNicProfile.getNetworkId()).thenReturn(NETWORK_ID);
    }

    private void setUpMockedNic() {
        when(_mockedNic.getUuid()).thenReturn("nicUuid");
        when(_mockedNic.getMacAddress()).thenReturn("macAddress");
        when(_mockedNic.getIPv4Address()).thenReturn(VM_IP);
        when(_mockedNic.getNetworkId()).thenReturn(NETWORK_ID);
    }

    private void setUpMockedStaticNatIp() {
        when(_mockedStaticNatIp.getUuid()).thenReturn("staticNatIpUuid");
        when(_mockedStaticNatIp.getAddress()).thenReturn(new Ip("10.10.10.2"));
        when(_mockedStaticNatIp.isOneToOneNat()).thenReturn(true);
        when(_mockedStaticNatIp.getVmIp()).thenReturn(VM_IP);
        when(_mockedStaticNatIp.getAssociatedWithNetworkId()).thenReturn(NETWORK_ID);
        when(_mockedStaticNatIp.getAssociatedWithVmId()).thenReturn(VM_ID);
        when(_mockedStaticNatIp.getState()).thenReturn(IpAddress.State.Allocated);
        when(_mockedStaticNatIp.getVlanId()).thenReturn(VLAN_ID);
    }

    private void setUpMockedStaticNatVlan() {
        when(_mockedStaticNatVlan.getUuid()).thenReturn("staticNatVlanUuid");
        when(_mockedStaticNatVlan.getVlanGateway()).thenReturn("10.10.10.1");
        when(_mockedStaticNatVlan.getVlanNetmask()).thenReturn("255.255.255.0");
    }

    private void setUpMockedFirewallRule() {
        when(_mockedFirewallRule.getUuid()).thenReturn("aclUuid");
        when(_mockedFirewallRule.getProtocol()).thenReturn("TCP");
        when(_mockedFirewallRule.getSourcePortStart()).thenReturn(1);
        when(_mockedFirewallRule.getSourcePortEnd()).thenReturn(20);
        when(_mockedFirewallRule.getSourceCidrList()).thenReturn(Lists.newArrayList("10.10.0.0/16"));
        when(_mockedFirewallRule.getState()).thenReturn(FirewallRule.State.Active);
        when(_mockedFirewallRule.getTrafficType()).thenReturn(FirewallRule.TrafficType.Egress);
        when(_mockedFirewallRule.getSourceIpAddressId()).thenReturn(SOURCE_IP_ADDRESS_ID);
    }

    private void setUpMockedNetworkAclItem() {
        when(_mockedNetworkAclItem.getUuid()).thenReturn("aclUuid");
        when(_mockedNetworkAclItem.getProtocol()).thenReturn("TCP");
        when(_mockedNetworkAclItem.getSourcePortStart()).thenReturn(1);
        when(_mockedNetworkAclItem.getSourcePortEnd()).thenReturn(20);
        when(_mockedNetworkAclItem.getSourceCidrList()).thenReturn(Lists.newArrayList("10.10.0.0/16"));
        when(_mockedNetworkAclItem.getNumber()).thenReturn(1337);
        when(_mockedNetworkAclItem.getState()).thenReturn(NetworkACLItem.State.Active);
        when(_mockedNetworkAclItem.getTrafficType()).thenReturn(NetworkACLItem.TrafficType.Egress);
        when(_mockedNetworkAclItem.getAction()).thenReturn(NetworkACLItem.Action.Allow);
    }

    private void setUpMockedDaoCalls() {
        when(_domainDao.findById(DOMAIN_ID)).thenReturn(_mockedDomain);
        when(_accountDao.findById(ACCOUNT_ID)).thenReturn(_mockedAccount);
        when(_networkDao.findById(NETWORK_ID)).thenReturn(_mockedNetwork);
        when(_networkOfferingDao.findById(NETWORK_OFFERING_ID)).thenReturn(_mockedNetworkOffering);
        when(_networkOfferingDao.findById(SHARED_NETWORK_OFFERING_ID)).thenReturn(_mockedSharedNetworkOffering);
        when(_networkOfferingDao.findById(L2_NETWORK_OFFERING_ID)).thenReturn(_mockedL2NetworkOffering);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(NETWORK_OFFERING_ID, Network.Service.SourceNat)).thenReturn(true);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(NETWORK_OFFERING_ID, Network.Service.StaticNat)).thenReturn(true);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(SHARED_NETWORK_OFFERING_ID, Network.Service.SourceNat)).thenReturn(true);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(SHARED_NETWORK_OFFERING_ID, Network.Service.StaticNat)).thenReturn(true);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(L2_NETWORK_OFFERING_ID, Network.Service.SourceNat)).thenReturn(false);
        when(_networkOfferingServiceMapDao.areServicesSupportedByNetworkOffering(L2_NETWORK_OFFERING_ID, Network.Service.StaticNat)).thenReturn(false);
        when(_networkModel.areServicesSupportedByNetworkOffering(NETWORK_OFFERING_ID, Network.Service.Firewall)).thenReturn(true);
        when(_networkModel.areServicesSupportedByNetworkOffering(SHARED_NETWORK_OFFERING_ID, Network.Service.Firewall)).thenReturn(true);
        when(_networkModel.areServicesSupportedByNetworkOffering(L2_NETWORK_OFFERING_ID, Network.Service.Firewall)).thenReturn(true);
        when(_vlanDao.listVlansByNetworkId(NETWORK_ID)).thenReturn(Lists.newArrayList(_mockedVlan));
        when(_vlanDao.findById(VLAN_ID)).thenReturn(_mockedVlan);
        when(_vlanDetailsDao.findDetail(anyLong(), anyString())).thenReturn(_mockedVlanDetail);
        when(_vpcDao.findById(VPC_ID)).thenReturn(_mockedVpc);
        when(_ipAddressDao.findById(SOURCE_IP_ADDRESS_ID)).thenReturn(_mockedStaticNatIp);
        when(_vpcDetailsDao.listDetailsKeyPairs(VPC_ID)).thenReturn(null);
        when(_nicDao.findByIp4AddressAndNetworkId("192.168.0.24", NETWORK_ID)).thenReturn(_mockedNic);
    }
}
