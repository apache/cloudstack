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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.cloudstack.utils.CloudStackVersion;
import org.junit.Test;

import com.cloud.upgrade.DatabaseUpgradeChecker.NoopDbUpgrade;
import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade41000to41100;
import com.cloud.upgrade.dao.Upgrade41100to41110;
import com.cloud.upgrade.dao.Upgrade41110to41120;
import com.cloud.upgrade.dao.Upgrade41120to41130;
import com.cloud.upgrade.dao.Upgrade41120to41200;
import com.cloud.upgrade.dao.Upgrade41510to41520;
import com.cloud.upgrade.dao.Upgrade41610to41700;
import com.cloud.upgrade.dao.Upgrade452to453;
import com.cloud.upgrade.dao.Upgrade453to460;
import com.cloud.upgrade.dao.Upgrade460to461;
import com.cloud.upgrade.dao.Upgrade461to470;
import com.cloud.upgrade.dao.Upgrade470to471;
import com.cloud.upgrade.dao.Upgrade471to480;
import com.cloud.upgrade.dao.Upgrade480to481;
import com.cloud.upgrade.dao.Upgrade490to4910;

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

        assertTrue(Arrays.equals(new String[] {"4.9.0", currentVersion.toString()}, upgrades[0].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[0].getUpgradedVersion());

    }

    @Test
    public void testCalculateUpgradePath410to412() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.10.0.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.12.0.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);

        assertNotNull(upgrades);
        assertTrue(upgrades.length >= 1);
        assertTrue(upgrades[0] instanceof Upgrade41000to41100);
        assertTrue(upgrades[1] instanceof Upgrade41100to41110);
        assertTrue(upgrades[2] instanceof Upgrade41110to41120);
        assertTrue(upgrades[3] instanceof Upgrade41120to41130);
        assertTrue(upgrades[4] instanceof Upgrade41120to41200);

        assertTrue(Arrays.equals(new String[] {"4.11.0.0", "4.11.1.0"}, upgrades[1].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[4].getUpgradedVersion());

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

        assertTrue(upgrades[0] instanceof Upgrade452to453);
        assertTrue(upgrades[1] instanceof Upgrade453to460);
        assertTrue(upgrades[2] instanceof Upgrade460to461);
        assertTrue(upgrades[3] instanceof Upgrade461to470);
        assertTrue(upgrades[4] instanceof Upgrade470to471);
        assertTrue(upgrades[5] instanceof Upgrade471to480);
        assertTrue(upgrades[6] instanceof Upgrade480to481);

        assertTrue(Arrays.equals(new String[] {"4.8.1", currentVersion.toString()}, upgrades[upgrades.length - 1].getUpgradableVersionRange()));
        assertEquals(currentVersion.toString(), upgrades[upgrades.length - 1].getUpgradedVersion());
    }

    @Test
    public void testCalculateUpgradePathUnkownDbVersion() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.99.0.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.99.1.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);
        assertNotNull(upgrades);
        assertEquals("We should have 2 upgrade steps", 2, upgrades.length);
        assertTrue(upgrades[1] instanceof NoopDbUpgrade);

    }

    @Test
    public void testCalculateUpgradePathFromKownDbVersion() {

        final CloudStackVersion dbVersion = CloudStackVersion.parse("4.17.0.0");
        assertNotNull(dbVersion);

        final CloudStackVersion currentVersion = CloudStackVersion.parse("4.99.1.0");
        assertNotNull(currentVersion);

        final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
        final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);
        assertNotNull(upgrades);
        assertTrue(upgrades.length > 2);
        assertTrue(upgrades[upgrades.length - 1] instanceof NoopDbUpgrade);

    }

     @Test
    public void testCalculateUpgradePathFromUnregisteredSecVersion() {
         final CloudStackVersion dbVersion = CloudStackVersion.parse("4.15.1.3");
         assertNotNull(dbVersion);

         final CloudStackVersion currentVersion = CloudStackVersion.parse("4.17.0.0");
         assertNotNull(currentVersion);

         final DatabaseUpgradeChecker checker = new DatabaseUpgradeChecker();
         final DbUpgrade[] upgrades = checker.calculateUpgradePath(dbVersion, currentVersion);
         assertNotNull("there should be upgrade paths", upgrades);
         assertTrue(upgrades.length > 1);
         assertTrue(upgrades[0] instanceof Upgrade41510to41520);
         assertTrue(upgrades[upgrades.length - 1] instanceof Upgrade41610to41700);
     }
}
