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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.maint.Version;
import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.Pair;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=VersionDao.class) @DB(txn=false)
public class VersionDaoImpl extends GenericDaoBase<VersionVO, Long> implements VersionDao {
    private static final Logger s_logger = Logger.getLogger(VersionDaoImpl.class);
    
    protected HashMap<Pair<String, String>, DbUpgrade[]> _upgradeMap = new HashMap<Pair<String, String>, DbUpgrade[]>();
    
    String _dumpPath = null;
    
    final GenericSearchBuilder<VersionVO, String> CurrentVersionSearch;
    final SearchBuilder<VersionVO> AllFieldsSearch;
    
    protected VersionDaoImpl() {
        super();
        
        _upgradeMap.put(new Pair<String, String>("2.1.7", "2.2.3"), new DbUpgrade[] { new Upgrade217to22(), new Upgrade221to222(), new UpgradeSnapshot217to223()});
        
        CurrentVersionSearch = createSearchBuilder(String.class);
        CurrentVersionSearch.select(null, Func.FIRST, CurrentVersionSearch.entity().getVersion());
        CurrentVersionSearch.and("step", CurrentVersionSearch.entity().getStep(), Op.EQ);
        CurrentVersionSearch.done();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("version", AllFieldsSearch.entity().getVersion(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.and("updated", AllFieldsSearch.entity().getUpdated(), Op.EQ);
        AllFieldsSearch.done();
        
    }
    
    protected VersionVO findByVersion(String version, Step step) {
        SearchCriteria<VersionVO> sc = AllFieldsSearch.create();
        sc.setParameters("version", version);
        sc.setParameters("step", step);
        
        return findOneBy(sc);
    }
    
    @DB
    protected String getCurrentVersion() {
        Transaction txn = Transaction.currentTxn();
        Connection conn = null;
        try {
            s_logger.debug("Checking to see if the database is at a version before it was the version table is created");
            
            conn = txn.getConnection();
    
            PreparedStatement pstmt = conn.prepareStatement("SHOW TABLES LIKE 'VERSION'");
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                pstmt.close();
                rs.close();
                pstmt = conn.prepareStatement("SHOW TABLES LIKE 'NICS'");
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    pstmt.close();
                    rs.close();
                    s_logger.debug("No version table and no nics table, returning 2.1.7");
                    return "2.1.7";
                } else {
                    pstmt.close();
                    rs.close();
                    s_logger.debug("No version table but has nics table, returning 2.1.2");
                    return "2.2.1";
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to get the current version", e);
        } 
        
        SearchCriteria<String> sc = CurrentVersionSearch.create();
        
        sc.setParameters("step", Step.Complete);
        Filter filter = new Filter(VersionVO.class, "updated", true, 0l, 1l);
        
        List<String> vers = customSearch(sc, filter);
        return vers.get(0);
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
    
    @DB
    protected void upgrade(String dbVersion, String currentVersion) throws ConfigurationException {
        s_logger.info("Database upgrade must be performed from " + dbVersion + " to " + currentVersion);
        
        DbUpgrade[] upgrades = _upgradeMap.get(new Pair<String, String>(dbVersion, currentVersion));
        if (upgrades == null) {
            throw new ConfigurationException("There is no upgrade path from " + dbVersion + " to " + currentVersion);
        }

        if (Version.compare(currentVersion, upgrades[upgrades.length - 1].getUpgradedVersion()) != 0) {
            throw new ConfigurationException("The end upgrade version is actually at " + upgrades[upgrades.length - 1].getUpgradedVersion() + " but our management server code version is at " + currentVersion);
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
            Transaction txn = Transaction.currentTxn();
            txn.start();
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
            persist(version);
            txn.commit();
        }
        
        for (DbUpgrade upgrade : upgrades) {
            s_logger.info("Cleanup upgrade " + upgrade.getClass().getSimpleName() + " to upgrade from " + upgrade.getUpgradableVersionRange()[0] + "-" + upgrade.getUpgradableVersionRange()[1] + " to " + upgrade.getUpgradedVersion());
            VersionVO version = findByVersion(upgrade.getUpgradedVersion(), Step.Upgrade);
            Transaction txn = Transaction.currentTxn();
            txn.start();
            File[] scripts = upgrade.getCleanupScripts();
            if (scripts != null) {
                for (File script : scripts) {
                    runScript(script);
                }
            }
            version.setStep(Step.Complete);
            version.setUpdated(new Date());
            update(version.getId(), version);
            txn.commit();
        }
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        _dumpPath = (String)params.get("upgrade.dump.path");
        if (_dumpPath == null) {
            _dumpPath = System.getenv("upgrade.dump.path");
            if (_dumpPath == null) {
                _dumpPath = "/var/log/";
            }
        }
        
        String dbVersion = getCurrentVersion();
        String currentVersion = this.getClass().getPackage().getImplementationVersion();
        if (currentVersion == null) {
            currentVersion = this.getClass().getSuperclass().getPackage().getImplementationVersion();
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("DB version = " + dbVersion + " Code Version = " + currentVersion);
        }
        
        if (Version.compare(dbVersion, currentVersion) > 0) {
            throw new ConfigurationException("Database version " + dbVersion + " is higher than management software version " + currentVersion);
        }
        
        if (Version.compare(dbVersion, currentVersion) == 0) {
            return true;
        }
        
        upgrade(dbVersion, currentVersion);
        
        return true;
    }
}
