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

package com.cloud.configuration;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.projects.ProjectManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.Ip;

public class ConfigurationManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConfigurationManagerTest.class);

    ConfigurationManagerImpl configurationMgr = new ConfigurationManagerImpl();

    DedicatePublicIpRangeCmd dedicatePublicIpRangesCmd = new DedicatePublicIpRangeCmdExtn();
    Class<?> _dedicatePublicIpRangeClass = dedicatePublicIpRangesCmd.getClass().getSuperclass();

    ReleasePublicIpRangeCmd releasePublicIpRangesCmd = new ReleasePublicIpRangeCmdExtn();
    Class<?> _releasePublicIpRangeClass = releasePublicIpRangesCmd.getClass().getSuperclass();

    @Mock AccountManager _accountMgr;
    @Mock ProjectManager _projectMgr;
    @Mock ResourceLimitService _resourceLimitMgr;
    @Mock NetworkManager _networkMgr;
    @Mock AccountDao _accountDao;
    @Mock VlanDao _vlanDao;
    @Mock AccountVlanMapDao _accountVlanMapDao;
    @Mock IPAddressDao _publicIpAddressDao;
    @Mock DataCenterDao _zoneDao;
    @Mock FirewallRulesDao _firewallDao;

    VlanVO vlan = new VlanVO(Vlan.VlanType.VirtualNetwork, "vlantag", "vlangateway","vlannetmask", 1L, "iprange", 1L, 1L, null, null, null);

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        configurationMgr._accountMgr = _accountMgr;
        configurationMgr._projectMgr = _projectMgr;
        configurationMgr._resourceLimitMgr = _resourceLimitMgr;
        configurationMgr._networkMgr = _networkMgr;
        configurationMgr._accountDao = _accountDao;
        configurationMgr._vlanDao = _vlanDao;
        configurationMgr._accountVlanMapDao = _accountVlanMapDao;
        configurationMgr._publicIpAddressDao = _publicIpAddressDao;
        configurationMgr._zoneDao = _zoneDao;
        configurationMgr._firewallDao = _firewallDao;

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(configurationMgr._accountMgr.getAccount(anyLong())).thenReturn(account);
        when(configurationMgr._accountDao.findActiveAccount(anyString(), anyLong())).thenReturn(account);
        when(configurationMgr._accountMgr.getActiveAccountById(anyLong())).thenReturn(account);

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());
        CallContext.register(user, account);

        when(configurationMgr._publicIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(1);

        doNothing().when(configurationMgr._resourceLimitMgr).checkResourceLimit(any(Account.class),
                any(ResourceType.class), anyLong());

        when(configurationMgr._accountVlanMapDao.persist(any(AccountVlanMapVO.class))).thenReturn(new AccountVlanMapVO());

        when(configurationMgr._vlanDao.acquireInLockTable(anyLong(), anyInt())).thenReturn(vlan);

        Field dedicateIdField = _dedicatePublicIpRangeClass.getDeclaredField("id");
        dedicateIdField.setAccessible(true);
        dedicateIdField.set(dedicatePublicIpRangesCmd, 1L);

        Field accountNameField = _dedicatePublicIpRangeClass.getDeclaredField("accountName");
        accountNameField.setAccessible(true);
        accountNameField.set(dedicatePublicIpRangesCmd, "accountname");

        Field projectIdField = _dedicatePublicIpRangeClass.getDeclaredField("projectId");
        projectIdField.setAccessible(true);
        projectIdField.set(dedicatePublicIpRangesCmd, null);

        Field domainIdField = _dedicatePublicIpRangeClass.getDeclaredField("domainId");
        domainIdField.setAccessible(true);
        domainIdField.set(dedicatePublicIpRangesCmd, 1L);

        Field releaseIdField = _releasePublicIpRangeClass.getDeclaredField("id");
        releaseIdField.setAccessible(true);
        releaseIdField.set(releasePublicIpRangesCmd, 1L);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testDedicatePublicIpRange() throws Exception {

        s_logger.info("Running tests for DedicatePublicIpRange API");

        /*
         * TEST 1: given valid parameters DedicatePublicIpRange should succeed
         */
        runDedicatePublicIpRangePostiveTest();

        /*
         * TEST 2: given invalid public ip range DedicatePublicIpRange should fail
         */
        runDedicatePublicIpRangeInvalidRange();
         /*
         * TEST 3: given public IP range that is already dedicated to a different account DedicatePublicIpRange should fail
         */
        runDedicatePublicIpRangeDedicatedRange();

         /*
         * TEST 4: given zone is of type Basic DedicatePublicIpRange should fail
         */
        runDedicatePublicIpRangeInvalidZone();

        /*
         * TEST 5: given range is already allocated to a different account DedicatePublicIpRange should fail
         */
        runDedicatePublicIpRangeIPAdressAllocated();
    }

    @Test
    public void testReleasePublicIpRange() throws Exception {

        s_logger.info("Running tests for DedicatePublicIpRange API");

        /*
         * TEST 1: given valid parameters and no allocated public ip's in the range ReleasePublicIpRange should succeed
         */
        runReleasePublicIpRangePostiveTest1();

        /*
         * TEST 2: given valid parameters ReleasePublicIpRange should succeed
         */
        runReleasePublicIpRangePostiveTest2();

        /*
         * TEST 3: given range doesn't exist
         */
        runReleasePublicIpRangeInvalidIpRange();

        /*
         * TEST 4: given range is not dedicated to any account
         */
        runReleaseNonDedicatedPublicIpRange();
    }

    void runDedicatePublicIpRangePostiveTest() throws Exception {
        Transaction txn = Transaction.open("runDedicatePublicIpRangePostiveTest");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByAccount(anyLong())).thenReturn(null);

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
                null, null, NetworkType.Advanced, null, null, true,  true, null, null);
        when(configurationMgr._zoneDao.findById(anyLong())).thenReturn(dc);

        List<IPAddressVO> ipAddressList = new ArrayList<IPAddressVO>();
        IPAddressVO ipAddress = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        ipAddressList.add(ipAddress);
        when(configurationMgr._publicIpAddressDao.listByVlanId(anyLong())).thenReturn(ipAddressList);

        try {
            Vlan result = configurationMgr.dedicatePublicIpRange(dedicatePublicIpRangesCmd);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing runDedicatePublicIpRangePostiveTest message: " + e.toString());
        } finally {
            txn.close("runDedicatePublicIpRangePostiveTest");
        }
    }

    void runDedicatePublicIpRangeInvalidRange() throws Exception {
        Transaction txn = Transaction.open("runDedicatePublicIpRangeInvalidRange");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(null);
        try {
            configurationMgr.dedicatePublicIpRange(dedicatePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unable to find vlan by id"));
        } finally {
            txn.close("runDedicatePublicIpRangeInvalidRange");
        }
    }

    void runDedicatePublicIpRangeDedicatedRange() throws Exception {
        Transaction txn = Transaction.open("runDedicatePublicIpRangeDedicatedRange");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        // public ip range is already dedicated
        List<AccountVlanMapVO> accountVlanMaps = new ArrayList<AccountVlanMapVO>();
        AccountVlanMapVO accountVlanMap = new AccountVlanMapVO(1, 1);
        accountVlanMaps.add(accountVlanMap);
        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(accountVlanMaps);

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
                null, null, NetworkType.Advanced, null, null, true,  true, null, null);
        when(configurationMgr._zoneDao.findById(anyLong())).thenReturn(dc);

        List<IPAddressVO> ipAddressList = new ArrayList<IPAddressVO>();
        IPAddressVO ipAddress = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        ipAddressList.add(ipAddress);
        when(configurationMgr._publicIpAddressDao.listByVlanId(anyLong())).thenReturn(ipAddressList);

        try {
            configurationMgr.dedicatePublicIpRange(dedicatePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Public IP range has already been dedicated"));
        } finally {
            txn.close("runDedicatePublicIpRangePublicIpRangeDedicated");
        }
    }

    void runDedicatePublicIpRangeInvalidZone() throws Exception {
        Transaction txn = Transaction.open("runDedicatePublicIpRangeInvalidZone");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(null);

        // public ip range belongs to zone of type basic
        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
                null, null, NetworkType.Basic, null, null, true,  true, null, null);
        when(configurationMgr._zoneDao.findById(anyLong())).thenReturn(dc);

        List<IPAddressVO> ipAddressList = new ArrayList<IPAddressVO>();
        IPAddressVO ipAddress = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        ipAddressList.add(ipAddress);
        when(configurationMgr._publicIpAddressDao.listByVlanId(anyLong())).thenReturn(ipAddressList);

        try {
            configurationMgr.dedicatePublicIpRange(dedicatePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Public IP range can be dedicated to an account only in the zone of type Advanced"));
        } finally {
            txn.close("runDedicatePublicIpRangeInvalidZone");
        }
    }

    void runDedicatePublicIpRangeIPAdressAllocated() throws Exception {
        Transaction txn = Transaction.open("runDedicatePublicIpRangeIPAdressAllocated");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByAccount(anyLong())).thenReturn(null);

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null,  "10.0.0.1/24",
                null, null, NetworkType.Advanced, null, null, true,  true, null, null);
        when(configurationMgr._zoneDao.findById(anyLong())).thenReturn(dc);

        // one of the ip addresses of the range is allocated to different account
        List<IPAddressVO> ipAddressList = new ArrayList<IPAddressVO>();
        IPAddressVO ipAddress = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        ipAddress.setAllocatedToAccountId(1L);
        ipAddressList.add(ipAddress);
        when(configurationMgr._publicIpAddressDao.listByVlanId(anyLong())).thenReturn(ipAddressList);

        try {
            configurationMgr.dedicatePublicIpRange(dedicatePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Public IP address in range is allocated to another account"));
        } finally {
            txn.close("runDedicatePublicIpRangeIPAdressAllocated");
        }
    }

    void runReleasePublicIpRangePostiveTest1() throws Exception {
        Transaction txn = Transaction.open("runReleasePublicIpRangePostiveTest1");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        List<AccountVlanMapVO> accountVlanMaps = new ArrayList<AccountVlanMapVO>();
        AccountVlanMapVO accountVlanMap = new AccountVlanMapVO(1, 1);
        accountVlanMaps.add(accountVlanMap);
        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(accountVlanMaps);

        // no allocated ip's
        when(configurationMgr._publicIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);

        when(configurationMgr._accountVlanMapDao.remove(anyLong())).thenReturn(true);
        try {
            Boolean result = configurationMgr.releasePublicIpRange(releasePublicIpRangesCmd);
            Assert.assertTrue(result);
        } catch (Exception e) {
            s_logger.info("exception in testing runReleasePublicIpRangePostiveTest1 message: " + e.toString());
        } finally {
            txn.close("runReleasePublicIpRangePostiveTest1");
        }
    }

    void runReleasePublicIpRangePostiveTest2() throws Exception {
        Transaction txn = Transaction.open("runReleasePublicIpRangePostiveTest2");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        List<AccountVlanMapVO> accountVlanMaps = new ArrayList<AccountVlanMapVO>();
        AccountVlanMapVO accountVlanMap = new AccountVlanMapVO(1, 1);
        accountVlanMaps.add(accountVlanMap);
        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(accountVlanMaps);

        when(configurationMgr._publicIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(1);

        List<IPAddressVO> ipAddressList = new ArrayList<IPAddressVO>();
        IPAddressVO ipAddress = new IPAddressVO(new Ip("75.75.75.75"), 1, 0xaabbccddeeffL, 10, false);
        ipAddressList.add(ipAddress);
        when(configurationMgr._publicIpAddressDao.listByVlanId(anyLong())).thenReturn(ipAddressList);

        when(configurationMgr._firewallDao.countRulesByIpId(anyLong())).thenReturn(0L);

        when(configurationMgr._networkMgr.disassociatePublicIpAddress(anyLong(), anyLong(), any(Account.class))).thenReturn(true);

        when(configurationMgr._vlanDao.releaseFromLockTable(anyLong())).thenReturn(true);

        when(configurationMgr._accountVlanMapDao.remove(anyLong())).thenReturn(true);
        try {
            Boolean result = configurationMgr.releasePublicIpRange(releasePublicIpRangesCmd);
            Assert.assertTrue(result);
        } catch (Exception e) {
            s_logger.info("exception in testing runReleasePublicIpRangePostiveTest2 message: " + e.toString());
        } finally {
            txn.close("runReleasePublicIpRangePostiveTest2");
        }
    }

    void runReleasePublicIpRangeInvalidIpRange() throws Exception {
        Transaction txn = Transaction.open("runReleasePublicIpRangeInvalidIpRange");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(null);
        try {
            configurationMgr.releasePublicIpRange(releasePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Please specify a valid IP range id"));
        } finally {
            txn.close("runReleasePublicIpRangeInvalidIpRange");
        }
    }

    void runReleaseNonDedicatedPublicIpRange() throws Exception {
        Transaction txn = Transaction.open("runReleaseNonDedicatedPublicIpRange");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(null);
        try {
            configurationMgr.releasePublicIpRange(releasePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("as it not dedicated to any account"));
        } finally {
            txn.close("runReleaseNonDedicatedPublicIpRange");
        }
    }


    public class DedicatePublicIpRangeCmdExtn extends DedicatePublicIpRangeCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class ReleasePublicIpRangeCmdExtn extends ReleasePublicIpRangeCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }
}
