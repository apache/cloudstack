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
package org.apache.cloudstack.quota;

import static org.mockito.ArgumentMatchers.nullable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.dao.ServiceOfferingDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.cloudstack.quota.vo.QuotaUsageVO;
import org.apache.cloudstack.usage.UsageTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.Pair;
import com.cloud.utils.db.TransactionLegacy;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaManagerImplTest extends TestCase {

    @Mock
    private AccountDao accountDao;
    @Mock
    private QuotaAccountDao quotaAcc;
    @Mock
    private UsageDao usageDao;
    @Mock
    private QuotaTariffDao quotaTariffDao;
    @Mock
    private QuotaUsageDao quotaUsageDao;
    @Mock
    private ServiceOfferingDao serviceOfferingDao;
    @Mock
    private QuotaBalanceDao quotaBalanceDao;
    @Mock
    private ConfigurationDao configDao;

    @Spy
    QuotaManagerImpl quotaManager = new QuotaManagerImpl();

    private void injectMockToField(Object mock, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field f = QuotaManagerImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(quotaManager, mock);
    }

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        // Dummy transaction stack setup
        TransactionLegacy.open("QuotaManagerImplTest");

        injectMockToField(accountDao, "_accountDao");
        injectMockToField(quotaAcc, "_quotaAcc");
        injectMockToField(usageDao, "_usageDao");
        injectMockToField(quotaTariffDao, "_quotaTariffDao");
        injectMockToField(quotaUsageDao, "_quotaUsageDao");
        injectMockToField(serviceOfferingDao, "_serviceOfferingDao");
        injectMockToField(quotaBalanceDao, "_quotaBalanceDao");
        injectMockToField(configDao, "_configDao");
    }

    @Test
    public void testConfig() throws ConfigurationException {
        Mockito.when(configDao.getConfiguration(Mockito.anyMapOf(String.class, Object.class))).thenReturn(new HashMap<String, String>());
        Map<String, Object> map = new HashMap<>();
        map.put("usage.stats.job.aggregation.range", "0");
        assertTrue(quotaManager.configure("quotaManager", map));
    }

    @Test
    public void testCalculateQuotaUsage() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.Type.NORMAL);
        List<AccountVO> accountVOList = new ArrayList<>();
        accountVOList.add(accountVO);
        Mockito.when(accountDao.listAll()).thenReturn(accountVOList);

        UsageVO usageVO = new UsageVO();
        usageVO.setQuotaCalculated(0);
        List<UsageVO> usageVOList = new ArrayList<UsageVO>();
        usageVOList.add(usageVO);
        Pair<List<? extends UsageVO>, Integer> usageRecords = new Pair<List<? extends UsageVO>, Integer>(usageVOList, usageVOList.size());
        Mockito.when(usageDao.getUsageRecordsPendingQuotaAggregation(Mockito.anyLong(), Mockito.anyLong())).thenReturn(usageRecords);

        QuotaUsageVO quotaUsageVO = new QuotaUsageVO();
        quotaUsageVO.setAccountId(2L);
        List<QuotaUsageVO> quotaListForAccount = new ArrayList<>();
        quotaListForAccount.add(quotaUsageVO);
        Mockito.doReturn(quotaListForAccount).when(quotaManager).aggregatePendingQuotaRecordsForAccount(Mockito.eq(accountVO), Mockito.eq(usageRecords));
        Mockito.doNothing().when(quotaManager).processQuotaBalanceForAccount(Mockito.eq(accountVO), Mockito.eq(quotaListForAccount));

        assertTrue(quotaManager.calculateQuotaUsage());
    }

    @Test
    public void testAggregatePendingQuotaRecordsForAccount() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.Type.NORMAL);

        UsageVO usageVO = new UsageVO();
        usageVO.setQuotaCalculated(0);
        usageVO.setUsageType(UsageTypes.ALLOCATED_VM);
        List<UsageVO> usageVOList = new ArrayList<UsageVO>();
        usageVOList.add(usageVO);
        Pair<List<? extends UsageVO>, Integer> usageRecords = new Pair<List<? extends UsageVO>, Integer>(usageVOList, usageVOList.size());

        QuotaUsageVO quotaUsageVO = new QuotaUsageVO();
        quotaUsageVO.setAccountId(2L);
        Mockito.doReturn(quotaUsageVO).when(quotaManager).updateQuotaAllocatedVMUsage(Mockito.eq(usageVO));

        assertTrue(quotaManager.aggregatePendingQuotaRecordsForAccount(accountVO, new Pair<List<? extends UsageVO>, Integer>(null, 0)).size() == 0);
        assertTrue(quotaManager.aggregatePendingQuotaRecordsForAccount(accountVO, usageRecords).size() == 1);
    }

    @Test
    public void testUpdateQuotaRecords() {
        UsageVO usageVO = new UsageVO();
        usageVO.setId(100L);
        usageVO.setQuotaCalculated(0);
        usageVO.setUsageType(UsageTypes.NETWORK_BYTES_SENT);
        usageVO.setRawUsage(9000000000.0);
        usageVO.setSize(1010101010L);

        QuotaTariffVO tariffVO = new QuotaTariffVO();
        tariffVO.setCurrencyValue(new BigDecimal(1));
        Mockito.when(quotaTariffDao.findTariffPlanByUsageType(nullable(Integer.class), nullable(Date.class))).thenReturn(tariffVO);

        QuotaUsageVO qu = quotaManager.updateQuotaNetwork(usageVO, UsageTypes.NETWORK_BYTES_SENT);
        assertTrue(qu.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0);
        qu = quotaManager.updateQuotaAllocatedVMUsage(usageVO);
        assertTrue(qu.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0);
        qu = quotaManager.updateQuotaDiskUsage(usageVO, UsageTypes.VOLUME);
        assertTrue(qu.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0);
        qu = quotaManager.updateQuotaRaw(usageVO, UsageTypes.VPN_USERS);
        assertTrue(qu.getQuotaUsed().compareTo(BigDecimal.ZERO) > 0);

        Mockito.verify(quotaUsageDao, Mockito.times(4)).persistQuotaUsage(Mockito.any(QuotaUsageVO.class));
        Mockito.verify(usageDao, Mockito.times(4)).persistUsage(Mockito.any(UsageVO.class));
    }

    @Test
    public void testProcessQuotaBalanceForAccount() {
        Date now = new Date();
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.Type.NORMAL);

        QuotaUsageVO quotaUsageVO = new QuotaUsageVO();
        quotaUsageVO.setAccountId(2L);
        quotaUsageVO.setStartDate(new Date(now.getTime()));
        quotaUsageVO.setEndDate(new Date(now.getTime()));
        List<QuotaUsageVO> quotaListForAccount = new ArrayList<>();
        quotaListForAccount.add(quotaUsageVO);

        quotaManager.processQuotaBalanceForAccount(accountVO, quotaListForAccount);
        Mockito.verify(quotaAcc, Mockito.times(1)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
    }

    private AccountVO accountVO = new AccountVO();

    @Test
    public void testAdminLockableAccount() {
        accountVO.setType(Account.Type.ADMIN);
        assertFalse(quotaManager.isLockable(accountVO));
    }

    @Test
    public void testNormalLockableAccount() {
        accountVO.setType(Account.Type.NORMAL);
        assertTrue(quotaManager.isLockable(accountVO));
    }

    @Test
    public void tesDomainAdmingLockableAccount() {
        accountVO.setType(Account.Type.DOMAIN_ADMIN);
        assertTrue(quotaManager.isLockable(accountVO));
    }

    @Test
    public void testReadOnlyAdminLockableAccount() {
        accountVO.setType(Account.Type.READ_ONLY_ADMIN);
        assertFalse(quotaManager.isLockable(accountVO));
    }

    @Test
    public void testResourceDomainAdminLockableAccount() {
        accountVO.setType(Account.Type.RESOURCE_DOMAIN_ADMIN);
        assertFalse(quotaManager.isLockable(accountVO));
    }

    @Test
    public void testProjectLockableAccount() {
        accountVO.setType(Account.Type.PROJECT);
        assertFalse(quotaManager.isLockable(accountVO));
    }

}
