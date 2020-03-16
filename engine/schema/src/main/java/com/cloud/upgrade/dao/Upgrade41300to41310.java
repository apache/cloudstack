package com.cloud.upgrade.dao;

import java.io.InputStream;
import java.sql.Connection;

public class Upgrade41300to41310 implements DbUpgrade {
    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.13.0.0", "4.13.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.13.1.0";
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
