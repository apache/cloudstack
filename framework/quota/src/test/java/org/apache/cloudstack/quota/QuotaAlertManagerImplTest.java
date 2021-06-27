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
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

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

    @Spy
    @InjectMocks
    private QuotaAlertManagerImpl quotaAlertManager = new QuotaAlertManagerImpl();

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        TransactionLegacy.open("QuotaAlertManagerImplTest");
    }

    @Test
    public void testCheckAndSendQuotaAlertEmails() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.ACCOUNT_TYPE_NORMAL);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);

        QuotaAccountVO acc = new QuotaAccountVO(2L);
        acc.setQuotaBalance(new BigDecimal(404));
        acc.setQuotaMinBalance(new BigDecimal(100));
        acc.setQuotaBalanceDate(new Date());
        acc.setQuotaAlertDate(null);
        acc.setQuotaEnforce(0);
        List<QuotaAccountVO> accounts = new ArrayList<>();
        accounts.add(acc);
        Mockito.when(quotaAcc.listAllQuotaAccount()).thenReturn(accounts);

        // Don't test sendQuotaAlert yet
        Mockito.doNothing().when(quotaAlertManager).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));
        Mockito.lenient().doReturn(true).when(quotaAlertManager).lockAccount(Mockito.anyLong());

        // call real method on send monthly statement
        Mockito.doCallRealMethod().when(quotaAlertManager).checkAndSendQuotaAlertEmails();

        // Case1: valid balance, no email should be sent
        quotaAlertManager.checkAndSendQuotaAlertEmails();
        Mockito.verify(quotaAlertManager, Mockito.times(0)).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));

        // Case2: low balance, email should be sent
        accounts.get(0).setQuotaBalance(new BigDecimal(99));
        //Mockito.when(quotaAcc.listAll()).thenReturn(accounts);
        quotaAlertManager.checkAndSendQuotaAlertEmails();
        Mockito.verify(quotaAlertManager, Mockito.times(1)).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));
    }

    @Test
    public void testSendQuotaAlert() throws UnsupportedEncodingException, MessagingException {
        Mockito.doCallRealMethod().when(quotaAlertManager).sendQuotaAlert(Mockito.any(QuotaAlertManagerImpl.DeferredQuotaEmail.class));

        AccountVO account = new AccountVO();
        account.setId(2L);
        account.setDomainId(1L);
        account.setType(Account.ACCOUNT_TYPE_NORMAL);
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
        Mockito.when(quotaAlertManager.mailSender.sendMail(Mockito.anyObject())).thenReturn(Boolean.TRUE);

        quotaAlertManager.sendQuotaAlert(email);
        assertTrue(email.getSendDate() != null);
        Mockito.verify(quotaAlertManager, Mockito.times(1)).sendQuotaAlert(Mockito.anyListOf(String.class), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(quotaAlertManager.mailSender, Mockito.times(1)).sendMail(Mockito.any(SMTPMailProperties.class));
    }

    @Test
    public void testGetDifferenceDays() {
        Date now = new Date();
        assertTrue(QuotaAlertManagerImpl.getDifferenceDays(now, now) == 0L);
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar c2 = (Calendar)c.clone();
        c2.add(Calendar.DATE, 1);
        assertEquals(1L, QuotaAlertManagerImpl.getDifferenceDays(c.getTime(), c2.getTime()));
    }

    @Test
    public void testLockAccount() {
        AccountVO accountVO = new AccountVO();
        accountVO.setId(2L);
        accountVO.setDomainId(1L);
        accountVO.setType(Account.ACCOUNT_TYPE_NORMAL);
        accountVO.setState(Account.State.enabled);
        Mockito.when(accountDao.findById(Mockito.anyLong())).thenReturn(accountVO);
        Mockito.when(accountDao.createForUpdate()).thenReturn(accountVO);
        Mockito.when(accountDao.update(Mockito.eq(accountVO.getId()), Mockito.eq(accountVO))).thenReturn(true);
        assertTrue(quotaAlertManager.lockAccount(accountVO.getId()));
    }
}
