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
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade229to2210 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade229to2210.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.9", "2.2.9"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.10";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-229to2210.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-229to2210.sql");
        }
        
        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`configuration` (category, instance, name, value, description) VALUES ('Network', 'DEFAULT', 'firewall.rule.ui.enabled', 'true', 'enable/disable UI that separates firewall rules from NAT/LB rules')");
            pstmt.execute();
            
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to perform data migration", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
    
}
