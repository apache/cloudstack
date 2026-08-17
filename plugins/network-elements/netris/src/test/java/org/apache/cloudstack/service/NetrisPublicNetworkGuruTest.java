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
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.netris.NetrisService;
import com.cloud.network.vpc.VpcOfferingVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.user.AccountVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.api.ApiConstants;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Arrays;

public class NetrisPublicNetworkGuruTest {

    @Mock
    private NetworkModel networkModel;
    @Mock
    private IPAddressDao ipAddressDao;
    @Mock
    private VpcDao vpcDao;
    @Mock
    private VlanDetailsDao vlanDetailsDao;
    @Mock
    private VpcOfferingDao vpcOfferingDao;
    @Mock
    private VpcOfferingServiceMapDao vpcOfferingServiceMapDao;
    @Mock
    private NetrisService netrisService;
    @Spy
    @InjectMocks
    private NetrisPublicNetworkGuru guru = new NetrisPublicNetworkGuru();

    @Mock
    private NetworkOfferingVO networkOffering;
    @Mock
    private DeploymentPlan deploymentPlan;
    @Mock
    private NetworkVO network;
    @Mock
    private AccountVO account;
    @Mock
    private NicProfile nicProfile;
    @Mock
    private VirtualMachineProfile virtualMachineProfile;
    @Mock
    private IPAddressVO ipAddressVpcVR;
    @Mock
    private IPAddressVO ipAddressVpcSourceNat;
    @Mock
    private VpcVO vpc;

    private AutoCloseable closeable;

    private static final long networkOfferingId = 10L;
    private static final long physicalNetworkId = 2L;
    private static final long zoneId = 1L;
    private static final long vpcId = 12L;
    private static final String vrNicIp = "10.10.10.10";
    private static final String vpcSourceNatIp = "10.10.20.20";

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        Mockito.when(networkOffering.getId()).thenReturn(networkOfferingId);
        Mockito.when(networkOffering.getTrafficType()).thenReturn(Networks.TrafficType.Public);
        Mockito.when(networkOffering.isSystemOnly()).thenReturn(true);
        Mockito.when(networkModel.isProviderForNetworkOffering(Network.Provider.Netris, networkOfferingId)).thenReturn(true);
        Mockito.when(network.getBroadcastDomainType()).thenReturn(Networks.BroadcastDomainType.Netris);
        Mockito.when(deploymentPlan.getDataCenterId()).thenReturn(zoneId);
        Mockito.when(deploymentPlan.getPhysicalNetworkId()).thenReturn(physicalNetworkId);
        Mockito.when(networkOffering.isRedundantRouter()).thenReturn(false);
        Mockito.when(nicProfile.getIPv4Address()).thenReturn(vrNicIp);
        Mockito.when(ipAddressDao.findByIp(vrNicIp)).thenReturn(ipAddressVpcVR);
        Mockito.when(ipAddressVpcVR.getVpcId()).thenReturn(vpcId);
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(vpc);
        Mockito.when(vpc.getId()).thenReturn(vpcId);
        Mockito.when(ipAddressDao.listByAssociatedVpc(vpcId, true))
                .thenReturn(Arrays.asList(ipAddressVpcVR, ipAddressVpcSourceNat));
        Ip ipMock = Mockito.mock(Ip.class);
        Mockito.when(ipMock.addr()).thenReturn(vrNicIp);
        Mockito.when(ipAddressVpcVR.getAddress()).thenReturn(ipMock);
        Ip ipVrMock = Mockito.mock(Ip.class);
        Mockito.when(ipVrMock.addr()).thenReturn(vpcSourceNatIp);
        Mockito.when(ipAddressVpcSourceNat.getAddress()).thenReturn(ipVrMock);
        Mockito.when(ipAddressVpcSourceNat.isSourceNat()).thenReturn(true);
        Mockito.when(ipAddressVpcSourceNat.isForSystemVms()).thenReturn(false);
        Mockito.when(ipAddressVpcSourceNat.getVlanId()).thenReturn(4L);
        VlanDetailsVO vlanDetailsVO = Mockito.mock(VlanDetailsVO.class);
        Mockito.when(vlanDetailsVO.getValue()).thenReturn("true");
        Mockito.when(vlanDetailsDao.findDetail(4L, ApiConstants.NETRIS_DETAIL_KEY)).thenReturn(vlanDetailsVO);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testCanHandleNetrisPublic() {
        Assert.assertTrue(guru.canHandle(networkOffering));
    }

