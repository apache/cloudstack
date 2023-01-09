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
package com.cloud.server;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.user.address.ListPublicIpAddressesCmd;
import org.apache.cloudstack.api.command.user.ssh.RegisterSSHKeyPairCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.dc.Vlan.VlanType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.user.Account;
import com.cloud.user.SSHKeyPair;
import com.cloud.user.SSHKeyPairVO;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.dao.UserVmDetailsDao;

@RunWith(MockitoJUnitRunner.class)
public class ManagementServerImplTest {

    @Mock
    SearchCriteria<IPAddressVO> sc;

    @Mock
    RegisterSSHKeyPairCmd regCmd;

    @Mock
    SSHKeyPairVO existingPair;

    @Mock
    Account account;

    @Mock
    SSHKeyPairDao sshKeyPairDao;

    @Mock
    SSHKeyPair sshKeyPair;

    @Mock
    IpAddressManagerImpl ipAddressManagerImpl;

    @Spy
    ManagementServerImpl spy;

    ConfigKey mockConfig;

    @Mock
    UserVmDetailsDao userVmDetailsDao;

    @Mock
    HostDetailsDao hostDetailsDao;

    @Before
    public void setup() {
        mockConfig = Mockito.mock(ConfigKey.class);
        Whitebox.setInternalState(ipAddressManagerImpl.getClass(), "SystemVmPublicIpReservationModeStrictness", mockConfig);
        spy._UserVmDetailsDao = userVmDetailsDao;
        spy._detailsDao = hostDetailsDao;
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testDuplicateRegistraitons(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = spy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(account).when(spy).getCaller();
        Mockito.lenient().doReturn(account).when(spy).getOwner(regCmd);

        Mockito.doNothing().when(spy).checkForKeyByName(regCmd, account);
        Mockito.lenient().doReturn(accountName).when(regCmd).getAccountName();

        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn("name").when(regCmd).getName();

        spy._sshKeyPairDao = sshKeyPairDao;
        Mockito.doReturn(1L).when(account).getAccountId();
        Mockito.doReturn(1L).when(account).getDomainId();
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        lenient().when(sshKeyPairDao.findByName(1L, 1L, "name")).thenReturn(null).thenReturn(null);
        when(sshKeyPairDao.findByPublicKey(1L, 1L, publicKeyMaterial)).thenReturn(null).thenReturn(existingPair);

        spy.registerSSHKeyPair(regCmd);
        spy.registerSSHKeyPair(regCmd);
    }
    @Test
    public void testSuccess(){
        String accountName = "account";
        String publicKeyString = "ssh-rsa very public";
        String publicKeyMaterial = spy.getPublicKeyFromKeyKeyMaterial(publicKeyString);

        Mockito.lenient().doReturn(1L).when(account).getAccountId();
        Mockito.doReturn(1L).when(account).getAccountId();
        spy._sshKeyPairDao = sshKeyPairDao;


        //Mocking the DAO object functions - NO object found in DB
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByPublicKey(1L, 1L,publicKeyMaterial);
        Mockito.lenient().doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).findByName(1L, 1L, accountName);
        Mockito.doReturn(Mockito.mock(SSHKeyPairVO.class)).when(sshKeyPairDao).persist(any(SSHKeyPairVO.class));

        //Mocking the User Params
        Mockito.doReturn(accountName).when(regCmd).getName();
        Mockito.doReturn(publicKeyString).when(regCmd).getPublicKey();
        Mockito.doReturn(account).when(spy).getOwner(regCmd);

        spy.registerSSHKeyPair(regCmd);
        Mockito.verify(spy, Mockito.times(3)).getPublicKeyFromKeyKeyMaterial(anyString());
    }

