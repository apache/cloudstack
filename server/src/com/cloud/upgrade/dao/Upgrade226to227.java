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

import org.apache.log4j.Logger;

public class Upgrade226to227 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade226to227.class);

    @Override
    public File[] getPrepareScripts() {
       return null;
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
        return new String[] { "2.2.6", "2.2.6" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.7";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
