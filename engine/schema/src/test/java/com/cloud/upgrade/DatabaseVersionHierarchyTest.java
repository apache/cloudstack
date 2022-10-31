package com.cloud.upgrade;

import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade41110to41120;
import com.cloud.upgrade.dao.Upgrade41120to41130;
import com.cloud.upgrade.dao.Upgrade41120to41200;
import com.cloud.upgrade.dao.Upgrade41500to41510;
import com.cloud.upgrade.dao.Upgrade41510to41520;
import com.cloud.upgrade.dao.Upgrade41520to41600;
import com.cloud.upgrade.dao.Upgrade41710to41800;
import com.cloud.upgrade.dao.Upgrade481to490;
import org.apache.cloudstack.utils.CloudStackVersion;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseVersionHierarchyTest {

    private DatabaseVersionHierarchy hierarchy;

    class DummyUpgrade implements DbUpgrade {
        @Override
        public String[] getUpgradableVersionRange() {
            return new String[0];
        }

        @Override
        public String getUpgradedVersion() {
            return null;
        }

        @Override
        public boolean supportsRollingUpgrade() {
            return false;
        }

        @Override
        public InputStream[] getPrepareScripts() {
            return new InputStream[0];
        }

        @Override
        public void performDataMigration(Connection conn) {

        }

        @Override
        public InputStream[] getCleanupScripts() {
            return new InputStream[0];
        }

    }

    @Before
    public void init() {
        DatabaseVersionHierarchy.DatabaseVersionHierarchyBuilder builder = DatabaseVersionHierarchy.builder()
                .next("0.0.5", new DummyUpgrade())
                .next("1.0.0.0", new DummyUpgrade())
                .next("1.0.1"   , new DummyUpgrade())
                .next("1.2.0"   , new DummyUpgrade())
                .next("2.0.0"   , new DummyUpgrade())
                .next("2.3.2"   , new DummyUpgrade())
                .next("3.4.5.6"   , new DummyUpgrade())
                .next("4.8.2.0" , new Upgrade481to490())
                .next("4.9.10.11"   , new DummyUpgrade())
                .next("4.11.1.0", new Upgrade41110to41120())
                .next("4.11.2.0", new Upgrade41120to41130())
                .next("4.11.3.0", new Upgrade41120to41200())
                .next("4.15.0.0", new Upgrade41500to41510())
                .next("4.15.1.0", new Upgrade41510to41520())
                .next("4.15.2.0", new Upgrade41520to41600())
                .next("4.15.4", new DummyUpgrade())
                .next("4.17.1.0", new Upgrade41710to41800());
        hierarchy = builder.build();
    }

    @Test
    public void getRecentVersionMiddle() {
        init();
        assertEquals("2.0.0", hierarchy.getRecentVersion(CloudStackVersion.parse("2.2.2")).toString());
    }
    @Test
    public void getRecentVersionEarly() {
        init();
        assertEquals(null, hierarchy.getRecentVersion(CloudStackVersion.parse("0.0.2")));
    }
    @Test
    public void getRecentVersionStart() {
        init();
        assertEquals(null, hierarchy.getRecentVersion(CloudStackVersion.parse("0.0.5")));
    }
    @Test
    public void getRecentVersionJust() {
        init();
        assertEquals("0.0.5", hierarchy.getRecentVersion(CloudStackVersion.parse("0.0.9")).toString());
    }
    @Test
    public void getRecentVersionExact() {
        init();
        assertEquals("0.0.5", hierarchy.getRecentVersion(CloudStackVersion.parse("1.0.0.0")).toString());
    }
}