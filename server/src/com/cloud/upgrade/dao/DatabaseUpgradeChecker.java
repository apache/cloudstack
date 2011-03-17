/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.upgrade.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.maint.Version;
import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class DatabaseUpgradeChecker implements SystemIntegrityChecker {
    private final Logger s_logger = Logger.getLogger(DatabaseUpgradeChecker.class);
    
    protected HashMap<Pair<String, String>, DbUpgrade[]> _upgradeMap = new HashMap<Pair<String, String>, DbUpgrade[]>();

    VersionDao _dao;
    public DatabaseUpgradeChecker() {
        _dao = ComponentLocator.inject(VersionDaoImpl.class);
        _upgradeMap.put(new Pair<String, String>("2.1.7", "2.2.3"), new DbUpgrade[] { new Upgrade217to22(), new Upgrade221to222(), new UpgradeSnapshot217to223()});
    }
    
    protected void runScript(File file) {
        try {
            FileReader reader = new FileReader(file);
            Connection conn = Transaction.getStandaloneConnection();
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            runner.runScript(reader);
        } catch (FileNotFoundException e) {
            throw new CloudRuntimeException("Unable to find upgrade script, schema-21to22.sql", e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read upgrade script, schema-21to22.sql", e);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute upgrade script, schema-21to22.sql", e);
        }
    }
    
    protected void upgrade(String dbVersion, String currentVersion) {
        s_logger.info("Database upgrade must be performed from " + dbVersion + " to " + currentVersion);
        
        String trimmedDbVersion = Version.trimToPatch(dbVersion);
        String trimmedCurrentVersion = Version.trimToPatch(currentVersion);
        
        DbUpgrade[] upgrades = _upgradeMap.get(new Pair<String, String>(trimmedDbVersion, trimmedCurrentVersion));
        if (upgrades == null) {
            throw new CloudRuntimeException("There is no upgrade path from " + dbVersion + " to " + currentVersion);
        }

        if (Version.compare(trimmedCurrentVersion, upgrades[upgrades.length - 1].getUpgradedVersion()) != 0) {
            throw new CloudRuntimeException("The end upgrade version is actually at " + upgrades[upgrades.length - 1].getUpgradedVersion() + " but our management server code version is at " + currentVersion);
        }
        
        boolean supportsRollingUpgrade = true;
        for (DbUpgrade upgrade : upgrades) {
            if (!upgrade.supportsRollingUpgrade()) {
                supportsRollingUpgrade = false;
                break;
            }
        }
        
        if (!supportsRollingUpgrade) {
            // TODO: Check if the other management server is still running by looking at the database.  If so, then throw an exception.
        }

        for (DbUpgrade upgrade : upgrades) {
            s_logger.info("Running upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
            Transaction txn = Transaction.open("Upgrade");
            txn.start();
            try {
                Connection conn;
                try {
                    conn = txn.getConnection();
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Unable to upgrade the database", e);
                }
                File[] scripts = upgrade.getPrepareScripts();
                if (scripts != null) {
                    for (File script : scripts) {
                        runScript(script);
                    }
                }
                upgrade.performDataMigration(conn);
                VersionVO version = new VersionVO(upgrade.getUpgradedVersion());
                _dao.persist(version);
                txn.commit();
            } finally {
                txn.close();
            }
        }
        
        for (DbUpgrade upgrade : upgrades) {
            s_logger.info("Cleanup upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
            VersionVO version = _dao.findByVersion(upgrade.getUpgradedVersion(), Step.Upgrade);
            Transaction txn = Transaction.open("Cleanup");
            txn.start();
            try {
                File[] scripts = upgrade.getCleanupScripts();
                if (scripts != null) {
                    for (File script : scripts) {
                        runScript(script);
                    }
                }
                version.setStep(Step.Complete);
                version.setUpdated(new Date());
                _dao.update(version.getId(), version);
                txn.commit();
            } finally {
                txn.close();
            }
        }
    }

    @Override
    public void check() {
        String dbVersion = _dao.getCurrentVersion();
        String currentVersion = this.getClass().getPackage().getImplementationVersion();
        if (currentVersion == null) {
            currentVersion = this.getClass().getSuperclass().getPackage().getImplementationVersion();
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("DB version = " + dbVersion + " Code Version = " + currentVersion);
        }
        
        if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) > 0) {
            throw new CloudRuntimeException("Database version " + dbVersion + " is higher than management software version " + currentVersion);
        }
        
        if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) == 0) {
            return;
        }
        
        upgrade(dbVersion, currentVersion);
    }
}
