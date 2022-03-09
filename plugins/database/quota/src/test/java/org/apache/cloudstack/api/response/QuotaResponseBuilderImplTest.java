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
package org.apache.cloudstack.api.response;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.command.QuotaEmailTemplateListCmd;
import org.apache.cloudstack.api.command.QuotaEmailTemplateUpdateCmd;
import org.apache.cloudstack.quota.QuotaService;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.dao.QuotaBalanceDao;
import org.apache.cloudstack.quota.dao.QuotaCreditsDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaTariffDao;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.apache.cloudstack.quota.vo.QuotaCreditsVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.TransactionLegacy;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaResponseBuilderImplTest extends TestCase {

    @Mock
    QuotaTariffDao quotaTariffDao;
    @Mock
    QuotaBalanceDao quotaBalanceDao;
    @Mock
    QuotaCreditsDao quotaCreditsDao;
    @Mock
    QuotaEmailTemplatesDao quotaEmailTemplateDao;

    @Mock
    UserDao userDao;
    @Mock
    QuotaService quotaService;
    @Mock
    AccountDao accountDao;
    @Inject
    AccountManager accountMgr;

    QuotaResponseBuilderImpl quotaResponseBuilder = new QuotaResponseBuilderImpl();

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException {
        // Dummy transaction stack setup
        TransactionLegacy.open("QuotaResponseBuilderImplTest");

        Field tariffDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_quotaTariffDao");
        tariffDaoField.setAccessible(true);
        tariffDaoField.set(quotaResponseBuilder, quotaTariffDao);

        Field balanceDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_quotaBalanceDao");
        balanceDaoField.setAccessible(true);
        balanceDaoField.set(quotaResponseBuilder, quotaBalanceDao);

        Field quotaCreditsDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_quotaCreditsDao");
        quotaCreditsDaoField.setAccessible(true);
        quotaCreditsDaoField.set(quotaResponseBuilder, quotaCreditsDao);

        Field quotaEmailTemplateDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_quotaEmailTemplateDao");
        quotaEmailTemplateDaoField.setAccessible(true);
        quotaEmailTemplateDaoField.set(quotaResponseBuilder, quotaEmailTemplateDao);

        Field userDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(quotaResponseBuilder, userDao);

        Field quotaServiceField = QuotaResponseBuilderImpl.class.getDeclaredField("_quotaService");
        quotaServiceField.setAccessible(true);
        quotaServiceField.set(quotaResponseBuilder, quotaService);

        Field accountDaoField = QuotaResponseBuilderImpl.class.getDeclaredField("_accountDao");
        accountDaoField.setAccessible(true);
        accountDaoField.set(quotaResponseBuilder, accountDao);

        Field regionMgrField = QuotaResponseBuilderImpl.class.getDeclaredField("_accountMgr");
        regionMgrField.setAccessible(true);
        regionMgrField.set(quotaResponseBuilder, accountMgr);
    }

    private QuotaTariffVO makeTariffTestData() {
        QuotaTariffVO tariffVO = new QuotaTariffVO();
        tariffVO.setUsageType(QuotaTypes.IP_ADDRESS);
        tariffVO.setUsageName("ip address");
        tariffVO.setUsageUnit("IP-Month");
        tariffVO.setCurrencyValue(BigDecimal.valueOf(100.19));
        tariffVO.setEffectiveOn(new Date());
        tariffVO.setUsageDiscriminator("");
        return tariffVO;
    }

    @Test
    public void testQuotaResponse() {
        QuotaTariffVO tariffVO = makeTariffTestData();
        QuotaTariffResponse response = quotaResponseBuilder.createQuotaTariffResponse(tariffVO);
        assertTrue(tariffVO.getUsageType() == response.getUsageType());
        assertTrue(tariffVO.getCurrencyValue().equals(response.getTariffValue()));
    }

    @Test
    public void testAddQuotaCredits() {
        final long accountId = 2L;
        final long domainId = 1L;
        final double amount = 11.0;
        final long updatedBy = 2L;

        QuotaCreditsVO credit = new QuotaCreditsVO();
        credit.setCredit(new BigDecimal(amount));

        Mockito.when(quotaCreditsDao.saveCredits(Mockito.any(QuotaCreditsVO.class))).thenReturn(credit);
        Mockito.when(quotaBalanceDao.lastQuotaBalance(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(Date.class))).thenReturn(new BigDecimal(111));
        Mockito.when(quotaService.computeAdjustedTime(Mockito.any(Date.class))).thenReturn(new Date());

        AccountVO account = new AccountVO();
        account.setState(Account.State.LOCKED);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(account);

        QuotaCreditsResponse resp = quotaResponseBuilder.addQuotaCredits(accountId, domainId, amount, updatedBy, true);
        assertTrue(resp.getCredits().compareTo(credit.getCredit()) == 0);
    }

    @Test
    public void testListQuotaEmailTemplates() {
        QuotaEmailTemplateListCmd cmd = new QuotaEmailTemplateListCmd();
        cmd.setTemplateName("some name");
        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        Mockito.when(quotaEmailTemplateDao.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);

        assertTrue(quotaResponseBuilder.listQuotaEmailTemplates(cmd).size() == 1);
    }

    @Test
    public void testUpdateQuotaEmailTemplate() {
        QuotaEmailTemplateUpdateCmd cmd = new QuotaEmailTemplateUpdateCmd();
        cmd.setTemplateBody("some body");
        cmd.setTemplateName("some name");
        cmd.setTemplateSubject("some subject");

        List<QuotaEmailTemplatesVO> templates = new ArrayList<>();

        Mockito.when(quotaEmailTemplateDao.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(templates);
        Mockito.when(quotaEmailTemplateDao.updateQuotaEmailTemplate(Mockito.any(QuotaEmailTemplatesVO.class))).thenReturn(true);

        // invalid template test
        assertFalse(quotaResponseBuilder.updateQuotaEmailTemplate(cmd));

        // valid template test
        QuotaEmailTemplatesVO template = new QuotaEmailTemplatesVO();
        template.setTemplateName("template");
        templates.add(template);
        assertTrue(quotaResponseBuilder.updateQuotaEmailTemplate(cmd));
    }

    @Test
    public void testCreateQuotaLastBalanceResponse() {
        List<QuotaBalanceVO> quotaBalance = new ArrayList<>();
        // null balance test
        try {
            quotaResponseBuilder.createQuotaLastBalanceResponse(null, new Date());
        } catch (InvalidParameterValueException e) {
            assertTrue(e.getMessage().equals("There are no balance entries on or before the requested date."));
        }

        // empty balance test
        try {
            quotaResponseBuilder.createQuotaLastBalanceResponse(quotaBalance, new Date());
        } catch (InvalidParameterValueException e) {
            assertTrue(e.getMessage().equals("There are no balance entries on or before the requested date."));
        }

        // valid balance test
        QuotaBalanceVO entry = new QuotaBalanceVO();
        entry.setAccountId(2L);
        entry.setCreditBalance(new BigDecimal(100));
        quotaBalance.add(entry);
        quotaBalance.add(entry);
        Mockito.lenient().when(quotaService.computeAdjustedTime(Mockito.any(Date.class))).thenReturn(new Date());
        QuotaBalanceResponse resp = quotaResponseBuilder.createQuotaLastBalanceResponse(quotaBalance, null);
        assertTrue(resp.getStartQuota().compareTo(new BigDecimal(200)) == 0);
    }

    @Test
    public void testStartOfNextDayWithoutParameters() {
        Date nextDate = quotaResponseBuilder.startOfNextDay();

        LocalDateTime tomorrowAtStartOfTheDay = LocalDate.now().atStartOfDay().plusDays(1);
        Date expectedNextDate = Date.from(tomorrowAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDate);
    }

    @Test
    public void testStartOfNextDayWithParameter() {
        Date anyDate = new Date(1242421545757532l);

        Date nextDayDate = quotaResponseBuilder.startOfNextDay(anyDate);

        LocalDateTime nextDayLocalDateTimeAtStartOfTheDay = anyDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1).atStartOfDay();
        Date expectedNextDate = Date.from(nextDayLocalDateTimeAtStartOfTheDay.atZone(ZoneId.systemDefault()).toInstant());

        Assert.assertEquals(expectedNextDate, nextDayDate);
    }
}
