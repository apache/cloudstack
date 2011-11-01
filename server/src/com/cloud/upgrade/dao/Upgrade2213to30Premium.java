/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2213to30Premium extends Upgrade2213to30 {
    final static Logger s_logger = Logger.getLogger(Upgrade2213to30Premium.class);

    @Override
    public File[] getPrepareScripts() {
    	 File[] scripts = super.getPrepareScripts();
         File[] newScripts = new File[2]; 
         newScripts[0] = scripts[0];

         String script = Script.findScript("", "db/db/schema-2213to30-premium.sql");
         if (script == null) {
             throw new CloudRuntimeException("Unable to find db/schema-2213to30-premium.sql");
         }
         
         newScripts[1] = new File(script);
         
         return newScripts;
    }

    @Override
    public void performDataMigration(Connection conn) {
    	super.performDataMigration(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return super.getCleanupScripts();
    }
 
}
