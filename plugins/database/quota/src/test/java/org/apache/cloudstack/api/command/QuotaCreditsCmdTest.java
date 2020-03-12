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
package org.apache.cloudstack.api.command;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;

import java.lang.reflect.Field;

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.QuotaService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaCreditsCmdTest extends TestCase {
    @Mock
    QuotaResponseBuilder responseBuilder;
    @Mock
    QuotaService quotaService;
    @Mock
    AccountService accountService;

    private QuotaCreditsCmd cmd;

    @Override
    @Before
    public void setUp() {
        cmd = new QuotaCreditsCmd();
    }

    @Test
    public void testQuotaCreditsCmd() throws NoSuchFieldException, IllegalAccessException {
        cmd.setAccountName("admin");
        cmd.setMinBalance(200.0);

        Field rbField = QuotaCreditsCmd.class.getDeclaredField("_responseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        Field qsbField = QuotaCreditsCmd.class.getDeclaredField("_quotaService");
        qsbField.setAccessible(true);
        qsbField.set(cmd, quotaService);

        Field asField = BaseCmd.class.getDeclaredField("_accountService");
        asField.setAccessible(true);
        asField.set(cmd, accountService);

        AccountVO acc = new AccountVO();
        acc.setId(2L);

        Mockito.when(accountService.getActiveAccountByName(nullable(String.class), nullable(Long.class))).thenReturn(acc);

        Mockito.when(responseBuilder.addQuotaCredits(nullable(Long.class), nullable(Long.class), nullable(Double.class), nullable(Long.class), nullable(Boolean.class))).thenReturn(new QuotaCreditsResponse());

        // No value provided test
        try {
            cmd.execute();
        } catch (ServerApiException e) {
            assertTrue(e.getErrorCode().equals(ApiErrorCode.PARAM_ERROR));
        }

        // With value provided test
        cmd.setValue(11.80);
        cmd.execute();
        Mockito.verify(quotaService, Mockito.times(0)).setLockAccount(anyLong(), anyBoolean());
        Mockito.verify(quotaService, Mockito.times(1)).setMinBalance(anyLong(), anyDouble());
        Mockito.verify(responseBuilder, Mockito.times(1)).addQuotaCredits(nullable(Long.class), nullable(Long.class), nullable(Double.class), nullable(Long.class), nullable(Boolean.class));
    }

}
