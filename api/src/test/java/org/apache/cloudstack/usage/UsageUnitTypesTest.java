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

package org.apache.cloudstack.usage;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UsageUnitTypesTest {

    private List<UsageUnitTypes> usageUnitTypes = Arrays.asList(UsageUnitTypes.values());

    @Test
    public void getByDescriptionTestAllTheDescriptionsMustReturnUsageUnitTypes() {
        usageUnitTypes.forEach(type -> {
            UsageUnitTypes usageUnitType = UsageUnitTypes.getByDescription(type.toString());
            Assert.assertEquals(type, usageUnitType);
        });
    }

    @Test
    public void getByDescriptionTestAllTheConstantNamesMustReturnUsageUnitTypes() {
        usageUnitTypes.forEach(type -> {
            UsageUnitTypes usageUnitType = UsageUnitTypes.getByDescription(type.name());
            Assert.assertEquals(type, usageUnitType);
        });
    }

    @Test (expected = IllegalArgumentException.class)
    public void getByDescriptionTestPassWrongTypeOrDescriptionAndThrowsIllegalArgumentException() {
        UsageUnitTypes.getByDescription("test");
    }
}
