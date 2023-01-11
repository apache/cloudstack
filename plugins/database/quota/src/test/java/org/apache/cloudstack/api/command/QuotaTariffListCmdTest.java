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

import junit.framework.TestCase;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.cloud.utils.Pair;

@RunWith(MockitoJUnitRunner.class)
public class QuotaTariffListCmdTest extends TestCase {
    @Mock
    QuotaResponseBuilder responseBuilder;

    @Test
    public void testQuotaTariffListCmd() throws NoSuchFieldException, IllegalAccessException {
        QuotaTariffListCmd cmd = new QuotaTariffListCmd();

        Field rbField = QuotaTariffListCmd.class.getDeclaredField("_responseBuilder");
        rbField.setAccessible(true);
        rbField.set(cmd, responseBuilder);

        List<QuotaTariffVO> quotaTariffVOList = new ArrayList<QuotaTariffVO>();
        QuotaTariffVO tariff = new QuotaTariffVO();
        tariff.setEffectiveOn(new Date());
        tariff.setCurrencyValue(new BigDecimal(100));
        tariff.setUsageType(QuotaTypes.VOLUME);

        quotaTariffVOList.add(new QuotaTariffVO());
        Mockito.when(responseBuilder.listQuotaTariffPlans(Mockito.eq(cmd))).thenReturn(new Pair<>(quotaTariffVOList, quotaTariffVOList.size()));
        Mockito.when(responseBuilder.createQuotaTariffResponse(Mockito.any(QuotaTariffVO.class))).thenReturn(new QuotaTariffResponse());

        cmd.execute();
        Mockito.verify(responseBuilder, Mockito.times(1)).createQuotaTariffResponse(Mockito.any(QuotaTariffVO.class));
    }
}
