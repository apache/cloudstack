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

package org.apache.cloudstack.quota.dao;

import com.cloud.utils.Pair;
import org.apache.cloudstack.quota.vo.QuotaAccountVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class QuotaAccountDaoImplTest {
    @Spy
    QuotaAccountDaoImpl quotaAccountDaoImplSpy;

    @Test
    public void listAllQuotaAccountTestShouldReturnNullWithAccountWithQuotaDisabled() {
        QuotaAccountVO accountWithQuotaDisabled = new QuotaAccountVO(1L);

        List<QuotaAccountVO> allQuotaAccounts = List.of(accountWithQuotaDisabled);
        Pair<List<QuotaAccountVO>,Integer> pair = new Pair<>(allQuotaAccounts, 1);

        Mockito.doReturn(pair).when(quotaAccountDaoImplSpy).listAllQuotaAccount(null, null);
        Mockito.doReturn(false).when(quotaAccountDaoImplSpy).getQuotaAccountEnabled(accountWithQuotaDisabled.getAccountId());

        int expected = quotaAccountDaoImplSpy.listAllQuotaAccount().size();
        Assert.assertEquals(0, expected);
    }

    @Test
    public void listAllQuotaAccountTestShouldReturnSizeOneWithAccountWithQuotaEnabled() {
        QuotaAccountVO accountWithQuotaEnabled = new QuotaAccountVO(2L);

        List<QuotaAccountVO> allQuotaAccounts = List.of(accountWithQuotaEnabled);
        Pair<List<QuotaAccountVO>,Integer> pair = new Pair<>(allQuotaAccounts, 1);

        Mockito.doReturn(pair).when(quotaAccountDaoImplSpy).listAllQuotaAccount(null, null);
        Mockito.doReturn(true).when(quotaAccountDaoImplSpy).getQuotaAccountEnabled(accountWithQuotaEnabled.getAccountId());

        int expected = quotaAccountDaoImplSpy.listAllQuotaAccount().size();
        Assert.assertEquals(1, expected);
    }

    @Test
    public void listAllQuotaAccountTestShouldReturnOnlyAccountsWithQuotaEnabled() {
        QuotaAccountVO accountWithQuotaEnabled = new QuotaAccountVO(1L);
        QuotaAccountVO accountWithQuotaDisabled = new QuotaAccountVO(2L);

        List<QuotaAccountVO> allQuotaAccounts = List.of(accountWithQuotaEnabled, accountWithQuotaDisabled);
        Pair<List<QuotaAccountVO>,Integer> pair = new Pair<>(allQuotaAccounts, 1);

        Mockito.doReturn(pair).when(quotaAccountDaoImplSpy).listAllQuotaAccount(null, null);
        Mockito.doReturn(true).when(quotaAccountDaoImplSpy).getQuotaAccountEnabled(accountWithQuotaEnabled.getAccountId());
        Mockito.doReturn(false).when(quotaAccountDaoImplSpy).getQuotaAccountEnabled(accountWithQuotaDisabled.getAccountId());

        int expected = quotaAccountDaoImplSpy.listAllQuotaAccount().size();
        Assert.assertEquals(1, expected);
    }

}
