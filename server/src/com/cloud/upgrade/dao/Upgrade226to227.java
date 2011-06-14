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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade226to227 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade226to227.class);
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected DiskOfferingDao _diskOfferingDao;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.5"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.6";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-226to227.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-226to227.sql");
        }
        
        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        List<DataCenterVO> dcs = _dcDao.listAll();
        for ( DataCenterVO dc : dcs ) {
            HostVO host = _hostDao.findSecondaryStorageHost(dc.getId());
            _snapshotDao.updateSnapshotSecHost(dc.getId(), host.getId());           
        }
        List<DiskOfferingVO> offerings = _diskOfferingDao.listAll();
        for ( DiskOfferingVO offering : offerings ) {
            if( offering.getDiskSize() <= 2 * 1024 * 1024) { // the unit is MB
                offering.setDiskSize(offering.getDiskSize() * 1024 * 1024);
                _diskOfferingDao.update(offering.getId(), offering);
            }
        }
        
        updateDomainLevelNetworks(conn);
        dropKeysIfExist(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
    
    private void updateDomainLevelNetworks(Connection conn) {
        s_logger.debug("Updating domain level specific networks...");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT n.id FROM networks n, network_offerings o WHERE n.shared=1 AND o.system_only=0 AND o.id=n.network_offering_id");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Object[]> networks = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] network = new Object[10];
                network[0] = rs.getLong(1); // networkId
                networks.add(network);
            }
            rs.close();
            pstmt.close();
            
            for (Object[] network : networks) {
                Long networkId = (Long) network[0];
                pstmt = conn.prepareStatement("SELECT * from domain_network_ref where network_id=?");
                pstmt.setLong(0, networkId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    s_logger.debug("Setting network id=" + networkId + " as domain specific shared network");
                    pstmt = conn.prepareStatement("UPDATE networks set is_domain_specific=1 where id=?");
                    pstmt.setLong(0, networkId);
                    pstmt.executeUpdate();
                }
                rs.close();
                pstmt.close();
            }
            
            s_logger.debug("Successfully updated domain level specific networks");
        } catch (SQLException e) {
            s_logger.error("Failed to set domain specific shared networks due to ", e);
            throw new CloudRuntimeException("Failed to set domain specific shared networks due to ", e);
        }
    }
    
    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> indexes = new HashMap<String, List<String>>();

        // domain router table
        List<String> keys = new ArrayList<String>();
        keys.add("unique_name");
        indexes.put("network_offerings", keys);

        s_logger.debug("Dropping keys that don't exist in 2.2.7 version of the DB...");

        // drop indexes now
        for (String tableName : indexes.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, indexes.get(tableName), false);
        }
    }
    
}