    @Test
    public void setParametersTestWhenStateIsFreeAndSystemVmPublicIsTrue() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.TRUE);

        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(IpAddress.State.Free.name());
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", "Free");
        Mockito.verify(sc, Mockito.times(1)).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsFreeAndSystemVmPublicIsFalse() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.FALSE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(IpAddress.State.Free.name());
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.FALSE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.times(1)).setParameters("state", "Free");
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsFalse() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.FALSE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    @Test
    public void setParametersTestWhenStateIsNullAndSystemVmPublicIsTrue() {
        Mockito.when(mockConfig.value()).thenReturn(Boolean.TRUE);
        ListPublicIpAddressesCmd cmd = Mockito.mock(ListPublicIpAddressesCmd.class);
        Mockito.when(cmd.getNetworkId()).thenReturn(10L);
        Mockito.when(cmd.getZoneId()).thenReturn(null);
        Mockito.when(cmd.getIpAddress()).thenReturn(null);
        Mockito.when(cmd.getPhysicalNetworkId()).thenReturn(null);
        Mockito.when(cmd.getVlanId()).thenReturn(null);
        Mockito.when(cmd.getId()).thenReturn(null);
        Mockito.when(cmd.isSourceNat()).thenReturn(null);
        Mockito.when(cmd.isStaticNat()).thenReturn(null);
        Mockito.when(cmd.getState()).thenReturn(null);
        Mockito.when(cmd.getTags()).thenReturn(null);
        spy.setParameters(sc, cmd, VlanType.VirtualNetwork, Boolean.TRUE);

        Mockito.verify(sc, Mockito.times(1)).setJoinParameters("vlanSearch", "vlanType", VlanType.VirtualNetwork);
        Mockito.verify(sc, Mockito.times(1)).setParameters("display", false);
        Mockito.verify(sc, Mockito.times(1)).setParameters("sourceNetworkId", 10L);
        Mockito.verify(sc, Mockito.never()).setParameters("forsystemvms", false);
    }

    private UserVmVO mockFilterUefiHostsTestVm(String uefiValue) {
        UserVmVO vm = Mockito.mock(UserVmVO.class);
        Mockito.when(vm.getId()).thenReturn(1L);
        if (uefiValue == null) {
            Mockito.when(userVmDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(null);
        } else {
            UserVmDetailVO detail = new UserVmDetailVO(vm.getId(), ApiConstants.BootType.UEFI.toString(), uefiValue, true);
            Mockito.when(userVmDetailsDao.findDetail(vm.getId(), ApiConstants.BootType.UEFI.toString())).thenReturn(detail);
            Mockito.when(hostDetailsDao.findByName(Host.HOST_UEFI_ENABLE)).thenReturn(new ArrayList<>(List.of(new DetailVO(1l, Host.HOST_UEFI_ENABLE, "true"), new DetailVO(2l, Host.HOST_UEFI_ENABLE, "false"))));
        }
        return vm;
    }

    private List<HostVO> mockHostsList(Long filtered) {
        List<HostVO> hosts = new ArrayList<>();
        HostVO h1 = new HostVO("guid-1");
        ReflectionTestUtils.setField(h1, "id", 1L);
        hosts.add(h1);
        HostVO h2 = new HostVO("guid-2");
        ReflectionTestUtils.setField(h2, "id", 2L);
        hosts.add(h2);
        HostVO h3 = new HostVO("guid-3");
        ReflectionTestUtils.setField(h3, "id", 3L);
        hosts.add(h3);
        if (filtered != null) {
            hosts.removeIf(h -> h.getId() == filtered);
        }
        return hosts;
    }

    @Test
    public void testFilterUefiHostsForMigrationBiosVm() {
        UserVmVO vm = mockFilterUefiHostsTestVm(null);
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), new ArrayList<>(), vm);
        Assert.assertTrue(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationBiosVmFiltered() {
        List<HostVO> filteredHosts = mockHostsList(2L);
        UserVmVO vm = mockFilterUefiHostsTestVm(null);
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), filteredHosts, vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(2, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiInvalidValueVm() {
        UserVmVO vm = mockFilterUefiHostsTestVm("");
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), new ArrayList<>(), vm);
        Assert.assertTrue(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsUnavailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        List<HostVO> filteredHosts = new ArrayList<>();
        Mockito.when(hostDetailsDao.findByName(Host.HOST_UEFI_ENABLE)).thenReturn(new ArrayList<>());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), filteredHosts, vm);
        Assert.assertFalse(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsAvailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.LEGACY.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), null, vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(1, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationNoHostsAvailable() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(new ArrayList<>(), null, vm);
        Assert.assertFalse(result.first());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMHostsAvailableFiltered() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.LEGACY.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), mockHostsList(3L), vm);
        Assert.assertTrue(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(1, result.second().size());
    }

    @Test
    public void testFilterUefiHostsForMigrationUefiVMNoHostsAvailableFiltered() {
        UserVmVO vm = mockFilterUefiHostsTestVm(ApiConstants.BootMode.SECURE.toString());
        Pair<Boolean, List<HostVO>> result = spy.filterUefiHostsForMigration(mockHostsList(null), mockHostsList(1L), vm);
        Assert.assertFalse(result.first());
        Assert.assertNotNull(result.second());
        Assert.assertEquals(0, result.second().size());
    }
}
