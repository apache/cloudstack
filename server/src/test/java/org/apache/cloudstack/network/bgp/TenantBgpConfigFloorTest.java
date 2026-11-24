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

package org.apache.cloudstack.network.bgp;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.junit.Assert;
import org.junit.Test;

/**
 * The global setting for the lowest BGP hold time a virtual router accepts.
 */
public class TenantBgpConfigFloorTest {

    static void assertKey(ConfigKey<?> key, String name, String defaultValue, ConfigKey.Scope... scopes) {
        Assert.assertEquals(name, key.key());
        String actualDefault = key.defaultValue();
        if (defaultValue.isEmpty()) {
            Assert.assertTrue(name + " must have no default", actualDefault == null || actualDefault.isEmpty());
        } else {
            Assert.assertEquals(name + " default", defaultValue, actualDefault);
        }
        Assert.assertTrue(name + " must be dynamic", key.isDynamic());
        List<ConfigKey.Scope> actualScopes = key.getScopes();
        for (ConfigKey.Scope scope : scopes) {
            Assert.assertTrue(name + " must have scope " + scope, actualScopes.contains(scope));
        }
        for (ConfigKey.Scope scope : Arrays.asList(ConfigKey.Scope.Zone, ConfigKey.Scope.Account, ConfigKey.Scope.Domain)) {
            if (!Arrays.asList(scopes).contains(scope)) {
                Assert.assertFalse(name + " must not have scope " + scope, actualScopes.contains(scope));
            }
        }
    }

    // The default minimum hold time of 3 refuses only a hold time of 0.
    @Test
    public void minHoldtimeDefaultsTo3() {
        assertKey(TenantBgpConfig.MinHoldtime, "vpc.gateway.bgp.min.holdtime", "3");
    }
}
