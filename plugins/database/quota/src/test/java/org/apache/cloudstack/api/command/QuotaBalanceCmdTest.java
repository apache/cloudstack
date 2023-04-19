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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.cloudstack.api.response.QuotaBalanceResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.quota.vo.QuotaBalanceVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.TestCase;

@RunWith(MockitoJUnitRunner.class)
public class QuotaBalanceCmdTest extends TestCase {

    @Mock
    QuotaResponseBuilder responseBuilder;

    @Test
    public void testQuotaBalanceCmd() throws NoSuchFieldException, IllegalAccessException {
        QuotaBalanceCmd cmd = new QuotaBalanceCmd();
        Field rbField = QuotaBalanceCmd.class.getDeclaredField("_responseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        List<QuotaBalanceVO> quotaBalanceVOList = new ArrayList<QuotaBalanceVO>();
        Mockito.when(responseBuilder.getQuotaBalance(Mockito.any(cmd.getClass()))).thenReturn(quotaBalanceVOList);
        Mockito.when(responseBuilder.createQuotaLastBalanceResponse(Mockito.eq(quotaBalanceVOList), Mockito.any(Date.class))).thenReturn(new QuotaBalanceResponse());
        Mockito.when(responseBuilder.createQuotaBalanceResponse(Mockito.eq(quotaBalanceVOList), Mockito.any(Date.class), Mockito.any(Date.class))).thenReturn(new QuotaBalanceResponse());
        Mockito.lenient().when(responseBuilder.startOfNextDay(Mockito.any(Date.class))).thenReturn(new Date());

        // end date not specified
        cmd.setStartDate(new Date());
        cmd.setEndDate(null);
        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).createQuotaLastBalanceResponse(Mockito.eq(quotaBalanceVOList), Mockito.any(Date.class));
        Mockito.verify(responseBuilder, Mockito.times(0)).createQuotaBalanceResponse(Mockito.eq(quotaBalanceVOList), Mockito.any(Date.class), Mockito.any(Date.class));

        // end date specified
        cmd.setEndDate(new Date());
        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).createQuotaBalanceResponse(Mockito.eq(quotaBalanceVOList), Mockito.any(Date.class), Mockito.any(Date.class));
    }
}
