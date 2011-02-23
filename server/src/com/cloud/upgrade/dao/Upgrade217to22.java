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

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade217to22 implements DbUpgrade {

    @Override
    public void prepare() {
        File file = PropertiesUtil.findConfigFile("schema-21to22.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22.sql");
        }
        
        try {
            FileReader reader = new FileReader(file);
            Connection conn = Transaction.getStandaloneConnection();
            ScriptRunner runner = new ScriptRunner(conn, false, false);
            runner.runScript(reader);
        } catch (FileNotFoundException e) {
            throw new CloudRuntimeException("Unable to find upgrade script, schema-21to22.sql", e);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to read upgrade script, schema-21to22.sql", e);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute upgrade script, schema-21to22.sql", e);
        }
    }
    
    @Override
    public void upgrade() {
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.7", "2.1.7" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.0";
    }
    
    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
