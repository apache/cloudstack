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
package org.apache.cloudstack.storage.template;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.VNF;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesService;
import com.cloud.network.security.SecurityGroup;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.SecurityGroupRuleVO;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.security.SecurityGroupVO;
import com.cloud.storage.VnfTemplateDetailVO;
import com.cloud.storage.VnfTemplateNicVO;
import com.cloud.storage.dao.VnfTemplateDetailsDao;
import com.cloud.storage.dao.VnfTemplateNicDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.NicDao;
import org.apache.cloudstack.api.command.user.template.RegisterVnfTemplateCmd;
import org.apache.cloudstack.api.command.user.template.UpdateVnfTemplateCmd;

import org.apache.cloudstack.api.command.user.vm.DeployVnfApplianceCmd;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VnfTemplateManagerImplTest {

    @Spy
    @InjectMocks
    VnfTemplateManagerImpl vnfTemplateManagerImpl;

    @Mock
    VnfTemplateDetailsDao vnfTemplateDetailsDao;
    @Mock
    VnfTemplateNicDao vnfTemplateNicDao;

    @Mock
    VirtualMachineTemplate template;

    @Mock
    NicDao nicDao;

    @Mock
    NetworkDao networkDao;

    @Mock
    NetworkModel networkModel;

    @Mock
    SecurityGroupManager securityGroupManager;

    @Mock
    SecurityGroupService securityGroupService;

    @Mock
    NetworkService networkService;

    @Mock
    IpAddressManager ipAddressManager;

    @Mock
    RulesService rulesService;

    @Mock
    FirewallRulesDao firewallRulesDao;

    @Mock
    FirewallService firewallService;

    final static long templateId = 100L;
    final static long vmId = 101L;
    final static long networkId = 101L;
    final static long securityGroupId = 102L;
    final static long zoneId = 103L;
    final static long publicIpId = 104L;
    final static String ipAddress = "10.10.10.10";
    final static Integer sshPort = 2222;
    final static Integer httpPort = 8080;
    final static Integer httpsPort = 8443;
    final Map<String, Object> vnfNics = new HashMap<>();
    final Map<String, Object> vnfDetails = new HashMap<>();

    @Before
    public void setUp() {
        vnfNics.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "1"),
                Map.entry("name", "eth1"),
                Map.entry("required", "true"),
                Map.entry("description", "The second NIC of VNF appliance")
        )));
        vnfNics.put("1", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "2"),
                Map.entry("name", "eth2"),
                Map.entry("required", "false"),
                Map.entry("description", "The third NIC of VNF appliance")
        )));
        vnfNics.put("2", new HashMap<>(Map.ofEntries(
                Map.entry("deviceid", "0"),
                Map.entry("name", "eth0"),
                Map.entry("description", "The first NIC of VNF appliance")
        )));

        vnfDetails.put("0", new HashMap<>(Map.ofEntries(
                Map.entry("accessMethods", "console,http,https"),
                Map.entry("username", "admin"),
                Map.entry("password", "password"),
                Map.entry("version", "4.19.0"),
                Map.entry("vendor", "cloudstack")
        )));

        VnfTemplateNicVO vnfNic1 = new VnfTemplateNicVO(templateId, 0L, "eth0", true, true, "first");
        VnfTemplateNicVO vnfNic2 = new VnfTemplateNicVO(templateId, 1L, "eth1", true, true, "second");
        VnfTemplateNicVO vnfNic3 = new VnfTemplateNicVO(templateId, 2L, "eth2", false, true, "third");
        Mockito.doReturn(Arrays.asList(vnfNic1, vnfNic2, vnfNic3)).when(vnfTemplateNicDao).listByTemplateId(templateId);

        when(template.getId()).thenReturn(templateId);
    }

    @Test
    public void testPersistVnfTemplateRegister() {
        RegisterVnfTemplateCmd cmd = new RegisterVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        vnfTemplateManagerImpl.persistVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(0)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdate() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdateWithoutNics() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfDetails", vnfDetails);
        ReflectionTestUtils.setField(cmd,"cleanupVnfNics", true);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(1)).deleteByTemplateId(templateId);
        Mockito.verify(vnfTemplateNicDao, Mockito.times(0)).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(5)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testPersistVnfTemplateUpdateWithoutDetails() {
        UpdateVnfTemplateCmd cmd = new UpdateVnfTemplateCmd();
        ReflectionTestUtils.setField(cmd,"vnfNics", vnfNics);
        ReflectionTestUtils.setField(cmd,"cleanupVnfDetails", true);

        vnfTemplateManagerImpl.updateVnfTemplate(templateId, cmd);

        Mockito.verify(vnfTemplateNicDao, Mockito.times(vnfNics.size())).persist(any(VnfTemplateNicVO.class));
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(1)).removeDetails(templateId);
        Mockito.verify(vnfTemplateDetailsDao, Mockito.times(0)).addDetail(eq(templateId), anyString(), anyString(), eq(true));
    }

    @Test
    public void testValidateVnfApplianceNicsWithRequiredNics() {
        List<Long> networkIds = Arrays.asList(200L, 201L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test
    public void testValidateVnfApplianceNicsWithAllNics() {
        List<Long> networkIds = Arrays.asList(200L, 201L, 202L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfApplianceNicsWithEmptyList() {
        List<Long> networkIds = new ArrayList<>();
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testValidateVnfApplianceNicsWithMissingNetworkId() {
        List<Long> networkIds = Arrays.asList(200L);
        vnfTemplateManagerImpl.validateVnfApplianceNics(template, networkIds);
    }

    @Test
    public void testGetManagementNetworkAndIp() {
        when(template.getId()).thenReturn(templateId);
        VnfTemplateNicVO vnfNic1 = new VnfTemplateNicVO(templateId, 0L, "eth0", true, true, "first");
        VnfTemplateNicVO vnfNic2 = new VnfTemplateNicVO(templateId, 1L, "eth1", true, false, "second");
        VnfTemplateNicVO vnfNic3 = new VnfTemplateNicVO(templateId, 2L, "eth2", false, false, "third");
        Mockito.doReturn(Arrays.asList(vnfNic1, vnfNic2, vnfNic3)).when(vnfTemplateNicDao).listByTemplateId(templateId);

        UserVm vm = Mockito.mock(UserVm.class);
        when(vm.getId()).thenReturn(vmId);
        NicVO nic1 = Mockito.mock(NicVO.class);
        NicVO nic2 = Mockito.mock(NicVO.class);
        NicVO nic3 = Mockito.mock(NicVO.class);
        when(nic1.getDeviceId()).thenReturn(0);
        when(nic1.getIPv4Address()).thenReturn(ipAddress);
        when(nic1.getNetworkId()).thenReturn(networkId);
        when(nic2.getDeviceId()).thenReturn(1);
        when(nic3.getDeviceId()).thenReturn(2);
        Mockito.doReturn(Arrays.asList(nic1, nic2, nic3)).when(nicDao).listByVmId(vmId);

        NetworkVO network = Mockito.mock(NetworkVO.class);
        when(network.getId()).thenReturn(networkId);
        when(network.getGuestType()).thenReturn(Network.GuestType.Isolated);
        when(network.getVpcId()).thenReturn(null);
        Mockito.doReturn(network).when(networkDao).findById(networkId);
        when(networkModel.areServicesSupportedInNetwork(networkId, Network.Service.StaticNat)).thenReturn(true);
        when(networkModel.areServicesSupportedInNetwork(networkId, Network.Service.Firewall)).thenReturn(true);

        Map<Network, String> networkAndIpMap = vnfTemplateManagerImpl.getManagementNetworkAndIp(template, vm);

        Assert.assertEquals(1, networkAndIpMap.size());
        Assert.assertTrue(networkAndIpMap.containsKey(network));
        Assert.assertTrue(networkAndIpMap.containsValue(ipAddress));
    }

    @Test
    public void testGetOpenPortsForVnfAppliance() {
        when(template.getId()).thenReturn(templateId);
        VnfTemplateDetailVO accessMethodsDetail = Mockito.mock(VnfTemplateDetailVO.class);
        when(accessMethodsDetail.getValue()).thenReturn("console,ssh-password,http,https");
        when(vnfTemplateDetailsDao.findDetail(templateId, VNF.AccessDetail.ACCESS_METHODS.name().toLowerCase())).thenReturn(accessMethodsDetail);

        VnfTemplateDetailVO sshPortDetail = Mockito.mock(VnfTemplateDetailVO.class);
        when(sshPortDetail.getValue()).thenReturn(String.valueOf(sshPort));
        when(vnfTemplateDetailsDao.findDetail(templateId, VNF.AccessDetail.SSH_PORT.name().toLowerCase())).thenReturn(sshPortDetail);

        VnfTemplateDetailVO httpPortDetail = Mockito.mock(VnfTemplateDetailVO.class);
        when(httpPortDetail.getValue()).thenReturn(String.valueOf(httpPort));
        when(vnfTemplateDetailsDao.findDetail(templateId, VNF.AccessDetail.HTTP_PORT.name().toLowerCase())).thenReturn(httpPortDetail);

        VnfTemplateDetailVO httpsPortDetail = Mockito.mock(VnfTemplateDetailVO.class);
        when(httpsPortDetail.getValue()).thenReturn(String.valueOf(httpsPort));
        when(vnfTemplateDetailsDao.findDetail(templateId, VNF.AccessDetail.HTTPS_PORT.name().toLowerCase())).thenReturn(httpsPortDetail);

        Set<Integer> ports = vnfTemplateManagerImpl.getOpenPortsForVnfAppliance(template);

        Assert.assertEquals(3, ports.size());
        Assert.assertTrue(ports.contains(sshPort));
        Assert.assertTrue(ports.contains(httpPort));
        Assert.assertTrue(ports.contains(httpsPort));
    }

    @Test
    public void testCreateSecurityGroupForVnfAppliance() {
        DataCenter zone = Mockito.mock(DataCenter.class);
        when(zone.isSecurityGroupEnabled()).thenReturn(true);

        DeployVnfApplianceCmd cmd = Mockito.mock(DeployVnfApplianceCmd.class);
        when(cmd.getVnfConfigureManagement()).thenReturn(true);
        when(cmd.getVnfCidrlist()).thenReturn(Arrays.asList("0.0.0.0/0"));

        Set<Integer> ports = new HashSet<>();
        ports.add(sshPort);
        ports.add(httpPort);
        ports.add(httpsPort);
        Mockito.doReturn(ports).when(vnfTemplateManagerImpl).getOpenPortsForVnfAppliance(template);

        Account owner = Mockito.mock(Account.class);
        when(owner.getDomainId()).thenReturn(1L);
        when(owner.getAccountName()).thenReturn("admin");

        SecurityGroupVO securityGroupVO = Mockito.mock(SecurityGroupVO.class);
        when(securityGroupVO.getId()).thenReturn(securityGroupId);
        Mockito.doReturn(securityGroupVO).when(securityGroupManager).createSecurityGroup(anyString(), anyString(), anyLong(), anyLong(), anyString());
        SecurityGroupRuleVO securityGroupRuleVO = Mockito.mock(SecurityGroupRuleVO.class);
        Mockito.doReturn(Arrays.asList(securityGroupRuleVO)).when(securityGroupService).authorizeSecurityGroupRule(anyLong(), anyString(), anyInt(), anyInt(),
                any(), any(), any(), any(), any());

        SecurityGroup result = vnfTemplateManagerImpl.createSecurityGroupForVnfAppliance(zone, template, owner, cmd);

        Assert.assertEquals(result, securityGroupVO);
        Mockito.verify(securityGroupService, Mockito.times(3)).authorizeSecurityGroupRule(anyLong(), anyString(), anyInt(), anyInt(),
                any(), any(), any(), any(), any());
    }

    @Test
    public void testCreateIsolatedNetworkRulesForVnfAppliance() throws InsufficientAddressCapacityException, ResourceUnavailableException,
            ResourceAllocationException, NetworkRuleConflictException {
        DataCenter zone = Mockito.mock(DataCenter.class);
        when(zone.getId()).thenReturn(zoneId);
        Account owner = Mockito.mock(Account.class);
        UserVm vm = Mockito.mock(UserVm.class);
        when(vm.getId()).thenReturn(vmId);
        DeployVnfApplianceCmd cmd = Mockito.mock(DeployVnfApplianceCmd.class);

        Map<Network, String> networkAndIpMap = new HashMap<>();
        NetworkVO network = Mockito.mock(NetworkVO.class);
        when(network.getId()).thenReturn(networkId);
        when(network.getVpcId()).thenReturn(null);
        networkAndIpMap.put(network, ipAddress);
        Mockito.doReturn(networkAndIpMap).when(vnfTemplateManagerImpl).getManagementNetworkAndIp(template, vm);

        Set<Integer> ports = new HashSet<>();
        ports.add(sshPort);
        ports.add(httpPort);
        ports.add(httpsPort);
        Mockito.doReturn(ports).when(vnfTemplateManagerImpl).getOpenPortsForVnfAppliance(template);

        FirewallRuleVO firewallRuleVO = Mockito.mock(FirewallRuleVO.class);

        IPAddressVO publicIp = Mockito.mock(IPAddressVO.class);
        when(publicIp.getId()).thenReturn(publicIpId);
        when(publicIp.isSourceNat()).thenReturn(true).thenReturn(false);
        Mockito.doReturn(publicIp).when(networkService).allocateIP(owner, zoneId, networkId, null, null);
        Mockito.doReturn(publicIp).when(ipAddressManager).associateIPToGuestNetwork(publicIpId, networkId, false);
        Mockito.doReturn(true).when(rulesService).enableStaticNat(publicIpId, vmId, networkId, ipAddress);
        when(firewallRulesDao.persist(any())).thenReturn(firewallRuleVO);
        Mockito.doReturn(true).when(firewallService).applyIngressFwRules(publicIpId, owner);

        vnfTemplateManagerImpl.createIsolatedNetworkRulesForVnfAppliance(zone, template, owner, vm, cmd);

        Mockito.verify(networkService, Mockito.times(2)).allocateIP(owner, zoneId, networkId, null, null);
        Mockito.verify(ipAddressManager, Mockito.times(2)).associateIPToGuestNetwork(publicIpId, networkId, false);
        Mockito.verify(rulesService, Mockito.times(1)).enableStaticNat(publicIpId, vmId, networkId, ipAddress);
        Mockito.verify(firewallRulesDao, Mockito.times(3)).persist(any());
        Mockito.verify(firewallService, Mockito.times(1)).applyIngressFwRules(publicIpId, owner);
    }
}
