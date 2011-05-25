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
import java.util.List;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade225to226 implements DbUpgrade {
    @Inject
    protected SnapshotDao _snapshotDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected DataCenterDao _dcDao;

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
        String script = Script.findScript("", "db/schema-225to226.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-224to225.sql");
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
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
    
}
