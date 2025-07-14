package com.cloud.upgrade.dao;

import com.cloud.utils.exception.CloudRuntimeException;

import java.io.InputStream;
import java.sql.Connection;

public class Upgrade42010to42020 implements DbUpgrade {
    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.20.1.0", "4.20.2.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.2.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42020.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[]{script};
    }

    @Override
    public void performDataMigration(Connection conn) {
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42020-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[]{script};
    }
}
