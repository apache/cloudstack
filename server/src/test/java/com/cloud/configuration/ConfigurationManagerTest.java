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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.cloudstack.api.command.admin.vlan.DedicatePublicIpRangeCmd;
import org.apache.cloudstack.api.command.admin.vlan.ReleasePublicIpRangeCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworkOfferingsCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.cloud.api.query.dao.NetworkOfferingJoinDao;
import com.cloud.api.query.vo.NetworkOfferingJoinVO;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterIpAddressDao;
import com.cloud.dc.dao.DomainVlanMapDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.projects.ProjectManager;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.VMInstanceDao;

public class ConfigurationManagerTest {

    private static final Logger s_logger = Logger.getLogger(ConfigurationManagerTest.class);

    @Spy
    @InjectMocks
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
    NetworkOfferingJoinDao networkOfferingJoinDao;
    @Mock
    AccountDao _accountDao;
    @Mock
    VlanDao _vlanDao;
    @Mock
    AccountVlanMapDao _accountVlanMapDao;
    @Mock
    DomainVlanMapDao _domainVlanMapDao;
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
    @Mock
    DataCenterIpAddressDao _privateIpAddressDao;
    @Mock
    VolumeDao _volumeDao;
    @Mock
    HostDao _hostDao;
    @Mock
    VMInstanceDao _vmInstanceDao;
    @Mock
    ClusterDao _clusterDao;
    @Mock
    HostPodDao _podDao;
    @Mock
    PhysicalNetworkDao _physicalNetworkDao;
    @Mock
    ImageStoreDao _imageStoreDao;
    @Mock
    ConfigurationDao _configDao;
    @Mock
    DiskOfferingVO diskOfferingVOMock;

    VlanVO vlan = new VlanVO(Vlan.VlanType.VirtualNetwork, "vlantag", "vlangateway", "vlannetmask", 1L, "iprange", 1L, 1L, null, null, null);

