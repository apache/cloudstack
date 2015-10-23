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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade306to307 extends Upgrade30xBase {
    final static Logger s_logger = Logger.getLogger(Upgrade306to307.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.6", "3.0.7"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.7";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-306to307.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-306to307.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateConcurrentConnectionsInNetworkOfferings(conn);
    }

    @Override
    public File[] getCleanupScripts() {

        return null;
    }

    protected void updateConcurrentConnectionsInNetworkOfferings(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        try {
            pstmt = conn.prepareStatement("select network_id, value from `cloud`.`network_details` where name='maxconnections'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long networkId = rs.getLong(1);
                int maxconnections = Integer.parseInt(rs.getString(2));
                pstmt = conn.prepareStatement("select network_offering_id from `cloud`.`networks` where id= ?");
                pstmt.setLong(1, networkId);
                rs1 = pstmt.executeQuery();
                if (rs1.next()) {
                    long network_offering_id = rs1.getLong(1);
                    pstmt = conn.prepareStatement("select concurrent_connections from `cloud`.`network_offerings` where id= ?");
                    pstmt.setLong(1, network_offering_id);
                    rs2 = pstmt.executeQuery();
                    if ((!rs2.next()) || (rs2.getInt(1) < maxconnections)) {
                        pstmt = conn.prepareStatement("update network_offerings set concurrent_connections=? where id=?");
                        pstmt.setInt(1, maxconnections);
                        pstmt.setLong(2, network_offering_id);
                        pstmt.executeUpdate();
                    }
                }
            }
            pstmt = conn.prepareStatement("drop table `cloud`.`network_details`");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            s_logger.info("[ignored] error during network offering update:" + e.getLocalizedMessage(), e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(rs1);
            closeAutoCloseable(pstmt);
        }
    }

}
