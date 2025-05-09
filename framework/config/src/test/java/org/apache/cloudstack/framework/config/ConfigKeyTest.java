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
package org.apache.cloudstack.framework.config;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.testing.EqualsTester;

import org.apache.cloudstack.framework.config.ConfigKey.Scope;

import com.cloud.utils.exception.CloudRuntimeException;

public class ConfigKeyTest {
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(new ConfigKey("cat", String.class, "naam", "nick", "bijnaam", true, Scope.Cluster),
                        new ConfigKey("hond", Boolean.class, "naam", "truus", "thrown name", false),
                        new ConfigKey(Long.class, "naam", "vis", "goud", "zwemt", true, Scope.Account, 3L)
                )
                .testEquals();
    }

    @Test
    public void testIsSameKeyAs() {
        ConfigKey key = new ConfigKey("cat", String.class, "naam", "nick", "bijnaam", true, Scope.Cluster);
        Assert.assertTrue("1 and one should be considered the same address", key.isSameKeyAs("naam"));
    }

    @Test(expected = CloudRuntimeException.class)
    public void testIsSameKeyAsThrowingCloudRuntimeException() {
        ConfigKey key = new ConfigKey("hond", Boolean.class, "naam", "truus", "thrown name", false);
        Assert.assertFalse("zero and 0L should be considered the same address", key.isSameKeyAs(0L));
    }

    @Test
    public void testDecode() {
        ConfigKey key = new ConfigKey("testcategoey", Boolean.class, "test", "true", "test descriptuin", false, List.of(Scope.Zone, Scope.StoragePool));
        int bitmask = key.getScopeBitmask();
        List<Scope> scopes = ConfigKey.Scope.decode(bitmask);
        Assert.assertEquals(bitmask, ConfigKey.Scope.getBitmask(scopes.toArray(new Scope[0])));
        for (Scope scope : scopes) {
            Assert.assertTrue(scope == Scope.Zone || scope == Scope.StoragePool);
        }
    }

    @Test
    public void testDecodeAsCsv() {
        ConfigKey key = new ConfigKey("testcategoey", Boolean.class, "test", "true", "test descriptuin", false, List.of(Scope.Zone, Scope.StoragePool));
        int bitmask = key.getScopeBitmask();
        String scopes = ConfigKey.Scope.decodeAsCsv(bitmask);
        Assert.assertTrue("Zone, StoragePool".equals(scopes));
    }

    @Test
    public void testGetDescendants() {
        List<Scope> descendants = ConfigKey.Scope.getAllDescendants(Scope.Zone.name());
        for (Scope descendant : descendants) {
            Assert.assertTrue(descendant == Scope.Cluster || descendant == Scope.StoragePool || descendant == Scope.ImageStore);
        }
    }
}
