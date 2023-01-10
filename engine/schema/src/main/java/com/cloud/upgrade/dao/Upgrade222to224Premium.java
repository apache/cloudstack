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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade222to224Premium extends Upgrade222to224 {

    @Override
    public InputStream[] getPrepareScripts() {
        InputStream[] newScripts = new InputStream[2];
        newScripts[0] = super.getPrepareScripts()[0];
        final String scriptFile = "META-INF/db/schema-222to224-premium.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }
        newScripts[1] = script;

        return newScripts;
    }

    @Override
    public void performDataMigration(Connection conn) {
        super.performDataMigration(conn);
        updateUserStats(conn);
    }

    private void updateUserStats(Connection conn) {
        try (   // update network_id information
                PreparedStatement pstmt = conn.prepareStatement(
                    "update cloud_usage.user_statistics uus, cloud.user_statistics us set uus.network_id = us.network_id where uus.id = us.id"
                    );
            ) {

            pstmt.executeUpdate();
            logger.debug("Upgraded cloud_usage user_statistics with networkId");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade user stats: ", e);
        }

        try (   // update network_id information in usage_network
                PreparedStatement pstmt1 =
                    conn.prepareStatement("update cloud_usage.usage_network un, cloud_usage.user_statistics us set un.network_id = "
                    + "us.network_id where us.account_id = un.account_id and us.data_center_id = un.zone_id and us.device_id = un.host_id");
            ) {
            pstmt1.executeUpdate();
            logger.debug("Upgraded cloud_usage usage_network with networkId");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade user stats: ", e);
        }
    }
}
