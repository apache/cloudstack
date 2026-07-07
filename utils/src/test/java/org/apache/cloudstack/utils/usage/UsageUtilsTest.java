//
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
//

package org.apache.cloudstack.utils.usage;

import com.cloud.utils.DateUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.TimeZone;

@RunWith(MockitoJUnitRunner.class)
public class UsageUtilsTest extends TestCase {

    TimeZone usageTimeZone = TimeZone.getTimeZone("GMT-3");

    @Test
    public void getJobExecutionTimeTestReturnsNullWhenConfigurationValueIsInvalid() {
        Date result = UsageUtils.getNextJobExecutionTime(usageTimeZone, "test");
        assertNull(result);
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenNextIsTrueAndExecutionTimeHasNotPassed() {
        Date currentDate = new Date();
        currentDate.setTime(1724296800000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "00:30", true);

            Assert.assertNotNull(result);
            Assert.assertEquals(1724297400000L, result.getTime());
        }
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenNextIsTrueAndExecutionTimeHasPassed() {
        Date currentDate = new Date();
        currentDate.setTime(1724297460000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "00:30", true);

            Assert.assertNotNull(result);
            Assert.assertEquals(1724383800000L, result.getTime());
        }
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenNextIsFalseAndExecutionTimeHasNotPassed() {
        Date currentDate = new Date();
        currentDate.setTime(1724296800000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "00:30", false);

            Assert.assertNotNull(result);
            Assert.assertEquals(1724211000000L, result.getTime());
        }
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenNextIsFalseAndExecutionTimeHasPassed() {
        Date currentDate = new Date();
        currentDate.setTime(1724297460000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "00:30", false);

            Assert.assertNotNull(result);
            Assert.assertEquals(1724297400000L, result.getTime());
        }
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenNextExecutionIsOnNextYear() {
        Date currentDate = new Date();
        currentDate.setTime(1767236340000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "00:00", true);

            Assert.assertNotNull(result);
            Assert.assertEquals(1767236400000L, result.getTime());
        }
    }

    @Test
    public void getJobExecutionTimeTestReturnsExpectedDateWhenPreviousExecutionWasOnPreviousYear() {
        Date currentDate = new Date();
        currentDate.setTime(1767236460000L);

        try (MockedStatic<DateUtil> dateUtilMockedStatic = Mockito.mockStatic(DateUtil.class)) {
            dateUtilMockedStatic.when(DateUtil::currentGMTTime).thenReturn(currentDate);

            Date result = UsageUtils.getJobExecutionTime(usageTimeZone, "23:59", false);

            Assert.assertNotNull(result);
            Assert.assertEquals(1767236340000L, result.getTime());
        }
    }

}
