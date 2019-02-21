///
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
///

package com.cloud.upgrade.flyway;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.jdbc.BaseJdbcMigration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class V4_7_0_1__update extends BaseJavaMigration {
    final static Logger s_logger = Logger.getLogger(V4_7_0_1__update.class);

    public void alterAddColumnToCloudUsage(final Connection conn) {
        final String alterTableSql = "ALTER TABLE `cloud_usage`.`cloud_usage` ADD COLUMN `quota_calculated` tinyint(1) DEFAULT 0 NOT NULL COMMENT 'quota calculation status'";
        try (PreparedStatement pstmt = conn.prepareStatement(alterTableSql)) {
            pstmt.executeUpdate();
            s_logger.info("Altered cloud_usage.cloud_usage table and added column quota_calculated");
        } catch (SQLException e) {
            if (e.getMessage().contains("quota_calculated")) {
                s_logger.warn("cloud_usage.cloud_usage table already has a column called quota_calculated");
            } else {
                throw new CloudRuntimeException("Unable to create column quota_calculated in table cloud_usage.cloud_usage", e);
            }
        }
    }

    @Override
    public void migrate(Context context) {
        Connection conn = context.getConnection();
        alterAddColumnToCloudUsage(conn);
    }

}