    @Test
    public void testCannotHandleNonNetrisPublic() {
        Mockito.when(networkModel.isProviderForNetworkOffering(Network.Provider.Netris, networkOfferingId)).thenReturn(false);
        Assert.assertFalse(guru.canHandle(networkOffering));
    }

    @Test
    public void testCannotHandleNonPublicTraffic() {
        Mockito.when(networkOffering.getTrafficType()).thenReturn(Networks.TrafficType.Guest);
        Assert.assertFalse(guru.canHandle(networkOffering));
    }

    @Test
    public void testDesignNetrisNetwork() {
        String name = "test-network";
        long vpcId = 10L;
        Network design = guru.design(networkOffering, deploymentPlan, network, name, vpcId, account);
        Assert.assertEquals(Network.State.Setup, design.getState());
        Assert.assertEquals(Networks.BroadcastDomainType.Netris, design.getBroadcastDomainType());
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAllocateNetrisNetworkMissingIpAddress() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Mockito.when(ipAddressDao.findByIp(vrNicIp)).thenReturn(null);
        guru.allocate(network, nicProfile, virtualMachineProfile);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAllocateNetrisNetworkMissingVpc() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(null);
        guru.allocate(network, nicProfile, virtualMachineProfile);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAllocateNetrisNetworkSourceNatIps() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Mockito.when(vpcDao.findById(vpcId)).thenReturn(null);
        guru.allocate(network, nicProfile, virtualMachineProfile);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testAllocateNetrisNetworkMissingIps() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        Mockito.when(ipAddressDao.listByAssociatedVpc(vpcId, true)).thenReturn(null);
        guru.allocate(network, nicProfile, virtualMachineProfile);
    }

    @Test
    public void testAllocateNetrisNetwork() throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        long vpcOfferingId = 20L;
        long accountId = 2L;
        long domainId = 8L;
        long networkId = 210L;
        String vpcName = "test-vpc";
        String vpcCidr = "172.20.10.1/28";
        String networkName = "vpc-tier";
        Mockito.when(vpc.getAccountId()).thenReturn(accountId);
        Mockito.when(vpc.getZoneId()).thenReturn(zoneId);
        Mockito.when(vpc.getDomainId()).thenReturn(domainId);
        Mockito.when(vpc.getVpcOfferingId()).thenReturn(vpcOfferingId);
        Mockito.when(vpc.getName()).thenReturn(vpcName);
        Mockito.when(vpc.getCidr()).thenReturn(vpcCidr);
        Mockito.when(network.getName()).thenReturn(networkName);
        Mockito.when(network.getId()).thenReturn(networkId);
        VpcOfferingVO vpcOffering = Mockito.mock(VpcOfferingVO.class);
        Mockito.when(vpcOfferingDao.findById(vpcOfferingId)).thenReturn(vpcOffering);
        Mockito.when(vpcOffering.getNetworkMode()).thenReturn(NetworkOffering.NetworkMode.NATTED);
        Mockito.when(vpcOfferingServiceMapDao.areServicesSupportedByVpcOffering(
                vpcOfferingId, new Network.Service[]{Network.Service.SourceNat})).thenReturn(true);
        Mockito.when(netrisService.createVpcResource(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(),
                Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(netrisService.createSnatRule(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(),
                Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(), Mockito.anyLong(),
                Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        guru.allocate(network, nicProfile, virtualMachineProfile);
        Mockito.verify(netrisService).createVpcResource(zoneId, accountId, domainId, vpcId, vpcName, true,
                vpcCidr, true);
        Mockito.verify(netrisService).createSnatRule(zoneId, accountId, domainId, vpcName, vpcId, networkName,
                networkId, true, vpcCidr, vpcSourceNatIp);
    }
}
