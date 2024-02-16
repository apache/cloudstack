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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.QuotaStatementImpl.QuotaStatementPeriods;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDaoImpl;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.TransactionLegacy;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaStatementTest extends TestCase {

    @Mock
    AccountDao accountDao;
    @Mock
    QuotaAccountDao quotaAcc;
    @Mock
    ConfigurationDao configDao;
    @Mock
    QuotaUsageDao quotaUsage;
    @Mock
    QuotaAlertManager alertManager;

    @Mock
    QuotaEmailConfigurationDaoImpl quotaEmailConfigurationDaoMock;

    @Mock
    QuotaEmailTemplatesDao quotaEmailTemplatesDaoMock;

    @Mock
    QuotaEmailTemplatesVO quotaEmailTemplatesVOMock;

    @Mock
    List<QuotaEmailTemplatesVO> listMock;

    @Spy
    @InjectMocks
    QuotaStatementImpl quotaStatement = new QuotaStatementImpl();

    private void injectMockToField(Object mock, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field f = QuotaStatementImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(quotaStatement, mock);
    }

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        // Dummy transaction stack setup
        TransactionLegacy.open("QuotaStatementImplTest");

        injectMockToField(accountDao, "_accountDao");
        injectMockToField(quotaAcc, "_quotaAcc");
        injectMockToField(configDao, "_configDao");
        injectMockToField(quotaUsage, "_quotaUsage");
        injectMockToField(alertManager, "_quotaAlert");
    }

    @Test
    public void testStatementPeriodBIMONTHLY() {
        Calendar date = Calendar.getInstance();

        //BIMONTHLY - first statement of month
        date.set(Calendar.DATE, QuotaStatementImpl.s_LAST_STATEMENT_SENT_DAYS + 1);
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.BIMONTHLY);
        assertTrue(period == null);

        //1 of this month
        date.set(Calendar.DATE, 1);
        period = quotaStatement.statementTime(date, QuotaStatementPeriods.BIMONTHLY);
        assertTrue(period != null);
        assertTrue(period.length == 2);
        assertTrue(period[0].toString(), period[0].before(period[1]));
        assertTrue(period[0].toString(), period[0].get(Calendar.DATE) == 1);
        assertTrue(period[1].toString(), period[1].get(Calendar.DATE) == 15);

        //BIMONTHLY - second statement of month
        date = Calendar.getInstance();
        date.set(Calendar.DATE, QuotaStatementImpl.s_LAST_STATEMENT_SENT_DAYS + 16);
        period = quotaStatement.statementTime(date, QuotaStatementPeriods.BIMONTHLY);
        assertTrue(period == null);

        //17 of this month
        date.set(Calendar.DATE, 17);
        period = quotaStatement.statementTime(date, QuotaStatementPeriods.BIMONTHLY);
        assertTrue(period != null);
        assertTrue(period.length == 2);
        assertTrue(period[0].toString(), period[0].before(period[1]));
        assertTrue(period[0].toString(), period[0].get(Calendar.DATE) == 16);

        //get last day of the previous month
        Calendar aCalendar = Calendar.getInstance();
        aCalendar.add(Calendar.MONTH, -1);
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);

        assertTrue(period[1].toString(), period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE));

    }

    @Test
    public void testStatementPeriodMONTHLY() {
        Calendar date = Calendar.getInstance();
        Calendar aCalendar = Calendar.getInstance();

        //MONTHLY
        date = Calendar.getInstance();
        date.set(Calendar.DATE, QuotaStatementImpl.s_LAST_STATEMENT_SENT_DAYS + 1);
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.MONTHLY);
        assertTrue(period == null);

        //1 of this month
        date.set(Calendar.DATE, QuotaStatementImpl.s_LAST_STATEMENT_SENT_DAYS - 1);
        period = quotaStatement.statementTime(date, QuotaStatementPeriods.MONTHLY);
        assertTrue(period != null);
        assertTrue(period.length == 2);
        assertTrue(period[0].toString(), period[0].before(period[1]));
        assertTrue(period[0].toString(), period[0].get(Calendar.DATE) == 1);

        //get last day of the previous month
        aCalendar = Calendar.getInstance();
        aCalendar.add(Calendar.MONTH, -1);
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);

        assertTrue(period[1].toString(), period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE));

    }

    @Test
    public void testStatementPeriodQUATERLY() {
        Calendar date = Calendar.getInstance();
        Calendar aCalendar = Calendar.getInstance();

        //QUATERLY
        date = Calendar.getInstance();
        date.set(Calendar.MONTH, Calendar.JANUARY); // 1 Jan
        date.set(Calendar.DATE, 1);
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.QUATERLY);
        assertTrue(period != null);
        assertTrue(period.length == 2);
        assertTrue("period[0].before(period[1])" + period[0].toString(), period[0].before(period[1]));
        assertTrue("period[0].get(Calendar.DATE) == 1" + period[0].toString(), period[0].get(Calendar.DATE) == 1);
        assertTrue("period[0].get(Calendar.MONTH) == Calendar.OCTOBER" + period[0].toString(), period[0].get(Calendar.MONTH) == Calendar.OCTOBER); //october

        //get last day of the previous month
        aCalendar = Calendar.getInstance();
        aCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        assertTrue(" period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE)" + period[1].toString(), period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE));
        assertTrue("period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH)" + period[1].toString(), period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH));

    }

    @Test
    public void testStatementPeriodHALFYEARLY() {
        Calendar date = Calendar.getInstance();
        Calendar aCalendar = Calendar.getInstance();

        //QUATERLY
        date = Calendar.getInstance();
        date.set(Calendar.MONTH, Calendar.JANUARY); // 1 Jan
        date.set(Calendar.DATE, 1);
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.HALFYEARLY);
        assertTrue(period != null);
        assertTrue(period.length == 2);
        assertTrue("period[0].before(period[1])" + period[0].toString(), period[0].before(period[1]));
        assertTrue("period[0].get(Calendar.DATE) == 1" + period[0].toString(), period[0].get(Calendar.DATE) == 1);
        assertTrue("period[0].get(Calendar.MONTH) == Calendar.JULY" + period[0].toString(), period[0].get(Calendar.MONTH) == Calendar.JULY); //july

        //get last day of the previous month
        aCalendar = Calendar.getInstance();
        aCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        assertTrue(" period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE)" + period[1].toString(), period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE));
        assertTrue("period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH)" + period[1].toString(), period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH));

    }

    @Test
    public void testStatementPeriodYEARLY() {
        Calendar date = Calendar.getInstance();
        Calendar aCalendar = Calendar.getInstance();

        //QUATERLY
        date = Calendar.getInstance();
        date.set(Calendar.MONTH, Calendar.JANUARY); // 1 Jan
        date.set(Calendar.DATE, 1);
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.YEARLY);
        assertTrue("period != null", period != null);
        assertTrue(period.length == 2);
        assertTrue("period[0].before(period[1])" + period[0].toString(), period[0].before(period[1]));
        assertTrue("period[0].get(Calendar.DATE) == 1" + period[0].toString(), period[0].get(Calendar.DATE) == 1);
        assertTrue("period[0].get(Calendar.MONTH) == Calendar.JANUARY" + period[0].toString(), period[0].get(Calendar.MONTH) == Calendar.JANUARY); //january

        //get last day of the previous month
        aCalendar = Calendar.getInstance();
        aCalendar.set(Calendar.MONTH, Calendar.DECEMBER);
        aCalendar.set(Calendar.DATE, aCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) + 1);
        assertTrue(" period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE)" + period[1].toString(), period[1].get(Calendar.DATE) == aCalendar.get(Calendar.DATE));
        assertTrue("period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH)" + period[1].toString(), period[1].get(Calendar.MONTH) == aCalendar.get(Calendar.MONTH));

    }


    @Test
    public void sendStatementTestUnconfiguredEmail() {
        boolean defaultConfigurationValue = QuotaConfig.QuotaEnableEmails.value();
        Mockito.doReturn(defaultConfigurationValue).when(alertManager).isQuotaEmailTypeEnabledForAccount(Mockito.any(AccountVO.class), Mockito.any(QuotaConfig.QuotaEmailTemplateTypes.class));

        Calendar date = Calendar.getInstance();
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        Mockito.lenient().when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);

        QuotaAccountVO acc = new QuotaAccountVO(2L);
        acc.setQuotaBalance(new BigDecimal(404));
        acc.setLastStatementDate(null);
        List<QuotaAccountVO> accounts = new ArrayList<>();
        accounts.add(acc);
        Mockito.lenient().when(quotaAcc.listAllQuotaAccount()).thenReturn(accounts);

        Mockito.lenient().when(quotaUsage.findTotalQuotaUsage(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class)))
                .thenReturn(new BigDecimal(100));

        // call real method on send monthly statement
        quotaStatement.sendStatement();
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.MONTHLY);
        if (period != null){
            Mockito.verify(alertManager, Mockito.times(1)).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));
        }
    }

    @Test
    public void sendStatementTestEnabledEmail() {
        Mockito.doReturn(true).when(alertManager).isQuotaEmailTypeEnabledForAccount(Mockito.any(AccountVO.class), Mockito.any(QuotaConfig.QuotaEmailTemplateTypes.class));

        Calendar date = Calendar.getInstance();
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        Mockito.lenient().when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);

        QuotaAccountVO acc = new QuotaAccountVO(2L);
        acc.setQuotaBalance(new BigDecimal(404));
        acc.setLastStatementDate(null);
        List<QuotaAccountVO> accounts = new ArrayList<>();
        accounts.add(acc);
        Mockito.lenient().when(quotaAcc.listAllQuotaAccount()).thenReturn(accounts);

        Mockito.lenient().when(quotaUsage.findTotalQuotaUsage(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyInt(), Mockito.any(Date.class), Mockito.any(Date.class)))
                .thenReturn(new BigDecimal(100));

        // call real method on send monthly statement
        quotaStatement.sendStatement();
        Calendar period[] = quotaStatement.statementTime(date, QuotaStatementPeriods.MONTHLY);
        if (period != null){
            Mockito.verify(alertManager, Mockito.times(1)).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));
        }
    }

    @Test
    public void sendStatementTestDisabledEmail() {
        QuotaAccountVO quotaAccountVoMock = Mockito.mock(QuotaAccountVO.class);
        Mockito.when(quotaAccountVoMock.getQuotaBalance()).thenReturn(BigDecimal.ONE);
        Mockito.when(quotaAcc.listAllQuotaAccount()).thenReturn(List.of(quotaAccountVoMock));
        AccountVO accountVoMock = Mockito.mock(AccountVO.class);
        Mockito.doReturn(accountVoMock).when(accountDao).findById(Mockito.anyLong());
        Mockito.doReturn(false).when(alertManager).isQuotaEmailTypeEnabledForAccount(Mockito.any(AccountVO.class), Mockito.any(QuotaConfig.QuotaEmailTemplateTypes.class));

        quotaStatement.sendStatement();

        Mockito.verify(quotaStatement, Mockito.never()).statementTime(Mockito.any(), Mockito.any());
    }

}
