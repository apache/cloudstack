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

import com.cloud.configuration.Config;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.TransactionLegacy;
import junit.framework.TestCase;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.naming.ConfigurationException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class QuotaServiceImplTest extends TestCase {

    @Mock
    AccountDao accountDao;
    @Mock
    QuotaAccountDao quotaAcc;
    @Mock
    QuotaUsageDao quotaUsageDao;
    @Mock
    DomainDao domainDao;
    @Mock
    ConfigurationDao configDao;
    @Mock
    QuotaBalanceDao quotaBalanceDao;
    @Mock
    QuotaResponseBuilder respBldr;

    QuotaServiceImpl quotaService = new QuotaServiceImpl();

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        // Dummy transaction stack setup
        TransactionLegacy.open("QuotaServiceImplTest");

        Field accountDaoField = QuotaServiceImpl.class.getDeclaredField("_accountDao");
        accountDaoField.setAccessible(true);
        accountDaoField.set(quotaService, accountDao);

        Field quotaAccountDaoField = QuotaServiceImpl.class.getDeclaredField("_quotaAcc");
        quotaAccountDaoField.setAccessible(true);
        quotaAccountDaoField.set(quotaService, quotaAcc);

        Field quotaUsageDaoField = QuotaServiceImpl.class.getDeclaredField("_quotaUsageDao");
        quotaUsageDaoField.setAccessible(true);
        quotaUsageDaoField.set(quotaService, quotaUsageDao);

        Field domainDaoField = QuotaServiceImpl.class.getDeclaredField("_domainDao");
        domainDaoField.setAccessible(true);
        domainDaoField.set(quotaService, domainDao);

        Field configDaoField = QuotaServiceImpl.class.getDeclaredField("_configDao");
        configDaoField.setAccessible(true);
        configDaoField.set(quotaService, configDao);

        Field balanceDaoField = QuotaServiceImpl.class.getDeclaredField("_quotaBalanceDao");
        balanceDaoField.setAccessible(true);
        balanceDaoField.set(quotaService, quotaBalanceDao);

        Field QuotaResponseBuilderField = QuotaServiceImpl.class.getDeclaredField("_respBldr");
        QuotaResponseBuilderField.setAccessible(true);
        QuotaResponseBuilderField.set(quotaService, respBldr);

        Mockito.when(configDao.getValue(Mockito.eq(Config.UsageAggregationTimezone.toString()))).thenReturn("IST");
        quotaService.configure("randomName", null);
    }

    @Test
    public void testFindQuotaBalanceVO() {
        final long accountId = 2L;
        final String accountName = "admin123";
        final long domainId = 1L;
        final Date startDate = new DateTime().minusDays(2).toDate();
        final Date endDate = new Date();

        List<QuotaBalanceVO> records = new ArrayList<>();
        QuotaBalanceVO qb = new QuotaBalanceVO();
        qb.setCreditBalance(new BigDecimal(100));
        qb.setAccountId(accountId);
        records.add(qb);

        Mockito.when(respBldr.startOfNextDay(Mockito.any(Date.class))).thenReturn(startDate);
        Mockito.when(quotaBalanceDao.findQuotaBalance(Mockito.eq(accountId), Mockito.eq(domainId), Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(records);
        Mockito.when(quotaBalanceDao.lastQuotaBalanceVO(Mockito.eq(accountId), Mockito.eq(domainId), Mockito.any(Date.class))).thenReturn(records);

        // with enddate
        assertTrue(quotaService.findQuotaBalanceVO(accountId, accountName, domainId, startDate, endDate).get(0).equals(qb));
        // without enddate
        assertTrue(quotaService.findQuotaBalanceVO(accountId, accountName, domainId, startDate, null).get(0).equals(qb));
    }

    @Test
    public void testGetQuotaUsage() {
        final long accountId = 2L;
        final String accountName = "admin123";
        final long domainId = 1L;
        final Date startDate = new DateTime().minusDays(2).toDate();
        final Date endDate = new Date();

        quotaService.getQuotaUsage(accountId, accountName, domainId, QuotaTypes.IP_ADDRESS, startDate, endDate);
        Mockito.verify(quotaUsageDao, Mockito.times(1)).findQuotaUsage(Mockito.eq(accountId), Mockito.eq(domainId), Mockito.eq(QuotaTypes.IP_ADDRESS), Mockito.any(Date.class), Mockito.any(Date.class));
    }

    @Test
    public void testSetLockAccount() {
        // existing account
        QuotaAccountVO quotaAccountVO = new QuotaAccountVO();
        Mockito.when(quotaAcc.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(quotaAccountVO);
        quotaService.setLockAccount(2L, true);
        Mockito.verify(quotaAcc, Mockito.times(0)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
        Mockito.verify(quotaAcc, Mockito.times(1)).updateQuotaAccount(Mockito.anyLong(), Mockito.any(QuotaAccountVO.class));

        // new account
        Mockito.when(quotaAcc.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(null);
        quotaService.setLockAccount(2L, true);
        Mockito.verify(quotaAcc, Mockito.times(1)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
    }

    @Test
    public void testSetMinBalance() {
        final long accountId = 2L;
        final double balance = 10.3F;

        // existing account setting
        QuotaAccountVO quotaAccountVO = new QuotaAccountVO();
        Mockito.when(quotaAcc.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(quotaAccountVO);
        quotaService.setMinBalance(accountId, balance);
        Mockito.verify(quotaAcc, Mockito.times(0)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
        Mockito.verify(quotaAcc, Mockito.times(1)).updateQuotaAccount(Mockito.anyLong(), Mockito.any(QuotaAccountVO.class));

        // no account with limit set
        Mockito.when(quotaAcc.findByIdQuotaAccount(Mockito.anyLong())).thenReturn(null);
        quotaService.setMinBalance(accountId, balance);
        Mockito.verify(quotaAcc, Mockito.times(1)).persistQuotaAccount(Mockito.any(QuotaAccountVO.class));
    }
}
