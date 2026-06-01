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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Upgrade42100to42200 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.21.0.0", "4.22.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.22.0.0";
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42100to42200.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSnapshotPolicyOwnership(conn);
        updateBackupScheduleOwnership(conn);
    }

    protected void updateSnapshotPolicyOwnership(Connection conn) {
        // set account_id and domain_id in snapshot_policy table from volume table
        String selectSql = "SELECT sp.id, v.account_id, v.domain_id FROM snapshot_policy sp, volumes v WHERE sp.volume_id = v.id AND (sp.account_id IS NULL AND sp.domain_id IS NULL)";
        String updateSql = "UPDATE snapshot_policy SET account_id = ?, domain_id = ? WHERE id = ?";

        try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectPstmt.executeQuery();
             PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                long policyId = rs.getLong(1);
                long accountId = rs.getLong(2);
                long domainId = rs.getLong(3);

                updatePstmt.setLong(1, accountId);
                updatePstmt.setLong(2, domainId);
                updatePstmt.setLong(3, policyId);
                updatePstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update snapshot_policy table with account_id and domain_id", e);
        }
    }

    protected void updateBackupScheduleOwnership(Connection conn) {
        // Set account_id and domain_id in backup_schedule table from vm_instance table
        String selectSql = "SELECT bs.id, vm.account_id, vm.domain_id FROM backup_schedule bs, vm_instance vm WHERE bs.vm_id = vm.id AND (bs.account_id IS NULL AND bs.domain_id IS NULL)";
        String updateSql = "UPDATE backup_schedule SET account_id = ?, domain_id = ? WHERE id = ?";

        try (PreparedStatement selectPstmt = conn.prepareStatement(selectSql);
             ResultSet rs = selectPstmt.executeQuery();
             PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {

            while (rs.next()) {
                long scheduleId = rs.getLong(1);
                long accountId = rs.getLong(2);
                long domainId = rs.getLong(3);

                updatePstmt.setLong(1, accountId);
                updatePstmt.setLong(2, domainId);
                updatePstmt.setLong(3, scheduleId);
                updatePstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update backup_schedule table with account_id and domain_id", e);
        }
    }
}
