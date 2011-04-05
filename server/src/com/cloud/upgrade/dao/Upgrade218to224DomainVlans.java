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
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;


public class Upgrade218to224DomainVlans implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade218to224DomainVlans.class);

    @Override
    public File[] getPrepareScripts() {
        return null;
    }
        
    @Override
    public void performDataMigration(Connection conn) {
        HashMap<Long, Long> networkDomainMap = new HashMap<Long, Long>();
        //populate domain_network_ref table
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM networks WHERE shared=1 AND traffic_type='Guest' AND guest_type='Direct'");
            ResultSet rs = pstmt.executeQuery();
            s_logger.debug("query is " + pstmt);
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                Long vlanId = null;
                Long domainId = null;
                
                pstmt = conn.prepareStatement("SELECT id FROM vlan WHERE network_id=? LIMIT 0,1");
                pstmt.setLong(1, networkId);
                s_logger.debug("query is " + pstmt);
                rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    vlanId = rs.getLong(1);
                }
                
                if (vlanId != null) {
                    pstmt = conn.prepareStatement("SELECT domain_id FROM account_vlan_map WHERE domain_id IS NOT NULL AND vlan_db_id=? LIMIT 0,1");
                    pstmt.setLong(1, vlanId);
                    s_logger.debug("query is " + pstmt);
                    rs = pstmt.executeQuery();
                    
                    while (rs.next()) {
                        domainId = rs.getLong(1);
                    }
                    
                    if (domainId != null) {
                        if (!networkDomainMap.containsKey(networkId)) {
                            networkDomainMap.put(networkId, domainId);
                        }
                    }
                }
            }
            
            //populate domain level networks
            for (Long networkId : networkDomainMap.keySet()) {
                pstmt = conn.prepareStatement("INSERT INTO domain_network_ref (network_id, domain_id) VALUES (?,    ?)");
                pstmt.setLong(1, networkId);
                pstmt.setLong(2, networkDomainMap.get(networkId));
                pstmt.executeUpdate();
            }
            
            rs.close();
            pstmt.close();
        }  catch (SQLException e) {
            throw new CloudRuntimeException("Unable to convert 2.1.x domain level vlans to 2.2.x domain level networks", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.8", "2.1.8" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.4";
    }
    
    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
