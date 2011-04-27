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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class UpgradeSnapshot217to224 implements DbUpgrade {

    @Override
    public File[] getPrepareScripts() {
        String file = Script.findScript("", "db/schema-snapshot-217to224.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-snapshot-217to224.sql");
        }
        
        return new File[] { new File(file)};
    }

    @Override
    public void performDataMigration(Connection conn) {        
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.7", "2.1.7" };
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
