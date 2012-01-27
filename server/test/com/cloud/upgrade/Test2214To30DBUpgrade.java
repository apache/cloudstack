/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DbTestUtils;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class Test2214To30DBUpgrade extends TestCase {
    private static final Logger s_logger = Logger.getLogger(Test2214To30DBUpgrade.class);

    @Override
    @Before
    public void setUp() throws Exception {
        DbTestUtils.executeScript("PreviousDatabaseSchema/clean-db.sql", false, true);
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
    public void test2213to30Upgrade() throws SQLException{
        s_logger.debug("Finding sample data from 2.2.14");
        DbTestUtils.executeScript("PreviousDatabaseSchema/2.2.14/advance_zone_2.2.14.sql", false, true);
        
        DatabaseUpgradeChecker checker = ComponentLocator.inject(DatabaseUpgradeChecker.class);
        
        checker.upgrade("2.2.14", "3.0.0");
        
        Connection conn = Transaction.getStandaloneConnection();
        
        try {
        	checkPhysicalNetworks(conn);
            
            checkNetworkOfferings(conn);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
        
    }

	protected void checkPhysicalNetworks(Connection conn) throws SQLException {
        PreparedStatement pstmt;

        pstmt = conn.prepareStatement("SELECT version FROM version ORDER BY id DESC LIMIT 1");
        ResultSet rs = pstmt.executeQuery();
        assert rs.next() : "No version selected";
        assert rs.getString(1).equals("3.0.0") : "VERSION stored is not 3.0.0: " + rs.getString(1);
        rs.close();
        pstmt.close();
        
        pstmt = conn.prepareStatement("SELECT COUNT(*) FROM physical_network");
        rs = pstmt.executeQuery();
        assert rs.next() : "No physical networks setup.";
        rs.close();
        pstmt.close();
            
      
	}
    
    protected void checkNetworkOfferings(Connection conn) throws SQLException {
    	//1) verify that all fields are present
    	List<String> fields = new ArrayList<String>();
    	fields.add("id");
    	fields.add("name");
    	fields.add("unique_name");
    	fields.add("display_text");
    	fields.add("nw_rate");
    	fields.add("mc_rate");
    	fields.add("traffic_type");
    	fields.add("specify_vlan");
    	fields.add("system_only");
    	fields.add("service_offering_id");
    	fields.add("tags");
    	fields.add("default");
    	fields.add("availability");
    	fields.add("state");
    	fields.add("removed");
    	fields.add("created");
    	fields.add("guest_type");
    	fields.add("dedicated_lb_service");
    	fields.add("shared_source_nat_service");
    	fields.add("specify_ip_ranges");
    	fields.add("sort_key");
    	fields.add("uuid");
    	fields.add("redundant_router_service");
    	fields.add("conserve_mode");
    	fields.add("elastic_ip_service");
    	fields.add("elastic_lb_service");
        
        PreparedStatement pstmt;        
        for (String field : fields) {
             pstmt = conn.prepareStatement("SHOW COLUMNS FROM network_offerings LIKE ?");
             pstmt.setString(1, field);
             ResultSet rs = pstmt.executeQuery();
             if (!rs.next()) {
            	throw new CloudRuntimeException("Field " + field + " is missing in upgraded network_offerings table"); 
             }
             rs.close();
             pstmt.close();
             
        }
        
    	//2) compare default network offerings
    }
    
}