    private static final String MAXIMUM_DURATION_ALLOWED = "3600";
    @Mock
    Network network;
    @Mock
    Account account;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

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
        runDedicatePublicIpRangeIPAddressAllocated();
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

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null,
                true, true, null, null);
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

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null,
                true, true, null, null);
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
        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Basic, null, null, true,
                true, null, null);
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

    void runDedicatePublicIpRangeIPAddressAllocated() throws Exception {
        TransactionLegacy txn = TransactionLegacy.open("runDedicatePublicIpRangeIPAddressAllocated");

        when(configurationMgr._vlanDao.findById(anyLong())).thenReturn(vlan);

        when(configurationMgr._accountVlanMapDao.listAccountVlanMapsByAccount(anyLong())).thenReturn(null);

        DataCenterVO dc = new DataCenterVO(UUID.randomUUID().toString(), "test", "8.8.8.8", null, "10.0.0.1", null, "10.0.0.1/24", null, null, NetworkType.Advanced, null, null,
                true, true, null, null);
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
            txn.close("runDedicatePublicIpRangeIPAddressAllocated");
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
        when(configurationMgr._domainVlanMapDao.listDomainVlanMapsByVlan(anyLong())).thenReturn(null);
        try {
            configurationMgr.releasePublicIpRange(releasePublicIpRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("as it not dedicated to any domain and any account"));
        } finally {
            txn.close("runReleaseNonDedicatedPublicIpRange");
        }
    }

    @Test
    public void searchForNetworkOfferingsTest() {
        NetworkOfferingJoinVO forVpcOfferingJoinVO = new NetworkOfferingJoinVO();
        forVpcOfferingJoinVO.setForVpc(true);
        List<NetworkOfferingJoinVO> offerings = Arrays.asList(new NetworkOfferingJoinVO(), new NetworkOfferingJoinVO(), forVpcOfferingJoinVO);

        Mockito.when(networkOfferingJoinDao.createSearchCriteria()).thenReturn(Mockito.mock(SearchCriteria.class));
        Mockito.when(networkOfferingJoinDao.search(Mockito.any(SearchCriteria.class), Mockito.any(Filter.class))).thenReturn(offerings);

        ListNetworkOfferingsCmd cmd = Mockito.spy(ListNetworkOfferingsCmd.class);
        Mockito.when(cmd.getPageSize()).thenReturn(10);

        assertThat(configurationMgr.searchForNetworkOfferings(cmd).second(), is(3));

        Mockito.when(cmd.getForVpc()).thenReturn(Boolean.FALSE);
        assertThat(configurationMgr.searchForNetworkOfferings(cmd).second(), is(2));
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
    public void checkIfPodIsDeletableSuccessTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(new ArrayList<ClusterVO>());

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfPodIsDeletableFailureOnPrivateIpAddressTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(1);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(new ArrayList<ClusterVO>());

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfPodIsDeletableFailureOnVolumeTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        ArrayList<VolumeVO> arrayList = new ArrayList<VolumeVO>();
        arrayList.add(volumeVO);
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(arrayList);
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(new ArrayList<ClusterVO>());

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfPodIsDeletableFailureOnHostTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        HostVO hostVO = Mockito.mock(HostVO.class);
        ArrayList<HostVO> arrayList = new ArrayList<HostVO>();
        arrayList.add(hostVO);
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(arrayList);
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(new ArrayList<ClusterVO>());

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfPodIsDeletableFailureOnVmInstanceTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        VMInstanceVO vMInstanceVO = Mockito.mock(VMInstanceVO.class);
        ArrayList<VMInstanceVO> arrayList = new ArrayList<VMInstanceVO>();
        arrayList.add(vMInstanceVO);
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(arrayList);
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(new ArrayList<ClusterVO>());

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfPodIsDeletableFailureOnClusterTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        Mockito.when(hostPodVO.getDataCenterId()).thenReturn(new Random().nextLong());
        Mockito.when(_podDao.findById(anyLong())).thenReturn(hostPodVO);

        ClusterVO clusterVO = Mockito.mock(ClusterVO.class);
        ArrayList<ClusterVO> arrayList = new ArrayList<ClusterVO>();
        arrayList.add(clusterVO);
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_volumeDao.findByPod(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_hostDao.findByPodId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_vmInstanceDao.listByPodId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_clusterDao.listByPodId(anyLong())).thenReturn(arrayList);

        configurationMgr.checkIfPodIsDeletable(new Random().nextLong());
    }

    @Test
    public void checkIfZoneIsDeletableSuccessTest() {
        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnHostTest() {
        HostVO hostVO = Mockito.mock(HostVO.class);
        ArrayList<HostVO> arrayList = new ArrayList<HostVO>();
        arrayList.add(hostVO);

        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(arrayList);
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnPodTest() {
        HostPodVO hostPodVO = Mockito.mock(HostPodVO.class);
        ArrayList<HostPodVO> arrayList = new ArrayList<HostPodVO>();
        arrayList.add(hostPodVO);

        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(arrayList);
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnPrivateIpAddressTest() {
        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(1);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnPublicIpAddressTest() {
        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(1);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnVmInstanceTest() {
        VMInstanceVO vMInstanceVO = Mockito.mock(VMInstanceVO.class);
        ArrayList<VMInstanceVO> arrayList = new ArrayList<VMInstanceVO>();
        arrayList.add(vMInstanceVO);

        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(arrayList);
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnVolumeTest() {
        VolumeVO volumeVO = Mockito.mock(VolumeVO.class);
        ArrayList<VolumeVO> arrayList = new ArrayList<VolumeVO>();
        arrayList.add(volumeVO);

        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(arrayList);
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(new ArrayList<PhysicalNetworkVO>());
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test(expected = CloudRuntimeException.class)
    public void checkIfZoneIsDeletableFailureOnPhysicalNetworkTest() {
        PhysicalNetworkVO physicalNetworkVO = Mockito.mock(PhysicalNetworkVO.class);
        ArrayList<PhysicalNetworkVO> arrayList = new ArrayList<PhysicalNetworkVO>();
        arrayList.add(physicalNetworkVO);

        Mockito.when(_hostDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostVO>());
        Mockito.when(_podDao.listByDataCenterId(anyLong())).thenReturn(new ArrayList<HostPodVO>());
        Mockito.when(_privateIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_publicIpAddressDao.countIPs(anyLong(), anyBoolean())).thenReturn(0);
        Mockito.when(_vmInstanceDao.listByZoneId(anyLong())).thenReturn(new ArrayList<VMInstanceVO>());
        Mockito.when(_volumeDao.findByDc(anyLong())).thenReturn(new ArrayList<VolumeVO>());
        Mockito.when(_physicalNetworkDao.listByZone(anyLong())).thenReturn(arrayList);
        Mockito.when(_imageStoreDao.findByZone(any(ZoneScope.class), anyBoolean())).thenReturn(new ArrayList<ImageStoreVO>());

        configurationMgr.checkIfZoneIsDeletable(new Random().nextLong());
    }

    @Test
    public void hasSameSubnetTest() {
        //Ipv4 Test
        boolean result;
        result = configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, false, null, null, null, null, null);
        Assert.assertFalse(result);
        try {
            configurationMgr.hasSameSubnet(true, "10.0.0.1", "255.255.255.0", "10.0.0.2", "255.255.255.0", "10.0.0.2", "10.0.0.10", false, null, null, null, null, null);
            Assert.fail();
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), "The gateway of the subnet should be unique. The subnet already has a gateway 10.0.0.1");
        }
        try {
            configurationMgr.hasSameSubnet(true, "10.0.0.1", "255.255.0.0", "10.0.0.2", "255.255.255.0", "10.0.0.2", "10.0.0.10", false, null, null, null, null, null);
            Assert.fail();
        } catch (InvalidParameterValueException e){
            Assert.assertEquals(e.getMessage(), "The subnet you are trying to add is a subset of the existing subnet having gateway 10.0.0.1 and netmask 255.255.0.0");
        }
        try {
            configurationMgr.hasSameSubnet(true, "10.0.0.1", "255.255.255.0", "10.0.0.2", "255.255.0.0", "10.0.0.2", "10.0.0.10", false, null, null, null, null, null);
            Assert.fail();
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), "The subnet you are trying to add is a superset of the existing subnet having gateway 10.0.0.1 and netmask 255.255.255.0");
        }
        result = configurationMgr.hasSameSubnet(true, "10.0.0.1", "255.255.255.0", "10.0.0.1", "255.255.255.0", "10.0.0.2", "10.0.0.10", false, null, null, null, null, null);
        Assert.assertTrue(result);

        //Ipv6 Test
        Network ipV6Network = mock(Network.class);
        when(ipV6Network.getIp6Gateway()).thenReturn("2001:db8:0:f101::1");
        when(ipV6Network.getIp6Cidr()).thenReturn("2001:db8:0:f101::0/64");
        doThrow(new InvalidParameterValueException("Exception from Mock: startIPv6 is not in ip6cidr indicated network!")).when(configurationMgr._networkModel).checkIp6Parameters("2001:db9:0:f101::2", "2001:db9:0:f101::a", "2001:db8:0:f101::1", "2001:db8:0:f101::0/64");
        doThrow(new InvalidParameterValueException("Exception from Mock: endIPv6 is not in ip6cidr indicated network!")).when(configurationMgr._networkModel).checkIp6Parameters("2001:db8:0:f101::a", "2001:db9:0:f101::2", "2001:db8:0:f101::1", "2001:db8:0:f101::0/64");
        doThrow(new InvalidParameterValueException("ip6Gateway and ip6Cidr should be defined when startIPv6/endIPv6 are passed in")).when(configurationMgr._networkModel).checkIp6Parameters(Mockito.anyString(), Mockito.anyString(), Mockito.isNull(String.class), Mockito.isNull(String.class));

        configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, "2001:db8:0:f101::1", "2001:db8:0:f101::0/64", "2001:db8:0:f101::2", "2001:db8:0:f101::a", ipV6Network);
        Assert.assertTrue(result);
        try {
            configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, "2001:db8:0:f101::2", "2001:db8:0:f101::0/64", "2001:db8:0:f101::2", "2001:db8:0:f101::a", ipV6Network);
            Assert.fail();
        } catch (InvalidParameterValueException e){
            Assert.assertEquals(e.getMessage(), "The input gateway 2001:db8:0:f101::2 is not same as network gateway 2001:db8:0:f101::1");
        }
        try {
            configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, "2001:db8:0:f101::1", "2001:db8:0:f101::0/63", "2001:db8:0:f101::2", "2001:db8:0:f101::a", ipV6Network);
            Assert.fail();
        } catch (InvalidParameterValueException e){
            Assert.assertEquals(e.getMessage(), "The input cidr 2001:db8:0:f101::0/63 is not same as network cidr 2001:db8:0:f101::0/64");
        }

        try {
            configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, "2001:db8:0:f101::1", "2001:db8:0:f101::0/64", "2001:db9:0:f101::2", "2001:db9:0:f101::a", ipV6Network);
            Assert.fail();
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), "Exception from Mock: startIPv6 is not in ip6cidr indicated network!");
        }
        try {
            configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, "2001:db8:0:f101::1", "2001:db8:0:f101::0/64", "2001:db8:0:f101::a", "2001:db9:0:f101::2", ipV6Network);
            Assert.fail();
        } catch(InvalidParameterValueException e) {
            Assert.assertEquals(e.getMessage(), "Exception from Mock: endIPv6 is not in ip6cidr indicated network!");
        }

        result = configurationMgr.hasSameSubnet(false, null, null, null, null, null, null, true, null, null, "2001:db8:0:f101::2", "2001:db8:0:f101::a", ipV6Network);
        Assert.assertTrue(result);
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetVlanNumberFromUriInvalidParameter() {
        configurationMgr.getVlanNumberFromUri("vlan");
    }

    @Test(expected = CloudRuntimeException.class)
    public void testGetVlanNumberFromUriInvalidSintax() {
        configurationMgr.getVlanNumberFromUri("xxx://7");
    }

    @Test
    public void testGetVlanNumberFromUriVlan() {
        Assert.assertEquals("7", configurationMgr.getVlanNumberFromUri("vlan://7"));
    }

    @Test
    public void testGetVlanNumberFromUriUntagged() {
        Assert.assertEquals("untagged", configurationMgr.getVlanNumberFromUri("vlan://untagged"));
    }

    @Test
    public void validateMaxRateEqualsOrGreaterTestAllGood() {
        configurationMgr.validateMaxRateEqualsOrGreater(1l, 2l, "IOPS Read");
    }

    @Test(expected = InvalidParameterValueException.class)
    public void validateMaxRateEqualsOrGreaterTestNormalRateGreaterThanMax() {
        configurationMgr.validateMaxRateEqualsOrGreater(3l, 2l, "IOPS Read");
    }

    @Test
    public void validateMaxRateNull() {
        configurationMgr.validateMaxRateEqualsOrGreater(3l, null, "IOPS Read");
    }

    @Test
    public void validateNormalRateNull() {
        configurationMgr.validateMaxRateEqualsOrGreater(null, 3l, "IOPS Read");
    }

    @Test
    public void validateAllNull() {
        configurationMgr.validateMaxRateEqualsOrGreater(null, 3l, "IOPS Read");
    }

    @Test
    public void validateMaximumIopsAndBytesLengthTestAllNull() {
        configurationMgr.validateMaximumIopsAndBytesLength(null, null, null, null);
    }

    @Test
    public void validateMaximumIopsAndBytesLengthTestDefaultLengthConfigs() {
        configurationMgr.validateMaximumIopsAndBytesLength(36000l, 36000l, 36000l, 36000l);
    }

    @Test
    public void shouldUpdateDiskOfferingTests(){
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString()));
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(Mockito.anyString(), nullable(String.class), nullable(Integer.class), nullable(Boolean.class), nullable(String.class), nullable(String.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(nullable(String.class), Mockito.anyString(), nullable(Integer.class), nullable(Boolean.class), nullable(String.class), nullable(String.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(nullable(String.class), nullable(String.class), Mockito.anyInt(), nullable(Boolean.class), nullable(String.class), nullable(String.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(nullable(String.class), nullable(String.class), nullable(int.class), Mockito.anyBoolean(), nullable(String.class), nullable(String.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateDiskOffering(nullable(String.class), nullable(String.class), nullable(int.class), nullable(Boolean.class), Mockito.anyString(), Mockito.anyString()));
    }

    @Test
    public void shouldUpdateDiskOfferingTestFalse(){
        Assert.assertFalse(configurationMgr.shouldUpdateDiskOffering(null, null, null, null, null, null));
    }

    @Test
    public void shouldUpdateIopsRateParametersTestFalse() {
        Assert.assertFalse(configurationMgr.shouldUpdateIopsRateParameters(null, null, null, null, null, null));
    }

    @Test
    public void shouldUpdateIopsRateParametersTests(){
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong()));
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateIopsRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong()));
    }

    @Test
    public void shouldUpdateBytesRateParametersTestFalse() {
        Assert.assertFalse(configurationMgr.shouldUpdateBytesRateParameters(null, null, null, null, null, null));
    }

    @Test
    public void shouldUpdateBytesRateParametersTests(){
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong()));
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong(), nullable(Long.class)));
        Assert.assertTrue(configurationMgr.shouldUpdateBytesRateParameters(nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), nullable(Long.class), Mockito.anyLong()));
    }

    @Test
    public void updateDiskOfferingTagsIfIsNotNullTestWhenTagsIsNull(){
        Mockito.doNothing().when(configurationMgr).updateDiskOfferingTagsIfIsNotNull(null, diskOfferingVOMock);
        this.configurationMgr.updateDiskOfferingTagsIfIsNotNull(null, diskOfferingVOMock);
        Mockito.verify(configurationMgr, Mockito.times(1)).updateDiskOfferingTagsIfIsNotNull(null, diskOfferingVOMock);
    }
    @Test
    public void updateDiskOfferingTagsIfIsNotNullTestWhenTagsIsNotNull(){
        String tags = "tags";
        Mockito.doNothing().when(configurationMgr).updateDiskOfferingTagsIfIsNotNull(tags, diskOfferingVOMock);
        this.configurationMgr.updateDiskOfferingTagsIfIsNotNull(tags, diskOfferingVOMock);
        Mockito.verify(configurationMgr, Mockito.times(1)).updateDiskOfferingTagsIfIsNotNull(tags, diskOfferingVOMock);
    }
}
