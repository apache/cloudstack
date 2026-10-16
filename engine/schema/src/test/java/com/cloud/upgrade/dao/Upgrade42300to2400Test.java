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
package com.cloud.upgrade.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;

import com.cloud.upgrade.NetworkRateBackfill;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42300to2400Test {

    private final Upgrade42300to2400 upgrade = new Upgrade42300to2400();

    @Test
    public void upgradesFrom4230To2400() {
        Assert.assertArrayEquals(new String[]{"4.23.0.0", "24.0.0"}, upgrade.getUpgradableVersionRange());
        Assert.assertEquals("24.0.0", upgrade.getUpgradedVersion());
    }

    @Test
    public void prepareScriptIsFound() throws IOException {
        final InputStream[] scripts = upgrade.getPrepareScripts();

        Assert.assertEquals(1, scripts.length);
        Assert.assertNotNull(scripts[0]);
        scripts[0].close();
    }

    @Test(expected = CloudRuntimeException.class)
    public void prepareScriptMissingFails() {
        final ClassLoader original = Thread.currentThread().getContextClassLoader();
        // a loader that has no parent can't see the script
        Thread.currentThread().setContextClassLoader(new ClassLoader(null) { });
        try {
            upgrade.getPrepareScripts();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    @Test
    public void dataMigrationBackfillsNetworkRatesOnTheUpgradeConnection() {
        final Connection conn = Mockito.mock(Connection.class);

        final List<Object> constructorArgs = new ArrayList<>();
        try (MockedConstruction<NetworkRateBackfill> backfills = Mockito.mockConstruction(NetworkRateBackfill.class,
                (mock, context) -> constructorArgs.addAll(context.arguments()))) {
            upgrade.performDataMigration(conn);

            Assert.assertEquals(1, backfills.constructed().size());
            Assert.assertEquals(List.of(conn), constructorArgs);
            Mockito.verify(backfills.constructed().get(0)).backfillNetworkRates();
        }
    }
}
