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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade218to22Premium extends Upgrade218to22 {
    @Override
    public File[] getPrepareScripts() {
        File[] scripts = super.getPrepareScripts();
        File[] newScripts = new File[2];
        newScripts[0] = scripts[0];

        String file = Script.findScript("", "db/schema-21to22-premium.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-premium.sql");
        }

        newScripts[1] = new File(file);

        return newScripts;
    }

    @Override
    public void performDataMigration(Connection conn) {
        super.performDataMigration(conn);
        updateUserStats(conn);
        updateUsageIpAddress(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        File[] scripts = super.getCleanupScripts();
        File[] newScripts = new File[1];
        // Change the array to 2 when you add in the scripts for premium.
        newScripts[0] = scripts[0];
        return newScripts;
    }

    private void updateUserStats(Connection conn) {
        try ( // update device_id information
            PreparedStatement pstmt = conn.prepareStatement(
                    "update cloud_usage.user_statistics uus set device_id = (select device_id from cloud.user_statistics us where uus.id = us.id)"
                    );
            ) {
            pstmt.executeUpdate();

            s_logger.debug("Upgraded cloud_usage user_statistics with deviceId");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade user stats: ", e);
        }

        try ( // update host_id information in usage_network
            PreparedStatement pstmt1 = conn.prepareStatement(
                    "update cloud_usage.usage_network un set host_id = "
                    + "(select device_id from cloud_usage.user_statistics us where us.account_id = un.account_id and us.data_center_id = un.zone_id)");
                ) {
            pstmt1.executeUpdate();

            s_logger.debug("Upgraded cloud_usage usage_network with hostId");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade network usage stats: ", e);
        }
    }

    private void updateUsageIpAddress(Connection conn) {
        try ( // update id information
            PreparedStatement pstmt =
                conn.prepareStatement("update cloud_usage.usage_ip_address uip set id = "
                    + "(select id from cloud.user_ip_address ip where uip.public_ip_address = ip.public_ip_address and ip.data_center_id = uip.zone_id)");
            ) {
            pstmt.executeUpdate();

            s_logger.debug("Upgraded cloud_usage usage_ip_address with Id");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade usage_ip_address: ", e);
        }
    }

}
