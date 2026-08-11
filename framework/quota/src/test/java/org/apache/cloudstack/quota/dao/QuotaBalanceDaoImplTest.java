
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QuotaBalanceDaoImplTest {
    QuotaBalanceDaoImpl quotaBalanceDaoImplSpy = Mockito.spy(QuotaBalanceDaoImpl.class);

    @Mock
    QuotaBalanceVO quotaBalanceVoMock;

    @Test
    public void getLastQuotaBalanceTestLastEntryIsNullAndNoCreditsReturnsZero() {
        Mockito.doReturn(null).when(quotaBalanceDaoImplSpy).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(new ArrayList<>()).when(quotaBalanceDaoImplSpy).findCreditBalances(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        BigDecimal result = quotaBalanceDaoImplSpy.getLastQuotaBalance(1L, 2L);

        Assert.assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void getLastQuotaBalanceTestReturnsLastEntryAndNoCredits() {
        BigDecimal expected = BigDecimal.valueOf(-1542.46);
        Mockito.doReturn(quotaBalanceVoMock).when(quotaBalanceDaoImplSpy).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(expected).when(quotaBalanceVoMock).getCreditBalance();
        Mockito.doReturn(new ArrayList<>()).when(quotaBalanceDaoImplSpy).findCreditBalances(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        BigDecimal result = quotaBalanceDaoImplSpy.getLastQuotaBalance(5L, 8L);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getLastQuotaBalanceTestReturnsLastEntryPlusCredits() {
        BigDecimal balance = BigDecimal.valueOf(-1542.46);
        BigDecimal credit1 = new BigDecimal("150.14");
        BigDecimal credit2 = new BigDecimal("78.96");
        BigDecimal expected = balance.add(credit1).add(credit2);

        Mockito.doReturn(quotaBalanceVoMock).when(quotaBalanceDaoImplSpy).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(balance, credit1, credit2).when(quotaBalanceVoMock).getCreditBalance();
        Mockito.doReturn(Arrays.asList(quotaBalanceVoMock, quotaBalanceVoMock)).when(quotaBalanceDaoImplSpy).findCreditBalances(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        BigDecimal result = quotaBalanceDaoImplSpy.getLastQuotaBalance(5L, 8L);

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getLastQuotaBalanceTestReturnsLastEntryIsNullPlusCredits() {
        BigDecimal credit1 = new BigDecimal("150.14");
        BigDecimal credit2 = new BigDecimal("78.96");
        BigDecimal expected = credit1.add(credit2);

        Mockito.doReturn(null).when(quotaBalanceDaoImplSpy).getLastQuotaBalanceEntry(Mockito.anyLong(), Mockito.anyLong(), Mockito.any());
        Mockito.doReturn(credit1, credit2).when(quotaBalanceVoMock).getCreditBalance();
        Mockito.doReturn(Arrays.asList(quotaBalanceVoMock, quotaBalanceVoMock)).when(quotaBalanceDaoImplSpy).findCreditBalances(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(), Mockito.any());

        BigDecimal result = quotaBalanceDaoImplSpy.getLastQuotaBalance(5L, 8L);

        Assert.assertEquals(expected, result);
    }
}
