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

package com.cloud.network;

import static org.mockito.Matchers.any;
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

import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedGuestVlanRangeCmd;
import org.apache.cloudstack.context.CallContext;

import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.projects.ProjectManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.Transaction;

public class DedicateGuestVlanRangesTest {

    private static final Logger s_logger = Logger.getLogger(DedicateGuestVlanRangesTest.class);

    NetworkServiceImpl networkService = new NetworkServiceImpl();

    DedicateGuestVlanRangeCmd dedicateGuestVlanRangesCmd = new DedicateGuestVlanRangeCmdExtn();
    Class<?> _dedicateGuestVlanRangeClass = dedicateGuestVlanRangesCmd.getClass().getSuperclass();

    ReleaseDedicatedGuestVlanRangeCmd releaseDedicatedGuestVlanRangesCmd = new ReleaseDedicatedGuestVlanRangeCmdExtn();
    Class<?> _releaseGuestVlanRangeClass = releaseDedicatedGuestVlanRangesCmd.getClass().getSuperclass();

    ListDedicatedGuestVlanRangesCmd listDedicatedGuestVlanRangesCmd = new ListDedicatedGuestVlanRangesCmdExtn();
    Class<?> _listDedicatedGuestVlanRangeClass = listDedicatedGuestVlanRangesCmd.getClass().getSuperclass();


    @Mock AccountManager _accountMgr;
    @Mock AccountDao _accountDao;
    @Mock ProjectManager _projectMgr;
    @Mock PhysicalNetworkDao _physicalNetworkDao;
    @Mock DataCenterVnetDao _dataCenterVnetDao;
    @Mock AccountGuestVlanMapDao _accountGuestVlanMapDao;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        networkService._accountMgr = _accountMgr;
        networkService._accountDao = _accountDao;
        networkService._projectMgr = _projectMgr;
        networkService._physicalNetworkDao = _physicalNetworkDao;
        networkService._datacneter_vnet = _dataCenterVnetDao;
        networkService._accountGuestVlanMapDao = _accountGuestVlanMapDao;

        Account account = new AccountVO("testaccount", 1, "networkdomain", (short) 0, UUID.randomUUID().toString());
        when(networkService._accountMgr.getAccount(anyLong())).thenReturn(account);
        when(networkService._accountDao.findActiveAccount(anyString(), anyLong())).thenReturn(account);
        
        UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone", UUID.randomUUID().toString());

        CallContext.register(user, account);

        Field accountNameField = _dedicateGuestVlanRangeClass.getDeclaredField("accountName");
        accountNameField.setAccessible(true);
        accountNameField.set(dedicateGuestVlanRangesCmd, "accountname");

        Field projectIdField = _dedicateGuestVlanRangeClass.getDeclaredField("projectId");
        projectIdField.setAccessible(true);
        projectIdField.set(dedicateGuestVlanRangesCmd, null);

        Field domainIdField = _dedicateGuestVlanRangeClass.getDeclaredField("domainId");
        domainIdField.setAccessible(true);
        domainIdField.set(dedicateGuestVlanRangesCmd, 1L);

        Field physicalNetworkIdField = _dedicateGuestVlanRangeClass.getDeclaredField("physicalNetworkId");
        physicalNetworkIdField.setAccessible(true);
        physicalNetworkIdField.set(dedicateGuestVlanRangesCmd, 1L);

