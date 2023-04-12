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
package org.apache.cloudstack.quota.constant;

import junit.framework.TestCase;

import org.apache.cloudstack.api.response.UsageTypeResponse;
import org.apache.cloudstack.usage.UsageTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class QuotaTypesTest extends TestCase {

    @Test
    public void testQuotaTypesList() {
        Map<Integer, QuotaTypes> quotaTypes = QuotaTypes.listQuotaTypes();
        List<UsageTypeResponse> usageTypesResponseList = UsageTypes.listUsageTypes();
        for (UsageTypeResponse usageTypeResponse : usageTypesResponseList) {
            final Integer usageTypeInt = usageTypeResponse.getUsageType();
            assertTrue(quotaTypes.containsKey(usageTypeInt));
        }
    }

    @Test
    public void testQuotaTypeDescription() {
        assertNull(QuotaTypes.getDescription(-1));
        assertNotNull(QuotaTypes.getDescription(QuotaTypes.VOLUME));
    }
}
