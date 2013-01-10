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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

/**
 * @author htrippaers
 *
 */
public class Upgrade40to41 implements DbUpgrade {

	/**
	 *
	 */
	public Upgrade40to41() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getUpgradableVersionRange()
	 */
	@Override
	public String[] getUpgradableVersionRange() {
		return new String[] { "4.0.0", "4.1.0" };
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getUpgradedVersion()
	 */
	@Override
	public String getUpgradedVersion() {
		return "4.1.0";
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#supportsRollingUpgrade()
	 */
	@Override
	public boolean supportsRollingUpgrade() {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getPrepareScripts()
	 */
	@Override
	public File[] getPrepareScripts() {
		String script = Script.findScript("", "db/schema-40to410.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-40to410.sql");
        }

        return new File[] { new File(script) };
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#performDataMigration(java.sql.Connection)
	 */
	@Override
	public void performDataMigration(Connection conn) {
        upgradeEIPNetworkOfferings(conn);
	}

	/* (non-Javadoc)
	 * @see com.cloud.upgrade.dao.DbUpgrade#getCleanupScripts()
	 */
	@Override
	public File[] getCleanupScripts() {
		return new File[0];
	}

    private void upgradeEIPNetworkOfferings(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("select id, elastic_ip_service from `cloud`.`network_offerings` where traffic_type='Guest'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                // check if elastic IP service is enabled for network offering
                if (rs.getLong(2) != 0) {
                    //update network offering with eip_associate_public_ip set to true
                    pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings` set eip_associate_public_ip=? where id=?");
                    pstmt.setBoolean(1, true);
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set elastic_ip_service for network offerings with EIP service enabled.", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
