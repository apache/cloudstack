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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.mail.MessagingException;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailConfigurationDaoImpl;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailConfigurationVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.TransactionLegacy;

import junit.framework.TestCase;
import org.apache.cloudstack.utils.mailing.SMTPMailProperties;
import org.apache.cloudstack.utils.mailing.SMTPMailSender;

@RunWith(MockitoJUnitRunner.class)
public class QuotaAlertManagerImplTest extends TestCase {

    @Mock
    private AccountDao accountDao;
    @Mock
    private QuotaAccountDao quotaAcc;
    @Mock
    private UserDao userDao;
    @Mock
    private DomainDao domainDao;
    @Mock
    private QuotaEmailTemplatesDao quotaEmailTemplateDao;
    @Mock
    private ConfigurationDao configDao;

    @Mock
    private QuotaEmailConfigurationDaoImpl quotaEmailConfigurationDaoMock;

    @Mock
    private QuotaAccountVO quotaAccountVOMock;

    @Mock
    private List<QuotaAlertManagerImpl.DeferredQuotaEmail> deferredQuotaEmailListMock;

    @Mock
    private QuotaManagerImpl quotaManagerMock;

    @Mock
    private Date balanceDateMock;

    @Mock
    private AccountVO accountMock;

    @Spy
    @InjectMocks
    private QuotaAlertManagerImpl quotaAlertManager = new QuotaAlertManagerImpl();

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.Type.NORMAL);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);

        Mockito.doReturn(new BigDecimal(404)).when(quotaAccountVOMock).getQuotaBalance();
        Mockito.doReturn(new BigDecimal(100)).when(quotaAccountVOMock).getQuotaMinBalance();
        Mockito.doReturn(balanceDateMock).when(quotaAccountVOMock).getQuotaBalanceDate();
        Mockito.doReturn(null).when(quotaAccountVOMock).getQuotaAlertDate();
        Mockito.doReturn(0).when(quotaAccountVOMock).getQuotaEnforce();

        TransactionLegacy.open("QuotaAlertManagerImplTest");
    }

    @Test
    public void isQuotaEmailTypeEnabledForAccountTestConfigurationIsEnabledAndEmailIsConfiguredReturnConfiguredValue() {
        boolean expectedValue = !QuotaConfig.QuotaEnableEmails.value();
        QuotaEmailConfigurationVO quotaEmailConfigurationVoMock = Mockito.mock(QuotaEmailConfigurationVO.class);
        Mockito.when(quotaEmailConfigurationVoMock.isEnabled()).thenReturn(expectedValue);
        Mockito.doReturn(quotaEmailConfigurationVoMock).when(quotaEmailConfigurationDaoMock).findByAccountIdAndEmailTemplateType(Mockito.anyLong(), Mockito.any(QuotaConfig.QuotaEmailTemplateTypes.class));

        boolean result = quotaAlertManager.isQuotaEmailTypeEnabledForAccount(accountMock, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_EMPTY);

        Assert.assertEquals(expectedValue, result);
    }

    @Test
    public void isQuotaEmailTypeEnabledForAccountTestConfigurationIsEnabledAndEmailIsNotConfiguredReturnDefaultValue() {
        boolean defaultValue = QuotaConfig.QuotaEnableEmails.value();

        boolean result = quotaAlertManager.isQuotaEmailTypeEnabledForAccount(accountMock, QuotaConfig.QuotaEmailTemplateTypes.QUOTA_EMPTY);

        Assert.assertEquals(defaultValue, result);
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestNullAccountBalance() {
        Mockito.doReturn(null).when(quotaAccountVOMock).getQuotaBalance();
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(accountDao, Mockito.never()).findById(Mockito.any());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestNullAccount() {
        Mockito.doReturn(new BigDecimal(1)).when(quotaAccountVOMock).getQuotaBalance();
        Mockito.doReturn(null).when(accountDao).findById(Mockito.any());
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAccountVOMock, Mockito.never()).getQuotaBalanceDate();
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestEnoughBalance() {
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAlertManager, Mockito.never()).lockAccount(Mockito.anyLong());
        Mockito.verify(deferredQuotaEmailListMock, Mockito.never()).add(Mockito.any());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndLockAccountEnforcementFalse() {
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();

        quotaAlertManager._lockAccountEnforcement = false;
        Mockito.doReturn(1).when(quotaAccountVOMock).getQuotaEnforce();
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAlertManager, Mockito.never()).lockAccount(Mockito.anyLong());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndLockableFalse() {
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();

        quotaAlertManager._lockAccountEnforcement = true;
        Mockito.doReturn(1).when(quotaAccountVOMock).getQuotaEnforce();
        Mockito.doReturn(false).when(quotaManagerMock).isLockable(Mockito.any());
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAlertManager, Mockito.never()).lockAccount(Mockito.anyLong());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndIsLockableFalse() {
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();

        quotaAlertManager._lockAccountEnforcement = true;
        Mockito.doReturn(1).when(quotaAccountVOMock).getQuotaEnforce();
        Mockito.doReturn(false).when(quotaManagerMock).isLockable(Mockito.any());
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAlertManager, Mockito.never()).lockAccount(Mockito.anyLong());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndLockAccount() {
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();

        quotaAlertManager._lockAccountEnforcement = true;
        Mockito.doReturn(1).when(quotaAccountVOMock).getQuotaEnforce();
        Mockito.doReturn(true).when(quotaManagerMock).isLockable(Mockito.any());
        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(quotaAlertManager).lockAccount(Mockito.anyLong());
    }

    @Test
    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndAlertDateNotNullAndBalanceDateNotAfter() {
        Mockito.doReturn(new Date()).when(quotaAccountVOMock).getQuotaAlertDate();
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();
        Mockito.doReturn(false).when(balanceDateMock).after(Mockito.any());

        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(deferredQuotaEmailListMock, Mockito.never()).add(Mockito.any());
    }

    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndAlertDateNotNullAndGetDifferenceDaysSmallerThanOne() {
        Mockito.doReturn(new Date()).when(quotaAccountVOMock).getQuotaAlertDate();
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();
        Mockito.doReturn(true).when(balanceDateMock).after(Mockito.any());
        Mockito.doReturn(0L).when(quotaAlertManager).getDifferenceDays(Mockito.any(), Mockito.any());

        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(deferredQuotaEmailListMock, Mockito.never()).add(Mockito.any());
    }

    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndAlertDateNotNullAndBalanceAfterAndDifferenceBiggerThanOne() {
        Mockito.doReturn(new Date()).when(quotaAccountVOMock).getQuotaAlertDate();
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();
        Mockito.doReturn(true).when(balanceDateMock).after(Mockito.any());
        Mockito.doReturn(2).when(quotaAlertManager).getDifferenceDays(Mockito.any(), Mockito.any());

        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(deferredQuotaEmailListMock).add(Mockito.any());
    }

    public void checkQuotaAlertEmailForAccountTestBalanceLowerThanZeroAndAlertDateNull() {
        Mockito.doReturn(new BigDecimal(-1)).when(quotaAccountVOMock).getQuotaBalance();

        quotaAlertManager.checkQuotaAlertEmailForAccount(deferredQuotaEmailListMock, quotaAccountVOMock);
        Mockito.verify(deferredQuotaEmailListMock).add(Mockito.any());
    }

    @Test
    public void testSendQuotaAlert() throws UnsupportedEncodingException, MessagingException {
        Mockito.doCallRealMethod().when(quotaAlertManager).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));

        AccountVO account = new AccountVO();
        account.setId(2L);
        account.setDomainId(1L);
        account.setType(Account.Type.NORMAL);
        account.setAccountName("admin");
        account.setUuid("uuid");

        QuotaAccountVO quotaAccount = new QuotaAccountVO(2L);
        quotaAccount.setQuotaBalance(new BigDecimal(404));
        quotaAccount.setQuotaMinBalance(new BigDecimal(100));
        quotaAccount.setQuotaBalanceDate(new Date());
        quotaAccount.setQuotaAlertDate(null);
        quotaAccount.setQuotaEnforce(0);

        QuotaAlertManagerImpl.DeferredQuotaEmail email = new QuotaAlertManagerImpl.DeferredQuotaEmail(account, quotaAccount, new BigDecimal(100),
                QuotaConfig.QuotaEmailTemplateTypes.QUOTA_LOW);

        QuotaEmailTemplatesVO quotaEmailTemplatesVO = new QuotaEmailTemplatesVO();
        quotaEmailTemplatesVO.setTemplateSubject("Low quota");
        quotaEmailTemplatesVO.setTemplateBody("Low quota {{accountID}}");
        List<QuotaEmailTemplatesVO> emailTemplates = new ArrayList<>();
        emailTemplates.add(quotaEmailTemplatesVO);
        Mockito.when(quotaEmailTemplateDao.listAllQuotaEmailTemplates(Mockito.anyString())).thenReturn(emailTemplates);

        DomainVO domain = new DomainVO();
        domain.setUuid("uuid");
        domain.setName("/domain");
        Mockito.when(domainDao.findByIdIncludingRemoved(Mockito.anyLong())).thenReturn(new DomainVO());

        UserVO user = new UserVO();
        user.setUsername("user1");
        user.setEmail("user1@apache.org");
        List<UserVO> users = new ArrayList<>();
        users.add(user);
        Mockito.when(userDao.listByAccount(Mockito.anyLong())).thenReturn(users);

        quotaAlertManager.mailSender = Mockito.mock(SMTPMailSender.class);
        Mockito.doNothing().when(quotaAlertManager.mailSender).sendMail(Mockito.any());

        quotaAlertManager.sendQuotaAlert(email);
        assertTrue(email.getSendDate() != null);

        Mockito.verify(quotaAlertManager, Mockito.times(1)).sendQuotaAlert(Mockito.any(), Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(quotaAlertManager.mailSender, Mockito.times(1)).sendMail(Mockito.any(SMTPMailProperties.class));
    }

    @Test
    public void addHeaderAndFooterTestIfHeaderAndFootersAreAdded() {
        String body = quotaAlertManager.addHeaderAndFooter("body", "Header", "Footer");
        assertEquals("HeaderbodyFooter", body);
    }

    @Test
    public void addHeaderAndFooterTestIfHeaderAndFootersAreNotAddedIfEmpty() {
        String body = quotaAlertManager.addHeaderAndFooter("body", "", "");
        assertEquals("body", body);
    }

    @Test
    public void testGetDifferenceDays() {
        Date now = new Date();
        assertTrue(quotaAlertManager.getDifferenceDays(now, now) == 0L);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar c2 = (Calendar)c.clone();
        c2.add(Calendar.DATE, 1);
        assertEquals(1L, quotaAlertManager.getDifferenceDays(c.getTime(), c2.getTime()));
    }

    @Test
    public void testLockAccount() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.Type.NORMAL);
        accountVO.setState(Account.State.ENABLED);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);
        Mockito.when(accountDao.createForUpdate()).thenReturn(accountVO);
        Mockito.when(accountDao.update(Mockito.eq(accountVO.getId()), Mockito.eq(accountVO))).thenReturn(true);
        assertTrue(quotaAlertManager.lockAccount(accountVO.getId()));
    }
}
