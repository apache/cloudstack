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
package com.cloud.upgrade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.cluster.ClusterManagerImpl;
import com.cloud.maint.Version;
import com.cloud.upgrade.dao.DbUpgrade;
import com.cloud.upgrade.dao.Upgrade217to218;
import com.cloud.upgrade.dao.Upgrade218to22;
import com.cloud.upgrade.dao.Upgrade218to224DomainVlans;
import com.cloud.upgrade.dao.Upgrade221to222;
import com.cloud.upgrade.dao.Upgrade222to224;
import com.cloud.upgrade.dao.UpgradeSnapshot217to223;
import com.cloud.upgrade.dao.VersionDao;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.upgrade.dao.VersionVO;
import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.SystemIntegrityChecker;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class DatabaseUpgradeChecker implements SystemIntegrityChecker {
    private final Logger s_logger = Logger.getLogger(DatabaseUpgradeChecker.class);
    
    protected HashMap<String, DbUpgrade[]> _upgradeMap = new HashMap<String, DbUpgrade[]>();

    VersionDao _dao;
    public DatabaseUpgradeChecker() {
        _dao = ComponentLocator.inject(VersionDaoImpl.class);
        _upgradeMap.put("2.1.7", new DbUpgrade[] { new Upgrade217to218(), new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to223(), new Upgrade222to224()});
        _upgradeMap.put("2.1.8", new DbUpgrade[] { new Upgrade218to22(), new Upgrade221to222(), new UpgradeSnapshot217to223(), new Upgrade222to224(), new Upgrade218to224DomainVlans()});
        _upgradeMap.put("2.2.2", new DbUpgrade[] { new Upgrade222to224() });
        _upgradeMap.put("2.2.3", new DbUpgrade[] { new Upgrade222to224() });
    }
    
    protected void runScript(File file) {
        try {
            FileReader reader = new FileReader(file);
            Connection conn = Transaction.getStandaloneConnection();
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            runner.runScript(reader);
        } catch (FileNotFoundException e) {
            throw new CloudRuntimeException("Unable to find upgrade script: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read upgrade script: " + file.getAbsolutePath(), e);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute upgrade script: " + file.getAbsolutePath(), e);
        }
    }
    
    protected void upgrade(String dbVersion, String currentVersion) {
        s_logger.info("Database upgrade must be performed from " + dbVersion + " to " + currentVersion);
        
        String trimmedDbVersion = Version.trimToPatch(dbVersion);
        String trimmedCurrentVersion = Version.trimToPatch(currentVersion);
        
        DbUpgrade[] upgrades = _upgradeMap.get(trimmedDbVersion);
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
        
        if (!supportsRollingUpgrade && ClusterManagerImpl.arePeersRunning(null)) {
            throw new CloudRuntimeException("Unable to run upgrade because the upgrade sequence does not support rolling update and there are other management server nodes running");
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
                boolean upgradeVersion = true;
                
                if (upgrade.getUpgradedVersion().equals("2.1.8")) {
                    //we don't have VersionDao in 2.1.x
                    upgradeVersion = false;
                } else if (upgrade.getUpgradedVersion().equals("2.2.4")) {
                    try {
                        //specifically for domain vlan update from 2.1.8 to 2.2.4
                        PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM version WHERE version='2.2.4'");
                        ResultSet rs = pstmt.executeQuery();
                        if (rs.next()){
                            upgradeVersion = false;
                        } 
                    } catch (SQLException e) {
                        throw new CloudRuntimeException("Unable to update the version table", e);
                    } 
                }
                

                if (upgradeVersion) {
                    VersionVO version = new VersionVO(upgrade.getUpgradedVersion());
                    _dao.persist(version);  
                }
               
                txn.commit();
            } finally {
                txn.close();
            }
        }
        
        if (!ClusterManagerImpl.arePeersRunning(trimmedCurrentVersion)) {
            s_logger.info("Cleaning upgrades because all management server are now at the same version");
            for (DbUpgrade upgrade : upgrades) {
                s_logger.info("Cleanup upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
                VersionVO version = _dao.findByVersion(upgrade.getUpgradedVersion(), Step.Upgrade);
                
                if (version != null) {
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
        }
    }

    @Override
    public void check() {
        GlobalLock lock = GlobalLock.getInternLock("DatabaseUpgrade");
        try {
            s_logger.info("Grabbing lock to check for database integrity.");
            if (!lock.lock(20*60)) {
                throw new CloudRuntimeException("Unable to acquire lock to check for database integrity.");
            } 
                
            try {
                String dbVersion = _dao.getCurrentVersion();
                String currentVersion = this.getClass().getPackage().getImplementationVersion();
                if (currentVersion == null) {
                    currentVersion = this.getClass().getSuperclass().getPackage().getImplementationVersion();
                }
                
                s_logger.info("DB version = " + dbVersion + " Code Version = " + currentVersion);
                
                if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) > 0) {
                    throw new CloudRuntimeException("Database version " + dbVersion + " is higher than management software version " + currentVersion);
                }
                
                if (Version.compare(Version.trimToPatch(dbVersion), Version.trimToPatch(currentVersion)) == 0) {
                    s_logger.info("DB version and code version matches so no upgrade needed.");
                    return;
                }
                
                upgrade(dbVersion, currentVersion);
            } finally {
                lock.unlock();
            }
        } finally {
            lock.releaseRef();
        }
    }
}
