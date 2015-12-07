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

import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.TransactionLegacy;
import junit.framework.TestCase;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.quota.constant.QuotaConfig;
import org.apache.cloudstack.quota.dao.QuotaAccountDao;
import org.apache.cloudstack.quota.dao.QuotaEmailTemplatesDao;
import org.apache.cloudstack.quota.dao.QuotaUsageDao;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.apache.cloudstack.quota.vo.QuotaEmailTemplatesVO;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import javax.mail.MessagingException;
import javax.naming.ConfigurationException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class QuotaAlertManagerImplTest extends TestCase {

    @Mock
    AccountDao accountDao;
    @Mock
    QuotaAccountDao quotaAcc;
    @Mock
    UserDao userDao;
    @Mock
    DomainDao domainDao;
    @Mock
    QuotaEmailTemplatesDao quotaEmailTemplateDao;
    @Mock
    ConfigurationDao configDao;
    @Mock
    QuotaUsageDao quotaUsage;
    @Mock
    QuotaAlertManagerImpl.EmailQuotaAlert emailQuotaAlert;

    @Spy
    QuotaAlertManagerImpl quotaAlertManager = new QuotaAlertManagerImpl();

    private void injectMockToField(Object mock, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field f = QuotaAlertManagerImpl.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(quotaAlertManager, mock);
    }

    @Before
    public void setup() throws IllegalAccessException, NoSuchFieldException, ConfigurationException {
        // Dummy transaction stack setup
        TransactionLegacy.open("QuotaAlertManagerImplTest");

        injectMockToField(accountDao, "_accountDao");
        injectMockToField(quotaAcc, "_quotaAcc");
        injectMockToField(userDao, "_userDao");
        injectMockToField(domainDao, "_domainDao");
        injectMockToField(quotaEmailTemplateDao, "_quotaEmailTemplateDao");
        injectMockToField(configDao, "_configDao");
        injectMockToField(quotaUsage, "_quotaUsage");
        injectMockToField(emailQuotaAlert, "_emailQuotaAlert");
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
        Mockito.doReturn(true).when(quotaAlertManager).lockAccount(Mockito.anyLong());

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

        quotaAlertManager.sendQuotaAlert(email);
        assertTrue(email.getSendDate()!= null);
        Mockito.verify(emailQuotaAlert, Mockito.times(1)).sendQuotaAlert(Mockito.anyList(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testGetDifferenceDays() {
        Date now = new Date();
        assertTrue(QuotaAlertManagerImpl.getDifferenceDays(now, now) == 0L);
        assertTrue(QuotaAlertManagerImpl.getDifferenceDays(now, new DateTime(now).plusDays(1).toDate()) == 1L);
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
