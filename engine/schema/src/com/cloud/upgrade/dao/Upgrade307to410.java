// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.upgrade.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade307to410 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade307to410.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.7", "4.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-307to410.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-307to410.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateRegionEntries(conn);
    }

    private void updateRegionEntries(Connection conn) {
        final Properties dbProps = DbProperties.getDbProperties();
        int region_id = 1;
        String regionId = dbProps.getProperty("region.id");
        if (regionId != null) {
            region_id = Integer.parseInt(regionId);
        }
        try (PreparedStatement pstmt = conn.prepareStatement("update `cloud`.`region` set id = ?");){
            //Update regionId in region table
            s_logger.debug("Updating region table with Id: " + region_id);
            pstmt.setInt(1, region_id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while updating region entries", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-307to410-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-307to410-cleanup.sql");
        }

        return new File[] {new File(script)};
    }
}
