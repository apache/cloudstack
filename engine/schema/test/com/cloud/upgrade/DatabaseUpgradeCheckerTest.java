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
package com.cloud.upgrade;

import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade452to460;
import com.cloud.upgrade.dao.Upgrade460to461;
import com.cloud.upgrade.dao.Upgrade461to470;
import com.cloud.upgrade.dao.Upgrade470to471;
import com.cloud.upgrade.dao.Upgrade471to480;
import com.cloud.upgrade.dao.Upgrade480to481;
import com.cloud.upgrade.dao.Upgrade490to4910;
import com.cloud.upgrade.dao.Upgrade41100to41110;
import org.apache.cloudstack.utils.CloudStackVersion;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseUpgradeCheckerTest {

    @Test
    public void testCalculateUpgradePath480to481() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.8.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.8.1");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);
        assertTrue(upgrades.length >= 1);
        assertTrue(upgrades[0] instanceof Upgrade480to481);

    }

    @Test
    public void testCalculateUpgradePath490to4910() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.9.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.9.1.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);
        assertTrue(upgrades.length >= 1);
        assertTrue(upgrades[0] instanceof Upgrade490to4910);

        assertTrue(Arrays.equals(new String[] { "4.9.0", currentVersion.toString()}, upgrades[0].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[0].getUpgradedVersion());

    }

    @Test
    public void testCalculateUpgradePath4110to4111() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.11.0.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.11.1.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);
        assertTrue(upgrades.length >= 1);
        assertTrue(upgrades[0] instanceof Upgrade41100to41110);

        assertTrue(Arrays.equals(new String[] { "4.11.0.0", currentVersion.toString()},
                upgrades[0].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[0].getUpgradedVersion());

    }

    @Test
    public void testFindUpgradePath470to481() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.7.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.8.1");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);

        assertTrue(upgrades[0] instanceof Upgrade470to471);
        assertTrue(upgrades[1] instanceof Upgrade471to480);
        assertTrue(upgrades[2] instanceof Upgrade480to481);

    }

    @Test
    public void testFindUpgradePath452to490() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.5.2");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.9.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);

        assertTrue(upgrades[0] instanceof Upgrade452to460);
        assertTrue(upgrades[1] instanceof Upgrade460to461);
        assertTrue(upgrades[2] instanceof Upgrade461to470);
        assertTrue(upgrades[3] instanceof Upgrade470to471);
        assertTrue(upgrades[4] instanceof Upgrade471to480);
        assertTrue(upgrades[5] instanceof Upgrade480to481);

        assertTrue(Arrays.equals(new String[] { "4.8.1", currentVersion.toString()}, upgrades[6].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[6].getUpgradedVersion());

    }
}
