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

import com.cloud.utils.exception.CloudRuntimeException;

import java.io.InputStream;
import java.sql.Connection;

public interface DbUpgrade {
    String[] getUpgradableVersionRange();

    String getUpgradedVersion();

    default boolean supportsRollingUpgrade() {
        return false;
    }

    /**
     * @return the script to prepare the database schema for the
     * data migration step.
     */
    default InputStream[] getPrepareScripts() {
        String fromVersion = getUpgradableVersionRange()[0];
        String toVersion = getUpgradableVersionRange()[1];
        final String scriptFile = String.format("META-INF/db/schema-%sto%s.sql", fromVersion.replace(".", ""), toVersion.replace(".", ""));
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[]{script};
    }

    /**
     * Performs the actual data migration.
     */
    default void performDataMigration(Connection conn) {
    }

    default InputStream[] getCleanupScripts() {
        String fromVersion = getUpgradableVersionRange()[0];
        String toVersion = getUpgradableVersionRange()[1];
        final String scriptFile = String.format("META-INF/db/schema-%sto%s-cleanup.sql", fromVersion.replace(".", ""), toVersion.replace(".", ""));
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[]{script};
    }

    default boolean refreshPoolConnectionsAfterUpgrade() {
        return false;
    }
}
