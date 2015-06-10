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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.cloud.user.User;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Network.Capability;
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
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.Ip;

public class ConfigurationManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConfigurationManagerTest.class);

    ConfigurationManagerImpl configurationMgr = new ConfigurationManagerImpl();

    DedicatePublicIpRangeCmd dedicatePublicIpRangesCmd = new DedicatePublicIpRangeCmdExtn();
    Class<?> _dedicatePublicIpRangeClass = dedicatePublicIpRangesCmd.getClass().getSuperclass();

    ReleasePublicIpRangeCmd releasePublicIpRangesCmd = new ReleasePublicIpRangeCmdExtn();
    Class<?> _releasePublicIpRangeClass = releasePublicIpRangesCmd.getClass().getSuperclass();

    @Mock
    AccountManager _accountMgr;
    @Mock
    ProjectManager _projectMgr;
    @Mock
    ResourceLimitService _resourceLimitMgr;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    AccountDao _accountDao;
    @Mock
    VlanDao _vlanDao;
    @Mock
    AccountVlanMapDao _accountVlanMapDao;
    @Mock
    IPAddressDao _publicIpAddressDao;
    @Mock
    DataCenterDao _zoneDao;
    @Mock
    FirewallRulesDao _firewallDao;
    @Mock
    IpAddressManager _ipAddrMgr;
    @Mock
    NetworkModel _networkModel;

    VlanVO vlan = new VlanVO(Vlan.VlanType.VirtualNetwork, "vlantag", "vlangateway", "vlannetmask", 1L, "iprange", 1L, 1L, null, null, null);

    @Mock
    Network network;

    @Mock
    Account account;

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
        configurationMgr._ipAddrMgr = _ipAddrMgr;
        configurationMgr._networkModel = _networkModel;

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short)0, UUID.randomUUID().toString());
        when(configurationMgr._accountMgr.getAccount(anyLong())).thenReturn(account);
        when(configurationMgr._accountDao.findActiveAccount(anyString(), anyLong())).thenReturn(account);
        when(configurationMgr._accountMgr.getActiveAccountById(anyLong())).thenReturn(account);

        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString(), User.Source.UNKNOWN);
        CallContext.register(user, account);

        when(configurationMgr._publicIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(1);

        doNothing().when(configurationMgr._resourceLimitMgr).checkResourceLimit(any(Account.class), any(ResourceType.class), anyLong());

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
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangePostiveTest");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByAccount(anyLong())).thenReturn(null);

        DataCenterVO dc =
            new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null, true,
                true, null, null, null, null);
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
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangeInvalidRange");

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
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangeDedicatedRange");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        // public ip range is already dedicated
        List<AccountVlanMapVO> accountVlanMaps = new ArrayList<AccountVlanMapVO>();
        AccountVlanMapVO accountVlanMap = new AccountVlanMapVO(1, 1);
        accountVlanMaps.add(accountVlanMap);
        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(accountVlanMaps);

        DataCenterVO dc =
            new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null, true,
                true, null, null, null, null);
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
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangeInvalidZone");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByVlan(anyLong())).thenReturn(null);

        // public ip range belongs to zone of type basic
        DataCenterVO dc =
            new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Basic, null, null, true,
                true, null, null, null, null);
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
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangeIPAdressAllocated");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByAccount(anyLong())).thenReturn(null);

        DataCenterVO dc =
            new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null, true,
                true, null, null, null, null);
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
        TransactionLegacy txn = TransactionLegacy.open("runReleasePublicIpRangePostiveTest1");

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
        TransactionLegacy txn = TransactionLegacy.open("runReleasePublicIpRangePostiveTest2");

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

        when(configurationMgr._ipAddrMgr.disassociatePublicIpAddress(anyLong(), anyLong(), any(Account.class))).thenReturn(true);

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
        TransactionLegacy txn = TransactionLegacy.open("runReleasePublicIpRangeInvalidIpRange");

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
        TransactionLegacy txn = TransactionLegacy.open("runReleaseNonDedicatedPublicIpRange");

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

    @Test
    public void validateEmptyStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();

        configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
    }

    @Test
    public void validateInvalidStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();
        staticNatServiceCapabilityMap.put(Capability.AssociatePublicIP, "Frue and Talse");

        boolean caught = false;
        try {
            configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("(frue and talse)"));
            caught = true;
        }
        Assert.assertTrue("should not be accepted", caught);
    }

    @Test
    public void validateTTStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();
        staticNatServiceCapabilityMap.put(Capability.AssociatePublicIP, "true and Talse");
        staticNatServiceCapabilityMap.put(Capability.ElasticIp, "True");

        configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
    }

    @Test
    public void validateFTStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();
        staticNatServiceCapabilityMap.put(Capability.AssociatePublicIP, "false");
        staticNatServiceCapabilityMap.put(Capability.ElasticIp, "True");

        configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
    }

    @Test
    public void validateTFStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();
        staticNatServiceCapabilityMap.put(Capability.AssociatePublicIP, "true and Talse");
        staticNatServiceCapabilityMap.put(Capability.ElasticIp, "false");

        boolean caught = false;
        try {
            configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
        } catch (InvalidParameterValueException e) {
            Assert.assertTrue(
                e.getMessage(),
                e.getMessage().contains(
                    "Capability " + Capability.AssociatePublicIP.getName() + " can only be set when capability " + Capability.ElasticIp.getName() + " is true"));
            caught = true;
        }
        Assert.assertTrue("should not be accepted", caught);
    }

    @Test
    public void validateFFStaticNatServiceCapablitiesTest() {
        Map<Capability, String> staticNatServiceCapabilityMap = new HashMap<Capability, String>();
        staticNatServiceCapabilityMap.put(Capability.AssociatePublicIP, "false");
        staticNatServiceCapabilityMap.put(Capability.ElasticIp, "False");

        configurationMgr.validateStaticNatServiceCapablities(staticNatServiceCapabilityMap);
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

    @Test
    public void getVlanAccount() {
        Mockito.when(_vlanDao.findById(42l)).thenReturn(vlan);
        Mockito.when(_networkModel.getNetwork(1l)).thenReturn(network);
        Mockito.when(network.getAccountId()).thenReturn(1l);
        Mockito.when(_accountMgr.getAccount(1l)).thenReturn(account);
        Assert.assertNotNull(configurationMgr.getVlanAccount(42l));
    }

    @Test
    public void validateIp6Parameters() {

        //Validate Zone create
            //** with no IPv6
            validateIp6ParameterTest(false, null, null, null, null, true, true, true, "Zone create with no IPv6 parameters should be accepted");

            //** with invalid IPv6 DNS1 and invalid IPv6 DNS2
            validateIp6ParameterTest(true, "8.8.8.8", "8.8.4.4", null, null, true, true, true, "Zone create with invalid IPv6 DNS1/DNS1 parameters should not be accepted");

            //** with valid IPv6 DNS1 and DNS2
            validateIp6ParameterTest(false, "2620:0:ccc::2", "2620:0:ccd::2", null, null, true, true, true, "Zone create with valid IPv6 DNS1/DNS1 parameters should be accepted");

            //** with valid IPv6 DNS and invalid IPv6 Super Cidr
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "8.8.8.8", null, true, true, true, "Zone create with valid IPv6 DNS1/DNS1 and invalid IPv6 Super CIDR parameters should not be accepted");

            //** with valid IPv6 DNS, valid IPv6 Super Cidr and invalid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "6500", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, IPv6 Super CIDR and invalid asNumber parameters should not be accepted");

            //** with valid IPv6 DNS, valid IPv6 Super Cidr and no asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", null, true, true, true, "Zone create with valid IPv6 DNS1/DNS1, IPv6 Super CIDR and no asNumber parameters should not be accepted");

            //** with valid IPv6 DNS, invalid IPv6 Super Cidr and valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0:0/50", "65000", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, invalid IPv6 Super CIDR and valid asNumber parameters should not be accepted");

            //** with valid IPv6 DNS, no IPv6 Super Cidr and valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "", "65000", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, no IPv6 Super CIDR and valid asNumber parameters should not be accepted");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(null);
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(null);
            //** with valid IPv6 DNS, Unique valid IPv6 Super Cidr and Unique valid asNumber
            validateIp6ParameterTest(false, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, unique IPv6 Super CIDR and asNumber parameters should be accepted");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(null);
            //** with valid IPv6 DNS, duplicate valid IPv6 Super Cidr and Unique valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, with duplicate IPv6 Super CIDR and unique asNumber parameters should not be accepted");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(null);
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            //** with valid IPv6 DNS, Unique valid IPv6 Super Cidr and duplicate valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", true, true, true, "Zone create with valid IPv6 DNS1/DNS1, with unique IPv6 Super CIDR and duplicate asNumber parameters should not be accepted");

        //Validate Zone Edit which check no duplicate

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(null);
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(null);
            //** with valid IPv6 DNS, Unique valid IPv6 Super Cidr and Unique valid asNumber
            validateIp6ParameterTest(false, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", false, false, false, "Zone create with valid IPv6 DNS1/DNS1, unique IPv6 Super CIDR and asNumber parameters should be accepted with no duplicate checks");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            //** with valid IPv6 DNS, duplicate valid IPv6 Super Cidr and duplicate valid asNumber, with no duplicate checks.
            validateIp6ParameterTest(false, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", false, false, false, "Zone create with valid IPv6 DNS1/DNS1, duplicate IPv6 Super CIDR and asNumber parameters should be accepted with no duplicate checks");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(null);
            //** with valid IPv6 DNS, duplicate valid IPv6 Super Cidr and Unique valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", false, true, false, "Zone create with valid IPv6 DNS1/DNS1, duplicate IPv6 Super CIDR and unique asNumber parameters should not be accepted with no duplicate checks");

            when(configurationMgr._zoneDao.findByIp6SuperCidr(anyString())).thenReturn(null);
            when(configurationMgr._zoneDao.findByAsn(anyString())).thenReturn(new DataCenterVO(0l, null, null, null, null, null, null, null, null, null, null, null, null));
            //** with valid IPv6 DNS, Unique valid IPv6 Super Cidr and duplicate valid asNumber
            validateIp6ParameterTest(true, "2620:0:ccc::2", "2620:0:ccd::2", "2001:67c:2834:0:0:0:0:0/50", "65000", false, false, true, "Zone create with valid IPv6 DNS1/DNS1, unique IPv6 Super CIDR and duplicate asNumber parameters should not be accepted");

    }

    private void validateIp6ParameterTest(boolean shouldConditionFail, String ip6Dns1, String ip6Dns2, String ip6SuperCidr, String asNumber, boolean checkDuplicateName,
            boolean checkDuplicateIp6SuperCidr, boolean checkDuplicateAsn, String message) {
        boolean validationCheck = shouldConditionFail;
        try {
            configurationMgr.validateIp6Parameters(ip6Dns1, ip6Dns2, ip6SuperCidr, asNumber, checkDuplicateName, checkDuplicateIp6SuperCidr, checkDuplicateAsn);
        } catch (InvalidParameterValueException e) {
            validationCheck = !shouldConditionFail;
        }
        Assert.assertFalse(message, validationCheck);
    }
}
