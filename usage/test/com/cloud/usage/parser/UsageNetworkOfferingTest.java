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
package com.cloud.usage.parser;

import com.cloud.usage.UsageNetworkOfferingVO;
import com.cloud.usage.UsageVO;
import com.cloud.usage.dao.UsageDao;
import com.cloud.usage.dao.UsageNetworkOfferingDao;
import com.cloud.user.AccountVO;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RunWith(PowerMockRunner.class)
public class UsageNetworkOfferingTest extends TestCase {
    @Mock
    UsageNetworkOfferingDao usageNetworkOfferingDao;
    @Mock
    UsageDao usageDao;

    Date startDate = null;
    Date endDate = null;

    @Before
    public void setup() throws Exception {
        Calendar calendar = Calendar.getInstance();
        endDate = new Date(calendar.getTime().getTime());
        calendar.roll(Calendar.DAY_OF_YEAR, false);
        startDate = new Date(calendar.getTime().getTime());
    }

    @Test
    public void testNetworkOfferingAggregation() throws ConfigurationException {
        AccountVO account = new AccountVO();
        account.setId(2L);
        account.setDomainId(1L);
        UsageNetworkOfferingVO usageNo1 = new UsageNetworkOfferingVO(1L, 2L, 1L, 1L, 1L, 1L, false, startDate, null);
        UsageNetworkOfferingVO usageNo2 = new UsageNetworkOfferingVO(1L, 2L, 1L, 1L, 1L, 2L, false, startDate, null);
        List<UsageNetworkOfferingVO> usageNOs = new ArrayList<UsageNetworkOfferingVO>();
        usageNOs.add(usageNo1);
        usageNOs.add(usageNo2);
        PowerMockito.mockStatic(NetworkOfferingUsageParser.class);
        NetworkOfferingUsageParser.s_usageNetworkOfferingDao = usageNetworkOfferingDao;
        NetworkOfferingUsageParser.s_usageDao = usageDao;
        Mockito.when(usageNetworkOfferingDao.getUsageRecords(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(Date.class),
                Mockito.any(Date.class), Mockito.anyBoolean(), Mockito.anyInt())).thenReturn(usageNOs);
        NetworkOfferingUsageParser.parse(account, startDate, endDate);
        //Verify that 2 usage records are created for the same network Offering. One for each nicId
        Mockito.verify(usageDao, Mockito.times(2)).persist(Mockito.any(UsageVO.class));
    }
}
