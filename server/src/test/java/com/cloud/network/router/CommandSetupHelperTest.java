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

import com.cloud.agent.api.Command;
import com.cloud.agent.api.routing.SetBgpPeersCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.ASNumberVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.ASNumberDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.utils.net.Ip;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.network.BgpPeerVO;
import org.apache.cloudstack.network.dao.BgpPeerDetailsDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CommandSetupHelperTest {

    @InjectMocks
    protected CommandSetupHelper commandSetupHelper = new CommandSetupHelper();
    @Mock
    NicDao nicDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    VlanDao vlanDao;
    @Mock
    NetworkModel networkModel;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    ConfigurationManager configurationManager;
    @Mock
    NetworkOfferingDetailsDao networkOfferingDetailsDao;
    @Mock
    NetworkDetailsDao networkDetailsDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    RouterControlHelper routerControlHelper;
    @Mock
    DataCenterDao dcDao;
    @Mock
    ASNumberDao asNumberDao;
    @Mock
    BgpPeerDetailsDao bgpPeerDetailsDao;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(commandSetupHelper, "_nicDao", nicDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkDao", networkDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_ipAddressDao", ipAddressDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_vlanDao", vlanDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkModel", networkModel);
        ReflectionTestUtils.setField(commandSetupHelper, "_networkOfferingDao", networkOfferingDao);
        ReflectionTestUtils.setField(commandSetupHelper, "networkOfferingDetailsDao", networkOfferingDetailsDao);
        ReflectionTestUtils.setField(commandSetupHelper, "networkDetailsDao", networkDetailsDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_vpcDao", vpcDao);
        ReflectionTestUtils.setField(commandSetupHelper, "_routerControlHelper", routerControlHelper);
        ReflectionTestUtils.setField(commandSetupHelper, "_dcDao", dcDao);
    }

    @Test
    public void testUserDataDetails() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = new String("{test1=value1,test2=value2}");
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);

        List<String[]> metadata = vmDataCommand.getVmData();
        String[] metadataFile1 = metadata.get(0);
        String[] metadataFile2 = metadata.get(1);

        Assert.assertEquals("metadata", metadataFile1[0]);
        Assert.assertEquals("metadata", metadataFile2[0]);

        Assert.assertEquals("test1", metadataFile1[1]);
        Assert.assertEquals("test2", metadataFile2[1]);

        Assert.assertEquals("value1", metadataFile1[2]);
        Assert.assertEquals("value2", metadataFile2[2]);
    }

    @Test
    public void testNullUserDataDetails() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = null;
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);
        Assert.assertEquals(new ArrayList<>(), vmDataCommand.getVmData());
    }

    @Test
    public void testUserDataDetailsWithWhiteSpaces() {
        VmDataCommand vmDataCommand = new VmDataCommand("testVMname");
        String testUserDataDetails = new String("{test1 =value1,test2= value2 }");
        commandSetupHelper.addUserDataDetailsToCommand(vmDataCommand, testUserDataDetails);

        List<String[]> metadata = vmDataCommand.getVmData();
        String[] metadataFile1 = metadata.get(0);
        String[] metadataFile2 = metadata.get(1);

        Assert.assertEquals("metadata", metadataFile1[0]);
        Assert.assertEquals("metadata", metadataFile2[0]);

        Assert.assertEquals("test1", metadataFile1[1]);
        Assert.assertEquals("test2", metadataFile2[1]);

        Assert.assertEquals("value1", metadataFile1[2]);
        Assert.assertEquals("value2", metadataFile2[2]);
    }

    @Test
    public void testCreateVpcAssociatePublicIP() {
        VirtualRouter router = Mockito.mock(VirtualRouter.class);
        Ip ip = new Ip("10.10.10.10");
        IPAddressVO ipAddressVO = new IPAddressVO(ip, 1L, 0x0ac00000L, 2L, true);
        VlanVO vlanVO = new VlanVO();
        vlanVO.setNetworkId(15L);
        PublicIpAddress publicIpAddress = new PublicIp(ipAddressVO, vlanVO, 0x0ac00000L);
        List<PublicIpAddress> pubIpList = new ArrayList<>(1);
        pubIpList.add(publicIpAddress);
        Commands commands = new Commands(Command.OnError.Stop);
        Map<String, String> vlanMacAddress = new HashMap<>();
        NicVO nicVO = new NicVO("nic", 1L, 2L, VirtualMachine.Type.User);
        NetworkVO networkVO = new NetworkVO();
        networkVO.setNetworkOfferingId(12L);
        List<IPAddressVO> userIps = List.of(ipAddressVO);
        NetworkOfferingVO networkOfferingVO = new NetworkOfferingVO();
        Map<NetworkOffering.Detail, String> details = new HashMap<>();
        VpcVO vpc = new VpcVO();
        DataCenterVO dc = new DataCenterVO(1L, null, null, null, null, null, null, null, null, null, DataCenter.NetworkType.Advanced, null, null);

        when(router.getId()).thenReturn(14L);
        when(router.getDataCenterId()).thenReturn(4L);
        when(nicDao.listByVmId(ArgumentMatchers.anyLong())).thenReturn(List.of(nicVO));
        when(networkDao.findById(ArgumentMatchers.anyLong())).thenReturn(networkVO);
        when(ipAddressDao.listByAssociatedVpc(ArgumentMatchers.anyLong(), ArgumentMatchers.nullable(Boolean.class))).thenReturn(userIps);
        when(vlanDao.findById(ArgumentMatchers.anyLong())).thenReturn(vlanVO);
        when(networkModel.getNetworkRate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(1200);
        when(networkModel.getNetwork(ArgumentMatchers.anyLong())).thenReturn(networkVO);
        when(networkOfferingDao.findById(ArgumentMatchers.anyLong())).thenReturn(networkOfferingVO);
        when(configurationManager.getNetworkOfferingNetworkRate(ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong())).thenReturn(1200);
        when(networkModel.isSecurityGroupSupportedInNetwork(networkVO)).thenReturn(false);
        when(networkOfferingDetailsDao.getNtwkOffDetails(ArgumentMatchers.anyLong())).thenReturn(details);
        when(networkDetailsDao.findDetail(ArgumentMatchers.anyLong(), ArgumentMatchers.anyString())).thenReturn(null);
        when(vpcDao.findById(ArgumentMatchers.anyLong())).thenReturn(vpc);
        when(routerControlHelper.getRouterControlIp(ArgumentMatchers.anyLong())).thenReturn("10.1.11.101");
        when(dcDao.findById(ArgumentMatchers.anyLong())).thenReturn(dc);

        commandSetupHelper.createVpcAssociatePublicIPCommands(router, pubIpList, commands, vlanMacAddress);
        Assert.assertEquals(2, commands.size());
    }

    @Test
    public void testCreateBgpPeersCommandsForNetwork() {
        BgpPeerVO bgpPeer1 = Mockito.mock(BgpPeerVO.class);
        BgpPeerVO bgpPeer2 = Mockito.mock(BgpPeerVO.class);
        List<BgpPeerVO> bgpPeers = Arrays.asList(bgpPeer1, bgpPeer2);
        Commands cmds = new Commands(Command.OnError.Stop);
        VirtualRouter router = Mockito.mock(VirtualRouter.class);
        NetworkVO network = Mockito.mock(NetworkVO.class);

        long zoneId = 10L;
        long networkId = 11L;
        when(router.getDataCenterId()).thenReturn(zoneId);
        when(router.getVpcId()).thenReturn(null);
        when(network.getId()).thenReturn(networkId);
        ASNumberVO asNumberVO = Mockito.mock(ASNumberVO.class);
        when(asNumberDao.findByZoneAndNetworkId(zoneId, networkId)).thenReturn(asNumberVO);
        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dcDao.findById(zoneId)).thenReturn(dc);
        when(dc.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);

        commandSetupHelper.createBgpPeersCommands(bgpPeers, router, cmds, network);

        Assert.assertEquals(1, cmds.size());
        Command cmd = cmds.toCommands()[0];
        Assert.assertTrue(cmd instanceof SetBgpPeersCommand);
        Assert.assertEquals(2, ((SetBgpPeersCommand) cmd).getBpgPeers().length);
    }

    @Test
    public void testCreateBgpPeersCommandsForVpc() {
        BgpPeerVO bgpPeer1 = Mockito.mock(BgpPeerVO.class);
        BgpPeerVO bgpPeer2 = Mockito.mock(BgpPeerVO.class);
        List<BgpPeerVO> bgpPeers = Arrays.asList(bgpPeer1, bgpPeer2);
        Commands cmds = new Commands(Command.OnError.Stop);
        VirtualRouter router = Mockito.mock(VirtualRouter.class);
        NetworkVO network = Mockito.mock(NetworkVO.class);

        long zoneId = 10L;
        long vpcId = 11L;
        when(router.getDataCenterId()).thenReturn(zoneId);
        when(router.getVpcId()).thenReturn(vpcId);
        ASNumberVO asNumberVO = Mockito.mock(ASNumberVO.class);
        when(asNumberDao.findByZoneAndVpcId(zoneId, vpcId)).thenReturn(asNumberVO);

        long networkOfferingId = 12L;
        NetworkOfferingVO offering = Mockito.mock(NetworkOfferingVO.class);
        when(networkOfferingDao.findByIdIncludingRemoved(networkOfferingId)).thenReturn(offering);
        when(offering.getRoutingMode()).thenReturn(NetworkOffering.RoutingMode.Dynamic);
        NetworkVO network1 = Mockito.mock(NetworkVO.class);
        when(network1.getNetworkOfferingId()).thenReturn(networkOfferingId);
        NetworkVO network2 = Mockito.mock(NetworkVO.class);
        when(network2.getNetworkOfferingId()).thenReturn(networkOfferingId);
        when(networkDao.listByVpc(vpcId)).thenReturn(Arrays.asList(network1, network2));

        DataCenterVO dc = Mockito.mock(DataCenterVO.class);
        when(dcDao.findById(zoneId)).thenReturn(dc);
        when(dc.getNetworkType()).thenReturn(DataCenter.NetworkType.Advanced);

        commandSetupHelper.createBgpPeersCommands(bgpPeers, router, cmds, network);

        Assert.assertEquals(1, cmds.size());
        Command cmd = cmds.toCommands()[0];
        Assert.assertTrue(cmd instanceof SetBgpPeersCommand);
        Assert.assertEquals(4, ((SetBgpPeersCommand) cmd).getBpgPeers().length);
    }
}
