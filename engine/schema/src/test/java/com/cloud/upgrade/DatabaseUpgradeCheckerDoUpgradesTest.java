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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.utils.db.GlobalLock;
import org.junit.Test;

public class DatabaseUpgradeCheckerDoUpgradesTest {

    static class StubVersionDao extends VersionDaoImpl implements VersionDao {
        private final String currentVersion;

        StubVersionDao(String currentVersion) {
            this.currentVersion = currentVersion;
        }

        @Override
        public VersionVO findByVersion(String version, VersionVO.Step step) {
            return null;
        }

        @Override
        public String getCurrentVersion() {
            return currentVersion;
        }

    }

    private static class TestableChecker extends DatabaseUpgradeChecker {
        boolean initializeCalled = false;
        boolean upgradeCalled = false;
        boolean clusterHandlerCalled = false;
        String implVersionOverride = null;
        String sysVmMetadataOverride = "4.8.0";
        boolean standaloneOverride = true;

        TestableChecker(String daoVersion) {
            // set a stub DAO
            this._dao = new StubVersionDao(daoVersion);
        }

        @Override
        protected void initializeDatabaseEncryptors() {
            initializeCalled = true;
            // noop instead of doing DB work
        }

        @Override
        protected String getImplementationVersion() {
            return implVersionOverride;
        }

        @Override
        protected String parseSystemVmMetadata() {
            return sysVmMetadataOverride;
        }

        @Override
        boolean isStandalone() {
            return standaloneOverride;
        }

        @Override
        protected void upgrade(org.apache.cloudstack.utils.CloudStackVersion dbVersion, org.apache.cloudstack.utils.CloudStackVersion currentVersion) {
            upgradeCalled = true;
        }

        @Override
        protected void handleClusteredUpgradeRequired() {
            clusterHandlerCalled = true;
        }
    }

    @Test
    public void testDoUpgrades_noImplementationVersion_returnsEarly() {
        TestableChecker checker = new TestableChecker("4.8.0");
        checker.implVersionOverride = ""; // blank -> should return early

        GlobalLock lock = GlobalLock.getInternLock("test-noimpl");
        try {
            // acquire lock so doUpgrades can safely call unlock in finally
            lock.lock(1);
            checker.doUpgrades(lock);
        } finally {
            // ensure lock released if still held
            lock.releaseRef();
        }

        assertTrue("initializeDatabaseEncryptors should be called before returning", checker.initializeCalled);
        assertFalse("upgrade should not be called when implementation version is blank", checker.upgradeCalled);
        assertFalse("cluster handler should not be called", checker.clusterHandlerCalled);
    }

    @Test
    public void testDoUpgrades_dbUpToDate_noUpgrade() {
        // DB version = code version -> no upgrade
        TestableChecker checker = new TestableChecker("4.8.1");
        checker.implVersionOverride = "4.8.1";
        checker.sysVmMetadataOverride = "4.8.1";

        GlobalLock lock = GlobalLock.getInternLock("test-uptodate");
        try {
            lock.lock(1);
            checker.doUpgrades(lock);
        } finally {
            lock.releaseRef();
        }

        assertTrue(checker.initializeCalled);
        assertFalse(checker.upgradeCalled);
        assertFalse(checker.clusterHandlerCalled);
    }

    @Test
    public void testDoUpgrades_requiresUpgrade_standalone_invokesUpgrade() {
        TestableChecker checker = new TestableChecker("4.8.0");
        checker.implVersionOverride = "4.8.2"; // code is newer than DB
        checker.sysVmMetadataOverride = "4.8.2";
        checker.standaloneOverride = true;

        GlobalLock lock = GlobalLock.getInternLock("test-upgrade-standalone");
        try {
            lock.lock(1);
            checker.doUpgrades(lock);
        } finally {
            lock.releaseRef();
        }

        assertTrue(checker.initializeCalled);
        assertTrue("upgrade should be invoked in standalone mode", checker.upgradeCalled);
        assertFalse(checker.clusterHandlerCalled);
    }

    @Test
    public void testDoUpgrades_requiresUpgrade_clustered_invokesHandler() {
        TestableChecker checker = new TestableChecker("4.8.0");
        checker.implVersionOverride = "4.8.2"; // code is newer than DB
        checker.sysVmMetadataOverride = "4.8.2";
        checker.standaloneOverride = false;

        GlobalLock lock = GlobalLock.getInternLock("test-upgrade-clustered");
        try {
            lock.lock(1);
            checker.doUpgrades(lock);
        } finally {
            lock.releaseRef();
        }

        assertTrue(checker.initializeCalled);
        assertFalse("upgrade should not be invoked in clustered mode", checker.upgradeCalled);
        assertTrue("cluster handler should be invoked in clustered mode", checker.clusterHandlerCalled);
    }
}
