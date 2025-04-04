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

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QuotaCreditsListCmdTest {

    @Spy
    QuotaCreditsListCmd quotaCreditsListCmdSpy;

    @Test
    public void getEndDateTestReturnsNewDateWhenEndDateIsNull() {
        quotaCreditsListCmdSpy.setEndDate(null);

        try (MockedConstruction<Date> dateMockedConstruction = Mockito.mockConstruction(Date.class)) {
            Date result = quotaCreditsListCmdSpy.getEndDate();

            Assert.assertEquals(1, dateMockedConstruction.constructed().size());
            Assert.assertEquals(dateMockedConstruction.constructed().get(0), result);
        }
    }

    @Test
    public void getEndDateTestReturnsEndDateWhenItIsNotNull() {
        Date expected = new Date();
        quotaCreditsListCmdSpy.setEndDate(expected);

        Date result = quotaCreditsListCmdSpy.getEndDate();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getStartDateTestReturnsFirstDayOfTheCurrentMonthWhenStartDateIsNull() {
        quotaCreditsListCmdSpy.setStartDate(null);
        Date expected = DateUtils.truncate(new Date(), Calendar.MONTH);

        Date result = quotaCreditsListCmdSpy.getStartDate();

        Assert.assertEquals(expected, result);
    }

    @Test
    public void getStartDateTestReturnsStartDateWhenItIsNotNull() {
        Date expected = new Date();
        quotaCreditsListCmdSpy.setStartDate(expected);

        Date result = quotaCreditsListCmdSpy.getStartDate();

        Assert.assertEquals(expected, result);
    }
}
