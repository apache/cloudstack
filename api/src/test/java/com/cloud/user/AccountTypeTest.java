// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.user;

import org.junit.Assert;
import org.junit.Test;

public class AccountTypeTest {

    @Test
    public void ordinalTestIfInCorrectOrder(){
        Assert.assertEquals(0, Account.Type.NORMAL.ordinal());
        Assert.assertEquals(1, Account.Type.ADMIN.ordinal());
        Assert.assertEquals(2, Account.Type.DOMAIN_ADMIN.ordinal());
        Assert.assertEquals(3, Account.Type.RESOURCE_DOMAIN_ADMIN.ordinal());
        Assert.assertEquals(4, Account.Type.READ_ONLY_ADMIN.ordinal());
        Assert.assertEquals(5, Account.Type.PROJECT.ordinal());
        Assert.assertEquals(6, Account.Type.UNKNOWN.ordinal());
    }

    @Test
    public void getFromValueTestIfAllValuesAreReturned(){
        for (Account.Type accountType: Account.Type.values()) {
            if( accountType != Account.Type.UNKNOWN) {
                Assert.assertEquals(Account.Type.getFromValue(accountType.ordinal()), accountType);
            }
        }
    }

    @Test
    public void getFromValueTestInvalidValue(){
        Assert.assertEquals(Account.Type.getFromValue(null),null);
    }

    @Test
    public void stateToStringTestAllValues(){
        Assert.assertEquals(Account.State.ENABLED.toString(), "enabled");
        Assert.assertEquals(Account.State.DISABLED.toString(), "disabled");
        Assert.assertEquals(Account.State.LOCKED.toString(), "locked");
    }
}