        Field releaseIdField = _releaseGuestVlanRangeClass.getDeclaredField("id");
        releaseIdField.setAccessible(true);
        releaseIdField.set(releaseDedicatedGuestVlanRangesCmd, 1L);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
    }

    @Test
    public void testDedicateGuestVlanRange() throws Exception {
        s_logger.info("Running tests for DedicateGuestVlanRange API");

        /*
         * TEST 1: given valid parameters DedicateGuestVlanRange should succeed
         */
        runDedicateGuestVlanRangePostiveTest();

        /*
        * TEST 2: given invalid format for vlan range DedicateGuestVlanRange should fail
        */
        runDedicateGuestVlanRangeInvalidFormat();

        /*
         * TEST 3: given vlan range that doesn't exist in the system request should fail
         */
        runDedicateGuestVlanRangeInvalidRangeValue();

        /*
         * TEST 4: given vlan range has vlans that are allocated to a different account request should fail
         */
        runDedicateGuestVlanRangeAllocatedVlans();

        /*
         * TEST 5: given vlan range is already dedicated to another account request should fail
         */
        runDedicateGuestVlanRangeDedicatedRange();

        /*
         * TEST 6: given vlan range is partially dedicated to a different account request should fail
         */
        runDedicateGuestVlanRangePartiallyDedicated();
    }

    @Test
    public void testReleaseDedicatedGuestVlanRange() throws Exception {

        s_logger.info("Running tests for ReleaseDedicatedGuestVlanRange API");

        /*
         * TEST 1: given valid parameters ReleaseDedicatedGuestVlanRange should succeed
         */
        runReleaseDedicatedGuestVlanRangePostiveTest();

        /*
         * TEST 2: given range doesn't exist request should fail
         */
        runReleaseDedicatedGuestVlanRangeInvalidRange();
    }

    void runDedicateGuestVlanRangePostiveTest() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangePostiveTest");
        
        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2-5");

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");
        AccountGuestVlanMapVO accountGuestVlanMapVO = new AccountGuestVlanMapVO(1L,1L);

        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        when(networkService._datacneter_vnet.listAllocatedVnetsInRange(anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(null);

        when(networkService._accountGuestVlanMapDao.listAccountGuestVlanMapsByPhysicalNetwork(anyLong())).thenReturn(null);

        when(networkService._accountGuestVlanMapDao.persist(any(AccountGuestVlanMapVO.class))).thenReturn(accountGuestVlanMapVO);

        when(networkService._datacneter_vnet.update(anyLong(), any(DataCenterVnetVO.class))).thenReturn(true);

        List<DataCenterVnetVO> dataCenterVnetList = new ArrayList<DataCenterVnetVO>();
        DataCenterVnetVO dataCenterVnetVO = new DataCenterVnetVO("2-5", 1L, 1L);
        dataCenterVnetList.add(dataCenterVnetVO);
        when(networkService._datacneter_vnet.findVnet(anyLong(), anyString())).thenReturn(dataCenterVnetList);

        try {
            GuestVlan result = networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
            Assert.assertNotNull(result);
        } catch (Exception e) {
            s_logger.info("exception in testing runDedicateGuestVlanRangePostiveTest message: " + e.toString());
        } finally {
            txn.close("runDedicateGuestRangePostiveTest");
        }
    }

    void runDedicateGuestVlanRangeInvalidFormat() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangeInvalidFormat");

        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2");

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");

        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        try {
            networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Invalid format for parameter value vlan"));
        } finally {
            txn.close("runDedicateGuestVlanRangeInvalidFormat");
        }
    }

    void runDedicateGuestVlanRangeInvalidRangeValue() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangeInvalidRangeValue");

        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2-5");
        
        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "6-10", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");

        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        try {
            networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Unable to find guest vlan by range"));
        } finally {
            txn.close("runDedicateGuestVlanRangeInvalidRangeValue");
        }
    }

    void runDedicateGuestVlanRangeAllocatedVlans() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangeAllocatedVlans");
        
        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2-5");

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");
        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        List<DataCenterVnetVO> dataCenterList = new ArrayList<DataCenterVnetVO>();
        DataCenterVnetVO dataCenter = new DataCenterVnetVO("2-5", 1L, 1L);
        dataCenter.setAccountId(1L);
        dataCenterList.add(dataCenter);
        when(networkService._datacneter_vnet.listAllocatedVnetsInRange(anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(dataCenterList);

        try {
            networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("is allocated to a different account"));
        } finally {
            txn.close("runDedicateGuestVlanRangeAllocatedVlans");
        }
    }

    void runDedicateGuestVlanRangeDedicatedRange() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangeDedicatedRange");
        
        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2-5");

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");

        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        when(networkService._datacneter_vnet.listAllocatedVnetsInRange(anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(null);

        List<AccountGuestVlanMapVO> guestVlanMaps = new ArrayList<AccountGuestVlanMapVO>();
        AccountGuestVlanMapVO accountGuestVlanMap = new AccountGuestVlanMapVO(1L, 1L);
        accountGuestVlanMap.setGuestVlanRange("2-5");
        guestVlanMaps.add(accountGuestVlanMap);
        when(networkService._accountGuestVlanMapDao.listAccountGuestVlanMapsByPhysicalNetwork(anyLong())).thenReturn(guestVlanMaps);

        try {
            networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Vlan range is already dedicated"));
        } finally {
            txn.close("runDedicateGuestVlanRangeDedicatedRange");
        }
    }

    void runDedicateGuestVlanRangePartiallyDedicated() throws Exception {
        Transaction txn = Transaction.open("runDedicateGuestVlanRangePartiallyDedicated");
        
        Field dedicateVlanField = _dedicateGuestVlanRangeClass.getDeclaredField("vlan");
        dedicateVlanField.setAccessible(true);
        dedicateVlanField.set(dedicateGuestVlanRangesCmd, "2-5");

        PhysicalNetworkVO physicalNetwork = new PhysicalNetworkVO(1L, 1L, "2-5", "200", 1L, null, "testphysicalnetwork");
        physicalNetwork.addIsolationMethod("VLAN");

        when(networkService._physicalNetworkDao.findById(anyLong())).thenReturn(physicalNetwork);

        when(networkService._datacneter_vnet.listAllocatedVnetsInRange(anyLong(), anyLong(), anyInt(), anyInt())).thenReturn(null);

        List<AccountGuestVlanMapVO> guestVlanMaps = new ArrayList<AccountGuestVlanMapVO>();
        AccountGuestVlanMapVO accountGuestVlanMap = new AccountGuestVlanMapVO(2L, 1L);
        accountGuestVlanMap.setGuestVlanRange("4-8");
        guestVlanMaps.add(accountGuestVlanMap);
        when(networkService._accountGuestVlanMapDao.listAccountGuestVlanMapsByPhysicalNetwork(anyLong())).thenReturn(guestVlanMaps);

        try {
            networkService.dedicateGuestVlanRange(dedicateGuestVlanRangesCmd);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Vlan range is already dedicated"));
        } finally {
            txn.close("runDedicateGuestVlanRangePartiallyDedicated");
        }
    }
    
    void runReleaseDedicatedGuestVlanRangePostiveTest() throws Exception {
        Transaction txn = Transaction.open("runReleaseDedicatedGuestVlanRangePostiveTest");

        AccountGuestVlanMapVO accountGuestVlanMap = new AccountGuestVlanMapVO(1L, 1L);
        when(networkService._accountGuestVlanMapDao.findById(anyLong())).thenReturn(accountGuestVlanMap);
        doNothing().when(networkService._datacneter_vnet).releaseDedicatedGuestVlans(anyLong());
        when(networkService._accountGuestVlanMapDao.remove(anyLong())).thenReturn(true);

        try {
            Boolean result = networkService.releaseDedicatedGuestVlanRange(releaseDedicatedGuestVlanRangesCmd.getId());
            Assert.assertTrue(result);
        } catch (Exception e) {
            s_logger.info("exception in testing runReleaseGuestVlanRangePostiveTest1 message: " + e.toString());
        } finally {
            txn.close("runReleaseDedicatedGuestVlanRangePostiveTest");
        }
    }

    void runReleaseDedicatedGuestVlanRangeInvalidRange() throws Exception {
        Transaction txn = Transaction.open("runReleaseDedicatedGuestVlanRangeInvalidRange");

        when(networkService._accountGuestVlanMapDao.findById(anyLong())).thenReturn(null);

        try {
        	networkService.releaseDedicatedGuestVlanRange(releaseDedicatedGuestVlanRangesCmd.getId());
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("Dedicated guest vlan with specified id doesn't exist in the system"));
        } finally {
            txn.close("runReleaseDedicatedGuestVlanRangeInvalidRange");
        }
    }
    
    public class DedicateGuestVlanRangeCmdExtn extends DedicateGuestVlanRangeCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class ReleaseDedicatedGuestVlanRangeCmdExtn extends ReleaseDedicatedGuestVlanRangeCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }

    public class ListDedicatedGuestVlanRangesCmdExtn extends ListDedicatedGuestVlanRangesCmd {
        @Override
        public long getEntityOwnerId() {
            return 1;
        }
    }
}
