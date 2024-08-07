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

import com.cloud.dc.VlanDetailsVO;
import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.CreateOrUpdateNsxTier1NatRuleCommand;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.utils.NsxControllerUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class NsxPublicNetworkGuruTest {

    NetworkOffering offering;

    NsxPublicNetworkGuru guru;
    @Mock
    NsxServiceImpl nsxService;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    VlanDetailsDao vlanDetailsDao;
    @Mock
    VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    VpcOfferingDao vpcOfferingDao;
    @Mock
    NsxControllerUtils nsxControllerUtils;

    @Before
    public void setup() {
        guru = new NsxPublicNetworkGuru();

        ReflectionTestUtils.setField((PublicNetworkGuru) guru, "_ipAddressDao", ipAddressDao);
        ReflectionTestUtils.setField(guru, "vpcDao", vpcDao);
        ReflectionTestUtils.setField(guru, "vlanDetailsDao", vlanDetailsDao);
        ReflectionTestUtils.setField(guru, "vpcOfferingServiceMapDao", vpcOfferingServiceMapDao);
        ReflectionTestUtils.setField(guru, "nsxService", nsxService);
        ReflectionTestUtils.setField(guru, "vpcOfferingDao", vpcOfferingDao);
        ReflectionTestUtils.setField(guru, "nsxControllerUtils", nsxControllerUtils);

        offering = Mockito.mock(NetworkOffering.class);
        when(offering.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        when(offering.isForNsx()).thenReturn(true);
        when(offering.isSystemOnly()).thenReturn(true);
    }

    @Test
    public void testCanHandle() {
        Assert.assertTrue(guru.canHandle(offering));
    }

    @Test
    public void testCannotHandle() {
        NetworkOffering offering = Mockito.mock(NetworkOffering.class);

        when(offering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        Assert.assertFalse(guru.canHandle(offering));
    }

    @Test
    public void testDesign() {
        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Network network = Mockito.mock(Network.class);
        Account account = Mockito.mock(Account.class);

//        when(network.getTrafficType()).thenReturn(Networks.TrafficType.Public);

        Network designedNetwork = guru.design(offering, plan, network, "net1", 1L, account);
        Assert.assertEquals(Networks.TrafficType.Public, designedNetwork.getTrafficType());
    }

    @Test
    public void testDesign_whenOfferingIsForGuestTraffic() {
        DeploymentPlan plan = Mockito.mock(DeploymentPlan.class);
        Network network = Mockito.mock(Network.class);
        Account account = Mockito.mock(Account.class);

        when(offering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);

        Network designedNetwork = guru.design(offering, plan, network, "net1", 1L, account);
        Assert.assertNull(designedNetwork);
    }

    @Test
    public void testAllocate() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        String publicIpVR = "10.1.12.10";
        String publicIpNSX = "10.1.13.10";
        Network network = Mockito.mock(Network.class);
        NicProfile profile = Mockito.mock(NicProfile.class);
        VirtualMachineProfile vmProfile = Mockito.mock(VirtualMachineProfile.class);
        IPAddressVO srcNatIpOnVR = new IPAddressVO(new Ip(publicIpVR), 2L , 0xaabbccddeeffL, 2L, true);
        srcNatIpOnVR.setVpcId(12L);
        IPAddressVO srcNatIpOnNSX = new IPAddressVO(new Ip(publicIpNSX), 2L , 0xaabbccddeeffL, 3L, true);
        srcNatIpOnNSX.setVpcId(12L);
        VpcVO vpcVO = Mockito.mock(VpcVO.class);
        List<IPAddressVO> sourceNatList = List.of(srcNatIpOnNSX);
        VlanDetailsVO vlanDetailVO = new VlanDetailsVO(3L,ApiConstants.NSX_DETAIL_KEY, "true", false);
        VpcOfferingVO vpcOffering = Mockito.mock(VpcOfferingVO.class);


        when(profile.getIPv4Address()).thenReturn(publicIpVR);
        when(ipAddressDao.findByIp(anyString())).thenReturn(srcNatIpOnVR);
        when(vpcDao.findById(anyLong())).thenReturn(vpcVO);
        when(ipAddressDao.listByAssociatedVpc(12L, true)).thenReturn(sourceNatList);
        when(vlanDetailsDao.findDetail(anyLong(), anyString())).thenReturn(vlanDetailVO);
        when(vpcVO.getVpcOfferingId()).thenReturn(12L);
        when(vpcVO.getId()).thenReturn(12L);
        when(vpcVO.getName()).thenReturn("nsxVPCNet");
        when(vpcOfferingServiceMapDao.areServicesSupportedByVpcOffering(anyLong(), any())).thenReturn(true);
        when(nsxService.createVpcNetwork(anyLong(), anyLong(), anyLong(), anyLong(), anyString(), anyBoolean())).thenReturn(true);
        when(vpcOfferingDao.findById(anyLong())).thenReturn(vpcOffering);
        when(vpcOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED);
        when(nsxControllerUtils.sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class),
                anyLong())).thenReturn(new NsxAnswer(new NsxCommand(), true, ""));

        guru.allocate(network, profile, vmProfile);

        verify(nsxControllerUtils, times(1)).sendNsxCommand(any(CreateOrUpdateNsxTier1NatRuleCommand.class),
                anyLong());

    }
}
